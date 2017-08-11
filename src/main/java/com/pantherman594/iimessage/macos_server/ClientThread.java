/*
 * Copyright (c) 2017 David Shen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pantherman594.iimessage.macos_server;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;


public class ClientThread implements Runnable {
    private boolean active = true;
    private Socket threadSocket;
    private Thread thread;
    private PrintWriter output;
    private boolean isConnected = false;

    private SecretKeySpec secretKey;
    private Cipher cipher;
    private byte[] iv;

    ClientThread(Socket socket, String password) {
        System.out.println("New client thread");
        this.threadSocket = socket;
        try {
            cipher = Cipher.getInstance(Constants.AES_PADDING);
            this.secretKey = genSecretKey(password);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            // Create the streams
            output = new PrintWriter(threadSocket.getOutputStream(), true);
            BufferedReader input = new BufferedReader(new InputStreamReader(threadSocket.getInputStream()));

            boolean correct = false;
            String toTest = input.readLine(); // Step 1 of connecting: receive and verify test data to server (encrypted)
            System.out.println(toTest);
            JSONObject toTestJson = new JSONObject(toTest);

            if (toTestJson.has(Constants.IV)) {
                iv = new Base64().decode(toTestJson.getString(Constants.IV));

                if (toTestJson.has(Constants.ENCRYPTED)) {
                    toTest = decrypt(toTestJson.getString(Constants.ENCRYPTED));
                    toTestJson = new JSONObject(toTest);
                    correct = toTestJson.getString(Constants.ACTION).equals(Constants.Action.EST) && toTestJson.getString(Constants.Col.MSG).equals(Constants.TEST_DATA);
                }
            }

            JSONObject verifyJson = new JSONObject();
            verifyJson.put(Constants.ACTION, Constants.Action.EST);
            verifyJson.put(Constants.SUCCESS, correct);
            System.out.println("SUCCESS: " + correct);
            notifyIfAlive(verifyJson.toString()); // Step 2 of connecting: respond to client whether test data decryption was successful (unencrypted)

            if (correct) {
                isConnected = true;
            } else {
                isConnected = false;
                run();
                return;
            }

            int nullCount = 0;
            // Indefinite Loop that runs server functions
            while (isConnected) {
                // this will wait until a line of text has  been sent
                String clientInput = input.readLine();

                if (clientInput == null) {
                    // potentially disconnected
                    nullCount++;

                    if (nullCount > 50) {
                        // 50 nulls to me is disconnection
                        break;
                    }
                } else {
                    JSONObject inputJson = new JSONObject(clientInput);
                    if (inputJson.has(Constants.ENCRYPTED)) {
                        inputJson = new JSONObject(decrypt(inputJson.getString(Constants.ENCRYPTED)));
                    }
                    // Connected and received an input
                    System.out.println(inputJson.toString());

                    processInput(inputJson);
                }
            }

            System.out.println("Lost connection of Mobile Client");
        } catch (IOException e) {
            e.printStackTrace();

            System.out.println("Lost connection of Mobile Client due to socket error");
        } catch (JSONException e) {
            System.out.print("Failed to create json. ");
            e.printStackTrace();

            System.out.println("Lost connection of Mobile Client due to JSON error");
        } catch (Exception e) {
            e.printStackTrace();
        }
        killMobileThread();
    }

    private static SecretKeySpec genSecretKey(String password) throws UnsupportedEncodingException {
        if (password.length() < Constants.SECRET_PAD_LEN) {
            int missingLength = Constants.SECRET_PAD_LEN - password.length();
            StringBuilder passwordBuilder = new StringBuilder(password);
            for (int i = 0; i < missingLength; i++) {
                passwordBuilder.append(" ");
            }
            password = passwordBuilder.toString();
        }
        byte[] key = password.substring(0, Constants.SECRET_PAD_LEN).getBytes(Constants.CHARSET);
        return new SecretKeySpec(key, Constants.AES);
    }

    private void sendRaw(JSONObject message) {
        while (!isConnected) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        String messageString = message.toString();
        message = new JSONObject();
        try {
            message.put(Constants.ENCRYPTED, encrypt(messageString));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        notifyIfAlive(message.toString());
    }

    private String encrypt(String data) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            System.out.println(data);
            return new Base64().encodeToString(cipher.doFinal(data.getBytes(Constants.CHARSET)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String decrypt(String data) {
        try {
            System.out.println("algo:" + cipher.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            System.out.println(new String(cipher.doFinal(new Base64().decode(data)), Constants.CHARSET));
            return new String(cipher.doFinal(new Base64().decode(data)), Constants.CHARSET);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void processInput(JSONObject input) throws Exception {
        String action = input.getString(Constants.ACTION);
        switch(action) {
            case Constants.Action.REQ:
                long date = input.getLong(Constants.Col.DATE);
                sendRaw(MacOSServer.getDatabaseManager().getMessages(date));
                break;
            case Constants.Action.SEND:
                String chatId = input.getString(Constants.Col.CHAT_ID);
                String msg = input.getString(Constants.Col.MSG);

                String[] cmdString = {
                        "osascript",
                        MacOSServer.getMessageScript().getAbsolutePath(),
                        msg,
                        chatId
                };

                // Send iMessage via applescript
                try {
                    Runtime.getRuntime().exec(cmdString);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Notify client that message was received at the server
                JSONObject responseJson = new JSONObject();
                responseJson.put(Constants.SUCCESS, true);
                sendRaw(responseJson);
                break;
            case Constants.Action.READ:
                long messageId = input.getLong(Constants.Col.ID);
                MacOSServer.getDatabaseManager().markAsRead(messageId);

                // Notify client that message was received at the server
                responseJson = new JSONObject();
                responseJson.put(Constants.SUCCESS, true);
                sendRaw(responseJson);
                break;
        }
    }

    private void notifyIfAlive(String message) {
        if (!active) {
            System.out.println("Attempted to send message from inactive thread. Attempting to kill again...");
            killMobileThread();
            return;
        }
        if (thread.isAlive()) {
            System.out.println("Mobile thread is alive, sending msg to client");
            output.println(message);
            System.out.println(message);
        } else {
            System.out.println("Mobile thread is dead. Removing.");
            killMobileThread();
        }
    }

    void setThread(Thread thread) {
        this.thread = thread;
    }

    private void killMobileThread() {
        active = false;
        try {
            threadSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

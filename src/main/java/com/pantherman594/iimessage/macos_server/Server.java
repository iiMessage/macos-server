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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class Server {
    private static Server instance;

    Server(String ip, int port, String password) {
        if (instance == null) {
            instance = this;

            try {
                ServerSocket sSocket = new ServerSocket(port);  // using port 5000 by default for our server socket
                System.out.println("Server started at: " + new Date());

                Socket s = null;
                try {
                    s = new Socket(ip, port);
                } catch (Exception e) {
                    System.out.println("Port is not accessible publicly. Either forward port " + port + " or connect on local network for testing. For local testing, connect to " + InetAddress.getLocalHost().getHostAddress() + (port != 5000 ? ":" + port : "") + " instead.");
                } finally {
                    if (s != null) {
                        try {
                            s.close();
                        } catch (Exception ignored) {}
                    }
                }

                // loop that runs server functions
                while (instance != null) {
                    // Wait for a client to connect
                    Socket socket = sSocket.accept();

                    // Create a new custom thread to handle connection
                    ClientThread cT = new ClientThread(socket, password);

                    // Create thread and pass ID
                    Thread thread = new Thread(cT);
                    cT.setThread(thread);

                    // Start the thread!
                    thread.start();

                }

            } catch (IOException exception) {
                System.out.println("Error: " + exception);
            }

        } else {
            System.out.println("ERROR: You tried to create more than 1 Server");
        }
    }

    static Server getInstance() {
        return instance;
    }
}

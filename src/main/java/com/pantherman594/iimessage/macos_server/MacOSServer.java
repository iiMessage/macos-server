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

import java.io.*;
import java.net.URL;

public class MacOSServer {
    private static DatabaseManager databaseManager = new DatabaseManager();
    private static final String SCRIPT = "messages.applescript";

    public static void main(String[] args) throws Exception {
        int port = 5000;
        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
        }

        String password = new String(System.console().readPassword("Please enter a password: "));
        if (password.length() < 5) {
            System.out.println("Please use a more secure password.");
            System.exit(1);
            return;
        }

        URL checkIp = new URL("http://checkip.amazonaws.com");
        BufferedReader in = new BufferedReader(new InputStreamReader(checkIp.openStream()));
        String ip = in.readLine();
        System.out.println("To connect to the server, use the ip " + ip + (port != 5000 ? ":" + port : "") + " and your password.");

        if (getMessageScript().exists()) new Server(ip, port, password); // Ensure we have a messages.applescript in the current directory;
        else throw new Exception("File \"" + SCRIPT + "\" does not exist and could not be created.");
    }

    static String getHomeDirectory() {
        return System.getProperty("user.home");
    }

    static DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    static File getMessageScript() throws Exception {
        String jarFolder = new File(MacOSServer.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getPath().replace('\\', '/') + "/";
        File messagesFile = new File(jarFolder + SCRIPT);

        if (messagesFile.exists()) {
            System.out.println("File \"" + SCRIPT + "\" exists!");
        } else {
            System.out.println("File \"" + SCRIPT + "\" does not exist. Attempting to create...");
            InputStream stream = MacOSServer.class.getResourceAsStream("/" + SCRIPT);
            OutputStream resStreamOut = new FileOutputStream(jarFolder + SCRIPT);

            if (stream == null) {
                resStreamOut.close();
                throw new Exception("Cannot get resource \"" + SCRIPT + "\" from jar file.");
            }

            int readBytes;
            byte[] buffer = new byte[4096];
            while ((readBytes = stream.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
            stream.close();
            resStreamOut.close();
            if (messagesFile.exists()) {
                System.out.println("Success!");
            }
        }

        return messagesFile;
    }
}

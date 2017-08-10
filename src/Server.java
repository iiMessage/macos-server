import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class Server {
    private static Server instance;

    Server(int port, String password) {
        if (instance == null) {
            instance = this;

            try {
                ServerSocket sSocket = new ServerSocket(port);  // using port 5000 for our server socket
                System.out.println("Server started at: " + new Date());

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

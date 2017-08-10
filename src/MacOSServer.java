import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class MacOSServer {
    private static DatabaseManager databaseManager;

    private MacOSServer(int port, String password) {
        databaseManager = new DatabaseManager();
        if (doesMessageScriptExist()) new Server(port, password); // Ensure we have a ~/messages.applescript;
    }

    public static void main(String[] args) throws IOException {
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
        new MacOSServer(port, password);
    }

    static String getHomeDirectory() {
        return System.getProperty("user.home");
    }

    public static DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    private boolean doesMessageScriptExist() {
        String filePath = getHomeDirectory() + "/messages.applescript";
        File messagesFile = new File(filePath);

        if (messagesFile.exists()) {
            System.out.println("File \"/messages.applescript\" exists");
        } else {
            System.out.println("WARNING - \"/messages.applescript\" does NOT exist");
        }

        return messagesFile.exists();
    }
}

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.*;

class DatabaseManager {
    private Connection chatDb;
    private Connection contactsDb;

    DatabaseManager() {
        System.out.println("Starting another sql db connection");
        try {
            Class.forName("org.sqlite.JDBC");
            chatDb = DriverManager.getConnection("jdbc:sqlite:" + MacOSServer.getHomeDirectory() + "/Library/Messages/chat.db");
            contactsDb = DriverManager.getConnection("jdbc:sqlite:" + MacOSServer.getHomeDirectory() + "/Library/Application Support/AddressBook/AddressBook-v22.abcddb");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    JSONObject getMessages(long lastFetchTime) {
        JSONArray foundMessagesJSON = new JSONArray();

        try (Statement stmt = getChatDb().createStatement()){
            ResultSet resultSet = stmt.executeQuery("SELECT ROWID from message WHERE text NOT NULL AND date > " + lastFetchTime + " ORDER BY date DESC;");
            while (resultSet.next()) {
                long messageId = resultSet.getLong("ROWID");
                foundMessagesJSON.put(getMessageInfo(messageId));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        JSONObject response = new JSONObject();
        response.put(Constants.NUM_MESSAGES, foundMessagesJSON.length());
        if (foundMessagesJSON.length() > 0) response.put(Constants.INCOMING, foundMessagesJSON);
        return response;
    }

    private String messageInfoQuery(long messageId) {
        return "SELECT m.is_from_me, m.is_sent, m.is_read, m.date, m.text, m.handle_id, m.guid AS 'm.guid',\n" +
                "c.ROWID AS 'c.ROWID', c.guid AS 'c.guid', c.display_name AS 'c.display_name',\n" +
                "h.ROWID AS 'h.ROWID', h.id AS 'h.id'\n" +
                "FROM message m\n" +
                "JOIN chat_message_join cmj ON m.ROWID = cmj.message_id\n" +
                "JOIN chat c ON cmj.chat_id = c.ROWID\n" +
                "JOIN handle h ON handle_id = h.ROWID\n" +
                "WHERE m.ROWID=" + messageId + ";";
    }

    private JSONObject getMessageInfo(long messageId) {
        JSONObject foundMessageJSON = new JSONObject();

        try (Statement stmt = getChatDb().createStatement()) {
            ResultSet resultSet = stmt.executeQuery(messageInfoQuery(messageId));
            resultSet.next();
            if (resultSet.isAfterLast()) {
                System.out.println("Message not found: " + messageId);
                return foundMessageJSON;
            }

            String message = new String(resultSet.getBytes("text"), "UTF-8");
            int isFromMe = resultSet.getInt("is_from_me");
            int isSent = resultSet.getInt("is_sent");
            int isRead = resultSet.getInt("is_read");
            long date = resultSet.getLong("date");

            // get Chat values
            String chatId = resultSet.getString("c.guid");
            String chatName = resultSet.getString("c.display_name");

            // get Handle values
            String handleID = resultSet.getString("h.id");

            foundMessageJSON.put(Constants.Col.ID, messageId);
            foundMessageJSON.put(Constants.Col.MSG, message);
            foundMessageJSON.put(Constants.Col.DATE, date);
            foundMessageJSON.put(Constants.Col.IS_FROM_ME, isFromMe);
            foundMessageJSON.put(Constants.Col.IS_SENT, isSent);
            foundMessageJSON.put(Constants.Col.IS_READ, isRead);
            foundMessageJSON.put(Constants.Col.SENDER, getContactName(handleID));
            foundMessageJSON.put(Constants.Col.CHAT_ID, chatId);
            foundMessageJSON.put(Constants.Col.CHAT_NAME, chatName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return foundMessageJSON;
    }

    void markAsRead(long messageId) {
        try (Statement stmt = getChatDb().createStatement()) {
            stmt.executeUpdate("UPDATE message SET is_read=1 WHERE ROWID=" + messageId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String contactInfoQuery(String handleId) {
        return "SELECT ZCONTACT AS 'contact_id',\n" +
                "ZFIRSTNAME AS 'first_name', ZLASTNAME AS 'last_name'\n" +
                "FROM ZABCDCONTACTINDEX\n" +
                "JOIN ZABCDRECORD ON ZABCDRECORD.Z_PK = contact_id\n" +
                "WHERE ZABCDCONTACTINDEX.ZSTRINGFORINDEXING LIKE '%" + handleId + "%';";
    }

    private String getContactName(String handleId) {
        String name = handleId;
        if (name.startsWith("+1")) name = name.substring(2);
        try (Statement stmt = getContactsDb().createStatement()) {
            ResultSet resultSet = stmt.executeQuery(contactInfoQuery(name));
            resultSet.next();
            if (resultSet.isAfterLast()) {
                return handleId;
            }

            name = resultSet.getString("first_name") + " " + resultSet.getString("last_name");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return name;
    }

    private Connection getChatDb() {
        try {
            if (chatDb != null && !chatDb.isClosed()) {
                return chatDb;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                chatDb = DriverManager.getConnection("jdbc:sqlite:" + MacOSServer.getHomeDirectory() + "/Library/Messages/chat.db");
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
        return chatDb;
    }

    private Connection getContactsDb() {
        try {
            if (contactsDb != null && !contactsDb.isClosed()) {
                return contactsDb;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                contactsDb = DriverManager.getConnection("jdbc:sqlite:" + MacOSServer.getHomeDirectory() + "/Library/Application Support/AddressBook/AddressBook-v22.abcddb");
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
        return contactsDb;
    }
}


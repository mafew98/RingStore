package Client;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Properties;

/**
 * TEST SERVER. NOT USED
 */
public class ServerListener {
    public static void main(String[] args) throws IOException {
        int port = 24942;
        InetAddress localHost = InetAddress.getLocalHost();
        String serverIP = localHost.getHostAddress();
        Properties props = new Properties();
        props.load(new FileInputStream("./sysNodes.properties"));
        int serverId = 0;
        for (String ip : props.stringPropertyNames()) {
            if (ip.equals(serverIP)) {
                serverId = Integer.parseInt(props.getProperty(ip));
                break;
            }
        }

        System.out.println("Server " + serverId + " (" + serverIP + ") listening on port " + port);

        ServerSocket serverSocket = new ServerSocket(port);
        HashMap<String, String> dataStore = new HashMap<>();

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("🔗 Connection from: " + clientSocket.getInetAddress());

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            String rawMessage;

            while ((rawMessage = in.readLine()) != null) {
                System.out.println("Server " + serverId + " received: " + rawMessage);
                try {
                    Message msg = new Message(rawMessage);

                    if (msg.getType() == Message.MessageType.W) {
                        String key = msg.getKey();
                        String value = msg.getValue();
                        dataStore.put(key, value);
                        System.out.println(" Stored → " + key + " : " + value);
                    } else if (msg.getType() == Message.MessageType.R) {
                        String key = msg.getKey();
                        String value = dataStore.get(key);
                        if (value != null) {
                            String response = key + " = " + value;
                            System.out.println("READ → " + response);
                            out.println(response);
                        } else {
                            System.out.println(" READ → Key not found: " + key);
                            out.println("Key not found: " + key);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to parse message: " + rawMessage);
                    out.println("Server error");
                }
            }

            clientSocket.close();
        }
    }
}

package Client;

import java.io.*;
import java.net.*;
import java.util.HashMap;

public class ServerListener {
    public static void main(String[] args) throws IOException {
        int port = 24942;
        InetAddress localHost = InetAddress.getLocalHost();
        String serverIP = localHost.getHostAddress();

        int serverId = serverIP.equals("10.176.69.38") ? 1 :
                       serverIP.equals("10.176.69.39") ? 2 : 0;

        System.out.println(" Server " + serverId + " (" + serverIP + ") listening on port " + port);

        ServerSocket serverSocket = new ServerSocket(port);
        HashMap<String, String> dataStore = new HashMap<>();

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Connection from: " + clientSocket.getInetAddress());

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String rawMessage;
            while ((rawMessage = in.readLine()) != null) {
                System.out.println("Server " + serverId + " received: " + rawMessage);
                try {
                    Message msg = new Message(rawMessage);
                    if (msg.getType() == Message.MessageType.W) {
                        String key = msg.getKey();
                        String value = msg.getValue();
                        dataStore.put(key, value);
                        System.out.println("Stored → " + key + " : " + value);
                    } else if (msg.getType() == Message.MessageType.R) {
                        String key = msg.getKey();
                        String value = dataStore.get(key);
                        if (value != null) {
                            System.out.println("READ → " + key + " = " + value);
                        } else {
                            System.out.println("READ → Key not found: " + key);
                        }
                    }
                } catch (Exception e) {
                    System.err.println(" Failed to parse message: " + rawMessage);
                }
            }

            clientSocket.close();
        }
    }
}

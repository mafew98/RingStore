package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.text.SimpleDateFormat;

public class MessageReceiver extends Thread {
    private BufferedReader reader;
    private Integer nodeId;
    private int messageCount = 0;
    private static final int TOTAL_FOREIGN_MESSAGES = 100;
    
    private MessageQueue messageQueue;
    private ConnectionContext connectionContext;

    private int hashToServer(String key) {
        int[] serverIds = {6, 7, 8, 9}; // Corresponds to IPs in sysNodes.properties
        return serverIds[Math.abs(key.hashCode()) % serverIds.length];
    }

    private Map<Integer, String> getServerIdToIpMap() {
        Map<Integer, String> serverMap = new HashMap<>();
        for (Map.Entry<InetAddress, Integer> entry : connectionContext.getNodeIPMapping().entrySet()) {
            int id = entry.getValue();
            if (id >= 6 && id <= 9) {  // Only include servers
                serverMap.put(id, entry.getKey().getHostAddress());
            }
        }
        return serverMap;
    }
    /**
     * Constructor method
     * 
     * @param nodeId
     * @param connectionContext
     * @param messageBroker
     */
    public MessageReceiver(int nodeId, ConnectionContext connectionContext) {
        this.nodeId = nodeId; // receiver Node ID
        this.reader = connectionContext.getInputReaderHash().get(nodeId);
        this.messageQueue = connectionContext.getMessageQueue();
        this.connectionContext = connectionContext;
    }

    /**
     * Method to receive a message and trigger it's handling and processing.
     * Ends all input only to the socket to ensure that the socket remains up but no
     * messages are received.
     */
    @Override
    public void run() {
        try {
            if (connectionContext.getSequencerID() == nodeId) {
                processAllMessages();
            } else {
                processNonSequenceMessages();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to process all messages if on a sequencer node
     * 
     * @throws IOException
     */
    private void processAllMessages() throws IOException {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    String rawMessageContent;

    while ((rawMessageContent = reader.readLine()) != null) {
        String timestamp = sdf.format(new Date());
        System.out.println(String.format("[%s]Sequenced message received: %s", timestamp, rawMessageContent));

        // STEP 1: Parse message
        SequencedMessage sm = new SequencedMessage(rawMessageContent);
        Message message = sm.getSequencedMessage();

        // STEP 2: Extract key safely
        String key = message.getKey();
        if (key == null || key.isEmpty()) {
            System.err.println("Invalid key in message: " + rawMessageContent);
            continue;
        }

        int port = connectionContext.getPort();
        
        int primary = hashToServer(key);
            int[] serverIds = new int[3];
            for (int i = 0; i < 3; i++) {
                serverIds[i] = 6 + ((primary - 6 + i) % 4); // ensures values in [6,7,8,9]
            }

    

        // STEP 3: Dynamically load IPs from sysNodes.properties
        Map<Integer, String> serverMap = getServerIdToIpMap();
        boolean sent = false;

        for (int targetId : serverIds) {
            String serverIP = serverMap.get(targetId);
            if (serverIP == null) {
                System.err.println(" No IP mapping found for server ID: " + targetId);
                continue;
            }

            try (Socket serverSocket = new Socket(serverIP, port);
                 PrintWriter serverWriter = new PrintWriter(serverSocket.getOutputStream(), true);
                 BufferedReader serverReader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()))) {

                serverWriter.println(message.toString());
                serverWriter.flush();

                if (message.getType() == Message.MessageType.R) {
                    String response = serverReader.readLine();
                    System.out.println("Server Response: " + response);
                }

                System.out.println("Forwarded message to Server " + targetId + " @ " + serverIP);
                sent = true;
                // break;

            } catch (IOException e) {
                System.err.println("Failed to connect to Server " + targetId + " (" + serverIP + ")");
            }
        }

        if (!sent) {
            System.err.println("ERROR: Could not send to either primary or backup server.");
        }
    }
}

    /**
     * Method to process only non-sequencer messages on a non-sequencer node.
     * 
     * @throws IOException
     */
    private void processNonSequenceMessages() throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String rawMessageContent;
        while (messageCount < TOTAL_FOREIGN_MESSAGES && (rawMessageContent = reader.readLine()) != null) {
            String timestamp = sdf.format(new Date());
            System.out.println(String.format("[%s]Message received: {%s}", timestamp, rawMessageContent));
            processAppMessages(rawMessageContent);
            messageCount++;
        }
    }

    /**
     * Method to process application messages
     * 
     * @param rawMessageContent
     */
    public void processAppMessages(String rawMessageContent) {
        
        Message messageReceived = new Message(rawMessageContent);
        messageReceived.setSenderNodeId(nodeId);
        messageQueue.addMessageToQueue(messageReceived);
    }
}

package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;

import java.text.SimpleDateFormat;

public class MessageReceiver extends Thread {
    private BufferedReader reader;
    private Integer nodeId;
    private SequencerQueue sequencerQueue;
    private int messageCount = 0;
    private int sequencedMessageCount = 0;
    private static final int TOTAL_FOREIGN_MESSAGES = 100;
    private static final int TOTAL_SEQUENCE_MESSAGES = 500; // Sets the maximum messages expected from outside. Internal
                                                            // broadcasts go directly to the queues
    private MessageQueue messageQueue;
    private ConnectionContext connectionContext;

    private int hashToServer(String key) {
        return 1 + Math.abs(key.hashCode()) % 2; // returns 1 or 2
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
        this.sequencerQueue = connectionContext.getSequencerQueue();
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
                // We dont need to explicitly check for receiver count sent to the Sequencer
                // since it is internal and we dont have a channel.
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
    
            SequencedMessage sm = new SequencedMessage(rawMessageContent);
            Message message = sm.getSequencedMessage();
    
            String key = message.getKey();
            int serverId = (message.getType() == Message.MessageType.R)
                    ? (message.getKey() != null ? hashToServer(key) : 1)
                    : hashToServer(key);  // always hash on write
            String serverIP = (serverId == 1) ? "10.176.69.38" : "10.176.69.39";
            int port = connectionContext.getPort();
    
            boolean sent = false;
            while (message.retryAllowed() && !sent) {
                try (Socket serverSocket = new Socket(serverIP, port);
                     PrintWriter serverWriter = new PrintWriter(serverSocket.getOutputStream(), true)) {
    
                    serverWriter.println(message.toString());
                    serverWriter.flush();
                    System.out.println("✅ Forwarded message to Server " + serverId + " @ " + serverIP);
                    sent = true;
    
                } catch (IOException e) {
                    message.decrementRf();
                    System.err.println("❌ Failed to send to Server " + serverId + " — RF now: " + message.getRf());
                    if (!message.retryAllowed()) {
                        System.err.println("⚠️ RF exhausted. Server " + serverId + " assumed to be down.");
                    }
                }
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

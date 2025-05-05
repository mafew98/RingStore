package totalbroadcast;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Random;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MessageBroadcaster implements Runnable {
    private VectorClock vectorClock;
    private ConnectionContext connectionContext;
    private HashMap<Integer, PrintWriter> outputStreams; // Hash between nodeID and the output socket stream
    private final int currentNodeId;
    private final int MAX_NEW_MESSAGES = 2; // Sets the maximum messages expected.
    private final int MAX_SEQUENCED_MESSAGES = 10;
    private int sentSequencedMessageCount = 0;
    private int broadcastCount = 0;
    private final Random random = new Random();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    // Constructor
    public MessageBroadcaster(ConnectionContext connectionContext) throws IOException {
        this.connectionContext = connectionContext;
        this.vectorClock = connectionContext.getVectorClock();
        this.currentNodeId = connectionContext.getNodeId();
        this.outputStreams = connectionContext.getOutputWriterHash();
    }

    /**
     * Main Broadcaster method. Does the following actions:
     * 1. Go through all the sockets and create an output stream.
     * 2. Iteratively increment the vector clock value and prepare a message.
     * 3. Send the output messages to each of the streams
     */
    @Override
    public void run() {
        try {
            if (connectionContext.getSequencerID() == currentNodeId) {
                // If node is the Sequencer
                attemptAllMessageBroadcast();
            } else {
                attemptNonSequenceMessageBroadcast();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Properly handle thread interruptions
            e.printStackTrace();
        }
    }

    /**
     * Method to attempt both sequencer and application message broadcast.
     * 
     * @throws InterruptedException
     */
    private void attemptAllMessageBroadcast() throws InterruptedException {
        while (sentSequencedMessageCount < MAX_SEQUENCED_MESSAGES) {
            // looking for sequenced messages
            broadcastSequencerMessages();

            // Broadcast new messages
            broadcastAppMessages();

            // // Flush all outputstreams
            // flushOutputChannels();
        }
    }

    /**
     * Attempt only non sequencer message broadcast
     * 
     * @throws InterruptedException
     */
    private void attemptNonSequenceMessageBroadcast() throws InterruptedException {
        while (broadcastCount < MAX_NEW_MESSAGES) {
            broadcastAppMessages();
        }
    }

    /**
     * Method to broadcast sequencer messages from a sequencer node.
     */
    private void broadcastSequencerMessages() {
        while (connectionContext.peekSequencedBroadcastQueue() != null) {
            String timestamp = sdf.format(new Date());
            String rawMessageString = connectionContext.pollSequencedBroadcastQueue();
            // Send to server node only (Node 6)
            int serverNodeId = 6; // Node 6 = dc07
            PrintWriter serverWriter = outputStreams.get(serverNodeId);
            if (serverWriter != null) {
                try {
                    timestamp = sdf.format(new Date());
                    System.out.println(String.format("[%s]Sending Sequenced Message to Server Node [%d]: %s", timestamp, serverNodeId, rawMessageString));
                    serverWriter.println(rawMessageString);
                    serverWriter.flush();  // Ensure it's pushed
                } catch (Exception e) {
                    System.err.println(" Failed to send message to Server Node " + serverNodeId);
                    e.printStackTrace();
                }
            } else {
                System.err.println("No output stream found for Server Node " + serverNodeId);
            }

            sentSequencedMessageCount++;
        }
    }

    /**
     * Method to broadcast only the application messages. Used when broadcaster is
     * running on a non-sequencer node.
     * 
     * @throws InterruptedException
     */
    private void broadcastAppMessages() throws InterruptedException {
        if (broadcastCount < MAX_NEW_MESSAGES) {
            synchronized (vectorClock) {
                vectorClock.increment(currentNodeId);
            }
            String messageContent = "Message no." + (broadcastCount + 1) + " from " + currentNodeId;
            String rawMessageString = Message.createRawMessage(messageContent, vectorClock);
            String timestamp = sdf.format(new Date());
            System.out.println(String.format("[%s]Broadcasting: " + rawMessageString, timestamp));
            //for (int processId = 1; processId <= connectionContext.getMaxProcesses(); processId++) {
            for (int processId : outputStreams.keySet()) {

                if (processId != currentNodeId) {
                    try {
                        timestamp = sdf.format(new Date());
                        System.out.println(String.format("[%s]Broadcasting Message to [%d]", timestamp, processId));
                        outputStreams.get(processId).println(rawMessageString); // Using the printwriter object to write
                                                                                // to the
                        // outputstream
                    } catch (Exception e) {
                        System.err.println("Failed to send message to process " + processId);
                    }
                } else {
                    broadcastToSelf(rawMessageString);
                }
            }
            broadcastCount++;
            // Random wait to introduce message differences
            if (connectionContext.peekSequencedBroadcastQueue() == null) {
                Thread.sleep(30 + random.nextInt(10));
            }
        }
    }

    /**
     * Utility Method to flush all channels. Not generally required since using
     * Printwriter autoflush.
     */
    private void flushOutputChannels() {
        for (Map.Entry<Integer, PrintWriter> entry : outputStreams.entrySet()) {
            String timestamp = sdf.format(new Date());
            System.out.println(String.format("[%s]Flushing comm channel to [%d]", timestamp, entry.getKey()));
            entry.getValue().flush();
        }
    }

    /**
     * Broadcasts a message to oneself. Does this but just directly placing the
     * message in the message priority queue, skipping the receiver.
     * 
     * @param rawMessageString
     */
    private void broadcastToSelf(String rawMessageString) {
        connectionContext.getMessageQueue().addMessageToQueue(new Message(rawMessageString, currentNodeId));
    }
}
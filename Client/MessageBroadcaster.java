package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
    //private final int MAX_NEW_MESSAGES = 100; // Sets the maximum messages expected.
    //private final int MAX_SEQUENCED_MESSAGES = 500;
    // private int sentSequencedMessageCount = 0;
    // private int broadcastCount = 0;
    // private final Random random = new Random();
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
    if (connectionContext.getSequencerID() == currentNodeId) {
        // Sequencer doesn't broadcast messages from CLI
        return;
    }

    try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {
        while (true) {
            System.out.print("Enter message to send (or type 'exit'): ");
            String input = console.readLine();
            if (input.equalsIgnoreCase("exit")) break;
            
            String[] parts = input.split(":", 3);
            Message.MessageType type;
            Integer targetServer = null;
            try {
                type = Message.MessageType.valueOf(parts[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid message type. Use READ or WRITE.");
                continue;
            }

            if (!parts[1].isEmpty()) {
                try {
                    targetServer = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid server ID.");
                    continue;
                }
            }

            String content = parts[2];
            synchronized (vectorClock) {
                vectorClock.increment(currentNodeId);
            }

            String rawMessage = Message.createRawMessage(content, vectorClock, type, targetServer);
            PrintWriter sequencerWriter = outputStreams.get(connectionContext.getSequencerID());

            if (sequencerWriter != null) {
                sequencerWriter.println(rawMessage);
                sequencerWriter.flush();
                System.out.println("Sent to Sequencer: " + rawMessage);
            } else {
                System.err.println("Sequencer connection not found.");
            }

            // Sleep briefly to avoid racing with sequencer
            Thread.sleep(100);
        }
    } catch (IOException | InterruptedException e) {
        e.printStackTrace();
    }
}
}


    /**
     * Method to attempt both sequencer and application message broadcast.
     * 
     * @throws InterruptedException
     */

    // private void attemptAllMessageBroadcast() throws InterruptedException {
    //     while (sentSequencedMessageCount < MAX_SEQUENCED_MESSAGES) {
    //         // looking for sequenced messages
    //         broadcastSequencerMessages();
    //         // Broadcast new messages
    //         broadcastAppMessages();
    //         // // Flush all outputstreams
    //         // flushOutputChannels();
    //     }
    // }

    /**
     * Attempt only non sequencer message broadcast
     * 
     * @throws InterruptedException
     */
    // private void attemptNonSequenceMessageBroadcast() throws InterruptedException {
    //     while (broadcastCount < MAX_NEW_MESSAGES) {
    //         broadcastAppMessages();
    //     }
    // }

    /**
     * Method to broadcast sequencer messages from a sequencer node.
     */
    // private void broadcastSequencerMessages() {
    //     while (connectionContext.peekSequencedBroadcastQueue() != null) {
    //         String timestamp = sdf.format(new Date());
    //         String rawMessageString = connectionContext.pollSequencedBroadcastQueue();
    //         // Send to server node only (Node 6)
    //         int serverNodeId = 6; // Node 6 = dc07
    //         PrintWriter serverWriter = outputStreams.get(serverNodeId);
    //         if (serverWriter != null) {
    //             try {
    //                 timestamp = sdf.format(new Date());
    //                 System.out.println(String.format("[%s]Sending Sequenced Message to Server Node [%d]: %s", timestamp, serverNodeId, rawMessageString));
    //                 serverWriter.println(rawMessageString);
    //                 serverWriter.flush();  // Ensure it's pushed
    //             } catch (Exception e) {
    //                 System.err.println(" Failed to send message to Server Node " + serverNodeId);
    //                 e.printStackTrace();
    //             }
    //         } else {
    //             System.err.println("No output stream found for Server Node " + serverNodeId);
    //         }

    //         sentSequencedMessageCount++;
    //     }
    // }

    /**
     * Method to broadcast only the application messages. Used when broadcaster is
     * running on a non-sequencer node.
     * 
     * @throws InterruptedException
     */
//     private void broadcastAppMessages() throws InterruptedException {
//         if (broadcastCount < MAX_NEW_MESSAGES) {
//             synchronized (vectorClock) {
//                 vectorClock.increment(currentNodeId);
//             }
//             String messageContent = "Message no." + (broadcastCount + 1) + " from " + currentNodeId;
//             String rawMessageString = Message.createRawMessage(messageContent, vectorClock);
//             String timestamp = sdf.format(new Date());
//             System.out.println(String.format("[%s]Broadcasting: " + rawMessageString, timestamp));
//             //for (int processId = 1; processId <= connectionContext.getMaxProcesses(); processId++) {
//             for (int processId : outputStreams.keySet()) {

//                 if (processId != currentNodeId) {
//                     try {
//                         timestamp = sdf.format(new Date());
//                         System.out.println(String.format("[%s]Broadcasting Message to [%d]", timestamp, processId));
//                         outputStreams.get(processId).println(rawMessageString); // Using the printwriter object to write
//                                                                                 // to the
//                         // outputstream
//                     } catch (Exception e) {
//                         System.err.println("Failed to send message to process " + processId);
//                     }
//                 } else {
//                     //broadcastToSelf(rawMessageString);
//                 }
//             }
//             broadcastCount++;
//             // Random wait to introduce message differences
//             if (connectionContext.peekSequencedBroadcastQueue() == null) {
//                 Thread.sleep(30 + random.nextInt(10));
//             }
//         }
//     }

//     /**
//      * Utility Method to flush all channels. Not generally required since using
//      * Printwriter autoflush.
//      */
//     private void flushOutputChannels() {
//         for (Map.Entry<Integer, PrintWriter> entry : outputStreams.entrySet()) {
//             String timestamp = sdf.format(new Date());
//             System.out.println(String.format("[%s]Flushing comm channel to [%d]", timestamp, entry.getKey()));
//             entry.getValue().flush();
//         }
//     }

//     /**
//      * Broadcasts a message to oneself. Does this but just directly placing the
//      * message in the message priority queue, skipping the receiver.
//      * 
//      * @param rawMessageString
//      */
// //     private void broadcastToSelf(String rawMessageString) {
// //         connectionContext.getMessageQueue().addMessageToQueue(new Message(rawMessageString, currentNodeId));
// //     }
// // }
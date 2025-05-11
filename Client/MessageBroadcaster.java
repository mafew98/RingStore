package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.text.SimpleDateFormat;

public class MessageBroadcaster implements Runnable {
    private ConnectionContext connectionContext;
    private HashMap<Integer, PrintWriter> outputStreams; // Hash between nodeID and the output socket stream
    private final int currentNodeId;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    // Constructor
    public MessageBroadcaster(ConnectionContext connectionContext) throws IOException {
        this.connectionContext = connectionContext;
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

            Message.MessageType shortType = type;
            Message msg = new Message(shortType, 0, 3, content, currentNodeId); // seqNo=0 for now, RF=3
            String rawMessage = msg.toString(); // Final string sent to sequencer

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

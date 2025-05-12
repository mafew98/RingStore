/**
 * The MessageBroadcaster class handles client-side message generation and
 * dispatch to the sequencer. It captures user input via the terminal and
 * constructs formatted READ/WRITE messages to be sent to the sequencer node.
 * @author Mathew George
 * @author Yukta Shah
 **/
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

/**
 * Initializes the MessageBroadcaster with a connection context,
 * capturing this node’s ID and mapping of node IDs to output streams.
 *
 * @param connectionContext shared communication context for the current node
 * @throws IOException if output stream mapping fails
 */
    public MessageBroadcaster(ConnectionContext connectionContext) throws IOException {
        this.connectionContext = connectionContext;
        this.currentNodeId = connectionContext.getNodeId();
        this.outputStreams = connectionContext.getOutputWriterHash();
    }

/**
 * The main execution loop for the client-side CLI broadcaster.
 * Steps:
 * - Skips execution if the node is the sequencer (sequencer does not broadcast)
 * - Reads user input from terminal in format: Type:ServerID:Content
 * - Constructs and serializes a Message object
 * - Sends the message to the sequencer for ordering
 *
 * Handles graceful termination on "exit" input.
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

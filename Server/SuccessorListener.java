package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class SuccessorListener implements Runnable {
    private ConnectionContext connectionContext;
    private RingMutator ringMutator;

    public SuccessorListener(ConnectionContext connectionContext,  RingMutator ringMutator) {
        this.connectionContext = connectionContext;
        this.ringMutator = ringMutator;
    }

    @Override
    public void run() {
        System.out.println("SuccessorListener monitoring failures");
        try {
            int successorNode = connectionContext.getSuccessor();
            BufferedReader sucReader = connectionContext.getInputReader(successorNode);
            // Wait and read a failure.
            String rawMessageContent;
            while ((rawMessageContent = sucReader.readLine()) != null) {
                Message message = new Message(rawMessageContent);
                System.out.println("Received Message: " + rawMessageContent);

                if ((message.getMessageType()).equals("F")) {
                    Socket oldSocket = connectionContext.removeConnection(successorNode);
                    oldSocket.close();
                    // Delay for predecessor to restart
                    Thread.sleep(100);
                    // Attempt to contact new successor
                    connectToNewSuccessor(message);
                    Thread replacement = new Thread(new SuccessorListener(connectionContext, ringMutator));
                    connectionContext.successorListener = replacement;
                    replacement.start();
                    break;
                } else if ((message.getMessageType()).equals("R")) {
                    // get diffs and send it back immediately
                    handleReconnectionMessages(message); 
                    
                } else if ((message.getMessageType()).equals("D")) {
                    connectionContext.getDataStore().addDiffs(message.getMessageContent());
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("exiting successor thread");
        return;
        
    }

    /**
     * Method to connect to a new successsor node incase the previous successor fails.
     * @param message
     */
    public void connectToNewSuccessor(Message message) {
        int newSuccessor = message.getMessageOrderNo();
        while (newSuccessor != ConnectionContext.getNodeID()) {
            // Attempt to connect to new successor
            if (ringMutator.connectToNode(newSuccessor)) {
                System.out.println("Successful Connection to new successor " + newSuccessor);
                break;
            } else {
                // Attempting Increment
                newSuccessor = (newSuccessor + 1) % connectionContext.getMaxServers();
            }
        }
    }

    /**
     * Method to handle reconnection messages sent from a resurrected node.
     * @param message
     * @throws IOException
     */
    private void handleReconnectionMessages(Message message) throws IOException {
        System.out.println("Sending Diff Messages");
        PrintWriter sucWriter = connectionContext.getOutputWriter(connectionContext.getSuccessor());
        String diffs = String.format("D,,%d,%s",ConnectionContext.getNodeID(),connectionContext.getDataStore().getDiffs(message.getNodeNumber()));
        sucWriter.println(diffs);
    }
}

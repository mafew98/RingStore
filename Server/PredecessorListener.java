package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class PredecessorListener implements Runnable{
    private ConnectionContext connectionContext;
    private RingMutator ringMutator;
    private Socket oldSocket;
    private int currNodeNumber;
    private int TOTAL_SERVERS;

    public PredecessorListener(ConnectionContext connectionContext,  RingMutator ringMutator) {
        this.connectionContext = connectionContext;
        this.ringMutator = ringMutator;
    }

    // the assumption here is that no failures occur alongside writes. So the two operations have been split.
    @Override
    public void run() {
        System.out.println("PredecessorListener is Ready");
        if (connectionContext.isAcceptingConnections()) {
            currNodeNumber = ConnectionContext.getNodeID();
            TOTAL_SERVERS = connectionContext.getMaxServers();
            BufferedReader predReader;
            try {
                System.out.println(connectionContext.getPredecessor());
                predReader = connectionContext.getInputReader(connectionContext.getPredecessor());
                System.out.println("Predreader returned as : " + predReader);
                String rawMessageContent;
                while ((rawMessageContent = predReader.readLine()) != null) {
                    Message message = new Message(rawMessageContent);
                    System.out.println("Received Message: " + rawMessageContent);

                    if ((message.getMessageType()).equals("W")) {
                    
                        handleWriteMessages(message);

                    } else if ((message.getMessageType()).equals("F")) {
                        handleFailureMessages(message);

                    } else if ((message.getMessageType()).equals("R")) {
                        handleReconnectionMessages(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Exiting Thread");
            return;            
        }
    }

    // On getting a write message from the predecessor, add it to the datastore and decide to forward it.
    private void handleWriteMessages(Message message) throws IOException {
        // Check if correct node number
        int messageNodeNumber = message.getNodeNumber();
        int potSecondaryNode = ((messageNodeNumber + 1) % TOTAL_SERVERS);
        int potTertiaryNode = ((messageNodeNumber + 2) % TOTAL_SERVERS);
        if (currNodeNumber ==  potSecondaryNode || currNodeNumber == potTertiaryNode) {
            // Directly do the write with priority
            String[] KVPair = message.getMessageContent().split(":",2);
            connectionContext.getDataStore().writeData(Integer.parseInt(KVPair[0]), KVPair[1]);
            int successorNode = connectionContext.getSuccessor();
            // Forward the message if server present
            if (successorNode == potTertiaryNode) {
                System.out.println("The successor node is :" + successorNode);
                PrintWriter sucWriter = connectionContext.getOutputWriter(successorNode);
                sucWriter.println(message.getForwardMessage());
            }
        } else {
            System.out.println("Incorrect Message Received. Rejected.");
        }
    }

    private void handleFailureMessages(Message message) throws IOException {
        System.out.println("Disabling SL");
        connectionContext.stopSL();
        // wait for the new predecessor
        System.out.println("Handling failures");
        int predeccessor = connectionContext.getPredecessor();
        int newPredecessor = message.getMessageOrderNo(); //Sending predecessor number there.
        System.out.println("Waiting for Connection Request from node:" + newPredecessor);
        if (ringMutator.acceptConnectionFromNode(newPredecessor)) {
            System.out.println("Got the connection request");
            Socket oldSocket = connectionContext.removeConnection(predeccessor);
            if (oldSocket != null && !oldSocket.isClosed()) {
                oldSocket.close();
            }
            // Restarting itself
            Thread replacement = new Thread(new PredecessorListener(connectionContext, ringMutator));
            connectionContext.predecessorListener = replacement;
            replacement.start();
            connectionContext.startSL();
            throw new IOException("Exiting current PredecessorListener after replacement.");
        }
    }

    private void handleReconnectionMessages(Message message) {
        System.out.println("Handling Reconnection not implemented");
    }

     private void addToConnectionContext(int tempNodeId, Socket tempSocket) throws IOException {
        connectionContext.addConnection(tempNodeId, tempSocket);
        connectionContext.addInputReader(tempNodeId, new BufferedReader(new InputStreamReader(tempSocket.getInputStream()), 65536));
        connectionContext.addOutputWriter(tempNodeId, new PrintWriter(tempSocket.getOutputStream(), true)); // setting autoflush to true
    }

}

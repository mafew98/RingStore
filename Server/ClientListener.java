package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientListener implements Runnable {
    //1. start server socket to accept connections from clients
    //2. put request into the queue ordered by the sequence id of the message
    //3. Writer thread that writes to the others
    private Integer clientListenerPort=24942;
    ServerSocket clientListenerSocket;
    ConnectionContext connectionContext;
    DataStore dataStore;
    RingManager.RunningFlag runningFlag;

    public ClientListener(ConnectionContext connectionContext, RingManager.RunningFlag runningFlag) throws IOException{
        this.connectionContext = connectionContext;
        this.runningFlag = runningFlag;
        this.connectionContext = connectionContext;
        clientListenerSocket = new ServerSocket(clientListenerPort);
        connectionContext.setClientSocketServer(clientListenerSocket);
        this.dataStore = connectionContext.getDataStore();
    }

    @Override
    public void run() {
        try {
            System.out.println("ClientLister Ready");
            while(runningFlag.running) {
                Socket clientSocket = clientListenerSocket.accept();
                PrintWriter clientWriter = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
                if (connectionContext.isAcceptingConnections()) {              
                    // Accept any client access request
                    System.out.println("Connection established from: " + clientSocket.getInetAddress());
                    BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String rawMessageContent;
                    
                    while ((rawMessageContent = clientReader.readLine()) != null) {
                        Message message = new Message(rawMessageContent);
                        System.out.println("Received Message: " + rawMessageContent);
                        if ((message.getMessageType()).equals("R")) {
                            // Directly handle reads
                            String storedValue = dataStore.readData(Integer.parseInt(message.getMessageContent()));
                            clientWriter.println(String.format("Key %s : Value %s at Server %d", message.getMessageContent(), storedValue, ConnectionContext.getNodeID()));
                        }
                        else if ((message.getMessageType()).equals("W")) {
                            if (isMessageValid(message)) {
                                // Place writes in the write queue
                                connectionContext.getWriteQueue().addMessageToQueue(message);
                            } else {
                                clientWriter.println("Write Servers Unreachable. Write Failed");
                            }
                        }
                    }
                    // closing the client socket
                } else {
                    // Give Replies if not accepting connections
                    clientWriter.println("Server Down.");
                } 
                clientSocket.close();
            } 
        } catch (NumberFormatException | IOException e) {
            e.printStackTrace();
        }
        
    }

    private boolean isMessageValid(Message message) {
        int currentNodeId = connectionContext.getNodeID();
        // A client connection must be to the first or second server only!
        int successorNode = connectionContext.getSuccessor();
        int primaryNode = message.getNodeNumber();
        int secondaryNode = (primaryNode + 1) % (connectionContext.getMaxServers());
        int tertiaryNode = (primaryNode + 2) % (connectionContext.getMaxServers());
        boolean output = false;
        if (isValidSuccessor(currentNodeId, successorNode, primaryNode, secondaryNode, tertiaryNode)) {
            output = true;
        }
        System.out.println("isMessageValid: " + output);
        return output;
    }

    private boolean isValidSuccessor(int currentNodeId, int successorNode, int primaryNode, int secondaryNode, int tertiaryNode) {
        return ((currentNodeId == primaryNode && (successorNode == secondaryNode || successorNode == tertiaryNode)) || (currentNodeId == secondaryNode && successorNode == tertiaryNode));
    }
}

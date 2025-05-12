package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;

public class ServerListener implements Runnable{
    
    private ConnectionContext connectionContext;
    private Thread successorListener;
    private Thread predecessorListener;
    private RingMutator ringMutator;
    private ServerSocket serverSocket;

    public ServerListener(ConnectionContext connectionContext, RingMutator ringMutator) {
        this.connectionContext = connectionContext;
        this.ringMutator = ringMutator;
    }

    @Override
    public void run() {
        // 1. start the successor listener
        startSuccessorThread();
        // 2. start the predecessor listener
        startPredecessorThread();

        // 3. listener for incoming connections and handle
        while (true) {
            try {
                handleNewConnections();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleNewConnections() throws IOException {
        try {
            serverSocket = connectionContext.getServerSocket();
            while(connectionContext.isSLEnabled()) {
                connectionContext.setSocketTimeout(10);
                Socket tempSocket = serverSocket.accept();
                InetAddress remoteIP = tempSocket.getInetAddress();
                
                System.out.println("Accepted Connection from " + remoteIP);
                int tempNodeId = identifyConnector(remoteIP);
                if (tempNodeId == -1) {
                    throw new NumberFormatException("Incorrect Value found for Node ID");
                }

                if (checkValidSuccessor(tempNodeId)) {
                    //Restart the Successor listener
                    System.out.println("Valid Successor Detected");
                    addToConnectionContext(tempNodeId, tempSocket);
                    restartSuccessor(tempNodeId);
                } else if (checkValidPredecessor(tempNodeId)) {
                    // Restart Predecessor
                    System.out.println("Valid Predecessor Detected");
                    addToConnectionContext(tempNodeId, tempSocket);
                    restartPredecessor(tempNodeId);
                }
            }
        } catch (SocketTimeoutException e) {
            // No incoming connection in the last second, re-check isSLEnabled()
        }
    }

    /**
     * Add connections to the connection context
     * @param tempNodeId
     * @param tempSocket
     * @throws IOException
     */
    private void addToConnectionContext(int tempNodeId, Socket tempSocket) throws IOException {
        connectionContext.addConnection(tempNodeId, tempSocket);
        connectionContext.addInputReader(tempNodeId, new BufferedReader(new InputStreamReader(tempSocket.getInputStream()), 65536));
        connectionContext.addOutputWriter(tempNodeId, new PrintWriter(tempSocket.getOutputStream(), true)); // setting autoflush to true
    }

    /**
     * Begin the successor listener thread
     */
    private void startSuccessorThread() {
        System.out.println("Starting Successor Thread");
        successorListener = new Thread(new SuccessorListener(connectionContext, ringMutator));
        successorListener.start();
        connectionContext.successorListener = successorListener;
    }

    /**
     * Start the predecessor listener thread
     */
    private void startPredecessorThread() {
        System.out.println("Starting predecessor Thread");
        predecessorListener = new Thread(new PredecessorListener(connectionContext, ringMutator));
        predecessorListener.start();
        connectionContext.predecessorListener = predecessorListener;
    }

    /**
     * check validity of successor
     * @param tempNodeId
     * @return
     */
    private boolean checkValidSuccessor(int tempNodeId) {
        int currNodeId = ConnectionContext.getNodeID();
        int secondaryNodeId = (currNodeId + 1) % connectionContext.getMaxServers();
        int tertiaryNodeId = (currNodeId + 2) % connectionContext.getMaxServers();
        return (tempNodeId == secondaryNodeId || tempNodeId == tertiaryNodeId);
    }

    /**
     * check validity of predecessor
     * @param tempNodeId
     * @return
     */
    private boolean checkValidPredecessor(int tempNodeId) {
        int currNodeId = ConnectionContext.getNodeID();
        int secondaryPredId = (currNodeId - 1 + connectionContext.getMaxServers()) % connectionContext.getMaxServers();
        int tertiaryPredId = (currNodeId - 2 + connectionContext.getMaxServers()) % connectionContext.getMaxServers();
        return (tempNodeId == secondaryPredId || tempNodeId == tertiaryPredId);
    }

    private void restartPredecessor(int newPredecessor) throws IOException {
        int currPredecessor = connectionContext.getPredecessor();
        Socket predSocket = connectionContext.removeConnection(currPredecessor);
        predSocket.close();
        predecessorListener.interrupt();
        connectionContext.setPredecessor(newPredecessor);
        startPredecessorThread();
    }

    private void restartSuccessor(int newSuccessor) throws IOException {
        int currSuccessor = connectionContext.getSuccessor();
        Socket sucSocket = connectionContext.removeConnection(currSuccessor);
        sucSocket.close();
        successorListener.interrupt();
        connectionContext.setSuccessor(newSuccessor);
        startSuccessorThread();
    }

    private int identifyConnector(InetAddress remoteIP){
        int tempNodeId = -1;
        for (Map.Entry<Integer, InetAddress> entry: connectionContext.getNodeIPMapping().entrySet()) {
            if (entry.getValue().equals(remoteIP)) {
                tempNodeId = entry.getKey();
                System.out.println("Key for new connection found to be " + tempNodeId);
                break;
            }
        }
        return tempNodeId;
    }
}

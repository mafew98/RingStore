package Server;

import java.io.IOException;
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
                // TODO Auto-generated catch block
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
                int tempNodeId = 0;
                System.out.println("Accepted Connection from " + remoteIP);
                for (Map.Entry<Integer, InetAddress> entry: connectionContext.getNodeIPMapping().entrySet()) {
                    if (entry.getValue().equals(remoteIP)) {
                        tempNodeId = entry.getKey();
                        break;
                    }
                }
                if (checkValidSuccessor(tempNodeId)) {
                    //Restart the Successor listener
                    restartSuccessor(tempNodeId);
                } else if (checkValidPredecessor(tempNodeId)) {
                    // Restart Predecessor
                    restartPredecessor(tempNodeId);
                }
            }
        } catch (SocketTimeoutException e) {
            // No incoming connection in the last second, re-check isSLEnabled()
        }
    }

    private void startSuccessorThread() {
        successorListener = new Thread(new SuccessorListener(connectionContext, ringMutator));
        successorListener.start();
        connectionContext.successorListener = successorListener;
    }

    private void startPredecessorThread() {
        predecessorListener = new Thread(new PredecessorListener(connectionContext, ringMutator));
        predecessorListener.start();
        connectionContext.predecessorListener = predecessorListener;
    }

    private boolean checkValidSuccessor(int tempNodeId) {
        int currNodeId = ConnectionContext.getNodeID();
        int secondaryNodeId = (currNodeId + 1) % connectionContext.getMaxServers();
        int tertiaryNodeId = (currNodeId + 2) % connectionContext.getMaxServers();
        return (tempNodeId == secondaryNodeId || tempNodeId == tertiaryNodeId);
    }

    private boolean checkValidPredecessor(int tempNodeId) {
        int currNodeId = ConnectionContext.getNodeID();
        int secondaryPredId = (currNodeId + 1) % connectionContext.getMaxServers();
        int tertiaryPredId = (currNodeId + 2) % connectionContext.getMaxServers();
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
}

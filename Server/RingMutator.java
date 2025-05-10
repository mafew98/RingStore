package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class RingMutator {
    private final int TOTAL_SERVERS;
    private final int NodeId;
    private ServerSocket serverSocket;
    private final int MAX_RETRIES = 5;
    private ConnectionContext connectionContext;

    public RingMutator(ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
        this.NodeId = connectionContext.getNodeID();
        this.TOTAL_SERVERS = connectionContext.getMaxServers();
    }

    public void createLinks() throws IOException {
        startServer();
        int successorNode = (NodeId + 1) % TOTAL_SERVERS;
        int predecessorNode = (NodeId - 1 + TOTAL_SERVERS) % TOTAL_SERVERS;
        connectToNode(successorNode);
        acceptConnectionFromNode(predecessorNode);
        // send ready signals
        sendReadySignal(successorNode);
        // accept ready signals
        acceptReadySignal(predecessorNode);
    }

    /**
     * Starts the socket server to accept incoming connections
     * 
     * @throws IOException
     */
    private void startServer() throws IOException {
        serverSocket = connectionContext.createSocketServer();
        System.out.println("Node " + NodeId + " listening on port " + connectionContext.getPort());
    }

    private void connectToNode(int targetNode) throws IOException {
        HashMap<Integer, InetAddress> systemMapping= connectionContext.getNodeIPMapping();
        boolean connected = false;
        int attempts = 0;
        InetAddress nodeAddress = systemMapping.get(targetNode);
        while (!connected && attempts < MAX_RETRIES) {
            try {
                Socket outSocket = new Socket(nodeAddress, connectionContext.getPort());
                outSocket.setKeepAlive(true);
                connectionContext.addConnection(targetNode, outSocket);
                System.out.println("Connected to Node " + targetNode);
                connected = true;
            } catch (IOException e) {
                attempts++;
                System.err.println("Connection failed to Node " + targetNode + ". Attempt "
                        + attempts + " of " + MAX_RETRIES);
                if (attempts < MAX_RETRIES) {
                    try {
                        Thread.sleep(attempts * 10); // Wait 1 second before retrying
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                } else {
                    System.err.println("Failed to connect to Node " + targetNode + " after "
                            + MAX_RETRIES + " attempts.");
                }
            }
        }
    }

    private void acceptConnectionFromNode(int targetNode) throws IOException {
        InetAddress allowedIPAddress = connectionContext.getNodeIPFromNodeNumber(targetNode);
        Boolean connected = false;
        while (!connected) {
            Socket inSocket = serverSocket.accept();
            inSocket.setKeepAlive(true);
            InetAddress remoteSocketIP = inSocket.getInetAddress();
            if (remoteSocketIP.equals(allowedIPAddress)) {
                connectionContext.addConnection(targetNode, inSocket);
                System.out.println("Accepted connection from node " + targetNode);
                connected = true; //Ending connection search
            } else {
                // Reject the connection request received
                inSocket.close();
            }
        }
    }

    /**
     * Method to send READY signal to the successor node.
     * 
     * @throws IOException
     */
    private void sendReadySignal(int successorNode) throws IOException {
        Socket successorSocket = connectionContext.getConnectionHash().get(successorNode);
        PrintWriter printWriter = new PrintWriter(successorSocket.getOutputStream(), true); // setting autoflush to true
        connectionContext.addOutputWriter(successorNode, printWriter);
        printWriter.println("READY");
    }

    /**
     * acceptReadySignal() implements a wait for the predecessor node in the system to get
     * READY. This is not threaded since even if we are not listening
     * to the messages coming in on the inputstream, they are not lost since the OS
     * would handle and internally buffer there messages. So we can do this
     * computation in order and wait for each socket to get a READY without worry.
     * 
     * @throws IOException
     */
    private void acceptReadySignal(int predecessorNode) throws IOException {
        Socket predecessorSocket = connectionContext.getConnectionHash().get(predecessorNode);
        BufferedReader in = new BufferedReader(new InputStreamReader(predecessorSocket.getInputStream()), 65536); // 64KB Buffer
        while (!in.readLine().equals("READY")) {}
        connectionContext.addInputReader(predecessorNode, in);
        System.out.println("Received READY");  
    }
    
    // Delete server
}

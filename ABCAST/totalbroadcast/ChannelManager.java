package totalbroadcast;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ChannelManager {

    public final int TOTAL_MACHINES;
    private int NodeId;
    private ConnectionContext connectionContext;
    private HashMap<InetAddress, Integer> systemMapping;
    private HashMap<Integer, Socket> connectionHash;
    private ServerSocket serverSocket;
    private final int MAX_RETRIES = 3;

    // Constructor
    public ChannelManager(ConnectionContext connectionContext) {
        this.NodeId = connectionContext.getNodeId();
        this.systemMapping = connectionContext.getNodeIPMapping();
        this.connectionHash = connectionContext.getConnectionHash();
        this.connectionContext = connectionContext;
        this.TOTAL_MACHINES = connectionContext.getMaxProcesses();
    }

    /**
     * Function to Initialize connections between the nodes.
     * Connections between nodes are established such that a node initiates
     * connections to all nodes that are assigned a higher node number than itself
     * and accepts connections from nodes that have a lower node number than itself.
     * This logic creates pairwise channels efficiently. It also orchestrates
     * agreement on the READY state of each node.
     * 
     * @throws IOException
     * @throws NumberFormatException
     */
    public void initializeChannels() throws IOException, NumberFormatException {
        // Initiate Connection Channels
        startServer();

        connectToHigherIdNodes();
        acceptConnectionsFromLowerIdNodes();

        System.out.println("Node " + NodeId + " has established all connections.");

        // Signal readiness
        sendReadySignal();
        waitForReadySignals();
        System.out.println("Node " + NodeId + " is now ready to start broadcasting messages.");
        System.out.println("================================================");
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

    /**
     * Utility function to establish connection to all nodes with a higher node ID.
     * 
     * @throws IOException
     */
    private void connectToHigherIdNodes() throws IOException {
        for (InetAddress nodeAddress : systemMapping.keySet()) {
            if (systemMapping.get(nodeAddress) > NodeId) {
                connectToNode(nodeAddress);
            }
        }
    }

    /**
     * Utility function to connect to a node using sockets. Since it is possible
     * that the socket server may not be started on the connection target, this
     * function attempts connection 3 times.
     * 
     * @param nodeAddress
     * @throws IOException
     */
    private void connectToNode(InetAddress nodeAddress) throws IOException {
        boolean connected = false;
        int attempts = 0;
        while (!connected && attempts < MAX_RETRIES) {
            try {
                Socket outSocket = new Socket(nodeAddress, connectionContext.getPort());
                outSocket.setKeepAlive(true);
                connectionHash.put(systemMapping.get(nodeAddress), outSocket);
                System.out.println("Connected to Node " + systemMapping.get(nodeAddress));
                connected = true;
            } catch (IOException e) {
                attempts++;
                System.err.println("Connection failed to Node " + systemMapping.get(nodeAddress) + ". Attempt "
                        + attempts + " of " + MAX_RETRIES);
                if (attempts < MAX_RETRIES) {
                    try {
                        Thread.sleep(1000); // Wait 1 second before retrying
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                } else {
                    System.err.println("Failed to connect to Node " + systemMapping.get(nodeAddress) + " after "
                            + MAX_RETRIES + " attempts.");
                }
            }
        }
    }

    /**
     * Function to accept connections only from nodes with lower nodeID.
     * 
     * @throws IOException
     * @throws NumberFormatException
     */
    private void acceptConnectionsFromLowerIdNodes() throws IOException, NumberFormatException {
        while (connectionHash.size() < TOTAL_MACHINES - 1) {
            Socket inSocket = serverSocket.accept();
            inSocket.setKeepAlive(true);
            InetAddress remoteSocketIP = inSocket.getInetAddress();
            try {
                int requestorNodeId = systemMapping.get(remoteSocketIP);
                connectionHash.put(requestorNodeId, inSocket);
                System.out.println("Node " + NodeId + " accepted connection from Node " + requestorNodeId);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Method to send READY signal to all connected nodes.
     * 
     * @throws IOException
     */
    private void sendReadySignal() throws IOException {
        for (Map.Entry<Integer, Socket> entry : connectionHash.entrySet()) {
            PrintWriter printWriter = new PrintWriter(entry.getValue().getOutputStream(), true); // setting autoflush to
                                                                                                 // true
            connectionContext.addOutputWriter(entry.getKey(), printWriter);
            printWriter.println("READY");
        }
    }

    /**
     * waitForReadySignals() implements a wait for other nodes in the system to get
     * READY. This is not threaded since even if we are not listening
     * to the messages coming in on the inputstream, they are not lost since the OS
     * would handle and internally buffer there messages. So we can do this
     * computation in order and wait for each socket to get a READY without worry.
     * 
     * @throws IOException
     */
    private void waitForReadySignals() throws IOException {
        for (Map.Entry<Integer, Socket> entry : connectionHash.entrySet()) {
            BufferedReader in = new BufferedReader(new InputStreamReader(entry.getValue().getInputStream()), 65536); // 64KB
                                                                                                                     // Buffer
            while (!in.readLine().equals("READY")) {
            }
            connectionContext.addInputReader(entry.getKey(), in);
            System.out.println("Received READY");
        }
    }

    /**
     * Closes all the channels once the communication is complete.
     * 
     * @throws IOException
     */
    public void closeChannels() throws IOException {
        connectionContext.closeChannels();
        System.out.println("Closed all connections to " + NodeId);
    }
}
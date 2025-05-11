package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Date;

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

    public void createInitialLinks() throws IOException {
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

    public boolean connectToNode(int targetNode) {
        HashMap<Integer, InetAddress> systemMapping= connectionContext.getNodeIPMapping();
        boolean connected = false;
        int attempts = 0;
        InetAddress nodeAddress = systemMapping.get(targetNode);
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        String time = sdf.format(now);
        System.out.println(time + " | Connecting to Node" + targetNode + nodeAddress);
        while (!connected && attempts < MAX_RETRIES) {
            try {
                Socket outSocket = new Socket(nodeAddress, connectionContext.getPort());
                outSocket.setKeepAlive(true);
                connectionContext.addConnection(targetNode, outSocket);
                System.out.println("Connected to Node " + targetNode);
                connected = true;
                // Setting the successor Node
                connectionContext.setSuccessor(targetNode);     
            } catch (IOException e) {
                attempts++;
                System.err.println("Connection failed to Node " + targetNode + ". Attempt "
                        + attempts + " of " + MAX_RETRIES);
                if (attempts < MAX_RETRIES) {
                    try {
                        Thread.sleep(attempts * 100); // Wait 1 second before retrying
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                } else {
                    System.err.println("Failed to connect to Node " + targetNode + " after "
                            + MAX_RETRIES + " attempts.");
                }
            }
        }
        return connected;
    }

    public boolean acceptConnectionFromNode(int targetNode) throws IOException {
        InetAddress allowedIPAddress = connectionContext.getNodeIPFromNodeNumber(targetNode);
        Boolean connected = false;
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        String time = sdf.format(now);
        System.out.println(time + " | Waiting for connection from :" + allowedIPAddress);
        while (!connected) {
            connectionContext.setSocketTimeout(0);
            Socket inSocket = serverSocket.accept();
            inSocket.setKeepAlive(true);
            InetAddress remoteSocketIP = inSocket.getInetAddress();
            if (remoteSocketIP.equals(allowedIPAddress)) {
                connectionContext.addConnection(targetNode, inSocket);
                System.out.println("Accepted connection from node " + targetNode);
                connected = true; //Ending connection search
                //Setting the predeccessor node
                connectionContext.setPredecessor(targetNode);
            } else {
                // Reject the connection request received
                inSocket.close();
            }
        }
        return connected;
    }

    /**
     * Method to send READY signal to the successor node. Only used during initial setup
     * 
     * @throws IOException
     */
    public void sendReadySignal(int successorNode) throws IOException {
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
     * ONLY USED DURING INITIAL SETUP
     * 
     * @throws IOException
     */
    public void acceptReadySignal(int predecessorNode) throws IOException {
        Socket predecessorSocket = connectionContext.getConnectionHash().get(predecessorNode);
        BufferedReader in = new BufferedReader(new InputStreamReader(predecessorSocket.getInputStream()), 65536); // 64KB Buffer
        while (!in.readLine().equals("READY")) {}
        connectionContext.addInputReader(predecessorNode, in);
        System.out.println("Received READY");  
    }
    
    public void rebel() throws IOException{
        // 1. Stop accepting any connections
        connectionContext.stopAcceptionConnections(); 
        // 2. let predecessor and successor know
        sendFailMessage();
        // 3. close links
        Socket predecessorSocket = connectionContext.removeConnection(connectionContext.getPredecessor());
        Socket successorSocket = connectionContext.removeConnection(connectionContext.getSuccessor());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        predecessorSocket.close();
        successorSocket.close();
        connectionContext.setPredecessor(null);
        connectionContext.setSuccessor(null);
        System.out.println("Rebellion Complete. I am Isolated.");
    }

    private void sendFailMessage() throws IOException{
        int predeccessor = connectionContext.getPredecessor();
        int successor = connectionContext.getSuccessor();
        PrintWriter sucWriter = connectionContext.getOutputWriter(successor);
        sucWriter.println(String.format("F,%d,%d,", predeccessor, NodeId));
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        PrintWriter predecessorWriter = connectionContext.getOutputWriter(predeccessor);
        predecessorWriter.println(String.format("F,%d,%d,", successor, NodeId));
    }

    public void resurrect() {
        // 1. find the successor
        try {
            sendRequestToPredecessor();
            sendRequestToSuccessor();
            connectionContext.startAcceptingConnections(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendRequestToSuccessor() throws IOException {
        int currNodeId = ConnectionContext.getNodeID();
        for (int i=1; i < 3; i++) {
            int potSuccessor = (currNodeId + i) % connectionContext.getMaxServers();
            if (connectToNode(potSuccessor)){
                System.out.println("Connected to Successor " + potSuccessor);
                connectionContext.setSuccessor(potSuccessor);
                Socket newSocket = connectionContext.getConnectionSocket(potSuccessor);
                BufferedReader in = new BufferedReader(new InputStreamReader(newSocket.getInputStream()), 65536);
                connectionContext.addInputReader(potSuccessor, in);
                PrintWriter out = new PrintWriter(newSocket.getOutputStream(), true); // setting autoflush to true
                out.println(String.format("R,,%d,", currNodeId));
                break;
            }
        }
    }

    private void sendRequestToPredecessor() throws IOException {
        int currNodeId = ConnectionContext.getNodeID();
        for (int i=1; i < 3; i++) {
            int potPredecessor = (currNodeId - i + connectionContext.getMaxServers()) % connectionContext.getMaxServers();
            if (connectToNode(potPredecessor)){
                System.out.println("Connected to Predecessor " + potPredecessor);
                connectionContext.setPredecessor(potPredecessor);
                Socket newSocket = connectionContext.getConnectionSocket(potPredecessor);
                BufferedReader in = new BufferedReader(new InputStreamReader(newSocket.getInputStream()), 65536);
                connectionContext.addInputReader(potPredecessor, in);
                PrintWriter out = new PrintWriter(newSocket.getOutputStream(), true); // setting autoflush to true
                out.println(String.format("R,,%d,", currNodeId));
                break;
            }
        }
    }
}

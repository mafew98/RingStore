package Server;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionContext {
    private static int nodeId;
    private static final HashMap<Integer, InetAddress> nodeIPMapping = new HashMap<>();
    private static Integer port;
    private static Integer MAX_SERVERS;
    private ServerSocket serverSocket;
    private ServerSocket clientSocketServer;
    private ConcurrentHashMap<Integer, Socket> connectionHash = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, BufferedReader> inputReaderHash = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, PrintWriter> outputWriterHash = new ConcurrentHashMap<>();
    private DataStore dataStore;
    private WriteQueue writeQueue;
    private volatile Neighbors neighbors = new Neighbors();
    private AtomicBoolean acceptConnections = new AtomicBoolean(true);
    private AtomicBoolean enableServerListener = new AtomicBoolean(true);

    /**
     * Static method to read the systems property file.
     * 
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void readSystemProperties() throws FileNotFoundException, IOException {

        Properties readProps = new Properties();
        String rootPath = "./";
        String sysConfigPath = rootPath + "serverNodes.properties";
        try {
            readProps.load(new FileInputStream(sysConfigPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(readProps);
        // Creating and formatting the node-IP mapping
        for (String key : readProps.stringPropertyNames()) {
            if (key.startsWith("port")) {
                port = Integer.parseInt(readProps.getProperty(key));
            } else if (key.startsWith("serverCount")) {
                MAX_SERVERS = Integer.parseInt(readProps.getProperty(key));
            } else {
                nodeIPMapping.put(Integer.parseInt(key), InetAddress.getByName(readProps.getProperty(key)));
            }
        }
        System.out.println(nodeIPMapping);
    }

    /**
     * Static method to determine the current node and extract its node ID.
     * 
     * @return
     * @throws UnknownHostException
     */
    public static void setCurrentNodeID() throws UnknownHostException {
        try {
            InetAddress currHostIP = InetAddress.getByName(InetAddress.getLocalHost().getHostAddress());
            for (Map.Entry<Integer, InetAddress> entry : nodeIPMapping.entrySet()) {
                if (entry.getValue().equals(currHostIP)) {
                    nodeId = entry.getKey();
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static int getNodeID() {
        return nodeId;
    }

    /**
     * @return int
     */
    public int getMaxServers() {
        return MAX_SERVERS;
    }

    /**
     * Returns the communication port number
     * 
     * @return
     */
    public int getPort() {
        return port;
    }

    /**
     * Creates the socket server for the communication.
     * Specifies an OS buffer size of 10 connections so as to not lose connection
     * messages if they are received before we listen on the socket.
     * 
     * @return
     * @throws IOException
     */
    public ServerSocket createSocketServer() throws IOException {
        serverSocket = new ServerSocket(port, 10);
        return serverSocket;
    }

    public void stopSocketServer() throws IOException {
        System.out.println("Stopping Socket Server");
        serverSocket.close();
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void setClientSocketServer(ServerSocket clientServerSocket) {
        this.clientSocketServer = clientServerSocket;
    }

    public ServerSocket getClientServerSocket() {
        return clientSocketServer;
    }

    public HashMap<Integer, InetAddress> getNodeIPMapping() {
        return nodeIPMapping;
    }

    public InetAddress getNodeIPFromNodeNumber(int targetNodeId) {
        return nodeIPMapping.get(targetNodeId);
    }

    /**
     * Getter for nodeID: socket hash map
     * 
     * @return
     */
    public ConcurrentHashMap<Integer, Socket> getConnectionHash() {
        return this.connectionHash;
    }

    /**
     * Getter for nodeID: socket hash map
     * 
     * @param connectionHash
     */
    public void setConnectionHash(ConcurrentHashMap<Integer, Socket> connectionHash) {
        this.connectionHash = connectionHash;
    }

    public Socket getConnectionSocket(int targetNodeId) {
        return connectionHash.get(targetNodeId);
    }

    // Placeholder: Methods to add individual input and output streams.
    public void addConnection(int connectionNodeId, Socket socket) {
        connectionHash.put(connectionNodeId, socket);
    }

    // Removes a connection and its associated printWriter and bufferedReader
    // objects from the connection context
    public Socket removeConnection(int connectionNodeId) {
        inputReaderHash.remove(connectionNodeId);
        outputWriterHash.remove(connectionNodeId);
        return connectionHash.remove(connectionNodeId);
    }

    // Placeholder: Methods to add individual input and output streams. Not used;
    // for extensibility.
    public void addInputReader(int inputNodeId, BufferedReader inputReader) {
        inputReaderHash.put(inputNodeId, inputReader);
    }

    public BufferedReader getInputReader(int inputNodeId) throws IOException {
        BufferedReader inputReader = inputReaderHash.get(inputNodeId);
        System.out.println("Getting the input reader");
        if (inputReader == null) {
            Socket targetSocket = connectionHash.get(inputNodeId);
            inputReader = new BufferedReader(new InputStreamReader(targetSocket.getInputStream()));
            addInputReader(inputNodeId, inputReader);
        }
        System.out.println("The input reader is " + inputReader);
        return inputReader;
    }

    // Placeholder: Methods to add individual input and output streams
    public void addOutputWriter(int outputNodeId, PrintWriter outputWriter) {
        outputWriterHash.put(outputNodeId, outputWriter);
    }

    // Method to get the stored output writers
    public PrintWriter getOutputWriter(int outputNodeId) throws IOException {
        PrintWriter outputWriter = outputWriterHash.get(outputNodeId);
        if (outputWriter == null) {
            Socket targetSocket = connectionHash.get(outputNodeId);
            outputWriter = new PrintWriter(new OutputStreamWriter(targetSocket.getOutputStream()), true);
            addOutputWriter(outputNodeId, outputWriter);
        }
        return outputWriter;
    }

    /**
     * Pointer to the content storage
     * @return
     */
    public DataStore getDataStore() {
        return this.dataStore;
    }

    /**
     * Set the storage container
     */
    public void setDataStore() {
        this.dataStore = new DataStore();
    }

    /**
     * Queue to track all write requests
     */
    public void setWriteQueue() {
        this.writeQueue = new WriteQueue();
    }

    /**
     * Return the write queue
     * @return
     */
    public WriteQueue getWriteQueue() {
        return this.writeQueue;
    }

    /**
     * Class to define the predecessor and successor of a node in the server ring
     */
    static class Neighbors {
        private Integer predecessor;
        private Integer successor;

        public synchronized void setPredecessor(Integer p) {
            this.predecessor = p;
        }

        public synchronized Integer getPredecessor() {
            return predecessor;
        }

        public synchronized void setSuccessor(Integer s) {
            this.successor = s;
        }

        public synchronized Integer getSuccessor() {
            return successor;
        }

    }

    public void setPredecessor(Integer predecessorNodeId) {
        neighbors.setPredecessor(predecessorNodeId);
    }

    public Integer getPredecessor() {
        return neighbors.getPredecessor();
    }

    public void setSuccessor(Integer successorNodeId) {
        neighbors.setSuccessor(successorNodeId);
    }

    public Integer getSuccessor() {
        return neighbors.getSuccessor();
    }

    // This method is acceessed concurrently by multiple threads but does not need
    // synchronization since it is internally using an atomic boolean
    public boolean isAcceptingConnections() {
        return acceptConnections.get();
    }

    public void stopAcceptionConnections() {
        acceptConnections.set(false);
    }

    public void startAcceptingConnections() {
        acceptConnections.set(true);
    }

    // This method is acceessed concurrently by multiple threads but does not need
    // synchronization since it is internally using an atomic boolean
    public boolean isSLEnabled() {
        return enableServerListener.get();
    }

    public void stopSL() {
        enableServerListener.set(false);
    }

    public void startSL() {
        enableServerListener.set(true);
    }

    /**
     * Closes all sockets established during connection phase and shuts down the
     * server.
     * 
     * @throws IOException
     */
    public void closeChannels() throws IOException {
        for (Map.Entry<Integer, Socket> entry : connectionHash.entrySet()) {
            entry.getValue().close();
        }
        // Shutting down the server
        serverSocket.close();
    }

    public synchronized void setSocketTimeout(int timeoutMillis) throws SocketException {
        serverSocket.setSoTimeout(timeoutMillis);
    }

    // ====================================
    // THREAD STORAGE
    // ====================================
    public volatile Thread predecessorListener;
    public volatile Thread successorListener;

}
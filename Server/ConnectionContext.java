package Server;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConnectionContext {
    private static int nodeId;
    private static final HashMap<Integer, InetAddress> nodeIPMapping = new HashMap<>();
    private static Integer port;
    private static Integer MAX_SERVERS;
    private ServerSocket serverSocket;
    private ServerSocket clientSocketServer;
    private HashMap<Integer, Socket> connectionHash = new HashMap<>();
    private HashMap<Integer, BufferedReader> inputReaderHash = new HashMap<>();
    private HashMap<Integer, PrintWriter> outputWriterHash = new HashMap<>();
    private DataStore dataStore;
    private WriteQueue writeQueue;

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

    public void stopSocketServer() throws IOException{
        System.out.println("Stopping Socket Server");
        serverSocket.close();
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
    public HashMap<Integer, Socket> getConnectionHash() {
        return this.connectionHash;
    }

    /**
     * Getter for nodeID: socket hash map
     * 
     * @param connectionHash
     */
    public void setConnectionHash(HashMap<Integer, Socket> connectionHash) {
        this.connectionHash = connectionHash;
    }

    // Placeholder: Methods to add individual input and output streams. For
    // extensibility. Not currently used.
    public void addConnection(int nodeId, Socket socket) {
        connectionHash.put(nodeId, socket);
    }

    // Placeholder: Methods to add individual input and output streams. Not used;
    // for extensibility.
    public void addInputReader(int nodeId, BufferedReader inputReader) {
        inputReaderHash.put(nodeId, inputReader);
    }

    public BufferedReader getInputReader(int nodeId) {
        return inputReaderHash.get(nodeId);
    }
    // Placeholder: Methods to add individual input and output streams. Not used;
    // for extensibility.
    public void addOutputWriter(int nodeId, PrintWriter outputWriter) {
        outputWriterHash.put(nodeId, outputWriter);
    }

    public PrintWriter getOuputWriter(int nodeId) {
        return outputWriterHash.get(nodeId);
    }

    public DataStore getDataStore() {
        return this.dataStore;
    }

    public void setDataStore() {
        this.dataStore = new DataStore();
    }

    public void setWriteQueue() {
        this.writeQueue = new WriteQueue();
    }

    public WriteQueue getWriteQueue() {
        return this.writeQueue;
    }
}

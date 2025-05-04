package totalbroadcast;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Map;
import java.util.Properties;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Class to contain all information about the connections being made in the system.
 * @author Mathew George
 */
public class ConnectionContext {
    private static int nodeId;
    private static final HashMap<InetAddress, Integer> nodeIPMapping = new HashMap<InetAddress, Integer>();
    private HashMap<Integer, Socket> connectionHash = new HashMap<>();
    private HashMap<Integer, BufferedReader> inputReaderHash = new HashMap<>();
    private HashMap<Integer, PrintWriter> outputWriterHash = new HashMap<>();
    private ServerSocket serverSocket;
    private static int port = 24942; // Communication port for the whole system
    private int MAX_PROCESSES = 5;

    
    /** 
     * @return int
     */
    public int getMaxProcesses() {
        return MAX_PROCESSES;
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
     * Returns the current node ID
     * 
     * @return
     */
    public int getNodeId() {
        return nodeId;      // this is already set by getcurrentnodeid()
    }

    /**
     * Getter for IP: nodeID hash map
     * 
     * @return
     */
    public HashMap<InetAddress, Integer> getNodeIPMapping() {
        return nodeIPMapping;
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

    /**
     * Getter for the inputStream reader object
     * 
     * @return
     */
    public HashMap<Integer, BufferedReader> getInputReaderHash() {
        return inputReaderHash;
    }

    /**
     * Setter for the inputStream reader object
     * 
     * @return
     */
    public void setInputReaderHash(HashMap<Integer, BufferedReader> inputReaderHash) {
        this.inputReaderHash = inputReaderHash;
    }

    /**
     * Getter for the outputStream writer object
     * 
     * @return
     */
    public HashMap<Integer, PrintWriter> getOutputWriterHash() {
        return outputWriterHash;
    }

    /**
     * Setter for the outputStream writer object
     * 
     * @return
     */
    public void setOutputWriterHash(HashMap<Integer, PrintWriter> outputWriterHash) {
        this.outputWriterHash = outputWriterHash;
    }

    // Placeholder: Methods to add individual input and output streams. Not used;
    // for extensibility.
    public void addInputReader(int nodeId, BufferedReader inputReader) {
        inputReaderHash.put(nodeId, inputReader);
    }

    // Placeholder: Methods to add individual input and output streams. Not used;
    // for extensibility.
    public void addOutputWriter(int nodeId, PrintWriter outputWriter) {
        outputWriterHash.put(nodeId, outputWriter);
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

    /**
     * Static method to read the systems property file.
     * 
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void readSystemProperties() throws FileNotFoundException, IOException {
        Properties readProps = new Properties();
        String rootPath = "./";
        String sysConfigPath = rootPath + "sysNodes.properties";
        try {
            readProps.load(new FileInputStream(sysConfigPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(readProps);
        // Creating and formatting the node-IP mapping
        for (String key : readProps.stringPropertyNames()) {
            nodeIPMapping.put(InetAddress.getByName(key), Integer.parseInt(readProps.getProperty(key)));
        }
        System.out.println(nodeIPMapping);
    }

    /**
     * Static method to determine the current node and extract its node ID.
     * 
     * @return
     * @throws UnknownHostException
     */
    public static Integer getCurrentNodeID() throws UnknownHostException {
        try {
            InetAddress currHost = InetAddress.getLocalHost();
            nodeId = nodeIPMapping.get(InetAddress.getByName(currHost.getHostAddress()));
            return nodeId;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * VECTOR CLOCK
     */

    private VectorClock vectorClock;

    /**
     * Vector Clock getter
     * @return
     */
    public VectorClock getVectorClock() {
        return vectorClock;
    }

    /**
     * Vector Clock setter
     * @param vectorClock
     */
    public void setVectorClock(VectorClock vectorClock) {
        this.vectorClock = vectorClock;
    }
    

    /*
     * MESSAGE QUEUE
     */

    private MessageQueue messageQueue; // Shared message queue

    public MessageQueue getMessageQueue() {
        return this.messageQueue;
    }

    public void setMessageQueue(MessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    /**
    * SEQUENCER ATTRIBUTES
    */

    private int sequencerID;
    private ConcurrentLinkedQueue<String> sequencedBroadcastQueue = new ConcurrentLinkedQueue<>();  // Using raw Sequenced Messages here
    private SequencerQueue sequencerQueue;
    
    /**
     * Sets the ID for the current Sequencer process
     * @param sequencerID
     */
    public void setSequencerID(int sequencerID) {
        this.sequencerID = sequencerID;
    }

    /**
     * Returns the ID of the current sequencer process
     * @return
     */
    public int getSequencerID() {
        return this.sequencerID;
    }

    /**
     * Method to add sequenced messages to the broadcast queue.
     * Marks sequenced messages as "to be broadcast"
     * @param rawSequencedMessage
     */
    public void addToSequencedBroadcastQueue(String rawSequencedMessage) {
        this.sequencedBroadcastQueue.add(rawSequencedMessage);
    }

    /**
     * Method to peek at the Sequenced Broadcast Queue
     * @return
     */
    public String peekSequencedBroadcastQueue() {
        return this.sequencedBroadcastQueue.peek();
    }

    /**
     * Polls the Sequenced Broadcast Queue
     * @return
     */
    public String pollSequencedBroadcastQueue() {
        return this.sequencedBroadcastQueue.poll();
    }

    /**
     * Stores the sequencer queue object
     * @param sequencerQueue
     */
    public void setSequencerQueue(SequencerQueue sequencerQueue) {
        this.sequencerQueue = sequencerQueue;
    }

    /**
     * Gets the sequencer queue object
     * @return
     */
    public SequencerQueue getSequencerQueue() {
        return this.sequencerQueue;
    }

}
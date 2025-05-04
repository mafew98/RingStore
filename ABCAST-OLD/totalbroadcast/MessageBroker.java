package totalbroadcast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.PriorityBlockingQueue;

public class MessageBroker {

    private HashMap<Integer, Socket> connectionHash;
    protected PriorityBlockingQueue<Message> messageQueue; // Shared message queue
    private final int MAX_PROCESSES = 5;
    private final int MAX_PROCESSES_MESSAGES = 100;
    private ConnectionContext connectionContext;
    private ArrayList<Thread> receiverThreads = new ArrayList<>();
    private Thread broadcasterThread;
    private Thread sequencerThread;
    private boolean isSequencerNode = false;

    // Constructor
    public MessageBroker(ConnectionContext connectionContext) {
        this.connectionHash = connectionContext.getConnectionHash();
        this.connectionContext = connectionContext;
    }

    /**
     * Method to initialize all datastructures required to start communication
     * phase.
     */
    public void initialization() {
        // Initialize vector clock for the node
        connectionContext.setVectorClock(new VectorClock(MAX_PROCESSES));

        // Initialize Message Queue
        connectionContext.setMessageQueue(new MessageQueue(MAX_PROCESSES * MAX_PROCESSES_MESSAGES));

        // Initialize Sequence Queue
        connectionContext.setSequencerQueue(new SequencerQueue());

        // Initialize Sequencer Identification
        try {
            connectionContext.setSequencerID(1); // sets sequencer indication
            if (ConnectionContext.getCurrentNodeID() == 1) {
                isSequencerNode = true;
                System.out.println("I am the Sequencer.");
            }
        } catch (java.net.UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts the sequencer of a separate thread. Sequencer handles sequencing and
     * message delivery.
     */
    public void startSequencer() {
        sequencerThread = new Thread(new Sequencer(isSequencerNode, connectionContext));
        sequencerThread.start();
    }

    /**
     * Starts receiver threads that handle all messages from one node (tied to one
     * socket)
     */
    public void startReceivers() {
        for (Map.Entry<Integer, Socket> entry : connectionHash.entrySet()) {
            Thread receiver = new MessageReceiver(entry.getKey(), connectionContext);
            receiver.start();
            receiverThreads.add(receiver);
        }
    }

    /**
     * Starts the broadcaster thread that broadcasts messages to all other nodes.
     * 
     * @throws IOException
     */
    public void startBroadcaster() throws IOException {
        broadcasterThread = new Thread(new MessageBroadcaster(connectionContext));
        broadcasterThread.start();
    }

    /**
     * Method to enforce waiting for working reader/writer threads to finish.
     * 
     * @throws InterruptedException
     */
    public void waitForCompletion() throws InterruptedException {
        for (Thread receiver : receiverThreads) {
            receiver.join(); // Wait for each receiver to finish
        }
        if (broadcasterThread != null) {
            broadcasterThread.join(); // Wait for broadcaster to finish
        }

        if (sequencerThread != null) {
            sequencerThread.join();
        }

        // Send done to all active processes
        sendCompletionNotification();
        // threaded receiver with timeout
        waitForCompletionAcknowledgement();
    }

    /**
     * Method to send completion notification to all connected nodes.
     * 
     */
    private void sendCompletionNotification() {
        for (Map.Entry<Integer, PrintWriter> entry : connectionContext.getOutputWriterHash().entrySet()) {
            entry.getValue().println("COMPLETE");
        }
    }

    /**
     * Wait Method that waits for the completion acknowledgement
     * 
     */
    private void waitForCompletionAcknowledgement() {
        try {
            for (Map.Entry<Integer, BufferedReader> entry : connectionContext.getInputReaderHash().entrySet()) {
                while (!entry.getValue().readLine().equals("COMPLETE")) {
                }
                System.out.println("Received Complete from " + entry.getKey());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
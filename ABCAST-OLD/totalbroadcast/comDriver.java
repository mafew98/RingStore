package totalbroadcast;

import java.io.*;

public class comDriver {
    /**
     * Driver function that orchestras the causal broadcast.
     * 
     * @param args
     * @throws FileNotFoundException
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws FileNotFoundException, IOException, InterruptedException {
        // Loads system properties
        ConnectionContext.readSystemProperties();
        ConnectionContext.getCurrentNodeID();

        // Creates the communication context and invokes channel setup
        ConnectionContext connectionContext = new ConnectionContext();
        ChannelManager ChannelManager = new ChannelManager(connectionContext);
        ChannelManager.initializeChannels();

        // Create the message broker class that will begin the broadcast and deliver
        // messages
        MessageBroker messageBroker = new MessageBroker(connectionContext);
        messageBroker.initialization();
        messageBroker.startSequencer();
        messageBroker.startReceivers();
        messageBroker.startBroadcaster();
        messageBroker.waitForCompletion();

        // Close all the channels
        ChannelManager.closeChannels();
    }
}
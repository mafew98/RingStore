package Client;

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
        Integer currentNodeId = ConnectionContext.getCurrentNodeID();
        
        


        // If this is the server node (Node 6), run ServerListener
        if (currentNodeId == 6 || currentNodeId == 7 ) {
            System.out.println("This is the Server node (Node 6). Launching ServerListener...");
            ServerListener.main(args);
            return;
        }

        // Else: this is a client node (Node 1 to 5)
        System.out.println("This is a Client node (Node " + currentNodeId + "). Launching Total Order Broadcast...");

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
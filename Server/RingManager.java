package Server;

import java.io.FileNotFoundException;
import java.io.IOException;

public class RingManager {
    
    // Creates the communication context and invokes channel setup
    ConnectionContext connectionContext = new ConnectionContext();
    private static RunningFlag runningFlag = new RunningFlag();

    private static void loadRingProperties() throws FileNotFoundException, IOException {
        // Reading server system properties and setting the server number
        ConnectionContext.readSystemProperties();
        ConnectionContext.setCurrentNodeID();
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, InterruptedException{
        loadRingProperties();
        ConnectionContext connectionContext = new ConnectionContext();
        RingMutator ringMutator = new RingMutator(connectionContext);
        // Create the connections
        ringMutator.createLinks();
        // Create data storage
        connectionContext.setDataStore();
        // Create the write queue
        connectionContext.setWriteQueue();

        // Register shutdown hook for Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Ctrl+C detected. Shutting down...");
            runningFlag.running = false; 
            //Shutdown all the socket servers.
            try {
                connectionContext.getClientServerSocket().close();
                connectionContext.stopSocketServer();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
        }));

        //Starting threads
        Thread workerThread = new Thread(new Worker(connectionContext, runningFlag));
        Thread neighborListenerThread = new Thread(new NeighborListener(connectionContext, runningFlag));
        Thread clientListenerThread = new Thread(new ClientListener(connectionContext, runningFlag));

        // Keep main thread alive until Ctrl+C
        while (runningFlag.running) {
            // TODO: ADD KILL HERE!!
            Thread.sleep(1000);
        }

        System.out.println("Main thread exiting.");
    }

    public static class RunningFlag {
        public volatile boolean running = true;
    }
}
package Server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

public class RingManager {
    
    private static RunningFlag runningFlag = new RunningFlag();
    private static Thread successorListenerThread;
    private static Thread predecessorListenerThread;
    
    private static void loadRingProperties() throws FileNotFoundException, IOException {
        // Reading server system properties and setting the server number
        ConnectionContext.readSystemProperties();
        ConnectionContext.setCurrentNodeID();
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, InterruptedException{
        loadRingProperties();
        final ConnectionContext connectionContext = new ConnectionContext();
        RingMutator ringMutator = new RingMutator(connectionContext);
        // Create the connections
        ringMutator.createInitialLinks();
        // Create data storage
        connectionContext.setDataStore();
        // Create the write queue
        connectionContext.setWriteQueue();
        System.out.println("Beginning Request Handling");
        System.out.println("========================================");

        // Register shutdown hook for Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Ctrl+C detected. Shutting down...");
                runningFlag.running = false;

                //Shutdown all the socket servers.
                try {
                    connectionContext.getClientServerSocket().close();
                    connectionContext.stopSocketServer();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));

        System.out.println("Connection Hash is :" + connectionContext.getConnectionHash());

        //Starting threads
        //Start the connection listener

        Thread workerThread = new Thread(new Worker(connectionContext, runningFlag));
        workerThread.start();
        
        Thread ServerListenerThread = new Thread(new ServerListener(connectionContext, ringMutator));
        ServerListenerThread.start();
        Thread clientListenerThread = new Thread(new ClientListener(connectionContext, runningFlag));
        clientListenerThread.start();

    
        runServerCLI(connectionContext, ringMutator);
        connectionContext.closeChannels();
        workerThread.interrupt();
        ServerListenerThread.interrupt();
        System.out.println("Main thread exiting.");
    }

    private static void runServerCLI(ConnectionContext connectionContext, RingMutator ringMutator) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("RingServer CLI");
        System.out.println("========================================");
        System.out.println("Type 'help' for options and 'Ctrl + C' anytime to quit.");
    
        int currNodeID = ConnectionContext.getNodeID();
    
        while (runningFlag.running) {
            System.out.print(String.format("\nRingServerCLI@[%s] >> ", currNodeID));
            String input = scanner.nextLine().trim().toLowerCase();
    
            switch (input) {
                case "help":
                    System.out.println("\nAvailable commands:");
                    System.out.println("  help      - Show this help menu");
                    System.out.println("  print     - Display the current contents of the datastore");
                    System.out.println("  rebel     - Force the server to destory its links and stop actions");
                    System.out.println("  resurruct - Asks for mercy and join the Ring again");
                    break;
    
                case "print":
                    System.out.println("\nDatastore contents:");
                    System.out.println(connectionContext.getDataStore().getContent());
                    break;
    
                case "rebel":
                    //rebel here
                    ringMutator.rebel();
                    // successorListenerThread.interrupt();
                    // predecessorListenerThread.interrupt();
                    break;
                
                case "resurrect":
                    // The resurruction is upon us
                    ringMutator.resurrect();
                    successorListenerThread = new Thread(new SuccessorListener(connectionContext, ringMutator));
                    successorListenerThread.start();
                    predecessorListenerThread = new Thread(new PredecessorListener(connectionContext, ringMutator));
                    predecessorListenerThread.start();
                    break;
                
                case "":
                    // Ignore empty input
                    break;
    
                default:
                    System.out.println("Unknown command. Type 'help' for a list of commands.");
            }
        }
        scanner.close();
        System.out.println("CLI shutting down.");
    }

    public static class RunningFlag {
        public volatile boolean running = true;
    }
}
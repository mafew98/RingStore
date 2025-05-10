package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientListener implements Runnable {
    //1. start server socket to accept connections from clients
    //2. put request into the queue ordered by the sequence id of the message
    //3. Writer thread that writes to the others
    private Integer clientListenerPort=24942;
    ServerSocket clientListenerSocket;
    ConnectionContext connectionContext;
    DataStore dataStore;
    RingManager.RunningFlag runningFlag;

    public ClientListener(ConnectionContext connectionContext, RingManager.RunningFlag runningFlag) throws IOException{
        this.connectionContext = connectionContext;
        this.runningFlag = runningFlag;
        this.connectionContext = connectionContext;
        clientListenerSocket = new ServerSocket(clientListenerPort);
        connectionContext.setClientSocketServer(clientListenerSocket);
        this.dataStore = connectionContext.getDataStore();
    }

    @Override
    public void run() {
        try {    
            while(runningFlag.running) {
                // Accept any client access request
                Socket clientSocket = clientListenerSocket.accept();
                System.out.println("Connection established from: " + clientSocket.getInetAddress());

                BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String rawMessageContent;
                
                while ((rawMessageContent = clientReader.readLine()) != null) {
                    Message message = new Message(rawMessageContent);
                    if ((message.getMessageType()).equals("R")) {
                        // Directly handle reads
                        String storedValue = dataStore.readData(Integer.parseInt(message.getMessageContent()));
                        PrintWriter clientWriter = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                        clientWriter.println(String.format("Key %d : Value %s at Server %d", message.getMessageContent(), storedValue, ConnectionContext.getNodeID()));
                    }
                    else if ((message.getMessageType()).equals("W")) {
                        // Place writes in the write queue
                        connectionContext.getWriteQueue().addMessageToQueue(message);
                    }
                }
            } 
        } catch (NumberFormatException | IOException e) {
            e.printStackTrace();
        }
        
    }
}

package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class NeighborListener implements Runnable{
    private ConnectionContext connectionContext;
    private RingManager.RunningFlag runningFlag;

    public NeighborListener(ConnectionContext connectionContext,  RingManager.RunningFlag runningFlag) {
        this.connectionContext = connectionContext;
        this.runningFlag = runningFlag;
    }

    @Override
    public void run() {
        while(runningFlag.running) {
            int NodeId = connectionContext.getNodeID();
            int TOTAL_SERVERS = connectionContext.getMaxServers();
            int successorNode = (NodeId + 1) % TOTAL_SERVERS;
            int predecessorNode = (NodeId - 1 + TOTAL_SERVERS) % TOTAL_SERVERS;

            BufferedReader predReader = connectionContext.getInputReader(predecessorNode);
            String rawMessageContent;
            try {
                while ((rawMessageContent = predReader.readLine()) != null) {
                    Message message = new Message(rawMessageContent);
                    if ((message.getMessageType()).equals("W")) {
                        // Directly do the write with priority
                        String[] KVPair = message.getMessageContent().split(":",2);
                        connectionContext.getDataStore().writeData(Integer.parseInt(KVPair[0]), KVPair[1]);
                        // Forward the message
                        if (message.getReplicationFactor() > 1) {
                            PrintWriter sucWriter = connectionContext.getOuputWriter(successorNode);
                            sucWriter.println(message.getForwardMessage());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

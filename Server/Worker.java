package Server;

import java.io.PrintWriter;

import Server.WriteQueue.WriteQueue;

public class Worker implements Runnable{
    private WriteQueue writeQueue;
    private ConnectionContext connectionContext;
    private RingManager.RunningFlag runningFlag;

    public Worker(ConnectionContext connectionContext, RingManager.RunningFlag runningFlag) {
        this.connectionContext = connectionContext;
        this.runningFlag = runningFlag;
    }

    @Override
    public void run() {
        int TOTAL_SERVERS = connectionContext.getMaxServers();
        int successorNode = (ConnectionContext.getNodeID() + 1) % TOTAL_SERVERS;
        while(runningFlag.running) {
            try {
                if (writeQueue.peekWriteQueue() != null) {
                    // Handle the write to the current copy
                    Message topMessage = writeQueue.pollWriteQueue();
                    String[] KVPair = topMessage.getMessageContent().split(":",2);
                    connectionContext.getDataStore().writeData(Integer.parseInt(KVPair[0]), KVPair[1]);
                    // Forward the message
                    if (topMessage.getReplicationFactor() > 1) {
                        PrintWriter sucWriter = connectionContext.getOuputWriter(successorNode);
                        sucWriter.println(topMessage.getForwardMessage());
                    }
                } else {
                    Thread.sleep(3);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}

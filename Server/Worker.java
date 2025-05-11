package Server;

import java.io.IOException;
import java.io.PrintWriter;

public class Worker implements Runnable{
    private WriteQueue writeQueue;
    private ConnectionContext connectionContext;
    private RingManager.RunningFlag runningFlag;

    public Worker(ConnectionContext connectionContext, RingManager.RunningFlag runningFlag) {
        this.connectionContext = connectionContext;
        this.runningFlag = runningFlag;
        this.writeQueue = connectionContext.getWriteQueue();
    }

    @Override
    public void run() {
        int TOTAL_SERVERS = connectionContext.getMaxServers();
        int successorNode;
        while(runningFlag.running) {
            try {
                if (writeQueue.peekWriteQueue() != null) {
                    successorNode = connectionContext.getSuccessor();
                    // Handle the write to the current copy
                    Message topMessage = writeQueue.pollWriteQueue();
                    String[] KVPair = topMessage.getMessageContent().split(":",2);
                    connectionContext.getDataStore().writeData(Integer.parseInt(KVPair[0]), KVPair[1], topMessage.getNodeNumber());

                    // Forward the message
                    int messageNodeNumber = topMessage.getNodeNumber();
                    int potSecondaryNode = ((messageNodeNumber + 1) % TOTAL_SERVERS);
                    int potTertiaryNode = ((messageNodeNumber + 2) % TOTAL_SERVERS);
                    if (successorNode ==  potSecondaryNode || successorNode == potTertiaryNode) {
                        PrintWriter sucWriter = connectionContext.getOutputWriter(successorNode);
                        sucWriter.println(topMessage.getForwardMessage());
                    }
                } else {
                    Thread.sleep(3);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

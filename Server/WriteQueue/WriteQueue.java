package Server.WriteQueue;

import java.util.concurrent.PriorityBlockingQueue;

import Server.Message;

public class WriteQueue {
   
    protected PriorityBlockingQueue<Message> writeQueue; // Shared message queue

    public WriteQueue() {
        this.writeQueue = new PriorityBlockingQueue<>(500, new WriteComparator());;
    }

    /**
     * Utility process to adds messages received to the common priority queue. The
     * priority queue is blocking to ensure thread safety.
     * 
     * @param message
     */
    public void addMessageToQueue(Message message) {
        writeQueue.add(message);
    }

    /**
     * Utility method to poll the message queue
     * 
     * @return Message
     */
    public Message pollWriteQueue() {
        return writeQueue.poll();
    }

    /**
     * Utility method to peek at the message queue
     * 
     * @return Message
     */
    public Message peekWriteQueue() {
        return writeQueue.peek();
    }

    /**
     * Utility method to check if the message queue is empty
     * 
     * @return boolean
     */
    public boolean isWriteQueueEmpty() {
        return writeQueue.isEmpty();
    }

}

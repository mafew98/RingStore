package totalbroadcast;

import java.util.HashSet;
import java.util.concurrent.PriorityBlockingQueue;

public class MessageQueue {
    protected PriorityBlockingQueue<Message> messageQueue; // Shared message queue

    public MessageQueue(int initCapacity) {
        this.messageQueue = new PriorityBlockingQueue<>(initCapacity, new MessageComparator());
    }

    /**
     * Utility process to adds messages received to the common priority queue. The
     * priority queue is blocking to ensure thread safety.
     * 
     * @param message
     */
    public void addMessageToQueue(Message message) {
        messageQueue.add(message);
    }

    /**
     * Utility method to poll the message queue
     * 
     * @return Message
     */
    public Message pollMessageQueue() {
        return messageQueue.poll();
    }

    /**
     * Utility method to peek at the message queue
     * 
     * @return Message
     */
    public Message peekMessageQueue() {
        return messageQueue.peek();
    }

    /**
     * Utility method to check if the message queue is empty
     * 
     * @return boolean
     */
    public boolean isMessageQueueEmpty() {
        return messageQueue.isEmpty();
    }

    /**
     * Method that removes a sequenced message from the message queue after
     * delivery.
     * 
     * @param elements
     */
    public void removeAllSetElements(HashSet<Message> elements) {
        for (Message message : elements) {
            this.messageQueue.remove(message);
        }
    }
}

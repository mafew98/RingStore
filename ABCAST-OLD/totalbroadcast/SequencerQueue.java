package totalbroadcast;

import java.util.Comparator;
import java.util.HashSet;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * The Sequencer Queue is a priority blocking queue used by non-sequencer nodes
 * to store and order sequenced messages that it receives from the elected
 * sequencer.
 */
public class SequencerQueue {

    private PriorityBlockingQueue<SequencedMessage> sequencedQueue = new PriorityBlockingQueue<SequencedMessage>(500,
            new SequenceComparator());

    /**
     * Method to add sequenced message to the sequencer queue
     * 
     * @param sequencedMessage
     */
    public void addMessageToQueue(SequencedMessage sequencedMessage) {
        this.sequencedQueue.add(sequencedMessage);
    }

    /**
     * Method to peek at the sequenced messages queue
     * 
     * @return
     */
    public SequencedMessage peekSequenceQueue() {
        return sequencedQueue.peek();
    }

    /**
     * Method to poll the sequencer queue
     * 
     * @return
     */
    public SequencedMessage pollSequenceQueue() {
        return sequencedQueue.poll();
    }

    /**
     * check if the sequeunced queue is empty
     * 
     * @return
     */
    public boolean isEmpty() {
        return this.sequencedQueue.isEmpty();
    }

    /**
     * Returns the sequencer as a hash set. Used for quick comparisons
     */
    public HashSet<Message> sequencedSet() {
        HashSet<Message> hashSet = new HashSet<>();
        for (SequencedMessage sequencedMessage : sequencedQueue) {
            hashSet.add(sequencedMessage.getSequencedMessage());
        }
        return hashSet;
    }

}

/**
 * Comparator class used by the priority queue to order sequenced messages based
 * on sequenceNumber
 */
class SequenceComparator implements Comparator<SequencedMessage> {
    @Override
    public int compare(SequencedMessage sm1, SequencedMessage sm2) {
        return Integer.compare(sm1.getSequenceNumber(), sm2.getSequenceNumber()); // Ascending order
    }
}

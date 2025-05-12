package Client;

import java.util.Comparator;
/**
 * Comparator used to order messages based on their vector clocks.
 * In case of concurrent clocks, node IDs are used for tie-breaking.
 */
public class MessageComparator implements Comparator<Message> {
    @Override
    public int compare(Message m1, Message m2) {
        return Integer.compare(m1.getSeqNo(), m2.getSeqNo()); // Total ordering based on sequencer-assigned number
    }
}


package Server;

import java.util.Comparator;

public class WriteComparator implements Comparator<Message> {
    /**
     * Compares two messages. Initially, vector clocks are compared. If concurrent,
     * nodeIDs are taken for tie breaking.
     * 
     * @param m1
     * @param m2
     * @return int (-1, 0, 1)
     */
    @Override
    public int compare(Message m1, Message m2) {
        return Integer.compare(m1.getMessageOrderNo(), m2.getMessageOrderNo());
    }
}
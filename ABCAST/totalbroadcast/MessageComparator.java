package totalbroadcast;

import java.util.Comparator;

public class MessageComparator implements Comparator<Message> {
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
        VectorClock v1 = m1.getMessageClock();
        VectorClock v2 = m2.getMessageClock();
        int compRes = v1.compare(v2);
        if (compRes == 0) {
            return Integer.compare(m1.getNodeId(), m2.getNodeId());
        }
        return compRes;
    }
}

package Client;

import java.util.Comparator;

public class MessageComparator implements Comparator<Message> {
    @Override
    public int compare(Message m1, Message m2) {
        return Integer.compare(m1.getSeqNo(), m2.getSeqNo()); // Total ordering based on sequencer-assigned number
    }
}


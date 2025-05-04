package totalbroadcast;

import java.util.concurrent.atomic.AtomicIntegerArray;

public class VectorClock {
    private AtomicIntegerArray vectorClock;

    /**
     * Constructor to create a new vector clock.
     * 
     * @param NumOfProcesses
     */
    public VectorClock(int NumOfProcesses) {
        // Creates the vector clock and initializes the components to 0.
        this.vectorClock = new AtomicIntegerArray(NumOfProcesses);
    }

    /**
     * Constructor used when an array representation of vector clock is used. Mainly
     * used to handle message timestamps.
     * 
     * @param clockRepresentation
     */
    public VectorClock(String clockRepresentation) throws NumberFormatException {
        // Attempt to convert the representative clock string to an integer array
        int[] clockArray = new int[5];
        String[] messageParts = clockRepresentation.split(",", 5);

        for (int i = 0; i < 5; i++) {
            clockArray[i] = Integer.parseInt(messageParts[i]);
        }
        this.vectorClock = new AtomicIntegerArray(clockArray);
    }

    /**
     * Sets the vector clock to a new value.
     * 
     * @param clockVector
     */
    public synchronized void setClock(int[] clockVector) {
        this.vectorClock = new AtomicIntegerArray(clockVector);
    }

    /**
     * Increment a component of the clock.
     * 
     * @param processId
     */
    public synchronized void increment(int processId) {
        vectorClock.set(processId - 1, vectorClock.get(processId - 1) + 1);
    }

    /**
     * Get a component of the vector clock. Note that components go from 0 to
     * (MAX_PROCESSES - 1)
     * 
     * @param index
     * @return
     */
    public int getComponent(int index) {
        return vectorClock.get(index);
    }

    /**
     * Sets a vector clock component.
     * 
     * @param index
     * @param Value
     */
    public void setComponent(int index, int Value) {
        this.vectorClock.set(index, Value);
    }

    /**
     * Merge the received vector clock into the current clock
     * 
     * @param otherClock
     */
    public synchronized void merge(VectorClock otherClock) {
        for (int i = 0; i < vectorClock.length(); i++) {
            this.setComponent(i, Math.max(this.getComponent(i), otherClock.getComponent(i)));
        }
    }

    /**
     * Component wise comparison of two vector clocks. Returns -1, 0, 1 if happened
     * before, concurrent or happened after.
     * Represents the current clock's order with respect to the other clock
     * 
     * @param otherClock
     * @return
     */
    public int compare(VectorClock otherClock) {
        boolean less = false, greater = false;

        for (int i = 0; i < vectorClock.length(); i++) {
            if (this.getComponent(i) < otherClock.getComponent(i)) {
                less = true;
            } else if (this.getComponent(i) > otherClock.getComponent(i)) {
                greater = true;
            }
        }
        if (less && !greater)
            return -1;
        if (!less && greater)
            return 1;
        return 0;
    }

    /**
     * Check if a message can be delivered (i.e., check causal ordering).
     * This is a partial ordering check only. Total ordering is enforced in the
     * message class.
     * 
     * @param receivedClock
     * @param NodeId
     * @return
     */
    public synchronized boolean canDeliver(VectorClock receivedClock, int NodeId, int currentNodeId) {
        // Condition 1: C[b] = M[b] - 1 (b is receiver's process ID)
        if ((currentNodeId == NodeId) && (this.getComponent(NodeId - 1) != receivedClock.getComponent(NodeId - 1))) {
            return false;
        } else if ((currentNodeId != NodeId)
                && this.getComponent(NodeId - 1) != receivedClock.getComponent(NodeId - 1) - 1) {
            return false;
        }
        // Condition 2: C[k] >= M[k] for all other components
        for (int i = 0; i < vectorClock.length(); i++) {
            if (i != (NodeId - 1) && (this.getComponent(i) < receivedClock.getComponent(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * To get the string representation of the current vector clock.
     * Converts from [1,2,3,4] -> "1,2,3,4"
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(vectorClock.get(0));
        for (int i = 1; i < vectorClock.length(); i++) {
            sb.append(',');
            sb.append(vectorClock.get(i));
        }
        return sb.toString();
    }
}

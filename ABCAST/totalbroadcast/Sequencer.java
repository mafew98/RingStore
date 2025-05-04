package totalbroadcast;

public class Sequencer implements Runnable {
    public boolean isSequencer = false; // Sequencer Token
    public int sequenceNo;
    private ConnectionContext connectionContext;
    private MessageQueue messageQueue;
    private SequencerQueue sequencerQueue;
    private VectorClock vectorClock;
    private int DELIVERY_COUNT = 0;
    private final int TOTAL_MESSAGES = 500;

    public Sequencer(boolean isSequencer, ConnectionContext connectionContext) {
        // First time initialization
        this.sequenceNo = 0;
        if (isSequencer) {
            this.isSequencer = true;
        }
        this.connectionContext = connectionContext;
        this.messageQueue = connectionContext.getMessageQueue();
        this.vectorClock = connectionContext.getVectorClock();
        this.sequencerQueue = connectionContext.getSequencerQueue();
    }

    /**
     * Main sequencer method
     */
    @Override
    public void run() {
        while (DELIVERY_COUNT < TOTAL_MESSAGES) {
            if (isSequencer) {
                processAppMessages();
            } else {
                processSequenceMessages();
            }
        }
        System.out.println("Total Number of Messages Received: " + DELIVERY_COUNT);
    }

    /**
     * Method to process all application messages to sequence them.
     * Only need to check the top since these are already set according to priority.
     * If top isnt deliverable, the ones after are also not deliverable.
     */
    private void processAppMessages() {
        while (messageQueue.peekMessageQueue() != null && isDeliverable(messageQueue.peekMessageQueue())) {
            Message topMessage = messageQueue.pollMessageQueue();
            // deliver the message
            deliverMessage(topMessage);
            // sequence the message and put in Broadcast Queue
            sequenceNo += 1;
            connectionContext
                    .addToSequencedBroadcastQueue(SequencedMessage.createSequencedRawMessage(topMessage, sequenceNo));
            ;
        }
    }

    /**
     * Method to process and sequence received sequenced messages
     */
    private void processSequenceMessages() {
        if (!sequencerQueue.isEmpty()) {
            while (!sequencerQueue.isEmpty()) {
                SequencedMessage topSequencedMessage = sequencerQueue.pollSequenceQueue();
                if (sequenceNo == (topSequencedMessage.getSequenceNumber() - 1)) {
                    deliverMessage(topSequencedMessage.getSequencedMessage());
                    sequenceNo++;
                } else {
                    System.out.println("Cannot Sequence this message");
                }
            }
            messageQueue.removeAllSetElements(sequencerQueue.sequencedSet());
        }
    }

    /**
     * Compares the message at the top of the queue with the current vector clock to
     * determine if it is deliverable.
     * Synchronized method since multiple threads may access this.
     * 
     * @param message
     * @return boolean
     */
    private synchronized boolean isDeliverable(Message message) {
        return vectorClock.canDeliver(message.getMessageClock(), message.getNodeId(), connectionContext.getNodeId());
    }

    /**
     * Increments the vector clock to indicate delivery and delivers a message.
     * 
     * @param message
     */
    private synchronized void deliverMessage(Message message) {
        System.out.println(
                "Delivered: " + Message.createRawMessage(message.getMessageContent(), message.getMessageClock()));
        vectorClock.merge(message.getMessageClock());
        DELIVERY_COUNT++;
    }
}

package Client;
/**
 * The Message class represents the core message structure exchanged between clients,
 * the sequencer, and servers. Each message encapsulates either a READ or WRITE operation.
 * Format:
 * <Type>,<SequenceNumber>,<RetryFactor>,<MessageContent>
 *   - Type: R or W
 *   - SequenceNumber: total order position (assigned by sequencer)
 *   - RetryFactor (RF): retry attempts remaining
 *   - MessageContent: "key:value" for writes or "key" for reads
 *
 * @author Matthew George
 * @author Yukta Shah
 */
public class Message {

    public enum MessageType {
        R, W
    }

    private MessageType type;
    private int seqNo;
    private int rf;
    private String msgContent; // e.g., "1:Yukta" or "1"
    private int senderNodeId;

    /**
     * Constructs a Message by parsing a raw message string.
     * Assumes format: <Type>,<SeqNo>,<RF>,<msgContent>
     *
     * @param rawMessage the raw message string to parse
     */
    public Message(MessageType type, int seqNo, int rf, String msgContent, int senderNodeId) {
        this.type = type;
        this.seqNo = seqNo;
        this.rf = rf;
        this.msgContent = msgContent;
        this.senderNodeId = senderNodeId;
    }

    /**
     * Constructs a Message 
     *
     * @param rawMessage the raw message string
     */
    public Message(String rawMessage) {
        String[] parts = rawMessage.split(",", 4);
        this.type = MessageType.valueOf(parts[0]);
        this.seqNo = Integer.parseInt(parts[1]);
        this.rf = Integer.parseInt(parts[2]);
        this.msgContent = parts[3];
        this.senderNodeId = -1; // Optional; you can set it after construction
    }

    /**
     * Serializes this Message object back into string format.
     *
     * @return the message in wire format
     */
    @Override
    public String toString() {
        return type + "," + seqNo + "," + rf + "," + msgContent;
    }

    /**
     * Returns the type of the message (R or W).
     *
     * @return message type
     */

    public MessageType getType() {
        return type;
    }

    /**
     * Returns the sequence number of this message.
     *
     * @return sequence number
     */

    public int getSeqNo() {
        return seqNo;
    }

    /**
     * Returns the retry factor (RF) of the message.
     *
     * @return retry factor
     */

    public int getRf() {
        return rf;
    }

    /**
     * Returns the message content field (key or key:value).
     *
     * @return message content
     */

    public String getMsgContent() {
        return msgContent;
    }

    /**
     * Returns the sender node ID.
     *
     * @return sender node ID
     */

    public int getSenderNodeId() {
        return senderNodeId;
    }

    /**
     * Sets the retry factor (RF) of this message.
     *
     * @param rf new retry factor
     */

    public void setRf(int rf) {
        this.rf = rf;
    }

    /**
     * Sets the sequence number of this message.
     *
     * @param seqNo new sequence number
     */

    public void setSeqNo(int seqNo) {
        this.seqNo = seqNo;
    }

    /**
     * Sets the sender node ID for this message.
     *
     * @param nodeId sender node ID
     */

    public void setSenderNodeId(int senderNodeId) {
        this.senderNodeId = senderNodeId;
    }

    // Retry logic: reduce RF by 1
    public boolean retryAllowed() {
        return rf > 0;
    }

    public void decrementRf() {
        if (rf > 0) rf--;
    }

    // Utility: extract key from msgContent (before ':')
    public String getKey() {
        return msgContent.split(":")[0];
    }

    // Utility: extract value from msgContent (after ':') if present
    public String getValue() {
        if (msgContent.contains(":")) {
            return msgContent.split(":", 2)[1];
        } else {
            return null;
        }
    }
}

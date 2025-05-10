package Client;

public class Message {

    public enum MessageType {
        R, W
    }

    private MessageType type;
    private int seqNo;
    private int rf;
    private String msgContent; // e.g., "1:Yukta" or "1"
    private int senderNodeId;

    // Constructor from individual fields
    public Message(MessageType type, int seqNo, int rf, String msgContent, int senderNodeId) {
        this.type = type;
        this.seqNo = seqNo;
        this.rf = rf;
        this.msgContent = msgContent;
        this.senderNodeId = senderNodeId;
    }

    // Constructor from raw message string
    public Message(String rawMessage) {
        String[] parts = rawMessage.split(",", 4);
        this.type = MessageType.valueOf(parts[0]);
        this.seqNo = Integer.parseInt(parts[1]);
        this.rf = Integer.parseInt(parts[2]);
        this.msgContent = parts[3];
        this.senderNodeId = -1; // Optional; you can set it after construction
    }

    // Converts to string for transmission
    @Override
    public String toString() {
        return type + "," + seqNo + "," + rf + "," + msgContent;
    }

    // Getters
    public MessageType getType() {
        return type;
    }

    public int getSeqNo() {
        return seqNo;
    }

    public int getRf() {
        return rf;
    }

    public String getMsgContent() {
        return msgContent;
    }

    public int getSenderNodeId() {
        return senderNodeId;
    }

    // Setters
    public void setRf(int rf) {
        this.rf = rf;
    }

    public void setSeqNo(int seqNo) {
        this.seqNo = seqNo;
    }

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

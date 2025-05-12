package Server;

public class Message {
    private String type;
    private Integer messageOrderNo;
    private Integer nodeNumber;
    private String messageContent;

    /**
     * Message Constructor to handle raw messages.
     * A Raw message will be of the form "Type,SeqNo,NodeNo,key:value"
     * 
     * @param rawMessage
     */
    public Message(String rawMessage) {
        String[] messageParts = rawMessage.split(",", 4);
        this.type = messageParts[0];
        this.messageOrderNo = (messageParts[1].isEmpty())? 0 : Integer.parseInt(messageParts[1]);
        this.nodeNumber = Integer.parseInt(messageParts[2]);
        this.messageContent = messageParts[3];
    }

    /**
     * Getting message content
     * 
     * @return
     */
    public String getMessageContent() {
        return this.messageContent;
    }

    /**
     * Gets the ID of the source node of the message.
     * 
     * @return
     */
    public Integer getMessageOrderNo() {
        return this.messageOrderNo;
    }

    public int getNodeNumber() {
        return this.nodeNumber;
    }

    public String getMessageType() {
        return this.type;
    }

    public String getForwardMessage() {
        return String.format("%s,%d,%d,%s", type, messageOrderNo, nodeNumber, messageContent);
    }
}

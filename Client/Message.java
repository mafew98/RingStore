package Client;

public class Message {
    public enum MessageType {
        READ,
        WRITE
    }
    private String messageContent;
    private VectorClock messageClock;
    private Integer NodeId;
    private MessageType type;
    private Integer targetServer;

    /**
     * Message Constructor to handle raw messages.
     * A Raw message will be of the form "X,X,X,X: Message no. Y from Node Z"
     * 
     * @param rawMessage
     * @param NodeId
     */
    public Message(String rawMessage, Integer NodeId) {
        String[] messageParts = rawMessage.split(":", 4);
        this.messageClock = new VectorClock(messageParts[0]);
        this.type = MessageType.valueOf(messageParts[1]);
        this.targetServer = (messageParts[2] == null || messageParts[2].isEmpty()) ? null : Integer.parseInt(messageParts[2]);
        this.messageContent = messageParts[3];
        this.NodeId = NodeId;
    }

    /**
     * Message Constructor to handle raw messages and guess the node ID
     * A Raw message will be of the form "X,X,X,X: Message no. Y from Node Z"
     * 
     * @param rawMessage
     * @param NodeId
     */
    public Message(String rawMessage) throws NumberFormatException {
        String[] messageParts = rawMessage.split(":", 4);
        this.messageClock = new VectorClock(messageParts[0]);
        this.type = MessageType.valueOf(messageParts[1]);
        this.targetServer = (messageParts[2] == null || messageParts[2].isEmpty()) ? null : Integer.parseInt(messageParts[2]);
        this.messageContent = messageParts[3];
        int index = messageContent.indexOf("from Node ");
        if (index != -1) {
            this.NodeId = Integer.parseInt(messageContent.substring(index + 10).trim()); // Extract Z
        }
    }

    /**
     * Constructor creating a new message.
     * 
     * @param messageContent
     * @param messageClock
     * @param NodeId
     */
    public Message(String messageContent, VectorClock messageClock, int NodeId, MessageType type, Integer targetServer) {
        this.messageContent = messageContent;
        this.messageClock = messageClock;
        this.NodeId = NodeId;
        this.type = type;
        this.targetServer = targetServer;
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
     * Getting message clock (Not the same as current node's vector clock)
     * 
     * @return
     */
    public VectorClock getMessageClock() {
        return this.messageClock;
    }

    /**
     * Gets the ID of the source node of the message.
     * 
     * @return
     */
    public Integer getNodeId() {
        return this.NodeId;
    }

    public MessageType getType() {
        return this.type;
    }

    public Integer getTargetServer() {
        return this.targetServer;
    }
    /**
     * Static method to create a raw message.
     * 
     * @param messageContent
     * @param messageClock
     * @return
     */
    public static String createRawMessage(String messageContent, VectorClock messageClock, MessageType type, Integer targetServer) {
        String target = (targetServer == null) ? "" : targetServer.toString();
        return messageClock.toString() + ":" + type.name() + ":" + target + ":" + messageContent;
    }

    // Backward compatibility for existing code
public static String createRawMessage(String messageContent, VectorClock messageClock) {
    return createRawMessage(messageContent, messageClock, MessageType.WRITE, null); // Default to WRITE with no target
}

}

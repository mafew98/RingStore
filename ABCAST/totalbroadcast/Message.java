package totalbroadcast;

public class Message {
    private String messageContent;
    private VectorClock messageClock;
    private Integer NodeId;

    /**
     * Message Constructor to handle raw messages.
     * A Raw message will be of the form "X,X,X,X: Message no. Y from Node Z"
     * 
     * @param rawMessage
     * @param NodeId
     */
    public Message(String rawMessage, Integer NodeId) {
        String[] messageParts = rawMessage.split(":", 2);
        this.messageClock = new VectorClock(messageParts[0]);
        this.messageContent = messageParts[1];
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
        String[] messageParts = rawMessage.split(":", 2);
        this.messageClock = new VectorClock(messageParts[0]);
        this.messageContent = messageParts[1];
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
    public Message(String messageContent, VectorClock messageClock, int NodeId) {
        this.messageContent = messageContent;
        this.messageClock = messageClock;
        this.NodeId = NodeId;
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

    /**
     * Static method to create a raw message.
     * 
     * @param messageContent
     * @param messageClock
     * @return
     */
    public static String createRawMessage(String messageContent, VectorClock messageClock) {
        return (messageClock.toString() + ":" + messageContent);
    }
}

package totalbroadcast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SequencedMessage {
    private int messageSequenceNo;
    private Message sequencedMessage;

    // constructor for sequenced message components
    public SequencedMessage(int messageSequenceNo, Message sequencedMessage) {
        this.sequencedMessage = sequencedMessage;
        this.messageSequenceNo = messageSequenceNo;
    }

    // constructor for rawMessage ie, sequenced message as whole string.
    public SequencedMessage(String rawMessage) {
        String[] messageParts = rawMessage.split("-", 2);
        this.messageSequenceNo = Integer.parseInt(messageParts[0]);
        this.sequencedMessage = new Message(messageParts[1]);
    }

    /**
     * Getter that returns sequence number of a message
     * 
     * @return
     */
    public int getSequenceNumber() {
        return this.messageSequenceNo;
    }

    /**
     * Returns the sequenced message
     * 
     * @return
     */
    public Message getSequencedMessage() {
        return this.sequencedMessage;
    }

    /**
     * Checker method to determine is a message is sequenced. Message must be a raw
     * message (whole message as string. not Message object)
     * 
     * @param message
     * @return
     */
    public static boolean isSequencedMessage(String message) {
        String regex = "(\\d+)\\-(.*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(message);
        return matcher.matches();
    }

    /**
     * Method to convert a sequenced message to string (raw message)
     */
    public String toString() {
        return Integer.toString(this.messageSequenceNo) + "-"
                + Message.createRawMessage(sequencedMessage.getMessageContent(), sequencedMessage.getMessageClock());
    }

    /**
     * Static method to create a raw sequenced message
     * 
     * @param message
     * @param sequenceNo
     * @return
     */
    public static String createSequencedRawMessage(Message message, int sequenceNo) {
        return (sequenceNo + "-" + Message.createRawMessage(message.getMessageContent(), message.getMessageClock()));
    }
}

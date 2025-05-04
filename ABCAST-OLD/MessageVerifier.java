import java.io.*;
import java.util.*;

public class MessageVerifier {
    private static final Map<Integer, Integer> lastDelivered = new HashMap<>();
    static boolean correctOrder = true;
    static boolean correctDeferral = true;

    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.print("Enter filePath: ");
        String filePath = scanner.nextLine();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Delivered:")) {
                    processDelivered(line);
                } else if (line.startsWith("Unable to deliver")) {
                    processDeferred(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (correctOrder) {
                System.out.println("Messages for each node received in order!");
            }
            if (correctDeferral) {
                System.out.println("Deferred messages handled correctly");
            }
        }
    }

    private static void processDelivered(String line) {
        // System.out.println("Processing delivered message: " + line); // Debugging
        // Extract the message number and node ID
        try {
            String[] parts = line.split("Message no\\.");
            if (parts.length < 2)
                return; // Ignore malformed lines

            String[] messageParts = parts[1].trim().split(" from ");
            int messageNumber = Integer.parseInt(messageParts[0].trim());
            int nodeId = Integer.parseInt(messageParts[1].trim());

            // Ensure messages from the same node are delivered in increasing order
            lastDelivered.putIfAbsent(nodeId, 0);
            if (messageNumber <= lastDelivered.get(nodeId)) {
                System.err.println("Error: Out-of-order delivery detected! Node " + nodeId +
                        " delivered " + messageNumber + " after " + lastDelivered.get(nodeId));
                correctOrder = false;
            }

            // Update last delivered message for this node
            lastDelivered.put(nodeId, messageNumber);

        } catch (Exception e) {
            System.err.println("Failed to parse line: " + line);
            e.printStackTrace();
        }
    }

    private static void processDeferred(String line) {
        // System.out.println("Processing deferred message: " + line); // Debugging

        try {
            // Extract vector clocks and node ID
            String[] parts = line.split(":Message no\\.");
            if (parts.length < 2)
                return;

            String[] clockParts = parts[0].replace("Unable to deliver ", "").split(":");
            String[] systemClockParts = parts[1].split("Current Clock: ");

            int[] msgClock = parseVectorClock(clockParts[0]);
            int[] sysClock = parseVectorClock(systemClockParts[1]);

            String[] messageParts = systemClockParts[0].trim().split(" from ");
            int messageNumber = Integer.parseInt(messageParts[0].trim());
            // Fix: Remove any trailing non-numeric characters (e.g., `.`) from nodeId
            int nodeId = Integer.parseInt(messageParts[1].trim().replaceAll("\\D+$", ""));

            if (!isValidDeferred(msgClock, sysClock, nodeId)) {
                System.err.println("Error: Deferred message doesn't satisfy vector clock condition! Message: " + line);
                correctDeferral = false;
            }

        } catch (Exception e) {
            System.err.println("Failed to parse deferred message: " + line);
            e.printStackTrace();
        }
    }

    private static int[] parseVectorClock(String vectorClockStr) {
        return Arrays.stream(vectorClockStr.trim().split(","))
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    private static boolean isValidDeferred(int[] msgClock, int[] sysClock, int nodeId) {
        int nodeIndex = nodeId - 1; // Convert to 0-based index

        if (msgClock[nodeIndex] == sysClock[nodeIndex] + 1) {
            for (int i = 0; i < msgClock.length; i++) {
                if (i != nodeIndex && msgClock[i] > sysClock[i]) {
                    return true;
                }
            }
        } else if (msgClock[nodeIndex] > sysClock[nodeIndex] + 1) {
            return true;
        }
        return false;
    }
}
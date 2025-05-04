import java.io.*;
import java.util.*;

public class abVerifier {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java LogFileComparator <directory_path>");
            return;
        }

        File dir = new File(args[0]);
        if (!dir.isDirectory()) {
            System.out.println("Invalid directory: " + args[0]);
            return;
        }

        File[] logFiles = dir.listFiles((d, name) -> name.endsWith(".log"));
        if (logFiles == null || logFiles.length < 2) {
            System.out.println("Not enough log files found in the directory.");
            return;
        }

        List<List<String>> deliveredMessages = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();

        // Read "Delivered:" lines from each file
        for (File file : logFiles) {
            List<String> messages = extractDeliveredLines(file);
            if (messages.isEmpty()) {
                System.out.println("Warning: No 'Delivered:' lines found in " + file.getName());
            }
            deliveredMessages.add(messages);
            fileNames.add(file.getName());
        }

        // Compare the extracted sequences
        boolean identical = compareMessageSequences(deliveredMessages, fileNames);

        if (identical) {
            System.out.println("All log files have the 'Delivered:' messages in the same order.");
        } else {
            System.out.println("Mismatch detected in 'Delivered:' message order.");
        }
    }

    private static List<String> extractDeliveredLines(File file) {
        List<String> messages = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Delivered:")) {
                    messages.add(line.trim());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + file.getName());
            e.printStackTrace();
        }
        return messages;
    }

    private static boolean compareMessageSequences(List<List<String>> messageLists, List<String> fileNames) {
        List<String> reference = messageLists.get(0);
        String referenceFile = fileNames.get(0);
        boolean isIdentical = true;

        for (int i = 1; i < messageLists.size(); i++) {
            if (!reference.equals(messageLists.get(i))) {
                System.out.println("Mismatch found in file: " + fileNames.get(i));
                isIdentical = false;
            }
        }
        return isIdentical;
    }
}
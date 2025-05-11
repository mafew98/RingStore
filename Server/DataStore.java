package Server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class DataStore {
    private ConcurrentHashMap<Integer, String> content = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, List<Integer>> metadata = new ConcurrentHashMap<>();
    public DataStore() {
        this.content = new ConcurrentHashMap<>();
    }

    public void writeData(int key, String content, int hashServer) {
        this.content.put(key, content);
        System.out.println(String.format("Values Gotten: %d %s %d", key, content, hashServer));
        // Synchronize on metadata to ensure thread safety for list mutation
        synchronized (metadata) {
            List<Integer> keyList = metadata.get(hashServer);
            if (keyList == null) {
                keyList = new ArrayList<Integer>();
                metadata.put(hashServer, keyList);
            }
            keyList.add(key);
        }
    }

    public String readData(int key) {
        return this.content.get(key);
    }

    public String getContent() {
        return content.toString();
    }

    public String getDiffs(int targetNode) {
        // Static here
        int[] potvals = new int[] {
            targetNode,
            (targetNode - 1 + 7) % 7,
            (targetNode - 2 + 7) % 7
        };
        
        StringBuilder combinedResult = new StringBuilder();
        for (int serverNumber : potvals) {
            String partial = getContentPerServer(serverNumber);
            if (!partial.isEmpty()) {
                combinedResult.append(partial).append(",");
            }
        }

        // Remove trailing comma if present
        if (combinedResult.length() > 0) {
            combinedResult.setLength(combinedResult.length() - 1);
        }

        return combinedResult.toString();    
    }

    private String getContentPerServer(int serverNumber) {
        List<Integer> contentKeys = metadata.get(serverNumber);
        
        if (contentKeys == null || contentKeys.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (Integer key : contentKeys) {
            String value = content.get(key);
            if (value != null) {
                result.append(serverNumber).append("-").append(key).append("=").append(value).append(":");
            }
        }
        return result.toString();
    }

    public void addDiffs(String input) {
        if (input == null || input.isEmpty()) return;

        String[] entries = input.split(",");
        for (String entry : entries) {
            // Split into "serverNo-key" and "value"
            String[] keyValue = entry.split("=", 2);
            if (keyValue.length != 2) continue;

            String[] serverAndKey = keyValue[0].split("-", 2);
            if (serverAndKey.length != 2) continue;

            try {
                int serverNo = Integer.parseInt(serverAndKey[0]);
                int contentKey = Integer.parseInt(serverAndKey[1]);
                String contentValue = keyValue[1];

                // Update content map
                content.put(contentKey, contentValue);

                // Update metadata map
                List<Integer> keyList = metadata.get(serverNo);
                if (keyList == null) {
                    keyList = new ArrayList<Integer>();
                    metadata.put(serverNo, keyList);
                }
                keyList.add(contentKey);
            } catch (NumberFormatException e) {
                System.err.println("Invalid entry skipped: " + entry);
            }
        }
    }
    
}

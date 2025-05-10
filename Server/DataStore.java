package Server;

import java.util.concurrent.ConcurrentHashMap;

public class DataStore {
    ConcurrentHashMap<Integer, String> content;

    public DataStore() {
        this.content = new ConcurrentHashMap<>();
    }

    public void writeData(int key, String content) {
        this.content.put(key, content);
    }

    public String readData(int key) {
        return this.content.get(key);
    }
}

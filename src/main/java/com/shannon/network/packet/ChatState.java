package com.shannon.network.packet;

import java.util.ArrayList;
import java.util.List;

public class ChatState {
    public List<ChatMessage> messages = new ArrayList<>();

    public static class ChatMessage {
        public String sender;
        public String message;
        public long timestamp;

        public ChatMessage() {
        }

        public ChatMessage(String sender, String message, long timestamp) {
            this.sender = sender;
            this.message = message;
            this.timestamp = timestamp;
        }
    }
}

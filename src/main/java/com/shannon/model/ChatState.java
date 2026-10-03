package com.shannon.model;

import java.util.ArrayList;
import java.util.List;

/** The conversation with the bot, newest last. */
public class ChatState {
    /** The sender name the backend uses for the bot's own messages. */
    public static final String BOT_SENDER = "Shannon";

    public List<Message> messages = new ArrayList<>();

    public static class Message {
        /** {@link Kind#name()}; missing means {@link Kind#CHAT}. */
        public String kind;
        public String sender;
        public String message;
        public long timestamp;

        public Message() {
        }

        public Message(Kind kind, String sender, String message, long timestamp) {
            this.kind = kind.name();
            this.sender = sender;
            this.message = message;
            this.timestamp = timestamp;
        }

        public Kind kind() {
            if (kind == null) {
                return Kind.CHAT;
            }
            try {
                return Kind.valueOf(kind);
            } catch (IllegalArgumentException e) {
                return Kind.CHAT;
            }
        }

        public boolean fromBot() {
            return kind() == Kind.CHAT && BOT_SENDER.equals(sender);
        }
    }

    /** What a line in the conversation is. */
    public enum Kind {
        /** Something a player or the bot said. */
        CHAT,
        /** The bot started a task. */
        TASK_STARTED,
        /** The bot finished a task. */
        TASK_DONE,
        /** A task failed or was stopped. */
        TASK_ERROR
    }
}

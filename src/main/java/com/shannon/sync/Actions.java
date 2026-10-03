package com.shannon.sync;

/**
 * Every request the client can make, with its request body. Add a channel here and a handler in
 * {@code com.shannon.server.ServerActions} to add a new operation.
 */
public final class Actions {
    public static final ActionChannel<GiveItem> GIVE_ITEM = new ActionChannel<>("give_item", GiveItem.class);
    public static final ActionChannel<ToggleSkill> TOGGLE_SKILL = new ActionChannel<>("toggle_skill", ToggleSkill.class);
    public static final ActionChannel<Chat> CHAT = new ActionChannel<>("chat", Chat.class);
    public static final ActionChannel<TaskAction> TASK = new ActionChannel<>("task", TaskAction.class);
    public static final ActionChannel<Command> COMMAND = new ActionChannel<>("command", Command.class);
    public static final ActionChannel<ReactionUpdate> REACTION_UPDATE = new ActionChannel<>("reaction_update", ReactionUpdate.class);
    public static final ActionChannel<Empty> REACTION_RESET = new ActionChannel<>("reaction_reset", Empty.class);
    public static final ActionChannel<Empty> REQUEST_ADVANCEMENTS = new ActionChannel<>("request_advancements", Empty.class);
    public static final ActionChannel<Empty> VOICE_MODE = new ActionChannel<>("voice_mode", Empty.class);
    public static final ActionChannel<VoicePtt> VOICE_PTT = new ActionChannel<>("voice_ptt", VoicePtt.class);

    private Actions() {
    }

    public static class Empty {
    }

    /** Ask the bot to drop items from its inventory for the player. */
    public static class GiveItem {
        public String item;
        public int count;

        public GiveItem() {
        }

        public GiveItem(String item, int count) {
            this.item = item;
            this.count = count;
        }
    }

    public static class ToggleSkill {
        public String skillName;
        public boolean enabled;

        public ToggleSkill() {
        }

        public ToggleSkill(String skillName, boolean enabled) {
            this.skillName = skillName;
            this.enabled = enabled;
        }
    }

    public static class Chat {
        public String message;

        public Chat() {
        }

        public Chat(String message) {
            this.message = message;
        }
    }

    public static class TaskAction {
        public static final String DELETE = "delete";
        public static final String PRIORITIZE = "prioritize";

        public String action;
        public String taskId;

        public TaskAction() {
        }

        public TaskAction(String action, String taskId) {
            this.action = action;
            this.taskId = taskId;
        }
    }

    public static class Command {
        /** {@link BotCommand#name()}. */
        public String command;

        public Command() {
        }

        public Command(BotCommand command) {
            this.command = command.name();
        }
    }

    public static class ReactionUpdate {
        public String eventType;
        public boolean enabled;
        public int probability;
        /** Set only when the hostile detection distances change. */
        public Double criticalDistance;
        public Double detectionDistance;

        public ReactionUpdate() {
        }

        public ReactionUpdate(String eventType, boolean enabled, int probability) {
            this.eventType = eventType;
            this.enabled = enabled;
            this.probability = probability;
        }
    }

    public static class VoicePtt {
        public boolean pressed;

        public VoicePtt() {
        }

        public VoicePtt(boolean pressed) {
            this.pressed = pressed;
        }
    }
}

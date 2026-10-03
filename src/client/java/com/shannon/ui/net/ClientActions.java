package com.shannon.ui.net;

import com.shannon.sync.ActionChannel;
import com.shannon.sync.ActionPayload;
import com.shannon.sync.Actions;
import com.shannon.sync.BotCommand;
import com.shannon.ui.ShannonClient;
import com.shannon.ui.state.BotStatus;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

/**
 * Sends requests to the server.
 *
 * <p>Every call checks first that the server has this mod with the same protocol, so pressing a
 * key on a server without it does nothing instead of disconnecting the player.
 */
public final class ClientActions {
    private ClientActions() {
    }

    /** Whether the current server understands this mod's requests. */
    public static boolean available() {
        return ClientPlayNetworking.canSend(ActionPayload.ID);
    }

    public static <T> boolean send(ActionChannel<T> channel, T request) {
        if (!available()) {
            return false;
        }
        ClientPlayNetworking.send(ActionPayload.of(channel, request));
        return true;
    }

    public static boolean chat(String message) {
        boolean sent = send(Actions.CHAT, new Actions.Chat(message));
        if (sent) {
            noteSent(message);
        }
        return sent;
    }

    private static void noteSent(String message) {
        ShannonClient shannon = ShannonClient.get();
        if (shannon != null) {
            shannon.history().noteSent(message);
        }
    }

    /** How the bot recognises game chat addressed to it. */
    public static final String GAME_CHAT_PREFIX = "シャノン、";
    private static final int GAME_CHAT_LIMIT = 256;

    /**
     * Whether what the player types now goes to game chat: when the bot asked a question there,
     * the answer belongs in the same place, where everyone who saw the question sees the answer.
     */
    public static boolean talksInGameChat() {
        ShannonClient shannon = ShannonClient.get();
        return shannon != null && shannon.store().status() == BotStatus.WAITING;
    }

    /** Says {@code message} to the bot, in game chat when it is waiting for an answer, privately otherwise. */
    public static boolean talk(String message) {
        return talksInGameChat() ? sayInGameChat(message) : chat(message);
    }

    private static boolean sayInGameChat(String message) {
        ClientPlayNetworkHandler network = MinecraftClient.getInstance().getNetworkHandler();
        if (network == null) {
            return false;
        }
        String text = message.startsWith(GAME_CHAT_PREFIX) ? message : GAME_CHAT_PREFIX + message;
        network.sendChatMessage(text.length() > GAME_CHAT_LIMIT ? text.substring(0, GAME_CHAT_LIMIT) : text);
        noteSent(message);
        return true;
    }

    public static boolean command(BotCommand command) {
        return send(Actions.COMMAND, new Actions.Command(command));
    }

    public static boolean giveItem(String item, int count) {
        return send(Actions.GIVE_ITEM, new Actions.GiveItem(item, count));
    }

    public static boolean toggleSkill(String skillName, boolean enabled) {
        return send(Actions.TOGGLE_SKILL, new Actions.ToggleSkill(skillName, enabled));
    }

    public static boolean task(String action, String taskId) {
        return send(Actions.TASK, new Actions.TaskAction(action, taskId));
    }

    public static boolean requestAdvancements() {
        return send(Actions.REQUEST_ADVANCEMENTS, new Actions.Empty());
    }
}

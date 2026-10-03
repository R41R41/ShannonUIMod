package com.shannon.ui.net;

import com.shannon.sync.ActionChannel;
import com.shannon.sync.ActionPayload;
import com.shannon.sync.Actions;
import com.shannon.sync.BotCommand;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

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
        return send(Actions.CHAT, new Actions.Chat(message));
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

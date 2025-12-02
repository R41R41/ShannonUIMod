package com.shannon.state;

import com.shannon.network.packet.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 状態管理の中央クラス
 * 全ての状態を一元管理し、変更通知を提供
 */
public class StateManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(StateManager.class);
    private static StateManager instance;

    // 状態
    private TaskTreeState taskTreeState = new TaskTreeState();
    private DetailedLogsState logsState = new DetailedLogsState();
    private ConstantSkillsState skillsState = new ConstantSkillsState();
    private InventoryState inventoryState = new InventoryState();
    private ChatState chatState = new ChatState();

    // サーバーインスタンス
    private MinecraftServer server;

    // リスナー（将来的にリアクティブな更新に使用）
    private List<Consumer<StateType>> listeners = new ArrayList<>();

    public enum StateType {
        TASK_TREE,
        LOGS,
        SKILLS,
        INVENTORY,
        CHAT
    }

    private StateManager() {
        // シングルトン
    }

    public static StateManager getInstance() {
        if (instance == null) {
            instance = new StateManager();
        }
        return instance;
    }

    // ===== Setters =====

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public void updateTaskTreeState(TaskTreeState newState) {
        this.taskTreeState = newState;
        LOGGER.info("TaskTreeState updated: " + newState);
        notifyListeners(StateType.TASK_TREE);
        broadcastTaskTreeState();
    }

    public void updateLogsState(DetailedLogsState newState) {
        this.logsState = newState;
        LOGGER.info("DetailedLogsState updated: " + newState);
        notifyListeners(StateType.LOGS);
        broadcastLogsState();
    }

    public void updateSkillsState(ConstantSkillsState newState) {
        this.skillsState = newState;
        LOGGER.info("ConstantSkillsState updated: " + newState);
        notifyListeners(StateType.SKILLS);
        broadcastSkillsState();
    }

    public void updateInventoryState(InventoryState newState) {
        this.inventoryState = newState;
        notifyListeners(StateType.INVENTORY);
        broadcastInventoryState();
    }

    public void updateChatState(ChatState newState) {
        this.chatState = newState;
        LOGGER.info("ChatState updated: " + newState);
        notifyListeners(StateType.CHAT);
        broadcastChatState();
    }

    // ===== Getters =====

    public TaskTreeState getTaskTreeState() {
        return taskTreeState;
    }

    public DetailedLogsState getLogsState() {
        return logsState;
    }

    public ConstantSkillsState getSkillsState() {
        return skillsState;
    }

    public InventoryState getInventoryState() {
        return inventoryState;
    }

    public ChatState getChatState() {
        return chatState;
    }

    // ===== Broadcasting =====

    private void broadcastTaskTreeState() {
        if (taskTreeState == null || taskTreeState.goal == null || server == null) {
            return;
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, TaskTreeStatePacket.PACKET_ID)) {
                ServerPlayNetworking.send(player, new TaskTreeStatePacket(taskTreeState));
            }
        }
    }

    private void broadcastLogsState() {
        if (logsState == null || server == null) {
            return;
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, DetailedLogsStatePacket.PACKET_ID)) {
                ServerPlayNetworking.send(player, new DetailedLogsStatePacket(logsState));
            }
        }
    }

    private void broadcastSkillsState() {
        if (skillsState == null || skillsState.skills == null || server == null) {
            return;
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, ConstantSkillsStatePacket.PACKET_ID)) {
                ServerPlayNetworking.send(player, new ConstantSkillsStatePacket(skillsState));
            }
        }
    }

    private void broadcastInventoryState() {
        if (inventoryState == null || server == null) {
            return;
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, InventoryStatePacket.PACKET_ID)) {
                ServerPlayNetworking.send(player, new InventoryStatePacket(inventoryState));
            }
        }
    }

    private void broadcastChatState() {
        if (chatState == null || server == null) {
            return;
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, ChatStatePacket.PACKET_ID)) {
                ServerPlayNetworking.send(player, new ChatStatePacket(chatState));
            }
        }
    }

    // ===== Listeners (将来的な拡張用) =====

    public void addListener(Consumer<StateType> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<StateType> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(StateType type) {
        for (Consumer<StateType> listener : listeners) {
            listener.accept(type);
        }
    }

    // ===== Utility Methods =====

    /**
     * 全ての状態をクリア
     */
    public void clearAll() {
        taskTreeState = new TaskTreeState();
        logsState = new DetailedLogsState();
        skillsState = new ConstantSkillsState();
        inventoryState = new InventoryState();
        chatState = new ChatState();
        LOGGER.info("All states cleared");
    }
}

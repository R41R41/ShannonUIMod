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
    private TaskListStatePacket.TaskListState taskListState = new TaskListStatePacket.TaskListState();
    private DetailedLogsState logsState = new DetailedLogsState();
    private ConstantSkillsState skillsState = new ConstantSkillsState();
    private InventoryState inventoryState = new InventoryState();
    private ChatState chatState = new ChatState();
    private ReactionSettingsState reactionSettingsState = new ReactionSettingsState();

    // 選択中のタスクID
    private String selectedTaskId = null;

    // サーバーインスタンス
    private MinecraftServer server;

    // リスナー（将来的にリアクティブな更新に使用）
    private List<Consumer<StateType>> listeners = new ArrayList<>();

    public enum StateType {
        TASK_TREE,
        TASK_LIST,
        LOGS,
        SKILLS,
        INVENTORY,
        CHAT,
        REACTION_SETTINGS
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

    public void updateTaskListState(TaskListStatePacket.TaskListState newState) {
        this.taskListState = newState;
        String emergencyInfo = (newState != null && newState.emergencyTask != null)
                ? newState.emergencyTask.goal
                : "null";
        LOGGER.info("TaskListState updated: {} tasks, emergencyTask={}",
                (newState != null && newState.tasks != null ? newState.tasks.size() : 0),
                emergencyInfo);
        notifyListeners(StateType.TASK_LIST);
        broadcastTaskListState();
    }

    public TaskListStatePacket.TaskListState getTaskListState() {
        return taskListState;
    }

    public String getSelectedTaskId() {
        return selectedTaskId;
    }

    public void setSelectedTaskId(String taskId) {
        this.selectedTaskId = taskId;
    }

    private void broadcastTaskListState() {
        if (taskListState == null || server == null) {
            return;
        }
        String emergencyInfo = (taskListState.emergencyTask != null)
                ? taskListState.emergencyTask.goal
                : "null";
        LOGGER.info("📤 Broadcasting TaskListState: {} tasks, emergencyTask={}",
                taskListState.tasks != null ? taskListState.tasks.size() : 0,
                emergencyInfo);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, TaskListStatePacket.PACKET_ID)) {
                ServerPlayNetworking.send(player, new TaskListStatePacket(taskListState));
            }
        }
    }

    public void updateLogsState(DetailedLogsState newState) {
        // 新しいログを既存のログリストにマージ（上書きではなく追加）
        if (newState != null && newState.logs != null && !newState.logs.isEmpty()) {
            if (this.logsState == null) {
                this.logsState = new DetailedLogsState();
            }
            if (this.logsState.logs == null) {
                this.logsState.logs = new ArrayList<>();
            }

            // 新しいログを追加
            this.logsState.logs.addAll(newState.logs);

            // 最大100件に制限（古いログを削除）
            final int MAX_LOGS = 100;
            while (this.logsState.logs.size() > MAX_LOGS) {
                this.logsState.logs.remove(0);
            }

            LOGGER.info("DetailedLogsState merged: added {} logs, total {} logs",
                    newState.logs.size(), this.logsState.logs.size());
        }

        notifyListeners(StateType.LOGS);
        broadcastLogsState();
    }

    /**
     * ログをクリア
     */
    public void clearLogsState() {
        this.logsState = new DetailedLogsState();
        LOGGER.info("DetailedLogsState cleared");
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

    /**
     * ターゲットプレイヤーを取得（最初に接続しているプレイヤー）
     */
    public ServerPlayerEntity getTargetPlayer() {
        if (server == null) {
            return null;
        }
        var players = server.getPlayerManager().getPlayerList();
        return players.isEmpty() ? null : players.get(0);
    }

    // ===== Broadcasting =====

    private void broadcastTaskTreeState() {
        if (taskTreeState == null || taskTreeState.goal == null || server == null) {
            return;
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, TaskTreeStatePacket.PACKET_ID)) {
                try {
                    ServerPlayNetworking.send(player, new TaskTreeStatePacket(taskTreeState));
                } catch (Exception e) {
                    LOGGER.error("Failed to send TaskTreeState packet to player {}: {}", player.getName().getString(), e.getMessage());
                }
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

    // ===== ReactionSettings =====

    public void updateReactionSettingsState(ReactionSettingsState newState) {
        this.reactionSettingsState = newState;
        LOGGER.info("ReactionSettingsState updated: {} reactions",
                newState.reactions != null ? newState.reactions.size() : 0);
        notifyListeners(StateType.REACTION_SETTINGS);
        broadcastReactionSettingsState();
    }

    public ReactionSettingsState getReactionSettingsState() {
        return reactionSettingsState;
    }

    private void broadcastReactionSettingsState() {
        if (reactionSettingsState == null || server == null) {
            return;
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, ReactionSettingsStatePacket.PACKET_ID)) {
                ServerPlayNetworking.send(player, new ReactionSettingsStatePacket(reactionSettingsState));
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

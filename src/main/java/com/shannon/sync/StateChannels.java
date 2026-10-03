package com.shannon.sync;

import com.shannon.model.AdvancementsState;
import com.shannon.model.BotVitals;
import com.shannon.model.ChatState;
import com.shannon.model.ConstantSkillsState;
import com.shannon.model.DetailedLogsState;
import com.shannon.model.InventoryState;
import com.shannon.model.ReactionSettingsState;
import com.shannon.model.TaskListState;
import com.shannon.model.TaskTreeState;

import java.util.List;

/** Every state the server pushes to the client. Add a channel here to sync a new kind of state. */
public final class StateChannels {
    public static final SyncChannel<TaskTreeState> TASK_TREE = new SyncChannel<>("task_tree", TaskTreeState.class);
    public static final SyncChannel<TaskListState> TASK_LIST = new SyncChannel<>("task_list", TaskListState.class);
    public static final SyncChannel<InventoryState> INVENTORY = new SyncChannel<>("inventory", InventoryState.class);
    public static final SyncChannel<BotVitals> VITALS = new SyncChannel<>("vitals", BotVitals.class);
    public static final SyncChannel<ConstantSkillsState> SKILLS = new SyncChannel<>("skills", ConstantSkillsState.class);
    public static final SyncChannel<ChatState> CHAT = new SyncChannel<>("chat", ChatState.class);
    public static final SyncChannel<DetailedLogsState> LOGS = new SyncChannel<>("logs", DetailedLogsState.class);
    public static final SyncChannel<ReactionSettingsState> REACTIONS = new SyncChannel<>("reactions", ReactionSettingsState.class);
    public static final SyncChannel<AdvancementsState> ADVANCEMENTS = new SyncChannel<>("advancements", AdvancementsState.class);

    public static final List<SyncChannel<?>> ALL = List.of(
            TASK_TREE, TASK_LIST, INVENTORY, VITALS, SKILLS, CHAT, LOGS, REACTIONS, ADVANCEMENTS);

    private StateChannels() {
    }
}

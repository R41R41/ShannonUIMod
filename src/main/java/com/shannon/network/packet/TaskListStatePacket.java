package com.shannon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.List;

/**
 * タスクリストの状態を送信するパケット
 * 最大3つの通常タスク + 1つの緊急タスク
 */
public record TaskListStatePacket(TaskListState state) implements CustomPayload {
    public static final CustomPayload.Id<TaskListStatePacket> PACKET_ID = new CustomPayload.Id<>(
            Identifier.of("shannonuimod", "task_list_state"));

    public static final PacketCodec<RegistryByteBuf, TaskListStatePacket> PACKET_CODEC = PacketCodec.of(
            (value, buf) -> {
                // タスクリスト
                List<TaskListState.TaskInfo> tasks = value.state.tasks;
                buf.writeInt(tasks != null ? tasks.size() : 0);
                if (tasks != null) {
                    for (TaskListState.TaskInfo task : tasks) {
                        buf.writeString(task.id != null ? task.id : "");
                        buf.writeString(task.goal != null ? task.goal : "");
                        buf.writeString(task.status != null ? task.status : "pending");
                        buf.writeLong(task.createdAt);
                    }
                }

                // 緊急タスク
                buf.writeBoolean(value.state.emergencyTask != null);
                if (value.state.emergencyTask != null) {
                    buf.writeString(value.state.emergencyTask.id != null ? value.state.emergencyTask.id : "");
                    buf.writeString(value.state.emergencyTask.goal != null ? value.state.emergencyTask.goal : "");
                    buf.writeLong(value.state.emergencyTask.createdAt);
                }

                // 現在実行中のタスクID
                buf.writeBoolean(value.state.currentTaskId != null);
                if (value.state.currentTaskId != null) {
                    buf.writeString(value.state.currentTaskId);
                }
            },
            buf -> {
                TaskListState state = new TaskListState();

                // タスクリスト
                int taskCount = buf.readInt();
                state.tasks = new ArrayList<>();
                for (int i = 0; i < taskCount; i++) {
                    TaskListState.TaskInfo task = new TaskListState.TaskInfo();
                    task.id = buf.readString();
                    task.goal = buf.readString();
                    task.status = buf.readString();
                    task.createdAt = buf.readLong();
                    state.tasks.add(task);
                }

                // 緊急タスク
                if (buf.readBoolean()) {
                    state.emergencyTask = new TaskListState.EmergencyTaskInfo();
                    state.emergencyTask.id = buf.readString();
                    state.emergencyTask.goal = buf.readString();
                    state.emergencyTask.createdAt = buf.readLong();
                }

                // 現在実行中のタスクID
                state.currentTaskId = buf.readBoolean() ? buf.readString() : null;

                return new TaskListStatePacket(state);
            });

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

    /**
     * タスクリストの状態
     */
    public static class TaskListState {
        public List<TaskInfo> tasks;
        public EmergencyTaskInfo emergencyTask;
        public String currentTaskId;

        public static class TaskInfo {
            public String id;
            public String goal;
            public String status; // "pending", "executing", "paused"
            public long createdAt;
        }

        public static class EmergencyTaskInfo {
            public String id;
            public String goal;
            public long createdAt;
        }
    }
}

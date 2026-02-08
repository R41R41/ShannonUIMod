package com.shannon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.ArrayList;

public record TaskTreeStatePacket(TaskTreeState state) implements CustomPayload {
    public static final CustomPayload.Id<TaskTreeStatePacket> PACKET_ID = new CustomPayload.Id<>(
            Identifier.of("shannonuimod", "task_tree_state"));

    /** パケットに書き込む文字列の最大長（バッファオーバーフロー防止） */
    private static final int MAX_STRING_LENGTH = 4096;

    /** 文字列を安全な長さに切り詰める */
    private static String safeString(String str) {
        if (str == null) return "";
        if (str.length() > MAX_STRING_LENGTH) {
            return str.substring(0, MAX_STRING_LENGTH) + "...";
        }
        return str;
    }

    public static final PacketCodec<RegistryByteBuf, TaskTreeStatePacket> PACKET_CODEC = PacketCodec.of(
            (value, buf) -> {
                // 基本フィールド
                buf.writeString(safeString(value.state.goal));
                buf.writeString(safeString(value.state.strategy));
                buf.writeString(safeString(value.state.status));
                buf.writeBoolean(value.state.error != null);
                if (value.state.error != null)
                    buf.writeString(safeString(value.state.error));

                // currentSubTaskId
                buf.writeBoolean(value.state.currentSubTaskId != null);
                if (value.state.currentSubTaskId != null) {
                    buf.writeString(safeString(value.state.currentSubTaskId));
                }

                // hierarchicalSubTasks（新形式）
                if (value.state.hierarchicalSubTasks != null && !value.state.hierarchicalSubTasks.isEmpty()) {
                    buf.writeInt(value.state.hierarchicalSubTasks.size());
                    for (TaskTreeState.HierarchicalSubTask sub : value.state.hierarchicalSubTasks) {
                        writeHierarchicalSubTask(buf, sub);
                    }
                } else {
                    buf.writeInt(0);
                }

                // subTasks（旧形式、後方互換性）
                if (value.state.subTasks != null && !value.state.subTasks.isEmpty()) {
                    buf.writeInt(value.state.subTasks.size());
                    for (TaskTreeState.SubTask sub : value.state.subTasks) {
                        buf.writeString(safeString(sub.subTaskGoal));
                        buf.writeString(safeString(sub.subTaskStrategy));
                        buf.writeString(safeString(sub.subTaskStatus));
                        buf.writeBoolean(sub.subTaskResult != null);
                        if (sub.subTaskResult != null)
                            buf.writeString(safeString(sub.subTaskResult));
                    }
                } else {
                    buf.writeInt(0);
                }
            },
            buf -> {
                TaskTreeState state = new TaskTreeState();

                // 基本フィールド
                state.goal = buf.readString();
                state.strategy = buf.readString();
                state.status = buf.readString();
                state.error = buf.readBoolean() ? buf.readString() : null;

                // currentSubTaskId
                state.currentSubTaskId = buf.readBoolean() ? buf.readString() : null;

                // hierarchicalSubTasks（新形式）
                int hierarchicalCount = buf.readInt();
                if (hierarchicalCount > 0) {
                    state.hierarchicalSubTasks = new ArrayList<>();
                    for (int i = 0; i < hierarchicalCount; i++) {
                        state.hierarchicalSubTasks.add(readHierarchicalSubTask(buf));
                    }
                }

                // subTasks（旧形式）
                int subTaskCount = buf.readInt();
                if (subTaskCount > 0) {
                    state.subTasks = new ArrayList<>();
                    for (int i = 0; i < subTaskCount; i++) {
                        TaskTreeState.SubTask sub = new TaskTreeState.SubTask();
                        sub.subTaskGoal = buf.readString();
                        sub.subTaskStrategy = buf.readString();
                        sub.subTaskStatus = buf.readString();
                        sub.subTaskResult = buf.readBoolean() ? buf.readString() : null;
                        state.subTasks.add(sub);
                    }
                }

                return new TaskTreeStatePacket(state);
            });

    /**
     * HierarchicalSubTaskを再帰的に書き込む
     */
    private static void writeHierarchicalSubTask(RegistryByteBuf buf, TaskTreeState.HierarchicalSubTask sub) {
        buf.writeString(safeString(sub.id));
        buf.writeString(safeString(sub.goal));
        buf.writeString(safeString(sub.strategy));
        buf.writeString(safeString(sub.status));
        buf.writeString(safeString(sub.result));
        buf.writeString(safeString(sub.failureReason));
        buf.writeInt(sub.depth);
        buf.writeString(safeString(sub.parentId));
        buf.writeBoolean(sub.needsDecomposition);

        // children（再帰的に書き込む）
        if (sub.children != null && !sub.children.isEmpty()) {
            buf.writeInt(sub.children.size());
            for (TaskTreeState.HierarchicalSubTask child : sub.children) {
                writeHierarchicalSubTask(buf, child);
            }
        } else {
            buf.writeInt(0);
        }
    }

    /**
     * HierarchicalSubTaskを再帰的に読み込む
     */
    private static TaskTreeState.HierarchicalSubTask readHierarchicalSubTask(RegistryByteBuf buf) {
        TaskTreeState.HierarchicalSubTask sub = new TaskTreeState.HierarchicalSubTask();
        sub.id = buf.readString();
        sub.goal = buf.readString();
        sub.strategy = buf.readString();
        sub.status = buf.readString();
        sub.result = buf.readString();
        sub.failureReason = buf.readString();
        sub.depth = buf.readInt();
        sub.parentId = buf.readString();
        sub.needsDecomposition = buf.readBoolean();

        // children
        int childrenCount = buf.readInt();
        if (childrenCount > 0) {
            sub.children = new ArrayList<>();
            for (int i = 0; i < childrenCount; i++) {
                sub.children.add(readHierarchicalSubTask(buf));
            }
        }

        return sub;
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}

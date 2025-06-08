package com.shannon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TaskTreeStatePacket(TaskTreeState state) implements CustomPayload {
    public static final CustomPayload.Id<TaskTreeStatePacket> PACKET_ID = new CustomPayload.Id<>(
            Identifier.of("shannonuimod", "task_tree_state"));

    public static final PacketCodec<RegistryByteBuf, TaskTreeStatePacket> PACKET_CODEC = PacketCodec.of(
            (value, buf) -> {
                // ここでTaskTreeStateをPacketByteBufに書き込む
                buf.writeString(value.state.goal);
                buf.writeString(value.state.strategy);
                buf.writeString(value.state.status);
                buf.writeBoolean(value.state.error != null);
                if (value.state.error != null)
                    buf.writeString(value.state.error);

                if (value.state.subTasks != null) {
                    buf.writeInt(value.state.subTasks.size());
                    for (TaskTreeState.SubTask sub : value.state.subTasks) {
                        buf.writeString(sub.subTaskGoal);
                        buf.writeString(sub.subTaskStrategy);
                        buf.writeString(sub.subTaskStatus);
                        buf.writeBoolean(sub.subTaskResult != null);
                        if (sub.subTaskResult != null)
                            buf.writeString(sub.subTaskResult);
                    }
                } else {
                    buf.writeInt(-1);
                }
            },
            buf -> {
                // ここでPacketByteBufからTaskTreeStateを復元
                TaskTreeState state = new TaskTreeState();
                state.goal = buf.readString();
                state.strategy = buf.readString();
                state.status = buf.readString();
                state.error = buf.readBoolean() ? buf.readString() : null;

                int subTaskCount = buf.readInt();
                if (subTaskCount >= 0) {
                    state.subTasks = new java.util.ArrayList<>();
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

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}

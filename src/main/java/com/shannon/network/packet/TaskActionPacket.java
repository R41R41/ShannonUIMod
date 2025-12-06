package com.shannon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * タスク操作パケット（C2S）
 * 削除、優先実行などの操作を送信
 */
public record TaskActionPacket(String action, String taskId) implements CustomPayload {
    public static final CustomPayload.Id<TaskActionPacket> PACKET_ID = new CustomPayload.Id<>(
            Identifier.of("shannonuimod", "task_action"));

    // アクション定数
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_PRIORITIZE = "prioritize";

    public static final PacketCodec<RegistryByteBuf, TaskActionPacket> PACKET_CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeString(value.action != null ? value.action : "");
                buf.writeString(value.taskId != null ? value.taskId : "");
            },
            buf -> new TaskActionPacket(buf.readString(), buf.readString()));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}

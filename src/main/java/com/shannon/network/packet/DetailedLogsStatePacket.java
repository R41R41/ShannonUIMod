package com.shannon.network.packet;

import com.shannon.network.packet.DetailedLogsState.LogEntry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record DetailedLogsStatePacket(DetailedLogsState state) implements CustomPayload {
    public static final CustomPayload.Id<DetailedLogsStatePacket> PACKET_ID = new CustomPayload.Id<>(
            Identifier.of("shannonuimod", "detailed_logs_state"));

    public static final PacketCodec<RegistryByteBuf, DetailedLogsStatePacket> PACKET_CODEC = PacketCodec.of(
            (value, buf) -> {
                List<LogEntry> snapshot = value.state.logs != null
                        ? new ArrayList<>(value.state.logs) : List.of();
                buf.writeInt(snapshot.size());

                for (LogEntry log : snapshot) {
                    buf.writeString(log.timestamp != null ? log.timestamp : "");
                    buf.writeString(log.phase != null ? log.phase : "");
                    buf.writeString(log.level != null ? log.level : "");
                    buf.writeString(log.source != null ? log.source : "");
                    buf.writeString(log.content != null ? log.content : "");

                    boolean hasMetadata = log.metadata != null;
                    buf.writeBoolean(hasMetadata);

                    if (hasMetadata) {
                        buf.writeString(log.metadata.skillName != null ? log.metadata.skillName : "");
                        buf.writeString(log.metadata.toolName != null ? log.metadata.toolName : "");
                        buf.writeString(log.metadata.parameters != null ? log.metadata.parameters : "");
                        buf.writeString(log.metadata.result != null ? log.metadata.result : "");
                        buf.writeInt(log.metadata.duration != null ? log.metadata.duration : -1);
                        buf.writeString(log.metadata.error != null ? log.metadata.error : "");
                    }
                }
            },
            buf -> {
                DetailedLogsState state = new DetailedLogsState();
                int logsCount = buf.readInt();

                state.logs = new ArrayList<>();
                for (int i = 0; i < logsCount; i++) {
                    LogEntry log = new LogEntry();
                    log.timestamp = buf.readString();
                    log.phase = buf.readString();
                    log.level = buf.readString();
                    log.source = buf.readString();
                    log.content = buf.readString();

                    boolean hasMetadata = buf.readBoolean();
                    if (hasMetadata) {
                        log.metadata = new LogEntry.LogMetadata();
                        log.metadata.skillName = buf.readString();
                        log.metadata.toolName = buf.readString();
                        log.metadata.parameters = buf.readString();
                        log.metadata.result = buf.readString();
                        int duration = buf.readInt();
                        log.metadata.duration = duration >= 0 ? duration : null;
                        log.metadata.error = buf.readString();
                    }

                    state.logs.add(log);
                }

                return new DetailedLogsStatePacket(state);
            });

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}

package com.shannon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.ArrayList;

/**
 * S2C: サーバーからクライアントに進捗データを送信するパケット
 */
public record AdvancementsStatePacket(AdvancementsState state) implements CustomPayload {
    public static final CustomPayload.Id<AdvancementsStatePacket> PACKET_ID = new CustomPayload.Id<>(
            Identifier.of("shannonuimod", "advancements_state"));

    /** 文字列を安全な長さに切り詰める */
    private static final int MAX_STRING_LENGTH = 256;
    private static String safe(String str) {
        if (str == null) return "";
        return str.length() > MAX_STRING_LENGTH ? str.substring(0, MAX_STRING_LENGTH) + "..." : str;
    }

    public static final PacketCodec<RegistryByteBuf, AdvancementsStatePacket> PACKET_CODEC = PacketCodec.of(
            // エンコーダー（書き込み）
            (value, buf) -> {
                buf.writeString(safe(value.state.playerName));
                buf.writeLong(value.state.updatedAt);

                int categoryCount = value.state.categories != null ? value.state.categories.size() : 0;
                buf.writeInt(categoryCount);

                for (int c = 0; c < categoryCount; c++) {
                    AdvancementsState.Category cat = value.state.categories.get(c);
                    buf.writeString(safe(cat.categoryId));
                    buf.writeString(safe(cat.displayName));
                    buf.writeInt(cat.completed);
                    buf.writeInt(cat.total);

                    int advCount = cat.advancements != null ? cat.advancements.size() : 0;
                    buf.writeInt(advCount);

                    for (int a = 0; a < advCount; a++) {
                        AdvancementsState.Advancement adv = cat.advancements.get(a);
                        buf.writeString(safe(adv.title));
                        buf.writeString(safe(adv.description));
                        buf.writeBoolean(adv.done);
                        buf.writeString(safe(adv.progress));
                    }
                }
            },
            // デコーダー（読み込み）
            buf -> {
                AdvancementsState state = new AdvancementsState();
                state.playerName = buf.readString();
                state.updatedAt = buf.readLong();

                int categoryCount = buf.readInt();
                state.categories = new ArrayList<>();

                for (int c = 0; c < categoryCount; c++) {
                    AdvancementsState.Category cat = new AdvancementsState.Category();
                    cat.categoryId = buf.readString();
                    cat.displayName = buf.readString();
                    cat.completed = buf.readInt();
                    cat.total = buf.readInt();

                    int advCount = buf.readInt();
                    cat.advancements = new ArrayList<>();

                    for (int a = 0; a < advCount; a++) {
                        AdvancementsState.Advancement adv = new AdvancementsState.Advancement();
                        adv.title = buf.readString();
                        adv.description = buf.readString();
                        adv.done = buf.readBoolean();
                        adv.progress = buf.readString();
                        cat.advancements.add(adv);
                    }
                    state.categories.add(cat);
                }

                return new AdvancementsStatePacket(state);
            });

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}

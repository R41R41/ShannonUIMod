package com.shannon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record InventoryStatePacket(InventoryState state) implements CustomPayload {
    public static final CustomPayload.Id<InventoryStatePacket> PACKET_ID = new CustomPayload.Id<>(
            Identifier.of("shannonuimod", "inventory_state"));

    public static final PacketCodec<RegistryByteBuf, InventoryStatePacket> PACKET_CODEC = PacketCodec.of(
            (value, buf) -> {
                // items
                if (value.state.items != null) {
                    buf.writeInt(value.state.items.size());
                    for (InventoryState.Item item : value.state.items) {
                        buf.writeString(item.displayName);
                        buf.writeString(item.name);
                        buf.writeString(item.count);
                    }
                } else {
                    buf.writeInt(-1);
                }
                // mainHand
                writeItemOrNull(buf, value.state.mainHand);
                // offHand
                writeItemOrNull(buf, value.state.offHand);
                // head
                writeItemOrNull(buf, value.state.head);
                // chest
                writeItemOrNull(buf, value.state.chest);
                // legs
                writeItemOrNull(buf, value.state.legs);
                // feet
                writeItemOrNull(buf, value.state.feet);
                // isFull
                buf.writeBoolean(value.state.isFull);
            },
            buf -> {
                InventoryState state = new InventoryState();
                int itemCount = buf.readInt();
                if (itemCount >= 0) {
                    state.items = new java.util.ArrayList<>();
                    for (int i = 0; i < itemCount; i++) {
                        InventoryState.Item item = new InventoryState.Item();
                        item.displayName = buf.readString();
                        item.name = buf.readString();
                        item.count = buf.readString();
                        state.items.add(item);
                    }
                }
                state.mainHand = readItemOrNull(buf);
                state.offHand = readItemOrNull(buf);
                state.head = readItemOrNull(buf);
                state.chest = readItemOrNull(buf);
                state.legs = readItemOrNull(buf);
                state.feet = readItemOrNull(buf);
                state.isFull = buf.readBoolean();
                return new InventoryStatePacket(state);
            });

    private static void writeItemOrNull(RegistryByteBuf buf, InventoryState.Item item) {
        if (item == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeString(item.displayName);
            buf.writeString(item.name);
            buf.writeString(item.count);
        }
    }

    private static InventoryState.Item readItemOrNull(RegistryByteBuf buf) {
        if (!buf.readBoolean())
            return null;
        InventoryState.Item item = new InventoryState.Item();
        item.displayName = buf.readString();
        item.name = buf.readString();
        item.count = buf.readString();
        return item;
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}

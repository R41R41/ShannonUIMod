package com.shannon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record InventoryItemClickPacket(String itemName) implements CustomPayload {
    public static final CustomPayload.Id<InventoryItemClickPacket> PACKET_ID = new CustomPayload.Id<>(
            Identifier.of("shannonuimod", "inventory_item_click"));

    public static final PacketCodec<RegistryByteBuf, InventoryItemClickPacket> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            InventoryItemClickPacket::itemName,
            InventoryItemClickPacket::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
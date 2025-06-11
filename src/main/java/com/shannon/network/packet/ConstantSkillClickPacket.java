package com.shannon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ConstantSkillClickPacket(String skillName, boolean status) implements CustomPayload {
    public static final CustomPayload.Id<ConstantSkillClickPacket> PACKET_ID = new CustomPayload.Id<>(
            Identifier.of("shannonuimod", "constant_skill_click"));

    public static final PacketCodec<RegistryByteBuf, ConstantSkillClickPacket> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            ConstantSkillClickPacket::skillName,
            PacketCodecs.BOOLEAN,
            ConstantSkillClickPacket::status,
            ConstantSkillClickPacket::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
package com.shannon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.ArrayList;

public record ConstantSkillsStatePacket(ConstantSkillsState state) implements CustomPayload {
    public static final CustomPayload.Id<ConstantSkillsStatePacket> PACKET_ID = new CustomPayload.Id<>(
            Identifier.of("shannonuimod", "constant_skills_state"));

    public static final PacketCodec<RegistryByteBuf, ConstantSkillsStatePacket> PACKET_CODEC = PacketCodec.of(
            (value, buf) -> {
                if (value.state.skills != null) {
                    buf.writeInt(value.state.skills.size());
                    for (ConstantSkillsState.ConstantSkill skill : value.state.skills) {
                        buf.writeString(skill.skillName);
                        buf.writeString(skill.description);
                        buf.writeString(skill.status);
                    }
                } else {
                    buf.writeInt(-1);
                }
            },
            buf -> {
                ConstantSkillsState state = new ConstantSkillsState();
                int skillCount = buf.readInt();
                if (skillCount >= 0) {
                    state.skills = new ArrayList<>();
                    for (int i = 0; i < skillCount; i++) {
                        ConstantSkillsState.ConstantSkill skill = new ConstantSkillsState.ConstantSkill();
                        skill.skillName = buf.readString();
                        skill.description = buf.readString();
                        skill.status = buf.readString();
                        state.skills.add(skill);
                    }
                }
                return new ConstantSkillsStatePacket(state);
            });

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
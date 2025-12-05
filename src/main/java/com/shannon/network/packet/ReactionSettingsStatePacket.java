package com.shannon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.ArrayList;

public record ReactionSettingsStatePacket(ReactionSettingsState state) implements CustomPayload {
    public static final CustomPayload.Id<ReactionSettingsStatePacket> PACKET_ID = new CustomPayload.Id<>(
            Identifier.of("shannonuimod", "reaction_settings_state"));

    public static final PacketCodec<RegistryByteBuf, ReactionSettingsStatePacket> PACKET_CODEC = PacketCodec.of(
            (value, buf) -> {
                // reactions
                if (value.state.reactions != null) {
                    buf.writeInt(value.state.reactions.size());
                    for (ReactionSettingsState.ReactionConfig reaction : value.state.reactions) {
                        buf.writeString(reaction.eventType != null ? reaction.eventType : "");
                        buf.writeBoolean(reaction.enabled);
                        buf.writeInt(reaction.probability);
                        buf.writeBoolean(reaction.idleOnly);
                        buf.writeString(reaction.reactionType != null ? reaction.reactionType : "");
                    }
                } else {
                    buf.writeInt(0);
                }

                // constantSkills
                if (value.state.constantSkills != null) {
                    buf.writeInt(value.state.constantSkills.size());
                    for (ReactionSettingsState.ConstantSkillConfig skill : value.state.constantSkills) {
                        buf.writeString(skill.skillName != null ? skill.skillName : "");
                        buf.writeBoolean(skill.enabled);
                        buf.writeString(skill.description != null ? skill.description : "");
                    }
                } else {
                    buf.writeInt(0);
                }
            },
            buf -> {
                ReactionSettingsState state = new ReactionSettingsState();

                // reactions
                int reactionCount = buf.readInt();
                state.reactions = new ArrayList<>();
                for (int i = 0; i < reactionCount; i++) {
                    ReactionSettingsState.ReactionConfig reaction = new ReactionSettingsState.ReactionConfig();
                    reaction.eventType = buf.readString();
                    reaction.enabled = buf.readBoolean();
                    reaction.probability = buf.readInt();
                    reaction.idleOnly = buf.readBoolean();
                    reaction.reactionType = buf.readString();
                    state.reactions.add(reaction);
                }

                // constantSkills
                int skillCount = buf.readInt();
                state.constantSkills = new ArrayList<>();
                for (int i = 0; i < skillCount; i++) {
                    ReactionSettingsState.ConstantSkillConfig skill = new ReactionSettingsState.ConstantSkillConfig();
                    skill.skillName = buf.readString();
                    skill.enabled = buf.readBoolean();
                    skill.description = buf.readString();
                    state.constantSkills.add(skill);
                }

                return new ReactionSettingsStatePacket(state);
            });

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}

package com.shannon.sync;

import com.shannon.network.packet.ScreenshotRequestPacket;
import com.shannon.network.packet.ScreenshotResultPacket;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/** Registers the mod's payload types. Runs once on both sides, from the common entrypoint. */
public final class ShannonNetworking {
    private ShannonNetworking() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.playS2C().register(SyncPayload.ID, SyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ActionPayload.ID, ActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ScreenshotRequestPacket.PACKET_ID, ScreenshotRequestPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(ScreenshotResultPacket.PACKET_ID, ScreenshotResultPacket.PACKET_CODEC);
    }
}

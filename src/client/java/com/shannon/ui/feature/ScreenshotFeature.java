package com.shannon.ui.feature;

import com.shannon.http.ClientHttpServerManager;
import com.shannon.http.endpoints.ScreenshotEndpoint;
import com.shannon.network.packet.ScreenshotRequestPacket;
import com.shannon.network.packet.ScreenshotResultPacket;
import com.shannon.util.ScreenshotUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Lets the bot "see": the server asks this client for a picture from the bot's position, and the
 * client renders one and sends it back.
 */
public final class ScreenshotFeature {
    private ScreenshotFeature() {
    }

    public static void register() {
        ClientHttpServerManager.startServer();
        ClientTickEvents.END_CLIENT_TICK.register(client -> ScreenshotUtil.tick());
        ClientPlayNetworking.registerGlobalReceiver(ScreenshotRequestPacket.PACKET_ID, (payload, context) ->
                context.client().execute(() -> capture(payload)));
    }

    private static void capture(ScreenshotRequestPacket payload) {
        ScreenshotEndpoint.ScreenshotOptions options = new ScreenshotEndpoint.ScreenshotOptions();
        options.width = payload.width();
        options.height = payload.height();
        ScreenshotUtil.captureFromBotViewDelayed(options, payload.botName(), payload.botX(), payload.botY(),
                payload.botZ(), payload.botYaw(), payload.botPitch(), result -> {
                    if (!ClientPlayNetworking.canSend(ScreenshotResultPacket.PACKET_ID)) {
                        return;
                    }
                    ClientPlayNetworking.send(new ScreenshotResultPacket(
                            payload.requestId(),
                            result.success,
                            result.base64Image != null ? result.base64Image : "",
                            result.width,
                            result.height,
                            result.playerPosition != null ? result.playerPosition.x : 0,
                            result.playerPosition != null ? result.playerPosition.y : 0,
                            result.playerPosition != null ? result.playerPosition.z : 0,
                            result.playerRotation != null ? result.playerRotation.yaw : 0,
                            result.playerRotation != null ? result.playerRotation.pitch : 0,
                            result.error != null ? result.error : ""));
                });
    }
}

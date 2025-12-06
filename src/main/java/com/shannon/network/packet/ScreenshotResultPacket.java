package com.shannon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S パケット: クライアントからサーバーへスクリーンショット結果を送信
 */
public record ScreenshotResultPacket(
        String requestId,
        boolean success,
        String imageBase64,
        int width,
        int height,
        double playerX,
        double playerY,
        double playerZ,
        float playerYaw,
        float playerPitch,
        String error) implements CustomPayload {

    public static final CustomPayload.Id<ScreenshotResultPacket> PACKET_ID = new CustomPayload.Id<>(
            Identifier.of("shannonuimod", "screenshot_result"));

    public static final PacketCodec<RegistryByteBuf, ScreenshotResultPacket> PACKET_CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeString(value.requestId != null ? value.requestId : "");
                buf.writeBoolean(value.success);
                // 画像データはwriteByteArrayを使用（writeStringの32767バイト制限を回避）
                byte[] imageBytes = (value.imageBase64 != null ? value.imageBase64 : "")
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                buf.writeInt(imageBytes.length);
                buf.writeBytes(imageBytes);
                buf.writeInt(value.width);
                buf.writeInt(value.height);
                buf.writeDouble(value.playerX);
                buf.writeDouble(value.playerY);
                buf.writeDouble(value.playerZ);
                buf.writeFloat(value.playerYaw);
                buf.writeFloat(value.playerPitch);
                buf.writeString(value.error != null ? value.error : "");
            },
            buf -> {
                String requestId = buf.readString();
                boolean success = buf.readBoolean();
                // 画像データを読み取り
                int imageLength = buf.readInt();
                byte[] imageBytes = new byte[imageLength];
                buf.readBytes(imageBytes);
                String imageBase64 = new String(imageBytes, java.nio.charset.StandardCharsets.UTF_8);
                return new ScreenshotResultPacket(
                        requestId,
                        success,
                        imageBase64,
                        buf.readInt(),
                        buf.readInt(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readString());
            });

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}

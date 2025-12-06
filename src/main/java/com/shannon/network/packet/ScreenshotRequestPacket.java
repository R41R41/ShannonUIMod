package com.shannon.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C パケット: サーバーからクライアントへスクリーンショット撮影を要求
 * ボットの名前、位置、向きを指定し、そのエンティティの視点から撮影
 */
public record ScreenshotRequestPacket(
        String requestId,
        int width,
        int height,
        String context,
        // ボットの名前（ワールド内で検索するため）
        String botName,
        // ボットの位置（参考用）
        double botX,
        double botY,
        double botZ,
        // ボットの向き（参考用）
        float botYaw,
        float botPitch) implements CustomPayload {

    public static final CustomPayload.Id<ScreenshotRequestPacket> PACKET_ID = new CustomPayload.Id<>(
            Identifier.of("shannonuimod", "screenshot_request"));

    public static final PacketCodec<RegistryByteBuf, ScreenshotRequestPacket> PACKET_CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeString(value.requestId != null ? value.requestId : "");
                buf.writeInt(value.width);
                buf.writeInt(value.height);
                buf.writeString(value.context != null ? value.context : "");
                buf.writeString(value.botName != null ? value.botName : "Shannon");
                buf.writeDouble(value.botX);
                buf.writeDouble(value.botY);
                buf.writeDouble(value.botZ);
                buf.writeFloat(value.botYaw);
                buf.writeFloat(value.botPitch);
            },
            buf -> new ScreenshotRequestPacket(
                    buf.readString(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readString(),
                    buf.readString(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readFloat()));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}

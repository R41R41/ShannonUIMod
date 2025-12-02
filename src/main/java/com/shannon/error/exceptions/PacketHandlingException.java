package com.shannon.error.exceptions;

/**
 * パケット処理エラー
 * パケット送受信時のエラーを表現
 */
public class PacketHandlingException extends ModException {

    public PacketHandlingException(String packetType, Throwable cause) {
        super(
                "パケット処理失敗: " + packetType,
                "PACKET_HANDLING_ERROR",
                cause);
        addMetadata("packetType", packetType);
    }

    public PacketHandlingException(String packetType, String operation, Throwable cause) {
        super(
                "パケット処理失敗: " + packetType + " (" + operation + ")",
                "PACKET_HANDLING_ERROR",
                cause);
        addMetadata("packetType", packetType);
        addMetadata("operation", operation);
    }
}

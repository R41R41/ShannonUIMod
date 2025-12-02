package com.shannon.error.exceptions;

/**
 * JSON変換エラー
 * JSONシリアライズ/デシリアライズ時のエラーを表現
 */
public class JsonSerializationException extends ModException {

    public JsonSerializationException(Throwable cause) {
        super(
                "JSON変換失敗",
                "JSON_SERIALIZATION_ERROR",
                cause);
    }

    public JsonSerializationException(String operation, Throwable cause) {
        super(
                "JSON変換失敗: " + operation,
                "JSON_SERIALIZATION_ERROR",
                cause);
        addMetadata("operation", operation);
    }
}

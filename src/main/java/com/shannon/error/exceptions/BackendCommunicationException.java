package com.shannon.error.exceptions;

/**
 * Backend通信エラー
 * HTTPリクエスト送信時のエラーを表現
 */
public class BackendCommunicationException extends ModException {

    public BackendCommunicationException(String endpoint, Throwable cause) {
        super(
                "Backend通信失敗: " + endpoint,
                "BACKEND_COMM_ERROR",
                cause);
        addMetadata("endpoint", endpoint);
    }

    public BackendCommunicationException(String endpoint, String details) {
        super(
                "Backend通信失敗: " + endpoint + " - " + details,
                "BACKEND_COMM_ERROR");
        addMetadata("endpoint", endpoint);
        addMetadata("details", details);
    }
}

package com.shannon.error.exceptions;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * Modのカスタム例外基底クラス
 * 全ての例外はこのクラスを継承し、エラー情報を構造化
 */
public abstract class ModException extends Exception {
    private final String errorCode;
    private final Map<String, Object> metadata;
    private final long timestamp;

    public ModException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.metadata = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    public ModException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.metadata = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * エラーコードを取得
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * メタデータを追加
     */
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * メタデータを取得
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * タイムスタンプを取得
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * エラーをJSON形式で出力
     */
    public String toJson() {
        Gson gson = new Gson();
        Map<String, Object> errorInfo = new HashMap<>();
        errorInfo.put("errorCode", errorCode);
        errorInfo.put("message", getMessage());
        errorInfo.put("timestamp", timestamp);
        errorInfo.put("metadata", metadata);
        if (getCause() != null) {
            errorInfo.put("cause", getCause().getMessage());
        }
        return gson.toJson(errorInfo);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s", errorCode, getClass().getSimpleName(), getMessage());
    }
}

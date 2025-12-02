package com.shannon.error;

import com.shannon.error.exceptions.ModException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod全体のエラーハンドラー
 * エラーのログ出力と通知を一元管理
 */
public class ModErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModErrorHandler.class);

    /**
     * エラーを処理（詳細ログ出力）
     * 
     * @param e ModException
     */
    public static void handle(ModException e) {
        // ログ出力
        LOGGER.error("🚨 Error: {} - {}", e.getClass().getSimpleName(), e.getMessage());

        // 原因がある場合は表示
        if (e.getCause() != null) {
            LOGGER.error("  Caused by: {}", e.getCause().getMessage());
        }

        // JSON形式で詳細出力
        LOGGER.error("  Details: {}", e.toJson());

        // スタックトレース（デバッグ時のみ）
        if (com.shannon.config.ModConfig.DEBUG_MODE) {
            LOGGER.error("  StackTrace:", e);
        }
    }

    /**
     * エラーを処理（シンプルログ）
     * 
     * @param e 通常の例外
     */
    public static void handle(Exception e) {
        LOGGER.error("❌ Error: {}", e.getMessage());
        if (com.shannon.config.ModConfig.DEBUG_MODE) {
            LOGGER.error("  StackTrace:", e);
        }
    }

    /**
     * 警告レベルのエラー処理（サイレント）
     * 
     * @param e 例外
     */
    public static void handleSilent(Exception e) {
        LOGGER.warn("⚠️ Warning: {}", e.getMessage());
    }

    /**
     * 警告メッセージを出力
     * 
     * @param message 警告メッセージ
     */
    public static void warn(String message) {
        LOGGER.warn("⚠️ {}", message);
    }

    /**
     * 情報メッセージを出力
     * 
     * @param message 情報メッセージ
     */
    public static void info(String message) {
        LOGGER.info("ℹ️ {}", message);
    }

    /**
     * デバッグメッセージを出力（DEBUG_MODE時のみ）
     * 
     * @param message デバッグメッセージ
     */
    public static void debug(String message) {
        if (com.shannon.config.ModConfig.DEBUG_MODE) {
            LOGGER.debug("🐛 {}", message);
        }
    }

    private ModErrorHandler() {
        // ユーティリティクラスなのでインスタンス化を防ぐ
    }
}

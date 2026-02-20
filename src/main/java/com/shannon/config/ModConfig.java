package com.shannon.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ShannonUIModの設定を一元管理するクラス
 * 全ての設定値をここで定義し、変更を容易にする
 * config/shannonuimod.json ファイルで各サーバーごとの設定を上書き可能
 */
public class ModConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModConfig.class);

    // ===== Backend接続設定 =====

    /** Backend（Minebot）のホスト */
    public static final String BACKEND_HOST = "localhost";

    /** Backend（Minebot）のポート */
    public static final int BACKEND_PORT = 8082;

    /** BackendのベースURL */
    public static final String BACKEND_BASE_URL = "http://" + BACKEND_HOST + ":" + BACKEND_PORT;

    // ===== HTTPサーバー設定 =====

    /** HTTPサーバーのポート（デフォルト値、config/shannonuimod.json で上書き可能） */
    private static final int HTTP_SERVER_PORT_DEFAULT = 8081;

    /** HTTPサーバーのポート（Backendからのリクエストを受信） */
    public static final int HTTP_SERVER_PORT = loadHttpServerPort();

    /** クライアントサイドHTTPサーバーのポート（スクリーンショット等） */
    public static final int CLIENT_HTTP_SERVER_PORT = 8083;

    /**
     * config/shannonuimod.json からHTTPサーバーポートを読み込む
     * ファイルが存在しない場合はデフォルト値 (8081) を使用
     */
    private static int loadHttpServerPort() {
        Path configPath = Paths.get("config", "shannonuimod.json");
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                Gson gson = new Gson();
                JsonObject json = gson.fromJson(reader, JsonObject.class);
                if (json != null && json.has("httpServerPort")) {
                    int port = json.get("httpServerPort").getAsInt();
                    LOGGER.info("📋 config/shannonuimod.json からHTTPポートを読み込みました: {}", port);
                    return port;
                }
            } catch (IOException e) {
                LOGGER.warn("config/shannonuimod.json の読み込みに失敗しました。デフォルトポートを使用します。", e);
            }
        }
        return HTTP_SERVER_PORT_DEFAULT;
    }

    /** HTTPサーバーのスレッドプールサイズ */
    public static final int HTTP_THREAD_POOL_SIZE = 4;

    /** HTTPタイムアウト（秒） */
    public static final int HTTP_TIMEOUT_SECONDS = 10;

    // ===== エンドポイント定義 =====

    /** アイテム投げ捨てエンドポイント */
    public static final String ENDPOINT_THROW_ITEM = "/throw_item";

    /** スキル切り替えエンドポイント */
    public static final String ENDPOINT_SKILL_SWITCH = "/constant_skill_switch";

    /** チャットメッセージエンドポイント */
    public static final String ENDPOINT_CHAT_MESSAGE = "/chat_message";

    /** 反応設定更新エンドポイント */
    public static final String ENDPOINT_REACTION_SETTING_UPDATE = "/reaction_setting_update";

    /** 反応設定リセットエンドポイント */
    public static final String ENDPOINT_REACTION_SETTINGS_RESET = "/reaction_settings_reset";

    /** タスク削除エンドポイント */
    public static final String ENDPOINT_TASK_DELETE = "/task_delete";

    /** タスク優先実行エンドポイント */
    public static final String ENDPOINT_TASK_PRIORITIZE = "/task_prioritize";

    /** タスクリスト取得エンドポイント */
    public static final String ENDPOINT_TASK_LIST = "/task_list";

    // ===== ターゲットプレイヤー設定 =====

    /** ボット操作の対象プレイヤー名 */
    public static final String TARGET_PLAYER_NAME = "I_am_Shannon";
    // public static final String TARGET_PLAYER_NAME = "Player"; // デバッグ用

    // ===== UI設定 =====

    /** UIの外側マージン */
    public static final int UI_MARGIN = 10;

    /** UIの内側パディング */
    public static final int UI_PADDING = 5;

    /** UI行の高さ */
    public static final int UI_LINE_HEIGHT = 12;

    /** UIの背景色透過度 (0-255) */
    public static final int UI_BACKGROUND_ALPHA = 200;

    // ===== ログ設定 =====

    /** ログの最大保持数 */
    public static final int MAX_LOG_ENTRIES = 100;

    /** チャット履歴の最大保持数 */
    public static final int MAX_CHAT_HISTORY = 50;

    // ===== 接続タイムアウト設定 =====

    /** HTTP接続タイムアウト（ミリ秒） */
    public static final int CONNECTION_TIMEOUT_MS = 5000;

    /** HTTP読み取りタイムアウト（ミリ秒） */
    public static final int READ_TIMEOUT_MS = 10000;

    // ===== デバッグ設定 =====

    /** デバッグモード（詳細ログ出力） */
    public static final boolean DEBUG_MODE = false;

    /** パケット送受信のログ出力 */
    public static final boolean LOG_PACKETS = false;

    /** HTTP通信のログ出力 */
    public static final boolean LOG_HTTP = true;

    // ===== ヘルパーメソッド =====

    /**
     * BackendのフルURLを構築
     * 
     * @param endpoint エンドポイント (例: "/throw_item")
     * @return フルURL (例: "http://localhost:8082/throw_item")
     */
    public static String buildBackendUrl(String endpoint) {
        return BACKEND_BASE_URL + endpoint;
    }

    /**
     * 設定のサマリーをログ出力
     */
    public static void logConfiguration() {
        System.out.println("📋 ShannonUIMod Configuration:");
        System.out.println("  Backend: " + BACKEND_BASE_URL);
        System.out.println("  HTTP Server Port: " + HTTP_SERVER_PORT);
        System.out.println("  Target Player: " + TARGET_PLAYER_NAME);
        System.out.println("  Debug Mode: " + DEBUG_MODE);
    }

    private ModConfig() {
        // ユーティリティクラスなのでインスタンス化を防ぐ
    }
}

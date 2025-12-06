package com.shannon.http;

import com.shannon.config.ModConfig;
import com.shannon.http.endpoints.*;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * HTTPサーバーの管理クラス
 * エンドポイントの登録と起動を一元管理
 */
public class HttpServerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerManager.class);
    private static HttpServer server;

    /**
     * HTTPサーバーを起動
     */
    public static void startServer() {
        new Thread(() -> {
            try {
                server = HttpServer.create(new InetSocketAddress(ModConfig.HTTP_SERVER_PORT), 0);

                // エンドポイントを登録
                registerEndpoints();

                // スレッドプールを設定
                server.setExecutor(Executors.newFixedThreadPool(ModConfig.HTTP_THREAD_POOL_SIZE));

                // サーバー起動
                server.start();
                LOGGER.info("HTTPサーバーをポート{}で起動しました", ModConfig.HTTP_SERVER_PORT);
            } catch (IOException e) {
                LOGGER.error("HTTPサーバーの起動に失敗しました", e);
            }
        }, "HTTP-Server-Thread").start();
    }

    /**
     * エンドポイントを登録
     */
    private static void registerEndpoints() {
        if (server == null) {
            LOGGER.error("サーバーが初期化されていません");
            return;
        }

        // 各エンドポイントを登録
        server.createContext("/task", new TaskEndpoint());
        server.createContext("/task_logs", new TaskLogsEndpoint());
        server.createContext("/task_list", new TaskListEndpoint());
        server.createContext("/constant_skills", new ConstantSkillsEndpoint());
        server.createContext("/chat", new ChatEndpoint());
        server.createContext("/inventory_click", new InventoryClickEndpoint());
        server.createContext("/constant_skill_click", new ConstantSkillClickEndpoint());
        server.createContext("/chat_message", new ChatMessageEndpoint());
        server.createContext("/reaction_settings", new ReactionSettingsEndpoint());
        server.createContext("/bot_chat", new BotChatEndpoint());
        server.createContext("/screenshot", new ServerScreenshotEndpoint());
        server.createContext("/task_delete", new TaskDeleteEndpoint());
        server.createContext("/task_prioritize", new TaskPrioritizeEndpoint());

        LOGGER.info("全エンドポイントの登録が完了しました");
    }

    /**
     * HTTPサーバーを停止
     */
    public static void stopServer() {
        if (server != null) {
            server.stop(0);
            LOGGER.info("HTTPサーバーを停止しました");
        }
    }

    /**
     * サーバーインスタンスを取得
     */
    public static HttpServer getServer() {
        return server;
    }
}

package com.shannon.http;

import com.shannon.config.ModConfig;
import com.shannon.http.endpoints.ScreenshotEndpoint;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * クライアントサイドHTTPサーバーの管理クラス
 * スクリーンショット等のクライアント専用機能を提供
 */
public class ClientHttpServerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientHttpServerManager.class);
    private static HttpServer server;
    private static boolean isRunning = false;

    /**
     * クライアントサイドHTTPサーバーを起動
     */
    public static void startServer() {
        if (isRunning) {
            LOGGER.info("クライアントHTTPサーバーは既に起動しています");
            return;
        }

        new Thread(() -> {
            try {
                server = HttpServer.create(new InetSocketAddress(ModConfig.CLIENT_HTTP_SERVER_PORT), 0);

                // エンドポイントを登録
                registerEndpoints();

                // スレッドプールを設定
                server.setExecutor(Executors.newFixedThreadPool(2));

                // サーバー起動
                server.start();
                isRunning = true;
                LOGGER.info("📸 クライアントHTTPサーバーをポート{}で起動しました", ModConfig.CLIENT_HTTP_SERVER_PORT);
            } catch (IOException e) {
                LOGGER.error("クライアントHTTPサーバーの起動に失敗しました", e);
            }
        }, "Client-HTTP-Server-Thread").start();
    }

    /**
     * エンドポイントを登録
     */
    private static void registerEndpoints() {
        if (server == null) {
            LOGGER.error("サーバーが初期化されていません");
            return;
        }

        // スクリーンショットエンドポイント
        server.createContext("/screenshot", new ScreenshotEndpoint());

        LOGGER.info("クライアントエンドポイントの登録が完了しました");
    }

    /**
     * HTTPサーバーを停止
     */
    public static void stopServer() {
        if (server != null) {
            server.stop(0);
            isRunning = false;
            LOGGER.info("クライアントHTTPサーバーを停止しました");
        }
    }

    /**
     * サーバーが起動中かどうか
     */
    public static boolean isRunning() {
        return isRunning;
    }
}

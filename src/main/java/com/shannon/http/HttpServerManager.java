package com.shannon.http;

import com.shannon.config.ModConfig;
import com.shannon.http.endpoints.AdvancementsEndpoint;
import com.shannon.http.endpoints.BotChatEndpoint;
import com.shannon.http.endpoints.ChatEndpoint;
import com.shannon.http.endpoints.ConstantSkillsEndpoint;
import com.shannon.http.endpoints.ReactionSettingsEndpoint;
import com.shannon.http.endpoints.ServerScreenshotEndpoint;
import com.shannon.http.endpoints.TaskEndpoint;
import com.shannon.http.endpoints.TaskListEndpoint;
import com.shannon.http.endpoints.TaskLogsEndpoint;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The HTTP server the bot backend pushes state to.
 *
 * <p>It listens on the loopback address only, since the backend runs on the same machine, unless
 * {@code httpServerBindAddress} in the config says otherwise.
 */
public final class HttpServerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerManager.class);
    private static HttpServer server;
    private static ExecutorService executor;

    private HttpServerManager() {
    }

    public static synchronized void startServer() {
        if (server != null) {
            return;
        }
        try {
            InetAddress address = InetAddress.getByName(ModConfig.HTTP_SERVER_BIND_ADDRESS);
            HttpServer created = HttpServer.create(new InetSocketAddress(address, ModConfig.HTTP_SERVER_PORT), 0);
            created.createContext("/task", new TaskEndpoint());
            created.createContext("/task_logs", new TaskLogsEndpoint());
            created.createContext("/task_list", new TaskListEndpoint());
            created.createContext("/constant_skills", new ConstantSkillsEndpoint());
            created.createContext("/chat", new ChatEndpoint());
            created.createContext("/bot_chat", new BotChatEndpoint());
            created.createContext("/reaction_settings", new ReactionSettingsEndpoint());
            created.createContext("/screenshot", new ServerScreenshotEndpoint());
            created.createContext("/advancements", new AdvancementsEndpoint());
            executor = Executors.newFixedThreadPool(ModConfig.HTTP_THREAD_POOL_SIZE, runnable -> {
                Thread thread = new Thread(runnable, "ShannonUIMod-HTTP");
                thread.setDaemon(true);
                return thread;
            });
            created.setExecutor(executor);
            created.start();
            server = created;
            LOGGER.info("Listening for the bot backend on {}:{}", address.getHostAddress(), ModConfig.HTTP_SERVER_PORT);
        } catch (IOException e) {
            LOGGER.error("Could not start the HTTP server on port {}", ModConfig.HTTP_SERVER_PORT, e);
        }
    }

    public static synchronized void stopServer() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }
}

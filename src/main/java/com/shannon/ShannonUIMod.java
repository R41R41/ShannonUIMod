package com.shannon;

import com.shannon.network.packet.MessagePacket;
import com.shannon.network.packet.TaskTreeStatePacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import com.shannon.network.packet.TaskTreeState;
import net.minecraft.server.MinecraftServer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class ShannonUIMod implements ModInitializer {
	public static final String MOD_ID = "shannonuimod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static MinecraftServer SERVER_INSTANCE;

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");
		PayloadTypeRegistry.playS2C().register(MessagePacket.PACKET_ID, MessagePacket.PACKET_CODEC);
		PayloadTypeRegistry.playC2S().register(MessagePacket.PACKET_ID, MessagePacket.PACKET_CODEC);
		PayloadTypeRegistry.playS2C().register(TaskTreeStatePacket.PACKET_ID, TaskTreeStatePacket.PACKET_CODEC);
		PayloadTypeRegistry.playC2S().register(TaskTreeStatePacket.PACKET_ID, TaskTreeStatePacket.PACKET_CODEC);

		// C2Sパケット受信ハンドラの登録
		ServerPlayNetworking.registerGlobalReceiver(
				MessagePacket.PACKET_ID,
				(payload, context) -> {
					String msg = payload.message();
					ServerPlayerEntity player = context.player();
					// サーバー側で受信した文字列をログに出す例
					LOGGER.info("クライアントから受信: " + msg + " (from " + player.getName() + ")");
					// ここでサーバー側の処理を追加可能
					MyModServer.sendMessageToClient(player, msg);
				});

		// サーバーインスタンスをセット
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			SERVER_INSTANCE = server;
			LOGGER.info("SERVER_INSTANCEセット完了");
		});

		// 1. 別スレッドでHTTPサーバー起動
		new Thread(() -> {
			try {
				HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0); // ポート8081
				server.createContext("/task", exchange -> {
					try {
						if ("POST".equals(exchange.getRequestMethod())) {
							InputStream is = exchange.getRequestBody();
							String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
							LOGGER.info("受信したJSON: " + json);
							ObjectMapper mapper = new ObjectMapper();
							TaskTreeState state = mapper.readValue(json, TaskTreeState.class);
							LOGGER.info("受信したTaskTreeState: " + state);

							// 2. 受信したTaskTreeStateを全プレイヤーに送信
							if (SERVER_INSTANCE != null) {
								for (ServerPlayerEntity player : SERVER_INSTANCE.getPlayerManager().getPlayerList()) {
									if (ServerPlayNetworking.canSend(player, TaskTreeStatePacket.PACKET_ID)) {
										ServerPlayNetworking.send(player, new TaskTreeStatePacket(state));
										LOGGER.info("TaskTreeStateをプレイヤーに送信: " + player.getName());
									}
								}
							}

							String response = "OK";
							exchange.sendResponseHeaders(200, response.length());
							OutputStream os = exchange.getResponseBody();
							os.write(response.getBytes(StandardCharsets.UTF_8));
							os.close();
						} else {
							exchange.sendResponseHeaders(405, -1); // Method Not Allowed
						}
					} catch (Exception e) {
						LOGGER.error("HTTPリクエスト処理失敗", e);
						try {
							exchange.sendResponseHeaders(500, 0);
							exchange.getResponseBody().close();
						} catch (Exception ignored) {
						}
					}
				});
				server.setExecutor(null);
				server.start();
				LOGGER.info("HTTPサーバーを8081ポートで起動しました");
			} catch (Exception e) {
				LOGGER.error("HTTPサーバー起動失敗", e);
				try {
					// 500エラーを返す
					LOGGER.error("HTTPサーバー起動失敗", e);
				} catch (Exception ignored) {
				}
			}
		}).start();

		MyModServer.registerEvents();
	}
}
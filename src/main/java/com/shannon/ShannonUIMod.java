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
import com.shannon.network.packet.InventoryState;
import com.shannon.network.packet.InventoryStatePacket;
import com.shannon.network.packet.InventoryItemClickPacket;
import com.shannon.util.InventoryStateUtil;
import com.shannon.network.packet.ConstantSkillsState;
import com.shannon.network.packet.ConstantSkillsStatePacket;
import com.shannon.network.packet.ConstantSkillClickPacket;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import com.shannon.network.packet.PlayerStatusStatePacket;
import com.shannon.network.packet.PlayerStatusState;

public class ShannonUIMod implements ModInitializer {
	public static final String MOD_ID = "shannonuimod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static TaskTreeState taskTreeState = new TaskTreeState();
	public static ConstantSkillsState constantSkillsState = new ConstantSkillsState();
	public static InventoryState inventoryState = new InventoryState();
	public static MinecraftServer SERVER_INSTANCE;
	public static final String TARGET_PLAYER_NAME = "I_am_Sh4nnon";
	// public static final String TARGET_PLAYER_NAME = "Player";

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");
		PayloadTypeRegistry.playS2C().register(MessagePacket.PACKET_ID, MessagePacket.PACKET_CODEC);
		PayloadTypeRegistry.playC2S().register(MessagePacket.PACKET_ID, MessagePacket.PACKET_CODEC);
		PayloadTypeRegistry.playS2C().register(TaskTreeStatePacket.PACKET_ID, TaskTreeStatePacket.PACKET_CODEC);
		PayloadTypeRegistry.playC2S().register(TaskTreeStatePacket.PACKET_ID, TaskTreeStatePacket.PACKET_CODEC);
		PayloadTypeRegistry.playS2C().register(InventoryStatePacket.PACKET_ID, InventoryStatePacket.PACKET_CODEC);
		PayloadTypeRegistry.playC2S().register(InventoryStatePacket.PACKET_ID, InventoryStatePacket.PACKET_CODEC);
		PayloadTypeRegistry.playC2S().register(InventoryItemClickPacket.PACKET_ID,
				InventoryItemClickPacket.PACKET_CODEC);
		PayloadTypeRegistry.playS2C().register(ConstantSkillsStatePacket.PACKET_ID,
				ConstantSkillsStatePacket.PACKET_CODEC);
		PayloadTypeRegistry.playC2S().register(ConstantSkillClickPacket.PACKET_ID,
				ConstantSkillClickPacket.PACKET_CODEC);
		PayloadTypeRegistry.playS2C().register(PlayerStatusStatePacket.PACKET_ID, PlayerStatusStatePacket.PACKET_CODEC);

		// C2Sパケット受信ハンドラの登録
		ServerPlayNetworking.registerGlobalReceiver(
				MessagePacket.PACKET_ID,
				(payload, context) -> {
					String msg = payload.message();
					ServerPlayerEntity player = context.player();
					MyModServer.sendMessageToClient(player, msg);
				});

		ServerPlayNetworking.registerGlobalReceiver(
				InventoryItemClickPacket.PACKET_ID,
				(payload, context) -> {
					String itemName = payload.itemName();
					context.server().execute(() -> {
						try {
							java.net.URI uri = java.net.URI.create("http://localhost:8082/throw_item");
							java.net.HttpURLConnection conn = (java.net.HttpURLConnection) uri.toURL().openConnection();
							conn.setRequestMethod("POST");
							conn.setDoOutput(true);
							conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
							String json = "{\"itemName\":\"" + itemName + "\"}";
							try (java.io.OutputStream os = conn.getOutputStream()) {
								os.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
							}
							int responseCode = conn.getResponseCode();
							LOGGER.info("POST /throw_item response: " + responseCode);
							conn.disconnect();
						} catch (Exception e) {
							LOGGER.error("POST /throw_item 送信失敗", e);
						}
					});
				});

		ServerPlayNetworking.registerGlobalReceiver(
				ConstantSkillClickPacket.PACKET_ID,
				(payload, context) -> {
					String skillName = payload.skillName();
					boolean status = payload.status();
					context.server().execute(() -> {
						try {
							java.net.URI uri = java.net.URI.create("http://localhost:8082/constant_skill_switch");
							java.net.HttpURLConnection conn = (java.net.HttpURLConnection) uri.toURL().openConnection();
							conn.setRequestMethod("POST");
							conn.setDoOutput(true);
							conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
							String json = "{\"skillName\":\"" + skillName + "\",\"status\":\"" + status + "\"}";
							try (java.io.OutputStream os = conn.getOutputStream()) {
								os.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
							}
							int responseCode = conn.getResponseCode();
							LOGGER.info("POST /constant_skill_click response: " + responseCode);
							conn.disconnect();
						} catch (Exception e) {
							LOGGER.error("POST /constant_skill_click 送信失敗", e);
						}
					});
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
							taskTreeState = mapper.readValue(json, TaskTreeState.class);
							LOGGER.info("受信したTaskTreeState: " + taskTreeState);

							// 2. 受信したTaskTreeStateを全プレイヤーに送信
							sendTaskTreeStateToAllPlayers();

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
				server.createContext("/constant_skills", exchange -> {
					try {
						if ("POST".equals(exchange.getRequestMethod())) {
							InputStream is = exchange.getRequestBody();
							String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
							LOGGER.info("受信したJSON: " + json);
							ObjectMapper mapper = new ObjectMapper();
							List<ConstantSkillsState.ConstantSkill> skills = mapper.readValue(json,
									new TypeReference<List<ConstantSkillsState.ConstantSkill>>() {
									});
							constantSkillsState.skills = skills;
							LOGGER.info("受信したConstantSkillsState: " + constantSkillsState);
							sendConstantSkillsStateToAllPlayers();
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

	public static void sendTaskTreeStateToAllPlayers() {
		if (taskTreeState == null || taskTreeState.goal == null)
			return;
		if (SERVER_INSTANCE != null) {
			for (ServerPlayerEntity player : SERVER_INSTANCE.getPlayerManager().getPlayerList()) {
				if (ServerPlayNetworking.canSend(player, TaskTreeStatePacket.PACKET_ID)) {
					ServerPlayNetworking.send(player, new TaskTreeStatePacket(taskTreeState));
				}
			}
		}
	}

	public static void sendConstantSkillsStateToAllPlayers() {
		if (constantSkillsState == null || constantSkillsState.skills == null)
			return;
		if (SERVER_INSTANCE != null) {
			for (ServerPlayerEntity player : SERVER_INSTANCE.getPlayerManager().getPlayerList()) {
				if (ServerPlayNetworking.canSend(player, ConstantSkillsStatePacket.PACKET_ID)) {
					ServerPlayNetworking.send(player, new ConstantSkillsStatePacket(constantSkillsState));
				}
			}
		}
	}

	// 指定した名前のプレイヤーのインベントリを全クライアントに送信する
	public static void sendInventoryStateOfSh4nnonToAll() {
		if (SERVER_INSTANCE == null)
			return;
		ServerPlayerEntity shannon = null;
		for (ServerPlayerEntity player : SERVER_INSTANCE.getPlayerManager().getPlayerList()) {
			if (player.getName().getString().contains(TARGET_PLAYER_NAME)) {
				shannon = player;
				break;
			}
		}
		if (shannon == null) {
			LOGGER.info(TARGET_PLAYER_NAME + "という名前のプレイヤーが見つかりませんでした");
			return;
		}
		inventoryState = InventoryStateUtil.createInventoryState(shannon);
		if (inventoryState == null)
			return;
		for (ServerPlayerEntity player : SERVER_INSTANCE.getPlayerManager().getPlayerList()) {
			if (ServerPlayNetworking.canSend(player, InventoryStatePacket.PACKET_ID)) {
				ServerPlayNetworking.send(player, new InventoryStatePacket(inventoryState));
			}
		}
	}

	public static void sendPlayerStatusToAll() {
		ServerPlayerEntity targetPlayer = null;
		for (ServerPlayerEntity player : SERVER_INSTANCE.getPlayerManager().getPlayerList()) {
			if (player.getName().getString().contains(TARGET_PLAYER_NAME)) {
				targetPlayer = player;
				break;
			}
		}
		if (targetPlayer == null) {
			LOGGER.info(TARGET_PLAYER_NAME + "という名前のプレイヤーが見つかりませんでした");
			return;
		}

		if (targetPlayer.getWorld().isClient) {
			return;
		}

		float health = targetPlayer.getHealth();
		float maxHealth = targetPlayer.getMaxHealth();
		int hunger = targetPlayer.getHungerManager().getFoodLevel();

		if (SERVER_INSTANCE != null) {
			for (ServerPlayerEntity player : SERVER_INSTANCE.getPlayerManager().getPlayerList()) {
				if (ServerPlayNetworking.canSend(player, PlayerStatusStatePacket.PACKET_ID)) {
					PlayerStatusState state = new PlayerStatusState();
					state.health = health;
					state.maxHealth = maxHealth;
					state.hunger = hunger;
					ServerPlayNetworking.send(player, new PlayerStatusStatePacket(state));
				}
			}
		}
	}
}
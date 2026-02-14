package com.shannon;

import com.shannon.command.ShannonCommand;
import com.shannon.config.ModConfig;
import com.shannon.http.HttpServerManager;
import com.shannon.network.PacketHandlerRegistry;
import com.shannon.network.PacketRegistry;
import com.shannon.state.StateManager;
import com.shannon.util.InventoryStateUtil;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shannon.network.packet.AdvancementsStatePacket;
import com.shannon.network.packet.InventoryState;
import com.shannon.network.packet.PlayerStatusState;
import com.shannon.network.packet.PlayerStatusStatePacket;
import com.shannon.util.AdvancementCollector;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * ShannonUIMod メインエントリーポイント
 * 
 * 責任:
 * - Mod初期化
 * - 各コンポーネントの起動
 * - StateManagerへのアクセス提供
 * - プレイヤー状態送信のユーティリティメソッド
 */
public class ShannonUIMod implements ModInitializer {
	public static final String MOD_ID = "shannonuimod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final StateManager stateManager = StateManager.getInstance();

	@Override
	public void onInitialize() {
		LOGGER.info("🚀 ShannonUIMod initializing...");

		// 設定のログ出力（デバッグ時）
		if (ModConfig.DEBUG_MODE) {
			ModConfig.logConfiguration();
		}

		// パケットタイプの登録
		PacketRegistry.registerAll();

		// C2Sパケットハンドラの登録
		PacketHandlerRegistry.registerC2SHandlers();

		// コマンドの登録
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			ShannonCommand.register(dispatcher);
		});

		// サーバーインスタンスをStateManagerにセット
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			stateManager.setServer(server);
			LOGGER.info("StateManager initialized with server instance");
		});

		// HTTPサーバー起動
		HttpServerManager.startServer();

		// イベント登録
		MyModServer.registerEvents();

		// プレイヤーJOIN時に進捗データを自動送信
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity joinedPlayer = handler.getPlayer();
			String joinedName = joinedPlayer.getName().getString();
			LOGGER.info("[Advancements] Player joined: {}", joinedName);

			// 1tick待ってからデータ収集（プレイヤーのAdvancementTrackerの初期化完了を待つ）
			server.execute(() -> {
				try {
					String targetName = ModConfig.TARGET_PLAYER_NAME;

					// ターゲットプレイヤーがオンラインか確認
					boolean targetOnline = false;
					for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
						if (p.getName().getString().equals(targetName)) {
							targetOnline = true;
							break;
						}
					}

					if (!targetOnline) {
						LOGGER.debug("[Advancements] Target player '{}' not online, skipping", targetName);
						return;
					}

					// 進捗データを収集
					com.shannon.network.packet.AdvancementsState state =
							AdvancementCollector.collect(server, targetName);

					// ターゲットプレイヤー本人がJOINした場合: 全プレイヤーに送信
					// それ以外のプレイヤーがJOINした場合: そのプレイヤーにだけ送信
					if (joinedName.equals(targetName)) {
						for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
							if (ServerPlayNetworking.canSend(p, AdvancementsStatePacket.PACKET_ID)) {
								try {
									ServerPlayNetworking.send(p, new AdvancementsStatePacket(state));
								} catch (Exception e) {
									LOGGER.error("[Advancements] Failed to send to {}", p.getName().getString(), e);
								}
							}
						}
						LOGGER.info("[Advancements] Target player joined - sent to all clients ({} categories)",
								state.categories != null ? state.categories.size() : 0);
					} else {
						if (ServerPlayNetworking.canSend(joinedPlayer, AdvancementsStatePacket.PACKET_ID)) {
							try {
								ServerPlayNetworking.send(joinedPlayer, new AdvancementsStatePacket(state));
								LOGGER.info("[Advancements] Sent to joining player {} ({} categories)",
										joinedName, state.categories != null ? state.categories.size() : 0);
							} catch (Exception e) {
								LOGGER.error("[Advancements] Failed to send to {}", joinedName, e);
							}
						}
					}
				} catch (Exception e) {
					LOGGER.error("[Advancements] Error in JOIN handler", e);
				}
			});
		});

		LOGGER.info("✅ ShannonUIMod initialized successfully!");
	}

	// ===== Utility Methods =====

	/**
	 * StateManagerのゲッター（他クラスからアクセス用）
	 */
	public static StateManager getStateManager() {
		return stateManager;
	}

	/**
	 * 指定した名前のプレイヤーのインベントリを全クライアントに送信する
	 */
	public static void sendInventoryStateOfSh4nnonToAll() {
		MinecraftServer server = stateManager.getServer();
		if (server == null) {
			LOGGER.warn("Server is not initialized");
			return;
		}

		ServerPlayerEntity shannon = null;
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (player.getName().getString().contains(ModConfig.TARGET_PLAYER_NAME)) {
				shannon = player;
				break;
			}
		}
		if (shannon == null) {
			LOGGER.debug("{} プレイヤーが見つかりませんでした", ModConfig.TARGET_PLAYER_NAME);
			return;
		}
		sendInventoryStateToAll(shannon);
	}

	/**
	 * プレイヤーインスタンスを直接受け取るバージョン
	 */
	public static void sendInventoryStateToAll(ServerPlayerEntity player) {
		if (player == null) {
			return;
		}

		InventoryState newState = InventoryStateUtil.createInventoryState(player);
		if (newState == null) {
			return;
		}

		stateManager.updateInventoryState(newState);
	}

	/**
	 * プレイヤーステータス（体力、空腹度）を全クライアントに送信
	 */
	public static void sendPlayerStatusToAll() {
		MinecraftServer server = stateManager.getServer();
		if (server == null) {
			LOGGER.warn("Server is not initialized");
			return;
		}

		ServerPlayerEntity targetPlayer = null;
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (player.getName().getString().contains(ModConfig.TARGET_PLAYER_NAME)) {
				targetPlayer = player;
				break;
			}
		}
		if (targetPlayer == null) {
			LOGGER.debug("{} プレイヤーが見つかりませんでした", ModConfig.TARGET_PLAYER_NAME);
			return;
		}

		// ServerPlayerEntity は常にサーバーサイド（1.21.11でgetWorld()が削除されたため直接チェック不要）

		float health = targetPlayer.getHealth();
		float maxHealth = targetPlayer.getMaxHealth();
		int hunger = targetPlayer.getHungerManager().getFoodLevel();

		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
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

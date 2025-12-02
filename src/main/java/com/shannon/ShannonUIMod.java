package com.shannon;

import com.shannon.config.ModConfig;
import com.shannon.http.HttpServerManager;
import com.shannon.network.PacketHandlerRegistry;
import com.shannon.network.PacketRegistry;
import com.shannon.state.StateManager;
import com.shannon.util.InventoryStateUtil;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shannon.network.packet.InventoryState;
import com.shannon.network.packet.PlayerStatusState;
import com.shannon.network.packet.PlayerStatusStatePacket;
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

		// サーバーインスタンスをStateManagerにセット
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			stateManager.setServer(server);
			LOGGER.info("StateManager initialized with server instance");
		});

		// HTTPサーバー起動
		HttpServerManager.startServer();

		// イベント登録
		MyModServer.registerEvents();

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

		if (targetPlayer.getWorld().isClient) {
			return;
		}

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

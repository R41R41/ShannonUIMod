package com.shannon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.shannon.ShannonUIMod;
import com.shannon.config.ModConfig;
import com.shannon.client.BackendClient;
import com.shannon.network.packet.ChatState;
import com.shannon.state.StateManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * /shannon コマンド
 * 
 * 使い方: /shannon <message>
 * ボットにメッセージを送信します
 */
public class ShannonCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("ShannonCommand");

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("shannon")
                        .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                .executes(ShannonCommand::executeMessage)));

        LOGGER.info("✅ /shannon command registered");
    }

    private static int executeMessage(CommandContext<ServerCommandSource> context) {
        String message = StringArgumentType.getString(context, "message");
        ServerCommandSource source = context.getSource();

        // プレイヤー名を取得
        String senderName = "Unknown";
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            senderName = player.getName().getString();
        }

        final String finalSenderName = senderName;

        // ChatStateに追加
        StateManager stateManager = ShannonUIMod.getStateManager();
        ChatState chatState = stateManager.getChatState();
        ChatState.ChatMessage chatMessage = new ChatState.ChatMessage();
        chatMessage.sender = finalSenderName;
        chatMessage.message = message;
        chatMessage.timestamp = System.currentTimeMillis();
        chatState.messages.add(chatMessage);
        stateManager.updateChatState(chatState);

        // 非同期でバックエンドに送信
        new Thread(() -> {
            try {
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("sender", finalSenderName);
                requestBody.put("message", message);

                BackendClient.postJson(ModConfig.ENDPOINT_CHAT_MESSAGE, requestBody);

                LOGGER.info("💬 Command chat from {}: {}", finalSenderName, message);
            } catch (Exception e) {
                LOGGER.error("Failed to send message to backend: {}", e.getMessage());
            }
        }).start();

        // プレイヤーにフィードバック
        source.sendFeedback(() -> Text.literal("§7[Shannon] §fMessage sent: " + message), false);

        return 1;
    }
}

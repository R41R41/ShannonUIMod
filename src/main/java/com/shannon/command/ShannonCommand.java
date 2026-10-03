package com.shannon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.shannon.client.BackendClient;
import com.shannon.client.request.ChatMessageRequest;
import com.shannon.config.ModConfig;
import com.shannon.model.ChatState;
import com.shannon.state.StateManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** {@code /shannon <message>}: says something to the bot, for players without the client mod. */
public final class ShannonCommand {
    private ShannonCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("shannon")
                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                        .executes(ShannonCommand::say)));
    }

    private static int say(CommandContext<ServerCommandSource> context) {
        String message = StringArgumentType.getString(context, "message");
        String sender = context.getSource().getName();
        StateManager.getInstance().addChat(
                new ChatState.Message(ChatState.Kind.CHAT, sender, message, System.currentTimeMillis()));
        BackendClient.postJson(ModConfig.ENDPOINT_CHAT_MESSAGE, new ChatMessageRequest(sender, message));
        context.getSource().sendFeedback(
                () -> Text.translatableWithFallback("shannonuimod.command.sent", "シャノンに送りました: %s", message).formatted(Formatting.GRAY), false);
        return 1;
    }
}

package com.shannon.ui.screen;

import com.shannon.model.ChatState;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** How one line of the conversation reads, shared by the quick chat and the conversation tab. */
public final class ChatLine {
    private ChatLine() {
    }

    public static Text text(ChatState.Message message) {
        String body = message.message == null ? "" : message.message;
        return switch (message.kind()) {
            case TASK_STARTED -> Text.translatable("shannonuimod.chat.task_started", body).formatted(Formatting.GRAY);
            case TASK_DONE -> Text.translatable("shannonuimod.chat.task_done", body).formatted(Formatting.GREEN);
            case TASK_ERROR -> Text.translatable("shannonuimod.chat.task_error", body).formatted(Formatting.RED);
            case CHAT -> {
                MutableText name = message.fromBot()
                        ? Text.translatable("shannonuimod.name").formatted(Formatting.AQUA)
                        : Text.literal(message.sender == null ? "?" : message.sender);
                yield Text.literal("<").append(name).append(Text.literal("> ")).append(Text.literal(body));
            }
        };
    }
}

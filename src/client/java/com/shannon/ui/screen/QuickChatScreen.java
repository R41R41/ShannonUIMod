package com.shannon.ui.screen;

import com.shannon.model.ChatState;
import com.shannon.sync.StateChannels;
import com.shannon.ui.ShannonClient;
import com.shannon.ui.gfx.Gui;
import com.shannon.ui.gfx.Palette;
import com.shannon.ui.net.ClientActions;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * One line to talk to the bot, opened with a single key. Laid out like vanilla's chat input, with
 * the recent conversation above it.
 *
 * <p>The input is vanilla's text field, so IME composition, paste, selection and cursor keys all
 * behave as in chat.
 */
public class QuickChatScreen extends OverlayScreen {
    private static final int HISTORY_LINES = 8;
    private static final int HISTORY_WIDTH = 320;

    private TextFieldWidget input;

    public QuickChatScreen() {
        super(Text.translatable("shannonuimod.talk.title"));
    }

    @Override
    protected void init() {
        int prefix = Gui.width(prefix()) + 8;
        input = new TextFieldWidget(textRenderer, 4 + prefix, height - 12, width - 8 - prefix, 12,
                Text.translatable("shannonuimod.talk.title"));
        input.setMaxLength(256);
        input.setDrawsBackground(false);
        input.setPlaceholder(Text.translatable("shannonuimod.talk.placeholder").formatted(net.minecraft.util.Formatting.DARK_GRAY));
        addDrawableChild(input);
        setInitialFocus(input);
    }

    private static Text prefix() {
        return Text.translatable("shannonuimod.talk.prefix");
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            String message = this.input.getText().strip();
            if (!message.isEmpty()) {
                ClientActions.chat(message);
            }
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(2, height - 14, width - 2, height - 2, Palette.HUD_BOX);
        Gui.text(context, prefix(), 6, height - 12, Palette.AQUA);
        renderHistory(context);
        renderHints(context);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private void renderHistory(DrawContext context) {
        ChatState chat = ShannonClient.get().store().get(StateChannels.CHAT);
        if (chat == null || chat.messages == null) {
            return;
        }
        int maxWidth = Math.min(HISTORY_WIDTH, width - 8);
        List<OrderedText> lines = new ArrayList<>();
        for (int i = chat.messages.size() - 1; i >= 0 && lines.size() < HISTORY_LINES; i--) {
            List<OrderedText> wrapped = Gui.wrap(ChatLine.text(chat.messages.get(i)), maxWidth - 4);
            lines.addAll(0, wrapped);
        }
        while (lines.size() > HISTORY_LINES) {
            lines.remove(0);
        }
        int y = height - 34 - lines.size() * 9;
        for (OrderedText line : lines) {
            context.fill(2, y - 1, 2 + maxWidth, y + 8, 0x80000000);
            Gui.text(context, line, 4, y, Palette.WHITE);
            y += 9;
        }
    }

    private void renderHints(DrawContext context) {
        Text send = Text.translatable("shannonuimod.talk.send");
        Text close = Text.translatable("shannonuimod.talk.close");
        Text enter = Text.translatable("key.keyboard.enter");
        Text escape = Text.translatable("key.keyboard.escape");
        int width = Gui.keyHintWidth(enter, send) + 10 + Gui.keyHintWidth(escape, close);
        int x = this.width - width - 6;
        int y = height - 30;
        context.fill(x - 4, y - 2, x + width + 4, y + 14, 0x80000000);
        x += Gui.keyHint(context, enter, send, x, y) + 10;
        Gui.keyHint(context, escape, close, x, y);
    }
}

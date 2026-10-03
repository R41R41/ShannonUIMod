package com.shannon.ui.screen;

import com.shannon.model.ChatState;
import com.shannon.model.TaskTreeState;
import com.shannon.sync.StateChannels;
import com.shannon.ui.ShannonClient;
import com.shannon.ui.gfx.Gui;
import com.shannon.ui.gfx.Palette;
import com.shannon.ui.net.ClientActions;
import com.shannon.ui.state.BotStatus;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * One line to talk to the bot, opened with a single key. Laid out like vanilla's chat input, with
 * the recent conversation above it.
 *
 * <p>The input is vanilla's text field, so IME composition, paste, selection and cursor keys all
 * behave as in chat. While the bot waits for an answer, ready-made answers sit above the input:
 * a click or their number key, with the input still empty, sends one.
 */
public class QuickChatScreen extends OverlayScreen {
    private static final int HISTORY_LINES = 8;
    private static final int HISTORY_WIDTH = 320;

    private static final int MAX_CHOICES = 4;
    private static final int CHIP_H = 14;

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
        input.setPlaceholder(Text.translatable(choices().isEmpty() ? "shannonuimod.talk.placeholder"
                : "shannonuimod.talk.placeholder.choices").formatted(Formatting.DARK_GRAY));
        addDrawableChild(input);
        setInitialFocus(input);
    }

    private static Text prefix() {
        return Text.translatable(ClientActions.talksInGameChat()
                ? "shannonuimod.talk.prefix.public" : "shannonuimod.talk.prefix");
    }

    /** The answers offered now: the bot's own, or general ones; empty unless it waits. */
    private static List<Text> choices() {
        ShannonClient shannon = ShannonClient.get();
        if (shannon.store().status() != BotStatus.WAITING) {
            return List.of();
        }
        TaskTreeState tree = shannon.store().taskTree();
        List<Text> choices = new ArrayList<>();
        if (tree != null && tree.replyChoices != null) {
            for (String choice : tree.replyChoices) {
                if (choice != null && !choice.isBlank() && choices.size() < MAX_CHOICES) {
                    choices.add(Text.literal(choice.strip()));
                }
            }
        }
        if (choices.isEmpty()) {
            choices.add(Text.translatable("shannonuimod.reply.yes"));
            choices.add(Text.translatable("shannonuimod.reply.no"));
            choices.add(Text.translatable("shannonuimod.reply.up_to_you"));
        }
        return choices;
    }

    private static int chipWidth(Text choice) {
        return 4 + 10 + 4 + Gui.width(choice) + 6;
    }

    private int choiceAt(double mouseX, double mouseY) {
        List<Text> choices = choices();
        int x = 4;
        int y = height - 32;
        for (int i = 0; i < choices.size(); i++) {
            int w = chipWidth(choices.get(i));
            if (Gui.inside(mouseX, mouseY, x, y, w, CHIP_H)) {
                return i;
            }
            x += w + 4;
        }
        return -1;
    }

    private void answer(Text choice) {
        ClientActions.talk(choice.getString());
        close();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int index = choiceAt(click.x(), click.y());
        if (index >= 0) {
            answer(choices().get(index));
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        List<Text> choices = choices();
        if (this.input.getText().isEmpty() && key >= GLFW.GLFW_KEY_1 && key < GLFW.GLFW_KEY_1 + choices.size()) {
            answer(choices.get(key - GLFW.GLFW_KEY_1));
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            String message = this.input.getText().strip();
            if (!message.isEmpty()) {
                ClientActions.talk(message);
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
        List<Text> choices = choices();
        renderChoices(context, choices, mouseX, mouseY);
        renderHistory(context, choices.isEmpty() ? 0 : CHIP_H + 4);
        renderHints(context);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private void renderChoices(DrawContext context, List<Text> choices, int mouseX, int mouseY) {
        int x = 4;
        int y = height - 32;
        for (int i = 0; i < choices.size(); i++) {
            Text choice = choices.get(i);
            int w = chipWidth(choice);
            boolean hovered = Gui.inside(mouseX, mouseY, x, y, w, CHIP_H);
            context.fill(x, y, x + w, y + CHIP_H, hovered ? 0xC0303030 : 0x99000000);
            Gui.outline(context, x, y, w, CHIP_H, hovered ? Palette.WHITE : 0xFF6B6B6B);
            Gui.keycap(context, Text.literal(String.valueOf(i + 1)), x + 2, y + 1);
            Gui.text(context, choice, x + 18, y + 3, Palette.WHITE);
            x += w + 4;
        }
    }

    private void renderHistory(DrawContext context, int raise) {
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
        int y = height - 34 - raise - lines.size() * 9;
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

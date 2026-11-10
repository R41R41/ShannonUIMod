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

    private static final int MAX_CHOICES = 6;
    private static final int CHIP_H = 14;
    private static final int CHIP_GAP = 4;

    private TextFieldWidget input;

    public QuickChatScreen() {
        super(Text.translatable("shannonuimod.talk.title"));
    }

    @Override
    protected void init() {
        int prefix = Gui.width(prefix()) + 8;
        input = new TextFieldWidget(textRenderer, 4 + prefix, height - 12, width - 8 - prefix - hintsWidth() - 12, 12,
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

    /** Room the keycap and padding take inside a chip, besides the answer's text. */
    private static final int CHIP_PAD = 24;

    /**
     * Where each answer sits: rows above the input, filled left to right and wrapping upwards, so no
     * answer is hidden behind another element however many there are. A long answer wraps inside its
     * chip, which grows taller, so every word of it stays readable. {x, y, width, height} each.
     */
    private List<int[]> chipLayout(List<Text> choices) {
        int right = width - 4;
        List<List<int[]>> rows = new ArrayList<>();
        List<int[]> row = new ArrayList<>();
        int x = 4;
        for (Text choice : choices) {
            int w = Math.min(chipWidth(choice), right - 4);
            if (x > 4 && x + w > right) {
                rows.add(row);
                row = new ArrayList<>();
                x = 4;
            }
            int h = Math.max(CHIP_H, chipLines(choice, w).size() * Gui.LINE + 5);
            row.add(new int[]{x, 0, w, h});
            x += w + CHIP_GAP;
        }
        if (!row.isEmpty()) {
            rows.add(row);
        }
        List<int[]> boxes = new ArrayList<>();
        int bottom = height - 18;
        for (List<int[]> r : rows) {
            int rowH = 0;
            for (int[] box : r) {
                rowH = Math.max(rowH, box[3]);
            }
            for (int[] box : r) {
                box[1] = bottom - box[3];
                boxes.add(box);
            }
            bottom -= rowH + CHIP_GAP;
        }
        return boxes;
    }

    /** The answer's text as it fits a chip of width {@code w}: whole on one line when it can be. */
    private static List<OrderedText> chipLines(Text choice, int w) {
        if (Gui.width(choice) <= w - CHIP_PAD) {
            return List.of(choice.asOrderedText());
        }
        return Gui.wrap(choice, w - CHIP_PAD);
    }

    /** Height the chips take above the input, for the conversation drawn above them. */
    private int chipsHeight(List<int[]> boxes) {
        int top = height - 18;
        for (int[] box : boxes) {
            top = Math.min(top, box[1]);
        }
        // From the input bar's top edge up to just above the highest chip.
        return boxes.isEmpty() ? 0 : height - 14 - top + 2;
    }

    private int choiceAt(double mouseX, double mouseY) {
        List<int[]> boxes = chipLayout(choices());
        for (int i = 0; i < boxes.size(); i++) {
            int[] box = boxes.get(i);
            if (Gui.inside(mouseX, mouseY, box[0], box[1], box[2], box[3])) {
                return i;
            }
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
        List<int[]> boxes = chipLayout(choices);
        renderChoices(context, choices, boxes, mouseX, mouseY);
        renderHistory(context, chipsHeight(boxes));
        renderHints(context);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private void renderChoices(DrawContext context, List<Text> choices, List<int[]> boxes, int mouseX, int mouseY) {
        for (int i = 0; i < choices.size(); i++) {
            int x = boxes.get(i)[0], y = boxes.get(i)[1], w = boxes.get(i)[2], h = boxes.get(i)[3];
            boolean hovered = Gui.inside(mouseX, mouseY, x, y, w, h);
            context.fill(x, y, x + w, y + h, hovered ? 0xC0303030 : 0x99000000);
            Gui.outline(context, x, y, w, h, hovered ? Palette.WHITE : 0xFF6B6B6B);
            Gui.keycap(context, Text.literal(String.valueOf(i + 1)), x + 2, y + 1);
            List<OrderedText> lines = chipLines(choices.get(i), w);
            for (int line = 0; line < lines.size(); line++) {
                Gui.text(context, lines.get(line), x + 18, y + 3 + line * Gui.LINE, Palette.WHITE);
            }
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
        // Never reach up into the status card in the top-left corner: keep only the newest lines that fit below it.
        int bottom = height - 16 - raise;
        int fit = Math.max(0, (bottom - historyTop()) / 9);
        while (lines.size() > Math.min(HISTORY_LINES, fit)) {
            lines.remove(0);
        }
        int y = bottom - lines.size() * 9;
        for (OrderedText line : lines) {
            context.fill(2, y - 1, 2 + maxWidth, y + 8, 0x80000000);
            Gui.text(context, line, 4, y, Palette.WHITE);
            y += 9;
        }
    }

    private static int hintsWidth() {
        return Gui.keyHintWidth(Text.translatable("key.keyboard.enter"), Text.translatable("shannonuimod.talk.send"))
                + 10 + Gui.keyHintWidth(Text.translatable("key.keyboard.escape"), Text.translatable("shannonuimod.talk.close"));
    }

    /** The highest the conversation may start: just below the status card when it sits top-left. */
    private int historyTop() {
        ShannonClient shannon = ShannonClient.get();
        var config = shannon.config();
        if (!config.showCard || config.cardCorner != com.shannon.ui.ClientConfig.Corner.TOP_LEFT
                || client == null || client.options.hudHidden) {
            return 2;
        }
        return 4 + com.shannon.ui.hud.StatusCard.prepare(shannon).height() + 4;
    }

    /** Inside the input bar, at its right end: the answers above it never run under the hints. */
    private void renderHints(DrawContext context) {
        Text send = Text.translatable("shannonuimod.talk.send");
        Text close = Text.translatable("shannonuimod.talk.close");
        Text enter = Text.translatable("key.keyboard.enter");
        Text escape = Text.translatable("key.keyboard.escape");
        int x = this.width - hintsWidth() - 6;
        int y = height - 14;
        x += Gui.keyHint(context, enter, send, x, y) + 10;
        Gui.keyHint(context, escape, close, x, y);
    }
}

package com.shannon.ui.screen.tab;

import com.shannon.ShannonUIMod;
import com.shannon.model.ChatState;
import com.shannon.sync.StateChannels;
import com.shannon.ui.gfx.Gui;
import com.shannon.ui.gfx.Palette;
import com.shannon.ui.net.ClientActions;
import com.shannon.ui.screen.ChatLine;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** The whole conversation with the bot, with task events, a filter and an input line. */
public class ChatTab extends ShannonTab {
    private static final Identifier ICON = Identifier.of(ShannonUIMod.MOD_ID, "textures/chat.png");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
    private static final int TIME_W = 30;

    private enum Filter {
        ALL, BOT, EVENTS;

        boolean accepts(ChatState.Message message) {
            return switch (this) {
                case ALL -> true;
                case BOT -> message.fromBot();
                case EVENTS -> message.kind() != ChatState.Kind.CHAT;
            };
        }
    }

    /** One wrapped line of the log and the message it belongs to. */
    private record Line(OrderedText text, String time, ChatState.Message message) {
    }

    private static Filter filter = Filter.ALL;
    private final List<ButtonWidget> filterButtons = new ArrayList<>();
    private TextFieldWidget input;
    /** Lines scrolled up from the bottom; 0 follows new messages. */
    private int scrollUp;

    @Override
    public Identifier icon() {
        return ICON;
    }

    @Override
    public Text title() {
        return Text.translatable("shannonuimod.tab.chat");
    }

    @Override
    protected void build() {
        filterButtons.clear();
        int chipX = x + w;
        Filter[] filters = Filter.values();
        for (int i = filters.length - 1; i >= 0; i--) {
            Filter value = filters[i];
            Text label = Text.translatable("shannonuimod.chat.filter." + value.name().toLowerCase());
            int chipW = Gui.width(label) + 12;
            chipX -= chipW;
            ButtonWidget chip = ButtonWidget.builder(label, button -> {
                filter = value;
                scrollUp = 0;
                updateChips();
            }).dimensions(chipX, y - 13, chipW, 12).build();
            filterButtons.add(0, add(chip));
            chipX -= 2;
        }
        updateChips();

        int sendW = 44;
        input = add(new TextFieldWidget(client.textRenderer, x + 1, y + h - 19, w - sendW - 6, 18,
                Text.translatable("shannonuimod.talk.title")));
        input.setMaxLength(256);
        input.setPlaceholder(Text.translatable("shannonuimod.talk.placeholder").formatted(Formatting.DARK_GRAY));
        add(ButtonWidget.builder(Text.translatable("shannonuimod.talk.send"), button -> send())
                .dimensions(x + w - sendW, y + h - 20, sendW, 20)
                .build());
    }

    private void updateChips() {
        Filter[] filters = Filter.values();
        for (int i = 0; i < filterButtons.size(); i++) {
            filterButtons.get(i).active = filters[i] != filter;
        }
    }

    @Override
    public void onShow() {
        shannon.store().markRead();
    }

    private void send() {
        String message = input.getText().strip();
        if (!message.isEmpty() && ClientActions.chat(message)) {
            input.setText("");
            scrollUp = 0;
        }
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int key = keyInput.key();
        if (input != null && input.isFocused() && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            send();
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        shannon.store().markRead();
    }

    private List<Line> lines(int width) {
        List<Line> lines = new ArrayList<>();
        ChatState chat = shannon.store().get(StateChannels.CHAT);
        if (chat == null || chat.messages == null) {
            return lines;
        }
        for (ChatState.Message message : chat.messages) {
            if (!filter.accepts(message)) {
                continue;
            }
            List<OrderedText> wrapped = Gui.wrap(ChatLine.text(message), width);
            String time = message.timestamp > 0 ? TIME.format(Instant.ofEpochMilli(message.timestamp)) : "";
            for (int i = 0; i < wrapped.size(); i++) {
                lines.add(new Line(wrapped.get(i), i == 0 ? time : "", message));
            }
        }
        return lines;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        int logTop = y + 2;
        int logH = h - 26;
        Gui.inset(context, x, logTop, w, logH, 0xFF101010);
        int textX = x + 6 + TIME_W;
        int textW = w - 12 - TIME_W;
        List<Line> lines = lines(textW);
        int visible = (logH - 8) / 10;
        int maxUp = Math.max(0, lines.size() - visible);
        scrollUp = Math.max(0, Math.min(scrollUp, maxUp));
        int first = Math.max(0, lines.size() - visible - scrollUp);
        int last = Math.min(lines.size(), first + visible);
        if (lines.isEmpty()) {
            Gui.text(context, Text.translatable("shannonuimod.chat.empty"), x + 6, logTop + 5, Palette.GRAY);
        }
        ChatState.Message hovered = null;
        for (int i = first; i < last; i++) {
            Line line = lines.get(i);
            int lineY = logTop + 5 + (i - first) * 10;
            if (Gui.inside(mouseX, mouseY, x + 2, lineY - 1, w - 4, 10)) {
                hovered = line.message();
            }
        }
        for (int i = first; i < last; i++) {
            Line line = lines.get(i);
            int lineY = logTop + 5 + (i - first) * 10;
            if (hovered != null && line.message() == hovered) {
                context.fill(x + 2, lineY - 1, x + w - 2, lineY + 9, 0x1AFFFFFF);
            }
            if (!line.time().isEmpty()) {
                Gui.text(context, Text.literal(line.time()), x + 6, lineY, Palette.DARK_GRAY);
            }
            Gui.text(context, line.text(), textX, lineY, Palette.WHITE);
        }
        if (hovered != null) {
            screen.tooltip(List.of(Text.translatable("shannonuimod.chat.copy").formatted(Formatting.AQUA)));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int logTop = y + 2;
        int logH = h - 26;
        if (!Gui.inside(mouseX, mouseY, x, logTop, w, logH)) {
            return false;
        }
        List<Line> lines = lines(w - 12 - TIME_W);
        int visible = (logH - 8) / 10;
        int first = Math.max(0, lines.size() - visible - scrollUp);
        int index = first + (int) ((mouseY - logTop - 4) / 10);
        if (index >= first && index < lines.size()) {
            ChatState.Message message = lines.get(index).message();
            client.keyboard.setClipboard(ChatLine.text(message).getString());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        scrollUp += (int) Math.signum(amount) * 2;
        return true;
    }
}

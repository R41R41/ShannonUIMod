package com.shannon.ui.screen;

import com.shannon.model.DetailedLogsState;
import com.shannon.sync.StateChannels;
import com.shannon.ui.ShannonClient;
import com.shannon.ui.gfx.Gui;
import com.shannon.ui.gfx.Palette;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** The bot's developer log, for when something needs looking into. */
public class DebugLogScreen extends Screen {
    private final Screen parent;
    private int scrollUp;

    public DebugLogScreen(Screen parent) {
        super(Text.translatable("shannonuimod.logs.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close())
                .dimensions(width / 2 - 100, height - 28, 200, 20).build());
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, Palette.WHITE);
        int left = 20;
        int top = 28;
        int boxW = width - 40;
        int boxH = height - 64;
        Gui.inset(context, left, top, boxW, boxH, 0xFF101010);
        List<OrderedText> lines = new ArrayList<>();
        DetailedLogsState logs = ShannonClient.get().store().get(StateChannels.LOGS);
        if (logs != null && logs.logs != null) {
            for (DetailedLogsState.LogEntry entry : logs.logs) {
                int color = switch (entry.level == null ? "" : entry.level) {
                    case "error" -> Palette.RED;
                    case "warning" -> Palette.GOLD;
                    case "success" -> Palette.GREEN;
                    default -> Palette.GRAY;
                };
                String time = entry.timestamp != null && entry.timestamp.length() >= 19
                        ? entry.timestamp.substring(11, 19) : "";
                Text line = Text.literal(time + " ").withColor(Palette.DARK_GRAY)
                        .append(Text.literal("[" + (entry.source == null ? "?" : entry.source) + "] ").withColor(color))
                        .append(Text.literal(entry.content == null ? "" : entry.content).withColor(Palette.WHITE));
                lines.addAll(Gui.wrap(line, boxW - 12));
            }
        }
        if (lines.isEmpty()) {
            Gui.text(context, Text.translatable("shannonuimod.logs.empty"), left + 6, top + 6, Palette.GRAY);
            return;
        }
        int visible = (boxH - 10) / 10;
        scrollUp = Math.max(0, Math.min(scrollUp, Math.max(0, lines.size() - visible)));
        int first = Math.max(0, lines.size() - visible - scrollUp);
        for (int i = first; i < Math.min(lines.size(), first + visible); i++) {
            Gui.text(context, lines.get(i), left + 6, top + 6 + (i - first) * 10, Palette.WHITE);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollUp += (int) Math.signum(verticalAmount) * 3;
        return true;
    }
}

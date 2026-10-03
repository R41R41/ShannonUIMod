package com.shannon.ui.screen.tab;

import com.shannon.ui.ShannonClient;
import com.shannon.ui.gfx.Gui;
import com.shannon.ui.gfx.Icons;
import com.shannon.ui.gfx.Palette;
import com.shannon.ui.gfx.PixelIcon;
import com.shannon.ui.screen.ShannonScreen;
import com.shannon.ui.state.TaskHistory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * The tasks tab's log: recent tasks on the left, and for the chosen one what happened, what the
 * bot picked up and how long it took.
 */
final class HistoryView {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
    private static final int ROW = 14;
    private static final int LINE = 11;
    private static final int TIME_W = 30;
    /** Height of the summary under the log: a row of items and a line of numbers. */
    private static final int SUMMARY_H = 34;

    /** Survives reopening the screen. */
    private static int selected;
    private int listScroll;
    private int eventScroll;

    private final ShannonClient shannon;

    HistoryView(ShannonClient shannon) {
        this.shannon = shannon;
    }

    /** The chosen record, or {@code null} when there is none yet. */
    TaskHistory.Record record() {
        List<TaskHistory.Record> records = shannon.history().records();
        if (records.isEmpty()) {
            return null;
        }
        selected = Math.max(0, Math.min(selected, records.size() - 1));
        return records.get(selected);
    }

    void render(DrawContext context, ShannonScreen screen, int x, int y, int listW, int w, int h, int mouseX, int mouseY) {
        List<TaskHistory.Record> records = shannon.history().records();
        Gui.label(context, Text.translatable("shannonuimod.history.tasks"), x + 1, y);
        Gui.inset(context, x, y + 10, listW, h - 10, Palette.SLOT);
        int rightX = x + listW + 6;
        int rightW = w - listW - 6;
        Gui.label(context, Text.translatable("shannonuimod.history.log"), rightX + 1, y);
        Gui.inset(context, rightX, y + 10, rightW, h - 10, Palette.SLOT);
        if (records.isEmpty()) {
            Gui.paragraph(context, Text.translatable("shannonuimod.history.none"), x + 6, y + 16, listW - 12, Palette.WHITE, 6);
            return;
        }
        TaskHistory.Record chosen = record();
        int visible = (h - 14) / ROW;
        listScroll = Math.max(0, Math.min(listScroll, records.size() - visible));
        for (int i = 0; i < Math.min(visible, records.size() - listScroll); i++) {
            TaskHistory.Record record = records.get(i + listScroll);
            int rowY = y + 12 + i * ROW;
            if (record == chosen) {
                context.fill(x + 2, rowY, x + listW - 2, rowY + ROW, 0x2E000000);
                Gui.outline(context, x + 2, rowY, listW - 4, ROW, Palette.WHITE);
            } else if (Gui.inside(mouseX, mouseY, x + 2, rowY, listW - 4, ROW)) {
                context.fill(x + 2, rowY, x + listW - 2, rowY + ROW, 0x20FFFFFF);
            }
            outcomeIcon(record.outcome).draw(context, x + 5, rowY + 3);
            Gui.text(context, Gui.fit(record.goal, listW - 24), x + 17, rowY + 3, Palette.WHITE);
        }
        renderLog(context, chosen, rightX + 5, y + 15, rightW - 10, h - 20 - SUMMARY_H);
        renderSummary(context, screen, chosen, rightX + 5, y + h - SUMMARY_H - 2, rightW - 10, mouseX, mouseY);
    }

    private static PixelIcon outcomeIcon(TaskHistory.Outcome outcome) {
        return switch (outcome) {
            case RUNNING -> Icons.PICKAXE;
            case DONE -> Icons.CHECK;
            case FAILED -> Icons.WARNING;
        };
    }

    private void renderLog(DrawContext context, TaskHistory.Record record, int x, int y, int w, int h) {
        List<TaskHistory.Event> events = record.events;
        int visible = h / LINE;
        // Newest at the bottom, like chat; scrolling goes back in time.
        int maxScroll = Math.max(0, events.size() - visible);
        eventScroll = Math.max(0, Math.min(eventScroll, maxScroll));
        int first = Math.max(0, events.size() - visible - eventScroll);
        int last = Math.min(events.size(), first + visible);
        for (int i = first; i < last; i++) {
            TaskHistory.Event event = events.get(i);
            int lineY = y + (i - first) * LINE;
            Gui.label(context, Text.literal(TIME.format(Instant.ofEpochMilli(event.at))), x, lineY);
            int textX = x + TIME_W;
            PixelIcon icon = icon(event.kind);
            if (icon != null) {
                icon.draw(context, textX, lineY);
            }
            Text text = describe(event);
            Gui.text(context, Gui.fit(text.getString(), w - TIME_W - 12), textX + 12, lineY, color(event.kind));
        }
    }

    private static PixelIcon icon(TaskHistory.Kind kind) {
        return switch (kind) {
            case STARTED -> Icons.PLAY;
            case QUESTION -> Icons.QUESTION;
            case DANGER, FAILED -> Icons.WARNING;
            case DONE -> Icons.CHECK;
            case GAINED, ANSWER -> null;
        };
    }

    private static int color(TaskHistory.Kind kind) {
        return switch (kind) {
            case GAINED -> Palette.GREEN;
            case ANSWER -> Palette.AQUA;
            case DANGER, FAILED -> Palette.RED;
            default -> Palette.WHITE;
        };
    }

    private static Text describe(TaskHistory.Event event) {
        return switch (event.kind) {
            case STARTED -> Text.translatable("shannonuimod.history.started", event.text);
            case GAINED -> Text.translatable("shannonuimod.history.gained", event.count,
                    Gui.stack(event.item, 1).isEmpty() ? Text.literal(event.item) : Gui.stack(event.item, 1).getName());
            case QUESTION -> Text.literal(event.text);
            case ANSWER -> Text.translatable("shannonuimod.history.answer", event.text);
            case DANGER -> Text.literal(event.text);
            case DONE -> Text.translatable("shannonuimod.history.done");
            case FAILED -> Text.translatable("shannonuimod.history.failed");
        };
    }

    private static void renderSummary(DrawContext context, ShannonScreen screen, TaskHistory.Record record,
                                      int x, int y, int w, int mouseX, int mouseY) {
        context.fill(x, y - 3, x + w, y - 2, 0xFF6E6E6E);
        int slotX = x;
        int shown = 0;
        int maxSlots = Math.max(1, (w - 2) / 18);
        for (Map.Entry<String, Integer> gained : record.gained.entrySet()) {
            if (shown++ >= maxSlots) {
                break;
            }
            Gui.slot(context, slotX, y);
            Gui.item(context, gained.getKey(), gained.getValue(), slotX + 1, y + 1);
            if (Gui.inside(mouseX, mouseY, slotX, y, 18, 18)) {
                screen.tooltip(List.of(Gui.stack(gained.getKey(), 1).getName()));
            }
            slotX += 18;
        }
        if (record.gained.isEmpty()) {
            Gui.label(context, Text.translatable("shannonuimod.history.nothing_gained"), x, y + 5);
        }
        long minutes = Math.max(0, record.duration() / 60_000);
        Text numbers = Text.translatable("shannonuimod.history.numbers", minutes, Math.round(record.walked),
                record.questions, record.dangers);
        Gui.text(context, Gui.fit(numbers.getString(), w), x, y + 22, Palette.WHITE);
    }

    boolean mouseClicked(double mouseX, double mouseY, int x, int y, int listW, int h) {
        if (!Gui.inside(mouseX, mouseY, x, y + 12, listW, h - 12)) {
            return false;
        }
        int index = (int) ((mouseY - y - 12) / ROW) + listScroll;
        if (index >= 0 && index < shannon.history().records().size()) {
            selected = index;
            eventScroll = 0;
            return true;
        }
        return false;
    }

    boolean mouseScrolled(double mouseX, double amount, int x, int listW) {
        if (mouseX < x + listW) {
            listScroll -= (int) Math.signum(amount);
        } else {
            eventScroll += (int) Math.signum(amount);
        }
        return true;
    }
}

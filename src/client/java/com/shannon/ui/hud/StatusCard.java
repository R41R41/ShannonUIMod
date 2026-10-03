package com.shannon.ui.hud;

import com.shannon.model.BotVitals;
import com.shannon.model.ChatState;
import com.shannon.model.TaskTreeState;
import com.shannon.sync.StateChannels;
import com.shannon.ui.ShannonClient;
import com.shannon.ui.gfx.Gui;
import com.shannon.ui.gfx.Icons;
import com.shannon.ui.gfx.Palette;
import com.shannon.ui.input.KeyBindings;
import com.shannon.ui.state.BotStatus;
import com.shannon.ui.state.ClientStore;
import com.shannon.ui.state.TaskView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * The HUD card: who the bot is, what it is doing, and what it needs from the player.
 *
 * <p>The card is laid out as a list of rows, measured first and drawn second, so adding a row is
 * one entry in {@link #rows}.
 */
public final class StatusCard {
    public static final int WIDTH = 180;
    private static final int PAD = 6;
    private static final int GAP = 4;
    private static final int INNER = WIDTH - PAD * 2;

    private interface Row {
        int height();

        void draw(DrawContext context, int x, int y);
    }

    private StatusCard() {
    }

    /** Draws the card with its top-left corner at {@code x, y} and returns its height. */
    public static int render(DrawContext context, ShannonClient shannon, int x, int y) {
        List<Row> rows = rows(shannon);
        int height = PAD * 2 - GAP;
        for (Row row : rows) {
            height += row.height() + GAP;
        }
        BotStatus status = shannon.store().status();
        int edge = switch (status) {
            case WAITING -> Palette.WAITING_EDGE;
            case ERROR -> Palette.ERROR_EDGE;
            default -> Palette.HUD_EDGE;
        };
        Gui.hudBox(context, x, y, WIDTH, height, edge);
        int cursor = y + PAD;
        for (Row row : rows) {
            row.draw(context, x + PAD, cursor);
            cursor += row.height() + GAP;
        }
        return height;
    }

    private static List<Row> rows(ShannonClient shannon) {
        ClientStore store = shannon.store();
        BotStatus status = store.status();
        TaskTreeState tree = store.taskTree();
        BotVitals vitals = store.get(StateChannels.VITALS);
        List<Row> rows = new ArrayList<>();
        rows.add(header(shannon, status, vitals));

        String goal = tree != null ? TaskView.blankToNull(tree.goal) : null;
        switch (status) {
            case IDLE -> {
                rows.add(line(Text.translatable("shannonuimod.card.idle"), Palette.GRAY));
                if (goal != null) {
                    rows.add(line(Text.translatable("shannonuimod.card.last", goal), Palette.GRAY));
                }
            }
            case WORKING -> {
                if (goal != null) {
                    rows.add(line(Text.literal(goal), Palette.WHITE));
                }
                int[] progress = TaskView.progress(tree);
                if (progress[1] > 0) {
                    rows.add(progressRow(progress[0], progress[1]));
                }
                String action = TaskView.currentAction(tree);
                if (action != null) {
                    rows.add(labelled(Text.translatable("shannonuimod.card.now"), action));
                }
            }
            case WAITING -> {
                if (goal != null) {
                    rows.add(line(Text.literal(goal), Palette.GRAY));
                }
                rows.add(divider());
                ChatState.Message question = TaskView.lastBotMessage(store.get(StateChannels.CHAT));
                Text ask = question != null
                        ? Text.literal(question.message)
                        : Text.translatable("shannonuimod.card.waiting");
                rows.add(paragraph(ask, Palette.WHITE, 4));
                rows.add(hints(shannon, true));
            }
            case ERROR -> {
                if (goal != null) {
                    rows.add(line(Text.literal(goal), Palette.GRAY));
                }
                String error = tree != null ? TaskView.blankToNull(tree.error) : null;
                rows.add(paragraph(error != null ? Text.literal(error) : Text.translatable("shannonuimod.card.error"),
                        Palette.WHITE, 3));
                rows.add(hints(shannon, false));
            }
        }
        if (vitals != null && vitals.online) {
            rows.add(vitalsRow(vitals));
        }
        return rows;
    }

    // ===== Rows =====

    private static Row header(ShannonClient shannon, BotStatus status, BotVitals vitals) {
        BotLocator where = BotLocator.locate(MinecraftClient.getInstance(), shannon.botName(), vitals);
        boolean offline = vitals != null && !vitals.online;
        return new Row() {
            @Override
            public int height() {
                return 18;
            }

            @Override
            public void draw(DrawContext context, int x, int y) {
                Gui.face(context, x, y, 18);
                Gui.text(context, Text.translatable("shannonuimod.name"), x + 22, y, Palette.WHITE);
                if (offline) {
                    Gui.text(context, Text.translatable("shannonuimod.status.offline"), x + 22, y + 10, Palette.GRAY);
                } else {
                    Icons.status(status).draw(context, x + 22, y + 9);
                    Gui.text(context, Text.translatable(status.labelKey()), x + 34, y + 10, Icons.statusColor(status));
                }
                if (where != null) {
                    Text distance = Text.literal(Math.round(where.distance()) + "m");
                    int textX = x + INNER - Gui.width(distance);
                    Gui.text(context, distance, textX, y, Palette.WHITE);
                    Icons.arrow(where.relativeYaw()).draw(context, textX - 10, y);
                }
            }
        };
    }

    private static Row line(Text text, int color) {
        Text fitted = Gui.fit(text.getString(), INNER);
        return new Row() {
            @Override
            public int height() {
                return 9;
            }

            @Override
            public void draw(DrawContext context, int x, int y) {
                Gui.text(context, fitted, x, y, color);
            }
        };
    }

    private static Row labelled(Text label, String value) {
        int labelWidth = Gui.width(label) + 3;
        Text fitted = Gui.fit(value, INNER - labelWidth);
        return new Row() {
            @Override
            public int height() {
                return 9;
            }

            @Override
            public void draw(DrawContext context, int x, int y) {
                Gui.text(context, label, x, y, Palette.GRAY);
                Gui.text(context, fitted, x + labelWidth, y, Palette.WHITE);
            }
        };
    }

    private static Row paragraph(Text text, int color, int maxLines) {
        int height = Gui.paragraphHeight(text, INNER, maxLines) - 1;
        return new Row() {
            @Override
            public int height() {
                return height;
            }

            @Override
            public void draw(DrawContext context, int x, int y) {
                Gui.paragraph(context, text, x, y, INNER, color, maxLines);
            }
        };
    }

    private static Row progressRow(int done, int total) {
        Text count = Text.literal(done + "/" + total);
        return new Row() {
            @Override
            public int height() {
                return 9;
            }

            @Override
            public void draw(DrawContext context, int x, int y) {
                int countWidth = Gui.width(count);
                Gui.progress(context, x, y + 2, INNER - countWidth - 6, 5, done / (float) total);
                Gui.text(context, count, x + INNER - countWidth, y, Palette.GRAY);
            }
        };
    }

    private static Row divider() {
        return new Row() {
            @Override
            public int height() {
                return 1;
            }

            @Override
            public void draw(DrawContext context, int x, int y) {
                context.fill(x, y, x + INNER, y + 1, 0x24FFFFFF);
            }
        };
    }

    private static Row hints(ShannonClient shannon, boolean canReply) {
        Text talkKey = KeyBindings.keyName(shannon.keys().talk());
        Text commandKey = KeyBindings.keyName(shannon.keys().commands());
        Text reply = Text.translatable("shannonuimod.card.reply");
        Text decide = Text.translatable("shannonuimod.card.decide");
        return new Row() {
            @Override
            public int height() {
                return 12;
            }

            @Override
            public void draw(DrawContext context, int x, int y) {
                int cursor = x;
                if (canReply) {
                    cursor += Gui.keyHint(context, talkKey, reply, cursor, y) + 8;
                }
                Gui.keyHint(context, commandKey, decide, cursor, y);
            }
        };
    }

    private static Row vitalsRow(BotVitals vitals) {
        return new Row() {
            @Override
            public int height() {
                return 9;
            }

            @Override
            public void draw(DrawContext context, int x, int y) {
                Gui.hearts(context, x, y, vitals.health, vitals.maxHealth);
                Gui.food(context, x + INNER - 81, y, vitals.food);
            }
        };
    }
}

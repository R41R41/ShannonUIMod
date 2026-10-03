package com.shannon.ui.hud;

import com.shannon.model.BotVitals;
import com.shannon.model.ChatState;
import com.shannon.model.TaskTreeState;
import com.shannon.sync.StateChannels;
import com.shannon.ui.ClientConfig;
import com.shannon.ui.ShannonClient;
import com.shannon.ui.gfx.Gui;
import com.shannon.ui.gfx.Icons;
import com.shannon.ui.gfx.Palette;
import com.shannon.ui.gfx.PixelIcon;
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
 * <p>It comes in three sizes so it stays out of the way: one line while nothing needs the
 * player, the whole card for a while after something changes, and always the whole card while
 * the bot asks something, fails or is in danger. The full card is a list of rows, measured first
 * and drawn second, so adding a row is one entry in {@link #rows}.
 */
public final class StatusCard {
    public static final int WIDTH = 180;
    private static final int PAD = 6;
    private static final int GAP = 4;
    private static final int INNER = WIDTH - PAD * 2;
    /** Height of the one-line card. */
    private static final int LINE_H = 16;
    /** The longest goal the one-line card shows before cutting it short. */
    private static final int LINE_TEXT_MAX = 112;

    private interface Row {
        int height();

        void draw(DrawContext context, int x, int y);
    }

    /** How much of the card shows. */
    public enum Size {
        FULL, LINE, FACE
    }

    /** A card laid out and measured, ready to draw. */
    public interface Card {
        int width();

        int height();

        void draw(DrawContext context, int x, int y);
    }

    private StatusCard() {
    }

    /** The size the card should have right now, from the player's setting and what is going on. */
    public static Size size(ShannonClient shannon) {
        ClientConfig.CardSize setting = shannon.config().cardSize;
        MinecraftClient client = MinecraftClient.getInstance();
        // Holding the player list key shows everything, as it does for the player list.
        if (setting == ClientConfig.CardSize.FULL || shannon.attention().needed()
                || client.options.playerListKey.isPressed()) {
            return Size.FULL;
        }
        if (shannon.attention().changed()) {
            return setting == ClientConfig.CardSize.FACE ? Size.LINE : Size.FULL;
        }
        return setting == ClientConfig.CardSize.FACE ? Size.FACE : Size.LINE;
    }

    public static Card prepare(ShannonClient shannon) {
        return switch (size(shannon)) {
            case FULL -> full(shannon);
            case LINE -> line(shannon, false);
            case FACE -> line(shannon, true);
        };
    }

    private static int edge(ShannonClient shannon) {
        if (shannon.danger().inDanger()) {
            return Palette.ERROR_EDGE;
        }
        return switch (shannon.store().status()) {
            case WAITING -> Palette.WAITING_EDGE;
            case ERROR -> Palette.ERROR_EDGE;
            default -> Palette.HUD_EDGE;
        };
    }

    private static Card full(ShannonClient shannon) {
        List<Row> rows = rows(shannon);
        int total = PAD * 2 - GAP;
        for (Row row : rows) {
            total += row.height() + GAP;
        }
        int height = total;
        int edge = edge(shannon);
        return new Card() {
            @Override
            public int width() {
                return WIDTH;
            }

            @Override
            public int height() {
                return height;
            }

            @Override
            public void draw(DrawContext context, int x, int y) {
                Gui.hudBox(context, x, y, WIDTH, height, edge);
                int cursor = y + PAD;
                for (Row row : rows) {
                    row.draw(context, x + PAD, cursor);
                    cursor += row.height() + GAP;
                }
            }
        };
    }

    // ===== One line =====

    /** A piece of the one-line card: its width and how to draw it at a left edge. */
    private record Piece(int width, Drawer drawer) {
    }

    @FunctionalInterface
    private interface Drawer {
        void draw(DrawContext context, int x, int y);
    }

    private static Card line(ShannonClient shannon, boolean faceOnly) {
        ClientStore store = shannon.store();
        BotStatus status = store.status();
        BotVitals vitals = store.get(StateChannels.VITALS);
        boolean offline = vitals != null && !vitals.online;
        boolean danger = shannon.danger().inDanger();
        TaskTreeState tree = store.taskTree();
        List<Piece> pieces = new ArrayList<>();
        pieces.add(new Piece(12, (context, x, y) -> Gui.face(context, x, y - 2, 12)));
        if (!offline) {
            PixelIcon icon = danger ? Icons.WARNING : Icons.status(status);
            pieces.add(new Piece(9, (context, x, y) -> icon.draw(context, x, y - 1)));
        }
        if (!faceOnly) {
            Text label;
            int color = Palette.WHITE;
            String goal = tree != null ? TaskView.blankToNull(tree.goal) : null;
            if (offline) {
                label = Text.translatable("shannonuimod.status.offline");
                color = Palette.GRAY;
            } else if (danger) {
                label = shannon.danger().describe();
                color = Palette.RED;
            } else if (status == BotStatus.WORKING && goal != null) {
                label = Text.literal(goal);
            } else {
                label = Text.translatable(status.labelKey());
                color = Icons.statusColor(status);
            }
            Text fitted = Gui.fit(label.getString(), LINE_TEXT_MAX);
            int textColor = color;
            pieces.add(new Piece(Gui.width(fitted), (context, x, y) -> Gui.text(context, fitted, x, y, textColor)));
            int[] progress = TaskView.progress(tree);
            if (status == BotStatus.WORKING && progress[1] > 0) {
                float fraction = progress[0] / (float) progress[1];
                pieces.add(new Piece(24, (context, x, y) -> Gui.progress(context, x, y + 2, 24, 5, fraction)));
            }
            if (vitals != null && vitals.online && vitals.health <= vitals.maxHealth / 2f) {
                Text hearts = Text.literal(formatHearts(vitals.health));
                pieces.add(new Piece(10 + Gui.width(hearts), (context, x, y) -> {
                    Gui.heart(context, x, y - 1, vitals.health < 2f);
                    Gui.text(context, hearts, x + 10, y, Palette.RED);
                }));
            }
        }
        BotLocator where = offline ? null : BotLocator.locate(MinecraftClient.getInstance(), shannon.botName(), vitals);
        if (where != null) {
            Text distance = Text.literal(Math.round(where.distance()) + "m");
            PixelIcon arrow = Icons.arrow(where.relativeYaw());
            pieces.add(new Piece(9 + Gui.width(distance), (context, x, y) -> {
                arrow.draw(context, x, y);
                Gui.text(context, distance, x + 9, y, Palette.WHITE);
            }));
        }
        int gap = 4;
        int content = -gap;
        for (Piece piece : pieces) {
            content += piece.width() + gap;
        }
        int width = content + 8;
        int edge = edge(shannon);
        return new Card() {
            @Override
            public int width() {
                return width;
            }

            @Override
            public int height() {
                return LINE_H;
            }

            @Override
            public void draw(DrawContext context, int x, int y) {
                Gui.hudBox(context, x, y, width, LINE_H, edge);
                int cursor = x + 4;
                for (Piece piece : pieces) {
                    piece.drawer().draw(context, cursor, y + 4);
                    cursor += piece.width() + gap;
                }
            }
        };
    }

    /** Health as hearts, such as {@code 3.5}. */
    private static String formatHearts(float health) {
        float hearts = Math.round(health) / 2f;
        return hearts == Math.floor(hearts) ? String.valueOf((int) hearts) : String.valueOf(hearts);
    }

    private static List<Row> rows(ShannonClient shannon) {
        ClientStore store = shannon.store();
        BotStatus status = store.status();
        TaskTreeState tree = store.taskTree();
        BotVitals vitals = store.get(StateChannels.VITALS);
        boolean danger = shannon.danger().inDanger();
        List<Row> rows = new ArrayList<>();
        rows.add(header(shannon, status, vitals, danger));
        if (danger && status != BotStatus.WAITING) {
            rows.add(paragraph(shannon.danger().describe(), Palette.WHITE, 2));
        }

        String goal = tree != null ? TaskView.blankToNull(tree.goal) : null;
        switch (status) {
            case IDLE -> {
                if (!danger) {
                    rows.add(line(Text.translatable("shannonuimod.card.idle"), Palette.GRAY));
                }
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
        if (danger && status != BotStatus.WAITING && status != BotStatus.ERROR) {
            rows.add(dangerHints(shannon));
        }
        return rows;
    }

    // ===== Rows =====

    private static Row header(ShannonClient shannon, BotStatus status, BotVitals vitals, boolean danger) {
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
                } else if (danger) {
                    Icons.WARNING.draw(context, x + 22, y + 9);
                    Gui.text(context, Text.translatable("shannonuimod.status.danger"), x + 34, y + 10, Palette.RED);
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

    private static Row dangerHints(ShannonClient shannon) {
        Text commandKey = KeyBindings.keyName(shannon.keys().commands());
        Text action = Text.translatable("shannonuimod.card.danger_hint");
        return new Row() {
            @Override
            public int height() {
                return 12;
            }

            @Override
            public void draw(DrawContext context, int x, int y) {
                Gui.keyHint(context, commandKey, action, x, y);
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

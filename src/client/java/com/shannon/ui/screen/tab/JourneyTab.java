package com.shannon.ui.screen.tab;

import com.shannon.ShannonUIMod;
import com.shannon.model.BotVitals;
import com.shannon.model.JourneyState;
import com.shannon.model.TaskTreeState;
import com.shannon.sync.StateChannels;
import com.shannon.ui.gfx.Gui;
import com.shannon.ui.gfx.Icons;
import com.shannon.ui.gfx.Palette;
import com.shannon.ui.state.TaskView;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The way to the Ender Dragon: the milestones as a row of slots, lit when the bot first reached them,
 * with how long this world has been played, the deaths, and the times between milestones like a
 * speedrun's splits.
 */
public class JourneyTab extends ShannonTab {
    private static final Identifier ICON = Identifier.of(ShannonUIMod.MOD_ID, "textures/journey.png");
    private static final int SLOT = 18;
    private static final int TICKS_PER_SECOND = 20;
    /** An unreached milestone's item, greyed as vanilla greys an unearned advancement. */
    private static final int UNREACHED = 0xB08B8B8B;

    private int splitScroll;

    @Override
    public Identifier icon() {
        return ICON;
    }

    @Override
    public Text title() {
        return Text.translatable("shannonuimod.tab.journey");
    }

    private JourneyState journey() {
        return shannon.store().get(StateChannels.JOURNEY);
    }

    @Override
    public Text status() {
        JourneyState journey = journey();
        return journey == null ? null
                : Text.translatable("shannonuimod.journey.count", journey.marks.size(), JourneyState.MILESTONES.size());
    }

    /** Game time now, as the client's world has it, or -1 outside a world. */
    private long now() {
        return client.world == null ? -1 : client.world.getTime();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        JourneyState journey = journey();
        Gui.label(context, Text.translatable("shannonuimod.journey.way"), x + 1, y);
        if (journey == null) {
            Gui.paragraph(context, Text.translatable("shannonuimod.journey.none"), x + 1, y + 14, w - 2, Palette.LABEL, 3);
            return;
        }
        int pathBottom = renderPath(context, journey, mouseX, mouseY);
        int paneTop = pathBottom + 4;
        int paneH = y + h - paneTop;
        int leftW = (w - 4) / 2;
        Gui.inset(context, x, paneTop, leftW, paneH, Palette.SLOT);
        Gui.inset(context, x + leftW + 4, paneTop, w - leftW - 4, paneH, Palette.SLOT);
        renderStats(context, journey, x + 5, paneTop + 5, leftW - 10);
        renderSplits(context, journey, x + leftW + 9, paneTop + 5, w - leftW - 18, paneH - 10);
    }

    /** The milestones in one row (two on a narrow screen), joined by a line that turns green as far as she got. */
    private int renderPath(DrawContext context, JourneyState journey, int mouseX, int mouseY) {
        List<JourneyState.Milestone> all = JourneyState.MILESTONES;
        int perRow = Math.min(all.size(), Math.max(1, (w - 8 + 4) / (SLOT + 4)));
        int rows = (all.size() + perRow - 1) / perRow;
        int step = perRow > 1 ? Math.min(SLOT + 6, (w - 8 - SLOT) / (perRow - 1)) : 0;
        int top = y + 11;
        int height = rows * (SLOT + 4) + 4;
        Gui.inset(context, x, top, w, height, 0xFF6F6F6F);
        int next = nextIndex(journey);
        for (int i = 0; i < all.size(); i++) {
            JourneyState.Milestone milestone = all.get(i);
            int sx = x + 4 + (i % perRow) * step;
            int sy = top + 4 + (i / perRow) * (SLOT + 4);
            boolean reached = journey.reachedAt(milestone.id()) >= 0;
            if (i % perRow > 0) {
                int lineColor = reached ? Palette.XP : 0xFF4A4A4A;
                context.fill(sx - step + SLOT, sy + SLOT / 2 - 1, sx, sy + SLOT / 2 + 1, lineColor);
            }
            Gui.slot(context, sx, sy);
            Gui.item(context, milestone.icon(), 1, sx + 1, sy + 1);
            if (!reached) {
                context.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, UNREACHED);
            } else {
                Icons.CHECK.draw(context, sx + SLOT - 9, sy + SLOT - 8);
            }
            if (i == next) {
                Gui.outline(context, sx - 1, sy - 1, SLOT + 2, SLOT + 2, Palette.GOLD);
            }
            if (Gui.inside(mouseX, mouseY, sx, sy, SLOT, SLOT)) {
                screen.tooltip(tooltip(journey, milestone, i == next));
            }
        }
        return top + height;
    }

    /** The first milestone in order not yet reached, or -1 when all are. */
    private static int nextIndex(JourneyState journey) {
        for (int i = 0; i < JourneyState.MILESTONES.size(); i++) {
            if (journey.reachedAt(JourneyState.MILESTONES.get(i).id()) < 0) {
                return i;
            }
        }
        return -1;
    }

    private List<Text> tooltip(JourneyState journey, JourneyState.Milestone milestone, boolean next) {
        List<Text> lines = new ArrayList<>();
        lines.add(name(milestone));
        long at = journey.reachedAt(milestone.id());
        if (at >= 0) {
            lines.add(Text.translatable("shannonuimod.journey.reached_at", clock(at - journey.startTick)).formatted(Formatting.GREEN));
        } else {
            lines.add(Text.translatable(next ? "shannonuimod.journey.next" : "shannonuimod.journey.not_yet").formatted(Formatting.GRAY));
        }
        lines.add(Text.translatable("shannonuimod.journey.how." + milestone.id()).formatted(Formatting.DARK_GRAY));
        return lines;
    }

    private static Text name(JourneyState.Milestone milestone) {
        return Text.translatable("shannonuimod.journey." + milestone.id());
    }

    private static String clock(long ticks) {
        return TasksTab.clock(ticks / TICKS_PER_SECOND);
    }

    private void renderStats(DrawContext context, JourneyState journey, int left, int top, int width) {
        int cursor = top;
        int next = nextIndex(journey);
        Text nextName = next < 0 ? Text.translatable("shannonuimod.journey.all_done") : name(JourneyState.MILESTONES.get(next));
        cursor = row(context, left, width, cursor, Text.translatable("shannonuimod.journey.next_label"), nextName, Palette.YELLOW);
        long now = now();
        if (journey.startTick >= 0 && now >= journey.startTick) {
            cursor = row(context, left, width, cursor, Text.translatable("shannonuimod.journey.played"),
                    Text.literal(clock(now - journey.startTick)), Palette.WHITE);
        }
        Text deaths = journey.deaths == 0 ? Text.translatable("shannonuimod.journey.no_deaths")
                : Text.translatable("shannonuimod.journey.deaths_value", journey.deaths);
        cursor = row(context, left, width, cursor, Text.translatable("shannonuimod.journey.deaths"), deaths,
                journey.deaths == 0 ? Palette.WHITE : Palette.RED);
        if (journey.lastDeathTick >= 0 && now >= journey.lastDeathTick) {
            cursor = row(context, left, width, cursor, Text.translatable("shannonuimod.journey.since_death"),
                    Text.literal(clock(now - journey.lastDeathTick)), Palette.GRAY);
        }
        BotVitals vitals = shannon.store().get(StateChannels.VITALS);
        if (vitals != null && vitals.online && vitals.dimension != null) {
            Identifier dimension = Identifier.tryParse(vitals.dimension);
            Text where = dimension == null ? Text.literal(vitals.dimension)
                    : Text.translatableWithFallback("shannonuimod.journey.dimension." + dimension.getPath(), dimension.getPath());
            cursor = row(context, left, width, cursor, Text.translatable("shannonuimod.journey.dimension"), where, Palette.WHITE);
        }
        TaskTreeState tree = shannon.store().taskTree();
        if (tree != null && tree.planRevision != null) {
            int[] progress = TaskView.progress(tree);
            row(context, left, width, cursor, Text.translatable("shannonuimod.journey.plan"),
                    Text.translatable("shannonuimod.journey.plan_value", tree.planRevision, progress[0], progress[1]), Palette.WHITE);
        }
    }

    private static int row(DrawContext context, int x, int width, int y, Text label, Text value, int color) {
        Gui.text(context, label, x, y, Palette.GRAY);
        int room = width - Gui.width(label) - 6;
        Text fitted = Gui.width(value) <= room ? value : Gui.fit(value.getString(), room);
        Gui.text(context, fitted, x + width - Gui.width(fitted), y, color);
        return y + 12;
    }

    /** Each milestone reached, in the order she reached them: the time from the start and the time since the one before. */
    private void renderSplits(DrawContext context, JourneyState journey, int left, int top, int width, int height) {
        Gui.text(context, Text.translatable("shannonuimod.journey.splits"), left, top, Palette.GRAY);
        List<JourneyState.Mark> marks = new ArrayList<>(journey.marks);
        marks.sort(Comparator.comparingLong(mark -> mark.tick));
        int listTop = top + 12;
        if (marks.isEmpty()) {
            Gui.paragraph(context, Text.translatable("shannonuimod.journey.splits.none"), left, listTop, width, Palette.WHITE, 3);
            return;
        }
        int visible = Math.max(1, (height - 12) / Gui.LINE);
        splitScroll = Math.max(0, Math.min(splitScroll, marks.size() - visible));
        long previous = journey.startTick;
        for (int i = 0; i < splitScroll; i++) {
            previous = marks.get(i).tick;
        }
        for (int i = 0; i < Math.min(visible, marks.size() - splitScroll); i++) {
            JourneyState.Mark mark = marks.get(i + splitScroll);
            int rowY = listTop + i * Gui.LINE;
            Text at = Text.literal(clock(mark.tick - journey.startTick));
            Text delta = Text.literal("+" + clock(mark.tick - previous));
            previous = mark.tick;
            Gui.text(context, at, left, rowY, Palette.WHITE);
            int nameX = left + 40;
            int deltaW = Gui.width(delta);
            Text name = milestoneName(mark.id);
            Gui.text(context, Gui.fit(name.getString(), width - 40 - deltaW - 4), nameX, rowY, Palette.WHITE);
            Gui.text(context, delta, left + width - deltaW, rowY, Palette.GRAY);
        }
    }

    private static Text milestoneName(String id) {
        return Text.translatableWithFallback("shannonuimod.journey." + id, id);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        splitScroll -= (int) Math.signum(amount);
        return true;
    }
}

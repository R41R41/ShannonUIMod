package com.shannon.ui.screen.tab;

import com.shannon.ShannonUIMod;
import com.shannon.model.TaskListState;
import com.shannon.model.TaskTreeState;
import com.shannon.sync.Actions;
import com.shannon.sync.BotCommand;
import com.shannon.ui.gfx.Gui;
import com.shannon.ui.gfx.Icons;
import com.shannon.ui.gfx.Palette;
import com.shannon.ui.gfx.PixelIcon;
import com.shannon.ui.net.ClientActions;
import com.shannon.ui.state.BotStatus;
import com.shannon.ui.state.TaskHistory;
import com.shannon.ui.state.TaskView;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * The task the bot works on: its goal and progress on top, its plan as a tree on the left and the
 * chosen node's notes on the right; or, switched to the log, what the bot did in its recent tasks.
 *
 * <p>The bot holds one task at a time, so there is no queue to pick from; the plan is what the
 * player wants to follow.
 */
public class TasksTab extends ShannonTab {
    private static final Identifier ICON = Identifier.of(ShannonUIMod.MOD_ID, "textures/tasktree.png");
    /** The goal, the progress bar and the plan's line. */
    private static final int HEADER_H = 34;
    private static final int ROW = 11;
    private static final int INDENT = 8;
    /** The log view's list width, as it was before the plan view. */
    private static final int LOG_LIST_W = 118;

    /** Survive reopening the screen, like vanilla's selected recipe. */
    private static String selectedNode;
    private static boolean showLog;

    private ButtonWidget resume;
    private ButtonWidget cancel;
    private ButtonWidget again;
    private HistoryView log;
    private int treeScroll;
    private int detailScroll;
    /** The tree keeps the node worked on in view until the player scrolls it. */
    private boolean followCurrent = true;
    /** The rows drawn this frame, for clicks. */
    private final List<TaskView.Step> shown = new ArrayList<>();

    @Override
    public Identifier icon() {
        return ICON;
    }

    @Override
    public Text title() {
        return Text.translatable("shannonuimod.tab.tasks");
    }

    @Override
    public Text status() {
        return Text.translatable(shannon.store().status().labelKey());
    }

    @Override
    protected void build() {
        log = new HistoryView(shannon);
        int buttonW = 64;
        int buttonY = y + h - 20;
        int right = x + w;
        int switchW = 50;
        ButtonWidget now = add(ButtonWidget.builder(Text.translatable("shannonuimod.tasks.view.now"), button -> {
            showLog = false;
            screen.rebuild();
        }).dimensions(x, buttonY, switchW, 20).build());
        ButtonWidget history = add(ButtonWidget.builder(Text.translatable("shannonuimod.tasks.view.log"), button -> {
            showLog = true;
            screen.rebuild();
        }).dimensions(x + switchW + 2, buttonY, switchW, 20).build());
        now.active = showLog;
        history.active = !showLog;
        if (showLog) {
            resume = null;
            cancel = null;
            int againW = 140;
            again = add(ButtonWidget.builder(Text.translatable("shannonuimod.history.again"), button -> {
                        TaskHistory.Record record = log.record();
                        if (record != null && ClientActions.chat(record.repeatRequest())) {
                            screen.close();
                        }
                    })
                    .dimensions(right - againW, buttonY, againW, 20)
                    .tooltip(Tooltip.of(Text.translatable("shannonuimod.history.again.tip")))
                    .build());
            tick();
            return;
        }
        cancel = add(ButtonWidget.builder(Text.translatable("shannonuimod.tasks.cancel"), button -> {
                    String id = currentTaskId();
                    if (id != null) {
                        ClientActions.task(Actions.TaskAction.DELETE, id);
                    }
                })
                .dimensions(right - buttonW, buttonY, buttonW, 20)
                .tooltip(Tooltip.of(Text.translatable("shannonuimod.tasks.cancel.tip")))
                .build());
        resume = add(ButtonWidget.builder(Text.translatable("shannonuimod.tasks.resume"),
                        button -> ClientActions.command(BotCommand.RESUME))
                .dimensions(right - buttonW * 2 - 4, buttonY, buttonW, 20)
                .tooltip(Tooltip.of(Text.translatable("shannonuimod.tasks.resume.tip")))
                .build());
        tick();
    }

    @Override
    public void tick() {
        if (showLog) {
            if (again != null) {
                again.active = log.record() != null;
            }
            return;
        }
        if (resume == null) {
            return;
        }
        BotStatus status = shannon.store().status();
        resume.active = status == BotStatus.WAITING || status == BotStatus.ERROR;
        cancel.active = currentTaskId() != null;
    }

    private String currentTaskId() {
        TaskListState list = shannon.store().taskList();
        return list != null ? list.currentTaskId : null;
    }

    /** When the current task began, in the backend's clock (milliseconds), or 0. */
    private long startedAt() {
        TaskListState list = shannon.store().taskList();
        if (list == null || list.tasks == null || list.currentTaskId == null) {
            return 0;
        }
        for (TaskListState.TaskInfo task : list.tasks) {
            if (list.currentTaskId.equals(task.id)) {
                return task.createdAt;
            }
        }
        return 0;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        shown.clear();
        if (showLog) {
            log.render(context, screen, x, y, LOG_LIST_W, w, h - 24, mouseX, mouseY);
            return;
        }
        TaskTreeState tree = shannon.store().taskTree();
        Gui.label(context, Text.translatable("shannonuimod.tasks.detail"), x + 1, y);
        int headerTop = y + 10;
        Gui.inset(context, x, headerTop, w, HEADER_H, Palette.SLOT);
        int paneTop = paneTop();
        int paneH = h - 24 - (paneTop - y);
        int treeW = treeWidth();
        int detailX = x + treeW + 4;
        int detailW = w - treeW - 4;
        Gui.inset(context, x, paneTop, treeW, paneH, Palette.SLOT);
        Gui.inset(context, detailX, paneTop, detailW, paneH, Palette.SLOT);
        if (tree == null || TaskView.blankToNull(tree.goal) == null) {
            Gui.text(context, Text.translatable("shannonuimod.tasks.nothing"), x + 5, headerTop + 4, Palette.WHITE);
            return;
        }
        renderHeader(context, tree, x + 5, headerTop + 4, w - 10, mouseX, mouseY);
        List<TaskView.Step> steps = TaskView.steps(tree);
        TaskView.Step chosen = chosen(steps);
        renderTree(context, tree, steps, chosen, x, paneTop, treeW, paneH, mouseX, mouseY);
        renderDetail(context, tree, chosen, detailX + 5, paneTop + 5, detailW - 12, paneH - 10);
    }

    private int paneTop() {
        return y + 10 + HEADER_H + 4;
    }

    private int treeWidth() {
        return w * 3 / 5;
    }

    /** The goal; a progress bar over the plan; the plan's version and the time spent, or the reflex holding the body. */
    private void renderHeader(DrawContext context, TaskTreeState tree, int left, int top, int width, int mouseX, int mouseY) {
        Gui.text(context, Gui.fit(tree.goal, width), left, top, Palette.WHITE);
        if (Gui.inside(mouseX, mouseY, left, top, width, 9) && Gui.width(tree.goal) > width) {
            screen.tooltip(List.of(Text.literal(tree.goal)));
        }
        int[] progress = TaskView.progress(tree);
        if (progress[1] > 0) {
            Text count = Text.translatable("shannonuimod.tasks.done_of", progress[0], progress[1]);
            Gui.progress(context, left, top + 12, width - Gui.width(count) - 6, 5, progress[0] / (float) progress[1]);
            Gui.text(context, count, left + width - Gui.width(count), top + 10, Palette.WHITE);
        }
        int lineY = top + 21;
        String reflex = TaskView.blankToNull(tree.activeReflex);
        if (reflex != null) {
            Gui.text(context, Gui.fit(Text.translatable("shannonuimod.tasks.reflex", reflex).getString(), width), left, lineY, Palette.GOLD);
            return;
        }
        List<String> parts = new ArrayList<>();
        if (tree.planRevision != null) {
            parts.add(Text.translatable("shannonuimod.tasks.plan_version", tree.planRevision).getString());
        }
        long started = startedAt();
        if (started > 0 && "in_progress".equals(tree.status)) {
            parts.add(Text.translatable("shannonuimod.tasks.elapsed", clock((System.currentTimeMillis() - started) / 1000)).getString());
        }
        if (!parts.isEmpty()) {
            Gui.text(context, Gui.fit(String.join("  ·  ", parts), width), left, lineY, Palette.GRAY);
        }
    }

    /** {@code 12:34}, or {@code 1:02:03} past an hour. */
    public static String clock(long seconds) {
        seconds = Math.max(0, seconds);
        long hours = seconds / 3600;
        long minutes = seconds / 60 % 60;
        long rest = seconds % 60;
        return hours > 0 ? String.format("%d:%02d:%02d", hours, minutes, rest) : String.format("%d:%02d", minutes, rest);
    }

    /** The node the player chose, while it is still in the plan; else none. */
    private static TaskView.Step chosen(List<TaskView.Step> steps) {
        if (selectedNode == null) {
            return null;
        }
        for (TaskView.Step step : steps) {
            if (selectedNode.equals(step.task().id)) {
                return step;
            }
        }
        return null;
    }

    private void renderTree(DrawContext context, TaskTreeState tree, List<TaskView.Step> steps, TaskView.Step chosen,
                            int left, int top, int width, int height, int mouseX, int mouseY) {
        int visible = Math.max(1, (height - 4) / ROW);
        if (followCurrent && tree.currentSubTaskId != null) {
            for (int i = 0; i < steps.size(); i++) {
                if (tree.currentSubTaskId.equals(steps.get(i).task().id)) {
                    // The current node a little below the top, so its parents and next moves both show.
                    treeScroll = i - Math.min(2, visible / 3);
                    break;
                }
            }
        }
        treeScroll = Math.max(0, Math.min(treeScroll, steps.size() - visible));
        for (int i = 0; i < Math.min(visible, steps.size() - treeScroll); i++) {
            TaskView.Step step = steps.get(i + treeScroll);
            shown.add(step);
            int rowY = top + 2 + i * ROW;
            boolean current = step.task().id != null && step.task().id.equals(tree.currentSubTaskId);
            boolean hovered = Gui.inside(mouseX, mouseY, left + 2, rowY, width - 4, ROW);
            if (step == chosen) {
                context.fill(left + 2, rowY, left + width - 2, rowY + ROW, 0x2E000000);
                Gui.outline(context, left + 2, rowY, width - 4, ROW, Palette.WHITE);
            } else if (current) {
                context.fill(left + 2, rowY, left + width - 2, rowY + ROW, 0x30FFFF55);
            } else if (hovered) {
                context.fill(left + 2, rowY, left + width - 2, rowY + ROW, 0x20FFFFFF);
            }
            int indent = Math.min(step.depth(), 4) * INDENT;
            PixelIcon icon = step.done() ? Icons.CHECK : step.running() ? Icons.PLAY : step.failed() ? Icons.WARNING : Icons.PENDING;
            icon.draw(context, left + 5 + indent, rowY + 2);
            int color = step.running() ? Palette.WHITE : step.failed() ? Palette.RED : step.move() ? 0xFFD0D0D0 : Palette.GRAY;
            boolean rules = has(step.task().forbid) || has(step.task().caution);
            int textW = width - 20 - indent - (rules ? 6 : 0);
            String goal = step.task().goal == null ? "" : step.task().goal;
            Gui.text(context, Gui.fit(goal, textW), left + 17 + indent, rowY + 1, color);
            if (rules) {
                // A plan node with forbids or cautions: a small gold "!", read in the notes on the right.
                context.fill(left + width - 7, rowY + 2, left + width - 5, rowY + 7, Palette.GOLD);
                context.fill(left + width - 7, rowY + 8, left + width - 5, rowY + 10, Palette.GOLD);
            }
            if (hovered && step != chosen) {
                screen.tooltip(List.of(Text.literal(goal), Text.translatable("shannonuimod.tasks.node.click").formatted(Formatting.GRAY)));
            }
        }
    }

    private static boolean has(List<String> list) {
        return list != null && !list.isEmpty();
    }

    /** The chosen node's notes; with none chosen, the bot's own summary of the task and what makes it done. */
    private void renderDetail(DrawContext context, TaskTreeState tree, TaskView.Step chosen, int left, int top, int width, int height) {
        List<Line> lines = new ArrayList<>();
        if (chosen == null) {
            add(lines, Text.translatable("shannonuimod.tasks.summary"), Palette.GRAY, width);
            String summary = TaskView.blankToNull(tree.currentThinking);
            add(lines, summary == null ? Text.translatable("shannonuimod.tasks.summary.none") : Text.literal(summary),
                    Palette.WHITE, width);
            String condition = TaskView.blankToNull(tree.strategy);
            if (condition != null) {
                gap(lines);
                add(lines, Text.translatable("shannonuimod.tasks.condition"), Palette.GRAY, width);
                add(lines, Text.literal(condition), Palette.WHITE, width);
            }
        } else {
            TaskTreeState.SubTask node = chosen.task();
            add(lines, Text.literal(node.goal == null ? "" : node.goal), Palette.WHITE, width);
            String state = chosen.done() ? "done" : chosen.running() ? "now" : chosen.failed() ? "blocked" : "later";
            add(lines, Text.translatable("shannonuimod.tasks.node.state." + state), chosen.failed() ? Palette.RED : Palette.GRAY, width);
            field(lines, "blocked_why", chosen.failed() ? node.failureReason : null, Palette.RED, width);
            field(lines, "done_when", node.doneWhen, Palette.WHITE, width);
            field(lines, "where", node.where, Palette.WHITE, width);
            rules(lines, "forbid", node.forbid, Palette.RED, width);
            rules(lines, "caution", node.caution, Palette.GOLD, width);
            field(lines, "note", node.note, Palette.WHITE, width);
            // A plan node's result is only "done"; an act's move tells what happened.
            String result = TaskView.blankToNull(node.result);
            field(lines, "result", chosen.move() ? result : null, Palette.WHITE, width);
        }
        int visible = Math.max(1, height / Gui.LINE);
        detailScroll = Math.max(0, Math.min(detailScroll, lines.size() - visible));
        for (int i = 0; i < Math.min(visible, lines.size() - detailScroll); i++) {
            Line line = lines.get(i + detailScroll);
            if (line.text() != null) {
                Gui.text(context, line.text(), left, top + i * Gui.LINE, line.color());
            }
        }
        if (lines.size() > visible) {
            // A thin bar on the right edge: how much is out of view, and where.
            int barH = Math.max(6, height * visible / lines.size());
            int barY = top + (height - barH) * detailScroll / Math.max(1, lines.size() - visible);
            context.fill(left + width + 3, barY, left + width + 5, barY + barH, 0xFFC6C6C6);
        }
    }

    private record Line(OrderedText text, int color) {
    }

    private static void add(List<Line> lines, Text text, int color, int width) {
        for (OrderedText line : Gui.wrap(text, width)) {
            lines.add(new Line(line, color));
        }
    }

    private static void gap(List<Line> lines) {
        lines.add(new Line(null, 0));
    }

    private static void field(List<Line> lines, String key, String value, int color, int width) {
        String text = TaskView.blankToNull(value);
        if (text == null) {
            return;
        }
        gap(lines);
        add(lines, Text.translatable("shannonuimod.tasks.node." + key), Palette.GRAY, width);
        add(lines, Text.literal(text), color, width);
    }

    private static void rules(List<Line> lines, String key, List<String> values, int color, int width) {
        if (!has(values)) {
            return;
        }
        gap(lines);
        add(lines, Text.translatable("shannonuimod.tasks.node." + key), Palette.GRAY, width);
        for (String value : values) {
            add(lines, Text.literal("・" + value), color, width);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showLog) {
            return log.mouseClicked(mouseX, mouseY, x, y, LOG_LIST_W, h - 24);
        }
        int top = paneTop() + 2;
        if (!Gui.inside(mouseX, mouseY, x, top, treeWidth(), shown.size() * ROW)) {
            return false;
        }
        TaskView.Step step = shown.get((int) ((mouseY - top) / ROW));
        String id = step.task().id;
        // Clicking the chosen node again goes back to the task's summary.
        selectedNode = id != null && id.equals(selectedNode) ? null : id;
        detailScroll = 0;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (showLog) {
            return log.mouseScrolled(mouseX, amount, x, LOG_LIST_W);
        }
        if (mouseX < x + treeWidth()) {
            followCurrent = false;
            treeScroll -= (int) Math.signum(amount);
        } else {
            detailScroll -= (int) Math.signum(amount);
        }
        return true;
    }

    @Override
    public void onShow() {
        followCurrent = true;
    }
}

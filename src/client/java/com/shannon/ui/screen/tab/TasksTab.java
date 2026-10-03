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
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * The task queue on the left and the selected task's steps on the right; or, switched to the
 * log, what the bot did in its recent tasks.
 */
public class TasksTab extends ShannonTab {
    private static final Identifier ICON = Identifier.of(ShannonUIMod.MOD_ID, "textures/tasktree.png");
    private static final int LIST_W = 118;
    private static final int ROW = 14;

    /** Survives reopening the screen, like vanilla's selected recipe. */
    private static String selectedId;
    private static boolean showLog;

    private record Entry(String id, String goal, PixelIcon icon) {
    }

    private ButtonWidget resume;
    private ButtonWidget prioritize;
    private ButtonWidget cancel;
    private ButtonWidget again;
    private HistoryView log;
    private int listScroll;
    private int stepScroll;

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
                    String id = selectedTaskId();
                    if (id != null) {
                        ClientActions.task(Actions.TaskAction.DELETE, id);
                    }
                })
                .dimensions(right - buttonW, buttonY, buttonW, 20)
                .tooltip(Tooltip.of(Text.translatable("shannonuimod.tasks.cancel.tip")))
                .build());
        prioritize = add(ButtonWidget.builder(Text.translatable("shannonuimod.tasks.prioritize"), button -> {
                    String id = selectedTaskId();
                    if (id != null) {
                        ClientActions.task(Actions.TaskAction.PRIORITIZE, id);
                    }
                })
                .dimensions(right - buttonW * 2 - 4, buttonY, buttonW, 20)
                .tooltip(Tooltip.of(Text.translatable("shannonuimod.tasks.prioritize.tip")))
                .build());
        resume = add(ButtonWidget.builder(Text.translatable("shannonuimod.tasks.resume"),
                        button -> ClientActions.command(BotCommand.RESUME))
                .dimensions(right - buttonW * 3 - 8, buttonY, buttonW, 20)
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
        String selected = selectedTaskId();
        String current = currentTaskId();
        resume.active = status == BotStatus.WAITING || status == BotStatus.ERROR;
        prioritize.active = selected != null && !selected.equals(current);
        cancel.active = selected != null;
    }

    private String currentTaskId() {
        TaskListState list = shannon.store().taskList();
        return list != null ? list.currentTaskId : null;
    }

    private List<Entry> entries() {
        List<Entry> entries = new ArrayList<>();
        TaskListState list = shannon.store().taskList();
        if (list == null) {
            return entries;
        }
        if (list.emergencyTask != null) {
            entries.add(new Entry(list.emergencyTask.id, list.emergencyTask.goal, Icons.WARNING));
        }
        BotStatus status = shannon.store().status();
        if (list.tasks != null) {
            for (TaskListState.TaskInfo task : list.tasks) {
                PixelIcon icon = Icons.PENDING;
                if (task.id != null && task.id.equals(list.currentTaskId)) {
                    icon = Icons.status(status == BotStatus.IDLE ? BotStatus.WORKING : status);
                } else if ("awaiting_user".equals(task.status)) {
                    icon = Icons.QUESTION;
                } else if ("failed_terminal".equals(task.status)) {
                    icon = Icons.WARNING;
                }
                entries.add(new Entry(task.id, task.goal, icon));
            }
        }
        return entries;
    }

    /** The selected task, falling back to the current one and then to the first. */
    private String selectedTaskId() {
        List<Entry> entries = entries();
        for (Entry entry : entries) {
            if (entry.id() != null && entry.id().equals(selectedId)) {
                return selectedId;
            }
        }
        String current = currentTaskId();
        if (current != null) {
            return current;
        }
        return entries.isEmpty() ? null : entries.get(0).id();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        if (showLog) {
            log.render(context, screen, x, y, LIST_W, w, h - 24, mouseX, mouseY);
            return;
        }
        int listTop = y + 10;
        int listH = h - 10 - 24;
        Gui.label(context, Text.translatable("shannonuimod.tasks.queue"), x + 1, y);
        Gui.inset(context, x, listTop, LIST_W, listH, Palette.SLOT);
        renderList(context, mouseX, mouseY, listTop, listH);

        int rightX = x + LIST_W + 6;
        int rightW = w - LIST_W - 6;
        Gui.label(context, Text.translatable("shannonuimod.tasks.detail"), rightX + 1, y);
        Gui.inset(context, rightX, listTop, rightW, listH, Palette.SLOT);
        renderDetail(context, rightX + 6, listTop + 6, rightW - 12, listH - 12);
    }

    private void renderList(DrawContext context, int mouseX, int mouseY, int top, int height) {
        List<Entry> entries = entries();
        String selected = selectedTaskId();
        if (entries.isEmpty()) {
            Gui.paragraph(context, Text.translatable("shannonuimod.tasks.empty"), x + 6, top + 6, LIST_W - 12, Palette.WHITE, 3);
            return;
        }
        int visible = (height - 4) / ROW;
        listScroll = Math.max(0, Math.min(listScroll, entries.size() - visible));
        for (int i = 0; i < Math.min(visible, entries.size() - listScroll); i++) {
            Entry entry = entries.get(i + listScroll);
            int rowY = top + 2 + i * ROW;
            boolean isSelected = entry.id() != null && entry.id().equals(selected);
            if (isSelected) {
                context.fill(x + 2, rowY, x + LIST_W - 2, rowY + ROW, 0x2E000000);
                Gui.outline(context, x + 2, rowY, LIST_W - 4, ROW, Palette.WHITE);
            } else if (Gui.inside(mouseX, mouseY, x + 2, rowY, LIST_W - 4, ROW)) {
                context.fill(x + 2, rowY, x + LIST_W - 2, rowY + ROW, 0x20FFFFFF);
            }
            entry.icon().draw(context, x + 5, rowY + 3);
            Gui.text(context, Gui.fit(entry.goal(), LIST_W - 24), x + 17, rowY + 3, Palette.WHITE);
            if (Gui.inside(mouseX, mouseY, x + 2, rowY, LIST_W - 4, ROW) && Gui.width(entry.goal()) > LIST_W - 24) {
                screen.tooltip(List.of(Text.literal(entry.goal())));
            }
        }
    }

    private void renderDetail(DrawContext context, int left, int top, int width, int height) {
        String selected = selectedTaskId();
        String current = currentTaskId();
        TaskTreeState tree = shannon.store().taskTree();
        if (selected == null && (tree == null || TaskView.blankToNull(tree.goal) == null)) {
            Gui.paragraph(context, Text.translatable("shannonuimod.tasks.nothing"), left, top, width, Palette.WHITE, 3);
            return;
        }
        if (selected != null && !selected.equals(current)) {
            String goal = goalOf(selected);
            int used = Gui.paragraph(context, Text.literal(goal == null ? "" : goal), left, top, width, Palette.WHITE, 3);
            Gui.text(context, Text.translatable("shannonuimod.tasks.not_started"), left, top + used + 4, Palette.GRAY);
            return;
        }
        int cursor = top;
        if (tree != null && tree.goal != null) {
            cursor += Gui.paragraph(context, Text.literal(tree.goal), left, cursor, width, Palette.WHITE, 2) + 2;
        }
        int[] progress = TaskView.progress(tree);
        if (progress[1] > 0) {
            Text count = Text.literal(progress[0] + "/" + progress[1]);
            Gui.progress(context, left, cursor + 2, width - Gui.width(count) - 6, 6, progress[0] / (float) progress[1]);
            Gui.text(context, count, left + width - Gui.width(count), cursor, Palette.WHITE);
            cursor += 13;
        }

        String thinking = tree != null ? TaskView.blankToNull(tree.currentThinking) : null;
        int thinkingHeight = thinking == null ? 0 : 12 + Gui.paragraphHeight(Text.literal(thinking), width, 2) + 4;
        int stepsBottom = top + height - thinkingHeight;
        List<TaskView.Step> steps = TaskView.steps(tree);
        int visible = Math.max(0, (stepsBottom - cursor) / 11);
        stepScroll = Math.max(0, Math.min(stepScroll, steps.size() - visible));
        for (int i = 0; i < Math.min(visible, steps.size() - stepScroll); i++) {
            TaskView.Step step = steps.get(i + stepScroll);
            int indent = Math.min(step.depth(), 4) * 8;
            PixelIcon icon = step.done() ? Icons.CHECK : step.running() ? Icons.PLAY : step.failed() ? Icons.WARNING : Icons.PENDING;
            int color = step.running() ? Palette.WHITE : step.failed() ? Palette.RED : Palette.GRAY;
            icon.draw(context, left + indent, cursor + 1);
            String goal = step.task().goal == null ? "" : step.task().goal;
            Gui.text(context, Gui.fit(goal, width - indent - 13), left + indent + 12, cursor, color);
            cursor += 11;
        }
        if (thinking != null) {
            int lineY = top + height - thinkingHeight + 2;
            context.fill(left, lineY, left + width, lineY + 1, 0xFF6E6E6E);
            Gui.text(context, Text.translatable("shannonuimod.tasks.thinking"), left, lineY + 4, Palette.GRAY);
            Gui.paragraph(context, Text.literal(thinking), left, lineY + 15, width, Palette.WHITE, 2);
        }
    }

    private String goalOf(String id) {
        for (Entry entry : entries()) {
            if (id.equals(entry.id())) {
                return entry.goal();
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showLog) {
            return log.mouseClicked(mouseX, mouseY, x, y, LIST_W, h - 24);
        }
        int top = y + 12;
        int height = h - 10 - 24 - 4;
        if (!Gui.inside(mouseX, mouseY, x, top, LIST_W, height)) {
            return false;
        }
        int index = (int) ((mouseY - top) / ROW) + listScroll;
        List<Entry> entries = entries();
        if (index >= 0 && index < entries.size()) {
            selectedId = entries.get(index).id();
            tick();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (showLog) {
            return log.mouseScrolled(mouseX, amount, x, LIST_W);
        }
        if (mouseX < x + LIST_W) {
            listScroll -= (int) Math.signum(amount);
        } else {
            stepScroll -= (int) Math.signum(amount);
        }
        return true;
    }
}

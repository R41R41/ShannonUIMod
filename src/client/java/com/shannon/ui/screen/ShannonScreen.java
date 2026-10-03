package com.shannon.ui.screen;

import com.shannon.ui.ShannonClient;
import com.shannon.ui.gfx.Gui;
import com.shannon.ui.gfx.Palette;
import com.shannon.ui.input.KeyBindings;
import com.shannon.ui.screen.tab.AdvancementsTab;
import com.shannon.ui.screen.tab.ChatTab;
import com.shannon.ui.screen.tab.InventoryTab;
import com.shannon.ui.screen.tab.RequestTab;
import com.shannon.ui.screen.tab.SettingsTab;
import com.shannon.ui.screen.tab.ShannonTab;
import com.shannon.ui.screen.tab.SkillsTab;
import com.shannon.ui.screen.tab.TasksTab;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The detail screen: a vanilla-style container with creative-inventory tabs on top.
 *
 * <p>Tabs are listed once in {@link #createTabs}; the screen remembers the last one between
 * openings. The game keeps running behind it.
 */
public class ShannonScreen extends Screen {
    private static final int PANEL_W = 352;
    private static final int PANEL_H = 228;
    private static final int TAB_W = 28;
    private static final int TAB_H = 28;
    private static final int TAB_STEP = 30;

    private static Class<? extends ShannonTab> lastTab = TasksTab.class;

    private final List<ShannonTab> tabs = createTabs();
    private ShannonTab current;
    private List<Text> tooltip;
    private int px;
    private int py;
    private int pw;
    private int ph;

    public ShannonScreen() {
        this(lastTab);
    }

    public ShannonScreen(Class<? extends ShannonTab> initial) {
        super(Text.translatable("shannonuimod.screen.title"));
        current = tabs.get(0);
        for (ShannonTab tab : tabs) {
            if (tab.getClass() == initial) {
                current = tab;
            }
        }
    }

    /** Every tab, in order. Add a tab here. */
    private static List<ShannonTab> createTabs() {
        List<ShannonTab> list = new ArrayList<>();
        list.add(new TasksTab());
        list.add(new InventoryTab());
        list.add(new RequestTab());
        list.add(new SkillsTab());
        list.add(new ChatTab());
        list.add(new AdvancementsTab());
        list.add(new SettingsTab());
        return list;
    }

    @Override
    protected void init() {
        pw = Math.min(PANEL_W, width - 16);
        ph = Math.min(PANEL_H, height - 48);
        px = (width - pw) / 2;
        py = (height - ph) / 2 + 8;
        current.init(this, px + 7, py + 18, pw - 14, ph - 25);
        current.onShow();
    }

    /** Lets a tab add a vanilla widget to this screen. */
    public <T extends ClickableWidget> T addTabWidget(T widget) {
        return addDrawableChild(widget);
    }

    public void select(ShannonTab tab) {
        if (tab == current) {
            return;
        }
        current = tab;
        lastTab = tab.getClass();
        clearAndInit();
    }

    /** Lays the current tab out again, for tabs whose widgets depend on synced state. */
    public void rebuild() {
        clearAndInit();
    }

    /** Shows {@code lines} as a vanilla tooltip at the mouse after this frame's content. */
    public void tooltip(List<Text> lines) {
        tooltip = lines;
    }

    public static boolean shiftDown() {
        long window = net.minecraft.client.MinecraftClient.getInstance().getWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fillGradient(0, 0, width, height, Palette.DIM_TOP, Palette.DIM_BOTTOM);
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i) != current) {
                drawTab(context, i, false);
            }
        }
        Gui.panel(context, px, py, pw, ph);
        drawTab(context, tabs.indexOf(current), true);
        Gui.label(context, current.title(), px + 8, py + 6);
        Text status = current.status();
        if (status != null) {
            context.drawText(textRenderer, status, px + pw - 8 - Gui.width(status), py + 6, Palette.LABEL, false);
        }
    }

    private void drawTab(DrawContext context, int index, boolean selected) {
        int x = px + index * TAB_STEP;
        int y = py - TAB_H + 2;
        int h = selected ? TAB_H + 1 : TAB_H - 3;
        Gui.tab(context, x, selected ? y : y + 3, TAB_W, h, selected);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, tabs.get(index).icon(), x + 6, (selected ? y : y + 3) + 6,
                0, 0, 16, 16, 16, 16);
    }

    private int tabAt(double mouseX, double mouseY) {
        for (int i = 0; i < tabs.size(); i++) {
            if (Gui.inside(mouseX, mouseY, px + i * TAB_STEP, py - TAB_H + 2, TAB_W, TAB_H - 2)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        tooltip = null;
        super.render(context, mouseX, mouseY, deltaTicks);
        current.render(context, mouseX, mouseY, deltaTicks);
        renderHints(context);
        int hoveredTab = tabAt(mouseX, mouseY);
        if (hoveredTab >= 0) {
            tooltip = List.of(tabs.get(hoveredTab).title());
        }
        if (tooltip != null && !tooltip.isEmpty()) {
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
        }
    }

    private void renderHints(DrawContext context) {
        Text escape = Text.translatable("key.keyboard.escape");
        Text close = Text.translatable("shannonuimod.screen.close");
        Text tabKey = KeyBindings.keyName(ShannonClient.get().keys().details());
        Text next = Text.translatable("shannonuimod.screen.next_tab");
        int total = Gui.keyHintWidth(escape, close) + 12 + Gui.keyHintWidth(tabKey, next);
        int x = (width - total) / 2;
        int y = py + ph + 5;
        if (y + 12 > height) {
            return;
        }
        x += Gui.keyHint(context, escape, close, x, y) + 12;
        Gui.keyHint(context, tabKey, next, x, y);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int tab = tabAt(click.x(), click.y());
        if (tab >= 0) {
            select(tabs.get(tab));
            return true;
        }
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        return current.mouseClicked(click.x(), click.y(), click.buttonInfo().button());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return current.mouseScrolled(mouseX, mouseY, verticalAmount)
                || super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        return current.mouseDragged(click.x(), click.y(), offsetX, offsetY)
                || super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (current.keyPressed(input)) {
            return true;
        }
        if (getFocused() == null || !getFocused().isFocused() || !(getFocused() instanceof net.minecraft.client.gui.widget.TextFieldWidget)) {
            if (input.key() == KeyBindings.keyCode(ShannonClient.get().keys().details())) {
                select(tabs.get((tabs.indexOf(current) + 1) % tabs.size()));
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public void tick() {
        current.tick();
    }

    @Override
    public void removed() {
        lastTab = current.getClass();
    }
}

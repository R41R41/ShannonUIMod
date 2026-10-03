package com.shannon.ui.screen.tab;

import com.shannon.ShannonUIMod;
import com.shannon.model.AdvancementsState;
import com.shannon.sync.StateChannels;
import com.shannon.ui.gfx.Gui;
import com.shannon.ui.gfx.Palette;
import com.shannon.ui.net.ClientActions;
import com.shannon.ui.screen.ShannonScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The bot's advancements as vanilla's advancement trees: one tree per tab, with the layout vanilla
 * computes, connector lines and frame shapes. Drag or scroll to look around.
 */
public class AdvancementsTab extends ShannonTab {
    private static final Identifier ICON = Identifier.of(ShannonUIMod.MOD_ID, "textures/advancements.png");
    private static final int LIST_W = 104;
    private static final int NODE = 26;
    /** Vanilla's spacing between tree columns and rows. */
    private static final int COLUMN = 28;
    private static final int ROW = 27;
    private static final long REFRESH_MS = 20_000;

    private static int selectedCategory;
    private long requestedAt;
    private double panX = Double.NaN;
    private double panY;
    private int panCategory = -1;

    @Override
    public Identifier icon() {
        return ICON;
    }

    @Override
    public Text title() {
        return Text.translatable("shannonuimod.tab.advancements");
    }

    @Override
    public Text status() {
        AdvancementsState state = state();
        if (state == null) {
            return null;
        }
        int done = 0;
        for (AdvancementsState.Category category : state.categories) {
            done += category.completed;
        }
        return Text.translatable("shannonuimod.advancements.count", done);
    }

    private AdvancementsState state() {
        return shannon.store().get(StateChannels.ADVANCEMENTS);
    }

    @Override
    public void onShow() {
        refresh();
    }

    @Override
    public void tick() {
        refresh();
    }

    private void refresh() {
        long now = System.currentTimeMillis();
        AdvancementsState state = state();
        boolean stale = state == null || now - state.updatedAt > REFRESH_MS;
        if (stale && now - requestedAt > REFRESH_MS) {
            requestedAt = now;
            ClientActions.requestAdvancements();
        }
    }

    private static Text titleOf(String key, String plain) {
        return key != null ? Text.translatableWithFallback(key, plain == null ? "" : plain) : Text.literal(plain == null ? "" : plain);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        AdvancementsState state = state();
        if (state == null || state.categories.isEmpty()) {
            Gui.label(context, Text.translatable(state == null ? "shannonuimod.advancements.loading"
                    : "shannonuimod.advancements.none"), x + 1, y + 2);
            return;
        }
        selectedCategory = Math.max(0, Math.min(selectedCategory, state.categories.size() - 1));
        renderCategories(context, state, mouseX, mouseY);
        AdvancementsState.Category category = state.categories.get(selectedCategory);
        renderTree(context, category, mouseX, mouseY);
    }

    private void renderCategories(DrawContext context, AdvancementsState state, int mouseX, int mouseY) {
        for (int i = 0; i < state.categories.size(); i++) {
            AdvancementsState.Category category = state.categories.get(i);
            int rowY = y + i * 20;
            if (rowY + 18 > y + h) {
                break;
            }
            boolean selected = i == selectedCategory;
            boolean hovered = Gui.inside(mouseX, mouseY, x, rowY, LIST_W, 18);
            Gui.inset(context, x, rowY, LIST_W, 18, selected ? 0xFF6F6F6F : hovered ? 0xFF9A9A9A : Palette.SLOT);
            if (selected) {
                Gui.outline(context, x, rowY, LIST_W, 18, Palette.WHITE);
            }
            Gui.item(context, category.icon, 1, x + 1, rowY + 1);
            Text count = Text.literal(category.completed + "/" + category.total);
            Gui.text(context, Gui.fit(titleOf(category.titleKey, category.title).getString(),
                    LIST_W - 24 - Gui.width(count)), x + 19, rowY + 5, Palette.WHITE);
            Gui.text(context, count, x + LIST_W - 3 - Gui.width(count), rowY + 5, Palette.GRAY);
        }
    }

    private void renderTree(DrawContext context, AdvancementsState.Category category, int mouseX, int mouseY) {
        int tx = x + LIST_W + 6;
        int tw = w - LIST_W - 6;
        int th = h;
        Gui.inset(context, tx, y, tw, th, 0xFF3A3A3A);
        for (int gx = tx + 16; gx < tx + tw - 1; gx += 16) {
            context.fill(gx, y + 1, gx + 1, y + th - 1, 0x2E000000);
        }
        for (int gy = y + 16; gy < y + th - 1; gy += 16) {
            context.fill(tx + 1, gy, tx + tw - 1, gy + 1, 0x2E000000);
        }

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        Map<String, AdvancementsState.Advancement> byId = new HashMap<>();
        for (AdvancementsState.Advancement advancement : category.advancements) {
            byId.put(advancement.id, advancement);
            minX = Math.min(minX, advancement.x);
            minY = Math.min(minY, advancement.y);
        }
        if (panCategory != selectedCategory || Double.isNaN(panX)) {
            panCategory = selectedCategory;
            panX = 8 - minX * COLUMN;
            panY = 8 - minY * ROW;
        }
        int originX = tx + (int) panX;
        int originY = y + (int) panY;

        context.enableScissor(tx + 1, y + 1, tx + tw - 1, y + th - 1);
        // Connectors first, black under white, as vanilla draws them.
        for (int pass = 0; pass < 2; pass++) {
            for (AdvancementsState.Advancement child : category.advancements) {
                AdvancementsState.Advancement parent = child.parentId == null ? null : byId.get(child.parentId);
                if (parent != null) {
                    connect(context, originX, originY, parent, child, pass == 0);
                }
            }
        }
        AdvancementsState.Advancement hovered = null;
        for (AdvancementsState.Advancement advancement : category.advancements) {
            int nx = originX + Math.round(advancement.x * COLUMN);
            int ny = originY + Math.round(advancement.y * ROW);
            node(context, nx, ny, advancement);
            if (Gui.inside(mouseX, mouseY, nx, ny, NODE, NODE) && Gui.inside(mouseX, mouseY, tx, y, tw, th)) {
                hovered = advancement;
            }
        }
        context.disableScissor();

        if (hovered != null) {
            List<Text> lines = new ArrayList<>();
            lines.add(titleOf(hovered.titleKey, hovered.title).copy()
                    .formatted(hovered.done ? Formatting.YELLOW : Formatting.WHITE));
            lines.add(titleOf(hovered.descriptionKey, hovered.description).copy()
                    .formatted(hovered.done ? Formatting.GREEN : Formatting.GRAY));
            if (!hovered.done && hovered.progress != null && !hovered.progress.isEmpty()) {
                lines.add(Text.literal(hovered.progress).formatted(Formatting.GRAY));
            }
            lines.add(Text.translatable(hovered.done ? "shannonuimod.advancements.done"
                    : "shannonuimod.advancements.todo").formatted(Formatting.DARK_GRAY));
            screen.tooltip(lines);
        }
    }

    /** An elbow line from the parent's right side to the child's left side, as in vanilla. */
    private static void connect(DrawContext context, int originX, int originY,
                                AdvancementsState.Advancement parent, AdvancementsState.Advancement child,
                                boolean outline) {
        int px = originX + Math.round(parent.x * COLUMN) + NODE;
        int py = originY + Math.round(parent.y * ROW) + NODE / 2;
        int cx = originX + Math.round(child.x * COLUMN);
        int cy = originY + Math.round(child.y * ROW) + NODE / 2;
        int midX = (px + cx) / 2;
        int grow = outline ? 1 : 0;
        int color = outline ? 0xFF000000 : 0xFFFFFFFF;
        context.fill(px - grow, py - grow, midX + 1 + grow, py + 1 + grow, color);
        context.fill(midX - grow, Math.min(py, cy) - grow, midX + 1 + grow, Math.max(py, cy) + 1 + grow, color);
        context.fill(midX - grow, cy - grow, cx + grow, cy + 1 + grow, color);
    }

    /** A frame in vanilla's three shapes: square tasks, rounded goals, spiked challenges. */
    private static void node(DrawContext context, int nx, int ny, AdvancementsState.Advancement advancement) {
        int face = advancement.done ? Palette.ADV_DONE : Palette.ADV_TODO;
        int light = advancement.done ? Palette.ADV_DONE_LIGHT : Palette.ADV_TODO_LIGHT;
        int shadow = advancement.done ? Palette.ADV_DONE_SHADOW : Palette.ADV_TODO_SHADOW;
        int cut = "goal".equals(advancement.frame) ? 4 : 1;
        for (int row = 0; row < NODE; row++) {
            int inset = Math.max(0, cut - Math.min(row, NODE - 1 - row));
            context.fill(nx + inset, ny + row, nx + NODE - inset, ny + row + 1, 0xFF000000);
        }
        for (int row = 1; row < NODE - 1; row++) {
            int inset = Math.max(1, cut - Math.min(row, NODE - 1 - row) + 1);
            context.fill(nx + inset, ny + row, nx + NODE - inset, ny + row + 1, face);
        }
        context.fill(nx + cut + 1, ny + 1, nx + NODE - cut - 1, ny + 2, light);
        context.fill(nx + cut + 1, ny + NODE - 2, nx + NODE - cut - 1, ny + NODE - 1, shadow);
        if ("challenge".equals(advancement.frame)) {
            int spike = face;
            context.fill(nx - 2, ny + 10, nx, ny + 16, 0xFF000000);
            context.fill(nx + NODE, ny + 10, nx + NODE + 2, ny + 16, 0xFF000000);
            context.fill(nx + 10, ny - 2, nx + 16, ny, 0xFF000000);
            context.fill(nx + 10, ny + NODE, nx + 16, ny + NODE + 2, 0xFF000000);
            context.fill(nx - 1, ny + 11, nx + 1, ny + 15, spike);
            context.fill(nx + NODE - 1, ny + 11, nx + NODE + 1, ny + 15, spike);
            context.fill(nx + 11, ny - 1, nx + 15, ny + 1, spike);
            context.fill(nx + 11, ny + NODE - 1, nx + 15, ny + NODE + 1, spike);
        }
        Gui.item(context, advancement.icon, 1, nx + 5, ny + 5);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        AdvancementsState state = state();
        if (state == null) {
            return false;
        }
        for (int i = 0; i < state.categories.size(); i++) {
            if (Gui.inside(mouseX, mouseY, x, y + i * 20, LIST_W, 18)) {
                selectedCategory = i;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (mouseX < x + LIST_W + 6) {
            return false;
        }
        panX += deltaX;
        panY += deltaY;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX < x + LIST_W + 6) {
            return false;
        }
        if (ShannonScreen.shiftDown()) {
            panX += amount * 16;
        } else {
            panY += amount * 16;
        }
        return true;
    }
}

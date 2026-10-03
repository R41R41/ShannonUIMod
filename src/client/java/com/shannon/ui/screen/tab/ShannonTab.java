package com.shannon.ui.screen.tab;

import com.shannon.ui.ShannonClient;
import com.shannon.ui.screen.ShannonScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * One tab of the {@link ShannonScreen}.
 *
 * <p>A tab gets a content box, adds vanilla widgets with {@link #add} and draws the rest in
 * {@link #render}. The screen owns input routing, tooltips and the frame around the tab.
 */
public abstract class ShannonTab {
    protected final ShannonClient shannon = ShannonClient.get();
    protected final MinecraftClient client = MinecraftClient.getInstance();
    protected ShannonScreen screen;
    protected int x;
    protected int y;
    protected int w;
    protected int h;

    /** The 16 by 16 tab icon. */
    public abstract Identifier icon();

    public abstract Text title();

    /** Called whenever the screen is laid out, including after a resize. */
    public final void init(ShannonScreen screen, int x, int y, int w, int h) {
        this.screen = screen;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        build();
    }

    /** Adds this tab's widgets. The content box is {@link #x}, {@link #y}, {@link #w}, {@link #h}. */
    protected void build() {
    }

    /** Text drawn at the right end of the title row, or {@code null}. */
    public Text status() {
        return null;
    }

    /** Draws everything that is not a widget. Widgets are already drawn. */
    public abstract void render(DrawContext context, int mouseX, int mouseY, float deltaTicks);

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        return false;
    }

    public boolean keyPressed(KeyInput input) {
        return false;
    }

    public void tick() {
    }

    /** Called when the tab becomes visible. */
    public void onShow() {
    }

    protected <T extends ClickableWidget> T add(T widget) {
        return screen.addTabWidget(widget);
    }
}

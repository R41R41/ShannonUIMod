package com.shannon.ui.screen.widget;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * A vertical scroll region for vanilla widgets and drawn rows.
 *
 * <p>Content is placed at offsets from the top; scrolling moves the widgets and hides those that
 * leave the region, so clicks never reach a widget that is out of view.
 */
public final class ScrollArea {
    private record Placed(ClickableWidget widget, int offset) {
    }

    private final List<Placed> widgets = new ArrayList<>();
    private final int top;
    private final int bottom;
    private int contentHeight;
    private double scroll;

    public ScrollArea(int top, int bottom) {
        this.top = top;
        this.bottom = bottom;
    }

    /** Places {@code widget} at {@code offset} pixels below the top of the content. */
    public <T extends ClickableWidget> T place(T widget, int offset) {
        widgets.add(new Placed(widget, offset));
        contentHeight = Math.max(contentHeight, offset + widget.getHeight());
        apply();
        return widget;
    }

    public void setContentHeight(int height) {
        contentHeight = Math.max(contentHeight, height);
        apply();
    }

    public void setScroll(double value) {
        scroll = Math.max(0, Math.min(value, maxScroll()));
        apply();
    }

    public double scroll() {
        return scroll;
    }

    public boolean scrollBy(double amount) {
        if (maxScroll() <= 0) {
            return false;
        }
        setScroll(scroll - amount * 20);
        return true;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - (bottom - top));
    }

    /** Screen y of a row placed at {@code offset}. */
    public int y(int offset) {
        return top + offset - (int) scroll;
    }

    /** Whether a row of {@code height} at {@code offset} is fully in view. */
    public boolean visible(int offset, int height) {
        int y = y(offset);
        return y >= top && y + height <= bottom;
    }

    private void apply() {
        for (Placed placed : widgets) {
            int y = y(placed.offset());
            placed.widget().setY(y);
            placed.widget().visible = y >= top && y + placed.widget().getHeight() <= bottom;
        }
    }

    /** A thin vanilla-style scrollbar at {@code x} when the content overflows. */
    public void drawScrollbar(DrawContext context, int x) {
        int max = maxScroll();
        if (max <= 0) {
            return;
        }
        int height = bottom - top;
        int thumb = Math.max(12, height * height / contentHeight);
        int thumbY = top + (int) ((height - thumb) * (scroll / max));
        context.fill(x, top, x + 4, bottom, 0xFF000000);
        context.fill(x, thumbY, x + 4, thumbY + thumb, 0xFFC0C0C0);
        context.fill(x + 3, thumbY, x + 4, thumbY + thumb, 0xFF808080);
        context.fill(x, thumbY + thumb - 1, x + 4, thumbY + thumb, 0xFF808080);
    }
}

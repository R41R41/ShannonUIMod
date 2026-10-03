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
    /** Room kept at the right for the scrollbar when the content overflows. */
    public static final int SCROLLBAR_GUTTER = 8;
    private static final int SCROLLBAR_WIDTH = 6;

    /** A placed widget; {@code column} is 0 or 1 for a two-column cell, or -1 for free placement. */
    private record Placed(ClickableWidget widget, int offset, int column) {
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
        return place(widget, offset, -1);
    }

    /**
     * Places {@code widget} in {@code column} (0 or 1) of a two-column grid. Its x and width are set
     * by {@link #layoutColumns} once all content is placed.
     */
    public <T extends ClickableWidget> T placeCell(T widget, int offset, int column) {
        return place(widget, offset, column);
    }

    private <T extends ClickableWidget> T place(T widget, int offset, int column) {
        widgets.add(new Placed(widget, offset, column));
        contentHeight = Math.max(contentHeight, offset + widget.getHeight());
        apply();
        return widget;
    }

    public void setContentHeight(int height) {
        contentHeight = Math.max(contentHeight, height);
        apply();
    }

    /**
     * Sizes the two-column cells to fill {@code width} from {@code left}, leaving room for the
     * scrollbar only when the content overflows, so both margins match when it does not.
     */
    public void layoutColumns(int left, int width, int gap) {
        int usable = width - (maxScroll() > 0 ? SCROLLBAR_GUTTER : 0);
        int columnWidth = (usable - gap) / 2;
        for (Placed placed : widgets) {
            if (placed.column() >= 0) {
                placed.widget().setX(left + placed.column() * (columnWidth + gap));
                placed.widget().setWidth(columnWidth);
            }
        }
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

    /** A thin vanilla-style scrollbar ending at {@code right} when the content overflows. */
    public void drawScrollbar(DrawContext context, int right) {
        int x = right - SCROLLBAR_WIDTH;
        int max = maxScroll();
        if (max <= 0) {
            return;
        }
        int height = bottom - top;
        int thumb = Math.max(12, height * height / contentHeight);
        int thumbY = top + (int) ((height - thumb) * (scroll / max));
        // Vanilla's scroller: a dark track and a raised light thumb inset by a pixel.
        context.fill(x, top, right, bottom, 0xFF000000);
        context.fill(x + 1, thumbY + 1, right - 1, thumbY + thumb - 1, 0xFF808080);
        context.fill(x + 1, thumbY + 1, right - 2, thumbY + thumb - 2, 0xFFC0C0C0);
    }
}

package com.shannon.ui.gfx;

import net.minecraft.client.gui.DrawContext;

import java.util.HashMap;
import java.util.Map;

/**
 * A small pixel-art icon written as rows of characters, each mapped to a colour.
 *
 * <p>{@code '.'} and spaces are transparent. Drawing merges runs of equal colour into single fills,
 * so even a 12 by 12 icon is a handful of draw calls.
 */
public final class PixelIcon {
    private final char[][] grid;
    private final Map<Character, Integer> palette;

    private PixelIcon(char[][] grid, Map<Character, Integer> palette) {
        this.grid = grid;
        this.palette = palette;
    }

    /** Builds an icon; {@code colors} alternates characters and ARGB colours. */
    public static PixelIcon of(Object[] colors, String... rows) {
        Map<Character, Integer> palette = new HashMap<>();
        for (int i = 0; i + 1 < colors.length; i += 2) {
            palette.put((Character) colors[i], (Integer) colors[i + 1]);
        }
        char[][] grid = new char[rows.length][];
        for (int y = 0; y < rows.length; y++) {
            grid[y] = rows[y].toCharArray();
        }
        return new PixelIcon(grid, palette);
    }

    public int width() {
        int width = 0;
        for (char[] row : grid) {
            width = Math.max(width, row.length);
        }
        return width;
    }

    public int height() {
        return grid.length;
    }

    /** The same shape with one character's colour replaced. */
    public PixelIcon with(char key, int color) {
        Map<Character, Integer> copy = new HashMap<>(palette);
        copy.put(key, color);
        return new PixelIcon(grid, copy);
    }

    /** The same icon turned a quarter clockwise. */
    public PixelIcon rotatedClockwise() {
        int height = height();
        int width = width();
        char[][] rotated = new char[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char c = x < grid[y].length ? grid[y][x] : '.';
                rotated[x][height - 1 - y] = c;
            }
        }
        return new PixelIcon(rotated, palette);
    }

    public void draw(DrawContext context, int x, int y) {
        draw(context, x, y, 1, 1f);
    }

    public void draw(DrawContext context, int x, int y, int scale, float alpha) {
        for (int row = 0; row < grid.length; row++) {
            char[] line = grid[row];
            int start = 0;
            while (start < line.length) {
                char c = line[start];
                int end = start + 1;
                while (end < line.length && line[end] == c) {
                    end++;
                }
                Integer color = palette.get(c);
                if (color != null) {
                    context.fill(x + start * scale, y + row * scale, x + end * scale, y + (row + 1) * scale,
                            Palette.fade(color, alpha));
                }
                start = end;
            }
        }
    }
}

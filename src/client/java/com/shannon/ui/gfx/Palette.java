package com.shannon.ui.gfx;

/**
 * Colours taken from vanilla's own GUI textures and chat formatting codes, as ARGB.
 *
 * <p>Text drawn with a shadow gets vanilla's shadow colour automatically, so only the face colour
 * is listed here.
 */
public final class Palette {
    // Formatting codes (§f, §7, §8, §e, §b, §c, §a, §6)
    public static final int WHITE = 0xFFFFFFFF;
    public static final int GRAY = 0xFFAAAAAA;
    public static final int DARK_GRAY = 0xFF555555;
    public static final int YELLOW = 0xFFFFFF55;
    public static final int AQUA = 0xFF55FFFF;
    public static final int RED = 0xFFFF5555;
    public static final int GREEN = 0xFF55FF55;
    public static final int GOLD = 0xFFFFAA00;

    // Container texture
    public static final int PANEL = 0xFFC6C6C6;
    public static final int PANEL_LIGHT = 0xFFFFFFFF;
    public static final int PANEL_SHADOW = 0xFF555555;
    public static final int OUTLINE = 0xFF000000;
    /** Container titles: vanilla draws them in this colour without a shadow. */
    public static final int LABEL = 0xFF404040;

    // Slot texture
    public static final int SLOT = 0xFF8B8B8B;
    public static final int SLOT_SHADOW = 0xFF373737;
    public static final int SLOT_LIGHT = 0xFFFFFFFF;
    public static final int SLOT_HOVER = 0x80FFFFFF;

    // Inactive creative tab
    public static final int TAB_OFF = 0xFF8F8F8F;
    public static final int TAB_OFF_LIGHT = 0xFFB5B5B5;
    public static final int TAB_OFF_SHADOW = 0xFF5E5E5E;

    // HUD boxes, like the chat background
    public static final int HUD_BOX = 0x99000000;
    public static final int HUD_EDGE = 0x1AFFFFFF;
    public static final int WAITING_EDGE = 0xFF2E9C9C;
    public static final int ERROR_EDGE = 0xFFA33A3A;

    // Experience bar
    public static final int XP = 0xFF80FF20;
    public static final int XP_DARK = 0xFF4FA80F;
    public static final int BAR_BACK = 0xFF3A3A3A;

    // Tooltip, as vanilla draws it
    public static final int TOOLTIP_BACK = 0xF0100010;
    public static final int TOOLTIP_EDGE_TOP = 0x505000FF;
    public static final int TOOLTIP_EDGE_BOTTOM = 0x5028007F;

    // Advancement frames
    public static final int ADV_DONE = 0xFFE2B53B;
    public static final int ADV_DONE_LIGHT = 0xFFF7D774;
    public static final int ADV_DONE_SHADOW = 0xFFA87C1E;
    public static final int ADV_TODO = 0xFF9C9C9C;
    public static final int ADV_TODO_LIGHT = 0xFFC6C6C6;
    public static final int ADV_TODO_SHADOW = 0xFF6E6E6E;

    // Screen dimming behind containers
    public static final int DIM_TOP = 0xC0101010;
    public static final int DIM_BOTTOM = 0xD0101010;

    private Palette() {
    }

    /** {@code color} with its alpha multiplied by {@code alpha} (0 to 1). */
    public static int fade(int color, float alpha) {
        int a = Math.round(((color >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, alpha)));
        return (a << 24) | (color & 0x00FFFFFF);
    }
}

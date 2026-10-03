package com.shannon.ui.hud;

import com.shannon.model.VoiceState;
import com.shannon.ui.ShannonClient;
import com.shannon.ui.gfx.Gui;
import com.shannon.ui.gfx.Icons;
import com.shannon.ui.gfx.Palette;
import com.shannon.ui.input.KeyBindings;
import com.shannon.ui.state.VoiceStatus;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * A small box above the hotbar while push-to-talk is in use: that the bot listens, who else is
 * talking, and what it heard. It shows only while the key is held and for a few seconds after.
 */
public final class VoiceIndicator {
    /** How long the box waits for a transcript after the key is let go. */
    private static final long WAIT_MS = 10_000;
    /** How long a transcript stays. */
    private static final long SHOW_MS = 5_000;
    private static final int MAX_WIDTH = 260;
    /** Bottom edge, above the hotbar, hearts and vanilla's action bar text. */
    private static final int ABOVE_BOTTOM = 84;

    private VoiceIndicator() {
    }

    static void render(DrawContext context, ShannonClient shannon) {
        VoiceStatus voice = shannon.voice();
        long now = System.currentTimeMillis();
        VoiceState event = voice.event();
        List<Text> lines = new ArrayList<>();
        int color;
        boolean live = voice.holding();
        if (event != null && VoiceState.TRANSCRIPT.equals(event.event)) {
            if (!live && now - voice.eventAt() > SHOW_MS) {
                return;
            }
            color = Palette.WHITE;
            lines.add(Text.translatable("shannonuimod.voice.heard", event.text));
            lines.add(Text.translatable("minebot".equals(event.mode)
                    ? "shannonuimod.voice.to_bot" : "shannonuimod.voice.to_chat"));
        } else if (event != null && VoiceState.BLOCKED.equals(event.event)) {
            if (!live && now - voice.eventAt() > SHOW_MS) {
                return;
            }
            color = Palette.YELLOW;
            lines.add(event.blockedBy != null
                    ? Text.translatable("shannonuimod.voice.blocked", event.blockedBy)
                    : Text.translatable("shannonuimod.voice.blocked_anon"));
        } else if (event != null && VoiceState.UNAVAILABLE.equals(event.event)) {
            if (!live && now - voice.eventAt() > SHOW_MS) {
                return;
            }
            color = Palette.GRAY;
            lines.add(Text.translatable("shannonuimod.voice.unavailable"));
        } else if (live) {
            color = Palette.RED;
            long seconds = (now - voice.pressedAt()) / 1000;
            lines.add(Text.translatable("shannonuimod.voice.listening", seconds / 60 + ":" + String.format("%02d", seconds % 60)));
            lines.add(Text.translatable("shannonuimod.voice.release",
                    KeyBindings.keyName(shannon.keys().pushToTalk())));
        } else if (voice.releasedAt() > 0 && now - voice.releasedAt() < WAIT_MS && voice.releasedAt() > voice.eventAt()) {
            color = Palette.GRAY;
            lines.add(Text.translatable("shannonuimod.voice.processing"));
        } else {
            return;
        }
        int width = 0;
        List<OrderedText> wrapped = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            for (OrderedText line : Gui.wrap(lines.get(i), MAX_WIDTH)) {
                wrapped.add(line);
                width = Math.max(width, Gui.font().getWidth(line));
            }
        }
        int boxW = width + 12 + 14;
        int boxH = wrapped.size() * 10 + 6;
        int x = (context.getScaledWindowWidth() - boxW) / 2;
        int y = context.getScaledWindowHeight() - ABOVE_BOTTOM - boxH;
        Gui.hudBox(context, x, y, boxW, boxH, Palette.HUD_EDGE);
        (live ? Icons.MIC_ON : Icons.MIC_OFF).draw(context, x + 5, y + 4);
        for (int i = 0; i < wrapped.size(); i++) {
            Gui.text(context, wrapped.get(i), x + 20, y + 4 + i * 10, i == 0 ? color : Palette.GRAY);
        }
    }
}

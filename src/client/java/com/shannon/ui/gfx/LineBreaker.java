package com.shannon.ui.gfx;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.util.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Vanilla's word wrapping plus the one Japanese rule it lacks: a line never starts with closing
 * punctuation or a small kana. Such a character hangs at the end of the previous line instead, in
 * a little room kept free for it, so no line grows wider than asked.
 */
final class LineBreaker {
    /** Characters that must not begin a line. */
    private static final String NO_START = "、。，．,.・：；:;？！?!ー～…‥）」』】〕〉》］｝)]}’”"
            + "ぁぃぅぇぉっゃゅょゎゕゖァィゥェォッャュョヮヵヶ々";
    private static final String HANG_SAMPLE = "。";

    private LineBreaker() {
    }

    static List<OrderedText> wrap(TextRenderer font, StringVisitable text, int maxWidth) {
        int hang = font.getWidth(HANG_SAMPLE);
        if (maxWidth <= hang * 4) {
            return font.wrapLines(text, Math.max(1, maxWidth));
        }
        List<StringVisitable> lines = new ArrayList<>(font.getTextHandler().wrapLines(text, maxWidth - hang, Style.EMPTY));
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).getString();
            if (line.isEmpty() || NO_START.indexOf(line.charAt(0)) < 0) {
                continue;
            }
            StringVisitable[] parts = split(lines.get(i), 1);
            lines.set(i - 1, StringVisitable.concat(lines.get(i - 1), parts[0]));
            if (line.length() == 1) {
                lines.remove(i);
                i--;
            } else {
                lines.set(i, parts[1]);
            }
        }
        return Language.getInstance().reorder(lines);
    }

    /** Splits {@code text} after its first {@code count} characters, keeping each part's style. */
    private static StringVisitable[] split(StringVisitable text, int count) {
        List<StringVisitable> head = new ArrayList<>();
        List<StringVisitable> tail = new ArrayList<>();
        int[] left = {count};
        text.visit((style, string) -> {
            int take = Math.min(left[0], string.length());
            if (take > 0) {
                head.add(StringVisitable.styled(string.substring(0, take), style));
                left[0] -= take;
            }
            if (take < string.length()) {
                tail.add(StringVisitable.styled(string.substring(take), style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return new StringVisitable[]{StringVisitable.concat(head), StringVisitable.concat(tail)};
    }
}

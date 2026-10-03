package com.shannon.ui.screen.widget;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * Vanilla's slider for a value between {@code min} and {@code max}, rounded to whole steps.
 *
 * <p>Changes are reported as the knob moves; callers that talk to the server debounce them.
 */
public final class ValueSlider extends SliderWidget {
    private final double min;
    private final double max;
    private final DoubleFunction<Text> label;
    private final DoubleConsumer onChange;

    public ValueSlider(int x, int y, int width, int height, double min, double max, double value,
                       DoubleFunction<Text> label, DoubleConsumer onChange) {
        super(x, y, width, height, Text.empty(), (clamp(value, min, max) - min) / (max - min));
        this.min = min;
        this.max = max;
        this.label = label;
        this.onChange = onChange;
        updateMessage();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public double current() {
        return Math.round(min + (max - min) * this.value);
    }

    @Override
    protected void updateMessage() {
        setMessage(label.apply(current()));
    }

    @Override
    protected void applyValue() {
        onChange.accept(current());
    }
}

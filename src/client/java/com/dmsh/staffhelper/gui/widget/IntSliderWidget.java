package com.dmsh.staffhelper.gui.widget;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.IntConsumer;

public class IntSliderWidget extends SliderWidget {
    private final String label;
    private final IntConsumer listener;

    private int min;
    private int max;
    private int current;

    public IntSliderWidget(int x, int y, int w, int h, String label, int min, int max, int initial, IntConsumer listener) {
        super(x, y, w, h, Text.empty(), 0.0);
        this.label = label == null ? "" : label;
        this.listener = listener == null ? v -> {} : listener;
        setRange(min, max);
        setIntValue(initial);
    }

    public void setRange(int min, int max) {
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
        setIntValue(current);
    }

    public void setIntValue(int value) {
        this.current = clamp(value, min, max);
        int span = Math.max(1, max - min);
        this.value = span == 0 ? 0.0 : (double) (this.current - min) / (double) span;
        updateMessage();
    }

    public int getIntValue() {
        return current;
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Text.literal(label + ": " + current + "/" + max));
    }

    @Override
    protected void applyValue() {
        int span = Math.max(1, max - min);
        int next = min + (int) Math.round(this.value * span);
        next = clamp(next, min, max);
        if (next != current) {
            current = next;
            listener.accept(current);
        }
        updateMessage();
    }

    private static int clamp(int v, int min, int max) {
        if (v < min) return min;
        return Math.min(v, max);
    }
}

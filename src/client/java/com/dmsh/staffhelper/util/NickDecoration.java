package com.dmsh.staffhelper.util;

public record NickDecoration(String symbol, int rgb) {
    public NickDecoration {
        if (symbol == null || symbol.isEmpty()) {
            throw new IllegalArgumentException("symbol must not be empty");
        }

        rgb = rgb & 0x00FFFFFF;
    }
}

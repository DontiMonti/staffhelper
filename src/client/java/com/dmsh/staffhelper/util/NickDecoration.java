package com.dmsh.staffhelper.util;

/**
 * Описание декора для ника (символ + цвет).
 *
 * @param symbol символ(ы), которые будут добавляться после ника (например, "★" или "✦")
 * @param rgb    цвет в формате 0xRRGGBB
 */
public record NickDecoration(String symbol, int rgb) {
    public NickDecoration {
        if (symbol == null || symbol.isEmpty()) {
            throw new IllegalArgumentException("symbol must not be empty");
        }
        // нормализуем до 0xRRGGBB
        rgb = rgb & 0x00FFFFFF;
    }
}

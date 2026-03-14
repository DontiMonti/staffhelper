package com.dmsh.staffhelper.util;

import java.util.Map;

public final class NickDecorationsStore {
    private NickDecorationsStore() {}

    private static final Map<String, NickDecoration> DECORATIONS = Map.of(
            "dontimonti", new NickDecoration("\u2605", 0xFFD84D),
            "werkuk", new NickDecoration("\u2605", 0xFF76C6)
    );

    public static Map<String, NickDecoration> get() {
        return DECORATIONS;
    }

    public static int size() {
        return DECORATIONS.size();
    }
}

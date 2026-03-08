package com.dmsh.staffhelper.util;

import java.util.Collections;
import java.util.Map;

public final class NickDecorationsStore {
    private NickDecorationsStore() {}

    private static volatile Map<String, NickDecoration> DECORATIONS = Collections.emptyMap();

    public static Map<String, NickDecoration> get() {
        return DECORATIONS;
    }

    public static void set(Map<String, NickDecoration> newMap) {
        DECORATIONS = (newMap == null) ? Collections.emptyMap() : Map.copyOf(newMap);
    }

    public static int size() {
        return DECORATIONS.size();
    }
}

package com.dmsh.staffhelper.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** In-memory debug log for remote requests/responses. */
public final class DebugLogStore {
    private DebugLogStore() {}

    private static final int MAX = 500;
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final List<String> LINES = new ArrayList<>();

    public static synchronized void add(String line) {
        if (line == null) return;
        LINES.add("[" + LocalTime.now().format(TF) + "] " + line);
        while (LINES.size() > MAX) {
            LINES.remove(0);
        }
    }

    public static synchronized List<String> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(LINES));
    }
}

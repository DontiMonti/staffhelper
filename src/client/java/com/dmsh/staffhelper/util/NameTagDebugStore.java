package com.dmsh.staffhelper.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Stores the latest nametag processing snapshot for DebugScreen.
 */
public final class NameTagDebugStore {
    private NameTagDebugStore() {}

    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static Snapshot snapshot = new Snapshot("--:--:--", "", "", false, false, "", false, "", "", false);

    public static synchronized void update(
            String raw,
            String cleaned,
            boolean filtered,
            boolean hasServerMeta,
            String nick,
            boolean hasDecoration,
            String role
    ) {
        Snapshot next = new Snapshot(
                LocalTime.now().format(TF),
                safe(raw),
                safe(cleaned),
                filtered,
                hasServerMeta,
                safe(nick),
                hasDecoration,
                safe(role),
                snapshot.lastSeenLabel(),
                snapshot.suppressedMarkerLabel()
        );

        if (isSameData(snapshot, next)) return;
        snapshot = next;
    }

    public static synchronized Snapshot snapshot() {
        return snapshot;
    }

    public static synchronized void markRenderLabel(String label, boolean suppressed) {
        Snapshot next = new Snapshot(
                LocalTime.now().format(TF),
                snapshot.raw(),
                snapshot.cleaned(),
                snapshot.filtered(),
                snapshot.hasServerMeta(),
                snapshot.nick(),
                snapshot.hasDecoration(),
                snapshot.role(),
                safe(label),
                suppressed
        );
        if (!isSameData(snapshot, next)) {
            snapshot = next;
        }
    }

    private static boolean isSameData(Snapshot a, Snapshot b) {
        return Objects.equals(a.raw(), b.raw())
                && Objects.equals(a.cleaned(), b.cleaned())
                && a.filtered() == b.filtered()
                && a.hasServerMeta() == b.hasServerMeta()
                && Objects.equals(a.nick(), b.nick())
                && a.hasDecoration() == b.hasDecoration()
                && Objects.equals(a.role(), b.role())
                && Objects.equals(a.lastSeenLabel(), b.lastSeenLabel())
                && a.suppressedMarkerLabel() == b.suppressedMarkerLabel();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    public record Snapshot(
            String time,
            String raw,
            String cleaned,
            boolean filtered,
            boolean hasServerMeta,
            String nick,
            boolean hasDecoration,
            String role,
            String lastSeenLabel,
            boolean suppressedMarkerLabel
    ) {}
}

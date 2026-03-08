package com.dmsh.staffhelper.util;

import java.util.ArrayDeque;
import java.util.Deque;

public final class TpsTracker {
    private TpsTracker() {}

    private static volatile double tpsNow = 20.0;
    private static volatile long lastUpdateMs = 0L;

    private static final class Sample {
        final long ts;
        final double tps;
        Sample(long ts, double tps) { this.ts = ts; this.tps = tps; }
    }

    private static final Deque<Sample> samples = new ArrayDeque<>();
    private static final long WINDOW_MS = 15L * 60L * 1000L;

    public static void onWorldTimeUpdate() {
        long now = System.currentTimeMillis();
        long prev = lastUpdateMs;
        lastUpdateMs = now;

        if (prev <= 0) return;
        long dt = now - prev;
        if (dt <= 0) return;

        double est = 20000.0 / (double) dt;
        if (est < 0) est = 0;
        if (est > 20.0) est = 20.0;
        tpsNow = est;

        synchronized (samples) {
            samples.addLast(new Sample(now, est));
            prune(now);
        }
    }

    public static double getTpsNow() {
        return tpsNow;
    }

    public static double getTps5m() {
        return getTpsAvgWindowMs(5L * 60L * 1000L);
    }

    public static double getTps10m() {
        return getTpsAvgWindowMs(10L * 60L * 1000L);
    }

    public static double getTps15m() {
        return getTpsAvgWindowMs(15L * 60L * 1000L);
    }

    private static double getTpsAvgWindowMs(long windowMs) {
        long now = System.currentTimeMillis();
        synchronized (samples) {
            prune(now);
            if (samples.isEmpty()) return tpsNow;
            double sum = 0.0;
            int n = 0;
            for (Sample s : samples) {
                if ((now - s.ts) <= windowMs) {
                    sum += s.tps;
                    n++;
                }
            }
            return n == 0 ? tpsNow : (sum / n);
        }
    }

    private static void prune(long now) {
        while (!samples.isEmpty()) {
            Sample s = samples.peekFirst();
            if (s == null || (now - s.ts) > WINDOW_MS) samples.pollFirst();
            else break;
        }
    }
}

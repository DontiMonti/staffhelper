package com.dmsh.staffhelper.feature;

import com.dmsh.staffhelper.StaffHelperState;
import com.dmsh.staffhelper.gui.util.UiChrome;
import com.dmsh.staffhelper.util.AllowedUsersAccessGate;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NickSearchFeature {
    private static final long JOIN_ANIMATION_MS = 650L;
    private static volatile long joinAnimationStartMs = 0L;

    private static final Map<String, AnimatedNickRow> animatedRows = new LinkedHashMap<>();
    private static float panelProgress = 0.0f;

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            joinAnimationStartMs = System.currentTimeMillis();
            animatedRows.clear();
            panelProgress = 0.0f;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            joinAnimationStartMs = 0L;
            animatedRows.clear();
            panelProgress = 0.0f;
        });
        HudRenderCallback.EVENT.register(NickSearchFeature::renderHud);
    }

    private static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!AllowedUsersAccessGate.isModAllowed()) {
            animatedRows.clear();
            panelProgress = 0.0f;
            return;
        }
        if (StaffHelperState.CONFIG == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) return;

        List<PlayerListEntry> matched = collectMatched(mc);
        syncAnimatedRows(matched);
        float visibleRows = updateAnimatedRows(mc);

        float targetPanel = matched.isEmpty() ? 0.0f : 1.0f;
        panelProgress = approach(panelProgress, targetPanel, 0.22f);
        if (panelProgress <= 0.01f && visibleRows <= 0.01f) return;

        int targetX = StaffHelperState.CONFIG.nickWidgetX;
        int targetY = StaffHelperState.CONFIG.nickWidgetY;

        float scale = getScale();
        int height = getAnimatedWidgetHeight(visibleRows, scale, panelProgress);
        float joinProgress = joinProgress();
        float eased = easeOutCubic(joinProgress);
        int startY = -height - 16;
        int drawY = (joinProgress < 1.0f && targetPanel > 0.01f) ? lerpInt(startY, targetY, eased) : targetY;

        drawAnimatedWidget(ctx, targetX, drawY, scale, joinProgress >= 1.0f);
    }

    private static List<PlayerListEntry> collectMatched(MinecraftClient mc) {
        if (mc.player == null || mc.getNetworkHandler() == null) return List.of();
        if (!StaffHelperState.CONFIG.nickSearchEnabled) return List.of();

        List<String> patterns = StaffHelperState.CONFIG.nickPatterns;
        if (patterns == null || patterns.isEmpty()) return List.of();

        List<PlayerListEntry> matched = new ArrayList<>();
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            String name = entry.getProfile().getName();
            if (matchesAny(patterns, name)) matched.add(entry);
        }
        return matched;
    }

    private static void syncAnimatedRows(List<PlayerListEntry> matched) {
        for (AnimatedNickRow row : animatedRows.values()) {
            row.targetVisible = false;
        }

        int order = 0;
        for (PlayerListEntry e : matched) {
            String key = e.getProfile() != null ? e.getProfile().getName() : null;
            if (key == null || key.isBlank()) continue;
            AnimatedNickRow row = animatedRows.computeIfAbsent(key.toLowerCase(Locale.ROOT), k -> new AnimatedNickRow());
            Text dn = e.getDisplayName();
            row.display = dn != null ? dn : Text.literal(key);
            row.targetVisible = true;
            row.order = order++;
        }
    }

    private static float updateAnimatedRows(MinecraftClient mc) {
        float sum = 0.0f;
        List<String> toRemove = new ArrayList<>();
        boolean appearedAny = false;
        for (Map.Entry<String, AnimatedNickRow> entry : animatedRows.entrySet()) {
            AnimatedNickRow row = entry.getValue();
            if (row.targetVisible && !row.wasTargetVisible) {
                appearedAny = true;
            }
            float target = row.targetVisible ? 1.0f : 0.0f;
            row.progress = approach(row.progress, target, row.targetVisible ? 0.24f : 0.18f);
            row.wasTargetVisible = row.targetVisible;
            if (!row.targetVisible && row.progress <= 0.01f) {
                toRemove.add(entry.getKey());
                continue;
            }
            sum += row.progress;
        }
        for (String key : toRemove) {
            animatedRows.remove(key);
        }
        if (appearedAny && mc != null && mc.player != null) {
            mc.player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 0.975f, 1.08f);
        }
        return sum;
    }

    private static void drawAnimatedWidget(DrawContext ctx, int x, int y, float scale, boolean clampToScreen) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int padding = getPadding(scale);
        int lineH = getLineHeight(scale);
        int titleH = Math.max(mc.textRenderer.fontHeight + 2, Math.round(12 * scale));
        int width = getWidgetWidth(scale);
        float panelVisual = easeOutCubic(panelProgress);

        float visibleRows = 0.0f;
        for (AnimatedNickRow row : animatedRows.values()) {
            visibleRows += row.progress;
        }
        int height = getAnimatedWidgetHeight(visibleRows, scale, panelProgress);

        if (clampToScreen) {
            int screenW = mc.getWindow().getScaledWidth();
            int screenH = mc.getWindow().getScaledHeight();
            x = Math.max(0, Math.min(x, screenW - width));
            y = Math.max(0, Math.min(y, screenH - height));
        }

        UiChrome.drawPanel(ctx, x, y, width, height, 8, System.currentTimeMillis());

        int titleAlpha = Math.max(0, Math.min(255, Math.round(255 * panelVisual)));
        ctx.drawText(mc.textRenderer, Text.literal("NickSearch"), x + padding, y + padding, (titleAlpha << 24) | 0xFFFFFF, false);

        List<AnimatedNickRow> sorted = new ArrayList<>(animatedRows.values());
        sorted.sort(Comparator.comparingInt(r -> r.order));

        float yy = y + padding + titleH;
        for (AnimatedNickRow row : sorted) {
            if (row.progress <= 0.01f) continue;
            float rowVisual = easeOutCubic(row.progress) * panelVisual;
            int alpha = Math.max(0, Math.min(255, Math.round(234 * rowVisual)));
            int lineX = x + padding + Math.round((1.0f - rowVisual) * 8.0f);
            ctx.drawText(mc.textRenderer, row.display, lineX, Math.round(yy), (alpha << 24) | 0xEAEAEA, false);
            yy += row.progress * lineH;
        }
    }

    // Preview helpers for HUD editor
    public static int getWidgetWidthPreview() {
        return getWidgetWidth(getScale());
    }

    public static int getWidgetHeightPreview(int rows) {
        float scale = getScale();
        return getWidgetHeight(rows, scale);
    }

    public static void renderWidgetPreview(DrawContext ctx, int x, int y) {
        List<Text> demo = List.of(
                Text.literal("ExamplePlayer1"),
                Text.literal("Klaucnher_123"),
                Text.literal("Tester"),
                Text.literal("AdminGuy"),
                Text.literal("Someone")
        );
        renderWidgetText(ctx, x, y, demo, true);
    }

    private static void renderWidgetText(DrawContext ctx, int x, int y, List<Text> lines, boolean clampToScreen) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float scale = getScale();

        int padding = getPadding(scale);
        int lineH = getLineHeight(scale);
        int titleH = Math.max(12, Math.round(12 * scale));

        int width = getWidgetWidth(scale);
        int height = getWidgetHeight(lines.size(), scale);

        if (clampToScreen) {
            int screenW = mc.getWindow().getScaledWidth();
            int screenH = mc.getWindow().getScaledHeight();
            x = Math.max(0, Math.min(x, screenW - width));
            y = Math.max(0, Math.min(y, screenH - height));
        }

        UiChrome.drawPanel(ctx, x, y, width, height, 8, System.currentTimeMillis());

        ctx.drawText(mc.textRenderer, Text.literal("NickSearch"), x + padding, y + padding, 0xFFFFFFFF, false);

        int yy = y + padding + titleH;
        for (Text t : lines) {
            ctx.drawText(mc.textRenderer, t, x + padding, yy, 0xFFEAEAEA, false);
            yy += lineH;
        }
    }

    // Matching logic
    private static boolean matchesAny(List<String> patterns, String name) {
        for (String p : patterns) {
            if (matches(p, name)) return true;
        }
        return false;
    }

    private static boolean matches(String pattern, String name) {
        if (pattern == null || name == null) return false;

        pattern = pattern.trim();
        if (pattern.isEmpty()) return false;

        String p = pattern.toLowerCase(Locale.ROOT);
        String n = name.toLowerCase(Locale.ROOT);

        if (p.endsWith("*")) {
            String prefix = p.substring(0, p.length() - 1);
            return n.startsWith(prefix);
        }
        return n.startsWith(p) || n.contains(p);
    }

    private static float getScale() {
        if (StaffHelperState.CONFIG == null) return 1.0f;
        float v = StaffHelperState.CONFIG.nickBoxScale;
        if (Float.isNaN(v)) return 1.0f;
        return Math.max(0.6f, Math.min(2.0f, v));
    }

    private static int getWidgetWidth(float scale) {
        return Math.max(140, Math.round(180 * scale));
    }

    private static int getWidgetHeight(int rows, float scale) {
        int padding = getPadding(scale);
        int lineH = getLineHeight(scale);
        int titleH = Math.max(12, Math.round(12 * scale));
        return padding + titleH + (rows * lineH) + padding;
    }

    private static int getAnimatedWidgetHeight(float visibleRows, float scale, float openProgress) {
        int padding = getPadding(scale);
        int lineH = getLineHeight(scale);
        int titleH = Math.max(12, Math.round(12 * scale));
        int contentH = Math.max(1, Math.round(visibleRows * lineH));
        return padding + Math.max(1, Math.round(titleH * openProgress)) + contentH + padding;
    }

    private static int getPadding(float scale) {
        return Math.max(4, Math.round(6 * scale));
    }

    private static int getLineHeight(float scale) {
        return Math.max(10, Math.round(10 * scale));
    }

    private static float joinProgress() {
        long start = joinAnimationStartMs;
        if (start <= 0L) return 1.0f;
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed >= JOIN_ANIMATION_MS) return 1.0f;
        if (elapsed <= 0L) return 0.0f;
        return elapsed / (float) JOIN_ANIMATION_MS;
    }

    private static float easeOutCubic(float t) {
        float clamped = Math.max(0.0f, Math.min(1.0f, t));
        float inv = 1.0f - clamped;
        return 1.0f - (inv * inv * inv);
    }

    private static int lerpInt(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    private static float approach(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    private static final class AnimatedNickRow {
        private Text display = Text.literal("");
        private boolean targetVisible = false;
        private boolean wasTargetVisible = false;
        private float progress = 0.0f;
        private int order = 0;
    }
}

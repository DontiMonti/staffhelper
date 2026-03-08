package com.dmsh.staffhelper.feature;

import com.dmsh.staffhelper.StaffHelperState;
import com.dmsh.staffhelper.config.StaffHelperConfig;
import com.dmsh.staffhelper.gui.util.UiChrome;
import com.dmsh.staffhelper.util.AllowedUsersAccessGate;
import com.dmsh.staffhelper.util.RolesStore;
import com.dmsh.staffhelper.util.TpsTracker;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class StatsHudFeature {
    private StatsHudFeature() {}

    private static final long JOIN_ANIMATION_MS = 650L;
    private static volatile long joinAnimationStartMs = 0L;

    private static final Map<String, AnimatedStatsLine> animatedLines = new LinkedHashMap<>();
    private static float panelProgress = 0.0f;

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            joinAnimationStartMs = System.currentTimeMillis();
            animatedLines.clear();
            panelProgress = 0.0f;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            joinAnimationStartMs = 0L;
            animatedLines.clear();
            panelProgress = 0.0f;
        });
        HudRenderCallback.EVENT.register(StatsHudFeature::renderHud);
    }

    private static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!AllowedUsersAccessGate.isModAllowed()) {
            animatedLines.clear();
            panelProgress = 0.0f;
            return;
        }
        StaffHelperConfig cfg = StaffHelperState.CONFIG;
        if (cfg == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) return;

        PanelContent target = collectTargetContent(mc, cfg);
        syncAnimatedLines(target.lines());
        float visibleRows = updateAnimatedLines();

        float targetPanel = target.lines().isEmpty() ? 0.0f : 1.0f;
        panelProgress = approach(panelProgress, targetPanel, 0.22f);
        if (panelProgress <= 0.01f && visibleRows <= 0.01f) return;

        int targetX = cfg.statsWidgetX;
        int targetY = cfg.statsWidgetY;

        float joinProgress = joinProgress();
        float eased = easeOutCubic(joinProgress);
        int panelW = getAnimatedPanelWidth(mc, cfg, panelProgress);
        int startX = -panelW - 16;
        int drawX = (joinProgress < 1.0f && targetPanel > 0.01f) ? lerpInt(startX, targetX, eased) : targetX;

        drawAnimatedPanel(ctx, drawX, targetY, cfg, joinProgress >= 1.0f || targetPanel <= 0.01f);
    }

    private static PanelContent collectTargetContent(MinecraftClient mc, StaffHelperConfig cfg) {
        if (!cfg.statsEnabled) return new PanelContent(List.of());
        if (mc.player == null || mc.getNetworkHandler() == null) return new PanelContent(List.of());

        String nick = mc.player.getGameProfile().getName();
        if (nick == null) nick = "";

        String role = RolesStore.getRoleFor(nick);
        int roleColor = RolesStore.getRoleColorFor(nick);
        if ((role == null || role.isBlank()) && mc.player.getDisplayName() != null) {
            role = RolesStore.getRoleFor(mc.player.getDisplayName().getString());
            roleColor = RolesStore.getRoleColorFor(mc.player.getDisplayName().getString());
        }
        if (role == null || role.isBlank()) role = "UNKNOWN";

        int ping = -1;
        PlayerListEntry self = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        if (self != null) ping = self.getLatency();

        double tpsNow = TpsTracker.getTpsNow();
        double tps5 = TpsTracker.getTps5m();
        double tps10 = TpsTracker.getTps10m();
        double tps15 = TpsTracker.getTps15m();

        return buildContent(cfg, nick, role, roleColor, ping, tpsNow, tps5, tps10, tps15);
    }

    private static PanelContent buildContent(StaffHelperConfig cfg, String nick, String role, int roleColor, int ping, double tpsNow, double tps5, double tps10, double tps15) {
        if (cfg.statsHorizontal) {
            return new PanelContent(List.of(new LineItem("horizontal", buildHorizontalLine(cfg, nick, role, roleColor, ping, tpsNow, tps5, tps10, tps15))));
        }
        return new PanelContent(buildVerticalLines(cfg, nick, role, roleColor, ping, tpsNow, tps5, tps10, tps15));
    }

    private static MutableText buildHorizontalLine(StaffHelperConfig cfg, String nick, String role, int roleColor, int ping, double tpsNow, double tps5, double tps10, double tps15) {
        MutableText out = Text.literal("");
        String shownNick = (nick == null || nick.isBlank()) ? "Unknown" : nick;
        out.append(Text.literal(shownNick).formatted(Formatting.WHITE));

        if (cfg.statsShowRole) {
            out.append(Text.literal(" | ").formatted(Formatting.DARK_GRAY));
            out.append(Text.literal(role).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(normalizeRoleColor(roleColor)))));
        }

        if (cfg.statsShowPing) {
            out.append(Text.literal(" | ").formatted(Formatting.DARK_GRAY));
            out.append(Text.literal("PING: ").formatted(Formatting.WHITE));
            out.append(Text.literal(ping >= 0 ? (ping + "ms") : "?").formatted(pingColor(ping)));
        }

        if (cfg.statsShowTps) {
            if (cfg.statsShowTpsNow) {
                out.append(Text.literal(" | ").formatted(Formatting.DARK_GRAY));
                out.append(Text.literal("TPS: ").formatted(Formatting.WHITE));
                out.append(Text.literal(fmt1(tpsNow)).formatted(tpsColor(tpsNow)));
            }
            if (cfg.statsShowTps5m) {
                out.append(Text.literal(" | ").formatted(Formatting.DARK_GRAY));
                out.append(Text.literal("TPS 5M: ").formatted(Formatting.WHITE));
                out.append(Text.literal(fmt1(tps5)).formatted(tpsColor(tps5)));
            }
            if (cfg.statsShowTps10m) {
                out.append(Text.literal(" | ").formatted(Formatting.DARK_GRAY));
                out.append(Text.literal("TPS 10M: ").formatted(Formatting.WHITE));
                out.append(Text.literal(fmt1(tps10)).formatted(tpsColor(tps10)));
            }
            if (cfg.statsShowTps15m) {
                out.append(Text.literal(" | ").formatted(Formatting.DARK_GRAY));
                out.append(Text.literal("TPS 15M: ").formatted(Formatting.WHITE));
                out.append(Text.literal(fmt1(tps15)).formatted(tpsColor(tps15)));
            }
        }
        return out;
    }

    private static List<LineItem> buildVerticalLines(StaffHelperConfig cfg, String nick, String role, int roleColor, int ping, double tpsNow, double tps5, double tps10, double tps15) {
        List<LineItem> lines = new ArrayList<>();
        String shownNick = (nick == null || nick.isBlank()) ? "Unknown" : nick;
        MutableText firstLine = Text.literal(shownNick).formatted(Formatting.WHITE);
        if (cfg.statsShowRole) {
            firstLine.append(Text.literal(" | ").formatted(Formatting.DARK_GRAY));
            firstLine.append(Text.literal(role).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(normalizeRoleColor(roleColor)))));
        }
        lines.add(new LineItem("base", firstLine));
        if (cfg.statsShowPing) {
            lines.add(new LineItem("ping", Text.literal("PING: ").formatted(Formatting.WHITE)
                    .append(Text.literal(ping >= 0 ? (ping + "ms") : "?").formatted(pingColor(ping)))));
        }
        if (cfg.statsShowTps) {
            if (cfg.statsShowTpsNow) lines.add(new LineItem("tps_now", Text.literal("TPS: ").formatted(Formatting.WHITE).append(Text.literal(fmt1(tpsNow)).formatted(tpsColor(tpsNow)))));
            if (cfg.statsShowTps5m) lines.add(new LineItem("tps_5m", Text.literal("TPS 5M: ").formatted(Formatting.WHITE).append(Text.literal(fmt1(tps5)).formatted(tpsColor(tps5)))));
            if (cfg.statsShowTps10m) lines.add(new LineItem("tps_10m", Text.literal("TPS 10M: ").formatted(Formatting.WHITE).append(Text.literal(fmt1(tps10)).formatted(tpsColor(tps10)))));
            if (cfg.statsShowTps15m) lines.add(new LineItem("tps_15m", Text.literal("TPS 15M: ").formatted(Formatting.WHITE).append(Text.literal(fmt1(tps15)).formatted(tpsColor(tps15)))));
        }
        return lines;
    }

    private static void syncAnimatedLines(List<LineItem> targetLines) {
        for (AnimatedStatsLine line : animatedLines.values()) {
            line.targetVisible = false;
        }
        int order = 0;
        for (LineItem item : targetLines) {
            AnimatedStatsLine line = animatedLines.computeIfAbsent(item.key(), k -> new AnimatedStatsLine());
            line.text = item.text();
            line.targetVisible = true;
            line.order = order++;
        }
    }

    private static float updateAnimatedLines() {
        float sum = 0.0f;
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, AnimatedStatsLine> entry : animatedLines.entrySet()) {
            AnimatedStatsLine line = entry.getValue();
            float target = line.targetVisible ? 1.0f : 0.0f;
            line.progress = approach(line.progress, target, line.targetVisible ? 0.24f : 0.18f);
            if (!line.targetVisible && line.progress <= 0.01f) {
                toRemove.add(entry.getKey());
                continue;
            }
            sum += line.progress;
        }
        for (String key : toRemove) {
            animatedLines.remove(key);
        }
        return sum;
    }

    private static List<AnimatedStatsLine> sortedAnimatedLines() {
        List<AnimatedStatsLine> out = new ArrayList<>(animatedLines.values());
        out.sort(Comparator.comparingInt(l -> l.order));
        return out;
    }

    private static void drawAnimatedPanel(DrawContext ctx, int x, int y, StaffHelperConfig cfg, boolean clampToScreen) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null || mc.getWindow() == null) return;

        int padX = getPadX(cfg);
        int padY = getPadY(cfg);
        int lineH = mc.textRenderer.fontHeight;
        int radius = 8;
        float panelVisual = easeOutCubic(panelProgress);

        List<AnimatedStatsLine> lines = sortedAnimatedLines();
        float visibleRows = 0.0f;
        for (AnimatedStatsLine line : lines) {
            visibleRows += line.progress;
        }

        int targetW = getAnimatedPanelWidth(mc, cfg, panelVisual);
        int minW = Math.max((padX * 2) + 6, Math.round(44 * clampScale(cfg.statsBoxScale)));
        int w = lerpInt(minW, targetW, panelVisual);
        int contentH = Math.max(1, Math.round(visibleRows * lineH));
        int minContentH = Math.max(1, Math.round(lineH * 0.25f));
        int h = padY + Math.max(minContentH, Math.round(contentH * panelVisual)) + padY;

        if (clampToScreen) {
            int screenW = mc.getWindow().getScaledWidth();
            int screenH = mc.getWindow().getScaledHeight();
            x = Math.max(0, Math.min(x, screenW - w));
            y = Math.max(0, Math.min(y, screenH - h));
        }

        UiChrome.drawPanel(ctx, x, y, w, h, radius, System.currentTimeMillis());

        float yy = y + padY;
        for (AnimatedStatsLine line : lines) {
            float lineVisual = easeOutCubic(line.progress) * panelVisual;
            if (lineVisual <= 0.01f) continue;
            int drawY = Math.round(yy);
            int drawX = x + padX + Math.round((1.0f - lineVisual) * 10.0f);
            int alpha = Math.max(0, Math.min(255, Math.round(255 * lineVisual)));
            ctx.drawText(mc.textRenderer, line.text, drawX, drawY, (alpha << 24) | 0xFFFFFF, false);

            yy += line.progress * lineH;
        }
    }

    private static String fmt1(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static Formatting pingColor(int ping) {
        if (ping < 0) return Formatting.GRAY;
        if (ping <= 70) return Formatting.GREEN;
        if (ping <= 130) return Formatting.YELLOW;
        if (ping <= 200) return Formatting.GOLD;
        return Formatting.RED;
    }

    private static Formatting tpsColor(double tps) {
        if (tps >= 19.5) return Formatting.GREEN;
        if (tps >= 18.0) return Formatting.YELLOW;
        if (tps >= 16.0) return Formatting.GOLD;
        return Formatting.RED;
    }

    private static void drawPanel(DrawContext ctx, int x, int y, PanelContent content, boolean clampToScreen) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null || mc.getWindow() == null) return;
        StaffHelperConfig cfg = StaffHelperState.CONFIG != null ? StaffHelperState.CONFIG : new StaffHelperConfig();
        int padX = getPadX(cfg);
        int padY = getPadY(cfg);
        int lineH = mc.textRenderer.fontHeight;
        int radius = 8;

        int w = getPanelWidth(mc, cfg, content);
        int h = padY + content.lines().size() * lineH + padY;

        if (clampToScreen) {
            int screenW = mc.getWindow().getScaledWidth();
            int screenH = mc.getWindow().getScaledHeight();
            x = Math.max(0, Math.min(x, screenW - w));
            y = Math.max(0, Math.min(y, screenH - h));
        }

        UiChrome.drawPanel(ctx, x, y, w, h, radius, System.currentTimeMillis());

        int yy = y + padY;
        for (LineItem line : content.lines()) {
            ctx.drawText(mc.textRenderer, line.text(), x + padX, yy, 0xFFFFFFFF, false);
            yy += lineH;
        }
    }

    public static int getPreviewWidth(MinecraftClient mc) {
        if (mc == null) mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) return 200;

        StaffHelperConfig cfg = StaffHelperState.CONFIG != null ? StaffHelperState.CONFIG : new StaffHelperConfig();
        PanelContent content = buildContent(cfg, "DontiMonti", "MOD", RolesStore.DEFAULT_ROLE_COLOR, 42, 20.0, 19.9, 19.8, 19.7);
        int maxW = 0;
        for (LineItem line : content.lines()) {
            maxW = Math.max(maxW, mc.textRenderer.getWidth(line.text()));
        }
        return maxW + getPadX(cfg) * 2;
    }

    public static int getPreviewHeight() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int lineH = (mc != null && mc.textRenderer != null) ? mc.textRenderer.fontHeight : 9;
        StaffHelperConfig cfg = StaffHelperState.CONFIG != null ? StaffHelperState.CONFIG : new StaffHelperConfig();
        int padY = getPadY(cfg);
        PanelContent content = buildContent(cfg, "DontiMonti", "MOD", RolesStore.DEFAULT_ROLE_COLOR, 42, 20.0, 19.9, 19.8, 19.7);
        return padY + lineH * content.lines().size() + padY;
    }

    public static void renderPreview(DrawContext ctx, int x, int y) {
        StaffHelperConfig cfg = StaffHelperState.CONFIG != null ? StaffHelperState.CONFIG : new StaffHelperConfig();
        drawPanel(ctx, x, y, buildContent(cfg, "DontiMonti", "MOD", RolesStore.DEFAULT_ROLE_COLOR, 42, 20.0, 19.9, 19.8, 19.7), true);
    }

    private static int getPanelWidth(MinecraftClient mc, StaffHelperConfig cfg, PanelContent content) {
        int maxW = 0;
        for (LineItem line : content.lines()) {
            maxW = Math.max(maxW, mc.textRenderer.getWidth(line.text()));
        }
        return maxW + getPadX(cfg) * 2;
    }

    private static int getAnimatedPanelWidth(MinecraftClient mc, StaffHelperConfig cfg, float openProgress) {
        int maxW = 0;
        float open = Math.max(0.0f, Math.min(1.0f, openProgress));
        for (AnimatedStatsLine line : animatedLines.values()) {
            if (line.progress <= 0.01f) continue;
            int width = mc.textRenderer.getWidth(line.text);
            float lineVisual = easeOutCubic(line.progress) * (0.4f + 0.6f * open);
            int weighted = Math.round(width * (0.25f + 0.75f * lineVisual));
            maxW = Math.max(maxW, weighted);
        }
        if (maxW <= 0) maxW = 40;
        return maxW + getPadX(cfg) * 2;
    }

    private static int getPadX(StaffHelperConfig cfg) {
        return Math.max(4, Math.round(10 * clampScale(cfg.statsBoxScale)));
    }

    private static int getPadY(StaffHelperConfig cfg) {
        return Math.max(3, Math.round(7 * clampScale(cfg.statsBoxScale)));
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

    private static float clampScale(float v) {
        if (Float.isNaN(v)) return 1.0f;
        return Math.max(0.6f, Math.min(2.0f, v));
    }

    private static int normalizeRoleColor(int rgb) {
        if (rgb < 0 || rgb > 0xFFFFFF) return RolesStore.DEFAULT_ROLE_COLOR;
        return rgb;
    }

    private static float approach(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    private record PanelContent(List<LineItem> lines) {}
    private record LineItem(String key, Text text) {}

    private static final class AnimatedStatsLine {
        private Text text = Text.literal("");
        private boolean targetVisible = false;
        private float progress = 0.0f;
        private int order = 0;
    }
}

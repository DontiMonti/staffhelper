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

    private static final Map<String, AnimatedStatsChip> animatedChips = new LinkedHashMap<>();
    private static float panelProgress = 0.0f;

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            joinAnimationStartMs = System.currentTimeMillis();
            animatedChips.clear();
            panelProgress = 0.0f;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            joinAnimationStartMs = 0L;
            animatedChips.clear();
            panelProgress = 0.0f;
        });
        HudRenderCallback.EVENT.register(StatsHudFeature::renderHud);
    }

    private static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!AllowedUsersAccessGate.isModAllowed()) {
            animatedChips.clear();
            panelProgress = 0.0f;
            return;
        }
        StaffHelperConfig cfg = StaffHelperState.CONFIG;
        if (cfg == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) return;

        ChipContent target = collectTargetContent(mc, cfg);
        syncAnimatedChips(target.chips());
        float visibleRows = updateAnimatedChips();

        float targetPanel = target.chips().isEmpty() ? 0.0f : 1.0f;
        panelProgress = approach(panelProgress, targetPanel, 0.22f);
        if (panelProgress <= 0.01f && visibleRows <= 0.01f) return;

        int targetX = cfg.statsWidgetX;
        int targetY = cfg.statsWidgetY;

        float joinProgress = joinProgress();
        float eased = easeOutCubic(joinProgress);
        int panelW = getAnimatedPanelWidth(mc, cfg);
        int startX = -panelW - 16;
        int drawX = (joinProgress < 1.0f && targetPanel > 0.01f) ? lerpInt(startX, targetX, eased) : targetX;

        drawAnimatedPanel(ctx, drawX, targetY, cfg, joinProgress >= 1.0f || targetPanel <= 0.01f);
    }

    private static ChipContent collectTargetContent(MinecraftClient mc, StaffHelperConfig cfg) {
        if (!cfg.statsEnabled) return new ChipContent(List.of());
        if (mc.player == null || mc.getNetworkHandler() == null) return new ChipContent(List.of());

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

        return new ChipContent(buildChips(cfg, nick, role, roleColor, ping, tpsNow, tps5, tps10, tps15));
    }

    private static List<ChipItem> buildChips(StaffHelperConfig cfg, String nick, String role, int roleColor, int ping, double tpsNow, double tps5, double tps10, double tps15) {
        List<ChipItem> chips = new ArrayList<>();
        String shownNick = (nick == null || nick.isBlank()) ? "Unknown" : nick;

        chips.add(new ChipItem("nick",
                Text.literal("NICK: ").formatted(Formatting.GRAY)
                        .append(Text.literal(shownNick).formatted(Formatting.WHITE))));

        if (cfg.statsShowRole) {
            chips.add(new ChipItem("role",
                    Text.literal("ROLE: ").formatted(Formatting.GRAY)
                            .append(Text.literal(role).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(normalizeRoleColor(roleColor)))))));
        }

        if (cfg.statsShowPing) {
            chips.add(new ChipItem("ping",
                    Text.literal("PING: ").formatted(Formatting.GRAY)
                            .append(Text.literal(ping >= 0 ? (ping + "ms") : "?").formatted(pingColor(ping)))));
        }

        if (cfg.statsShowTps) {
            if (cfg.statsShowTpsNow) {
                chips.add(new ChipItem("tps_now",
                        Text.literal("TPS NOW: ").formatted(Formatting.GRAY)
                                .append(Text.literal(fmt1(tpsNow)).formatted(tpsColor(tpsNow)))));
            }
            if (cfg.statsShowTps5m) {
                chips.add(new ChipItem("tps_5m",
                        Text.literal("TPS 5M: ").formatted(Formatting.GRAY)
                                .append(Text.literal(fmt1(tps5)).formatted(tpsColor(tps5)))));
            }
            if (cfg.statsShowTps10m) {
                chips.add(new ChipItem("tps_10m",
                        Text.literal("TPS 10M: ").formatted(Formatting.GRAY)
                                .append(Text.literal(fmt1(tps10)).formatted(tpsColor(tps10)))));
            }
            if (cfg.statsShowTps15m) {
                chips.add(new ChipItem("tps_15m",
                        Text.literal("TPS 15M: ").formatted(Formatting.GRAY)
                                .append(Text.literal(fmt1(tps15)).formatted(tpsColor(tps15)))));
            }
        }
        return chips;
    }

    private static void syncAnimatedChips(List<ChipItem> targetChips) {
        for (AnimatedStatsChip chip : animatedChips.values()) {
            chip.targetVisible = false;
        }
        int order = 0;
        for (ChipItem item : targetChips) {
            AnimatedStatsChip chip = animatedChips.computeIfAbsent(item.key(), k -> new AnimatedStatsChip());
            chip.text = item.text();
            chip.targetVisible = true;
            chip.order = order++;
        }
    }

    private static float updateAnimatedChips() {
        float sum = 0.0f;
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, AnimatedStatsChip> entry : animatedChips.entrySet()) {
            AnimatedStatsChip chip = entry.getValue();
            float target = chip.targetVisible ? 1.0f : 0.0f;
            chip.progress = approach(chip.progress, target, chip.targetVisible ? 0.24f : 0.18f);
            if (!chip.targetVisible && chip.progress <= 0.01f) {
                toRemove.add(entry.getKey());
                continue;
            }
            sum += chip.progress;
        }
        for (String key : toRemove) {
            animatedChips.remove(key);
        }
        return sum;
    }

    private static List<AnimatedStatsChip> sortedAnimatedChips() {
        List<AnimatedStatsChip> out = new ArrayList<>(animatedChips.values());
        out.sort(Comparator.comparingInt(c -> c.order));
        return out;
    }

    private static void drawAnimatedPanel(DrawContext ctx, int x, int y, StaffHelperConfig cfg, boolean clampToScreen) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null || mc.getWindow() == null) return;

        float panelVisual = easeOutCubic(panelProgress);
        List<AnimatedStatsChip> chips = sortedAnimatedChips();
        List<Text> visibleTexts = collectVisibleChipTexts(chips);

        int targetW = getPanelWidth(mc, cfg, visibleTexts);
        int targetH = getPanelHeight(mc, cfg, visibleTexts);
        int minW = Math.max(Math.round(48 * clampScale(cfg.statsBoxScale)), 40);
        int minH = Math.max(Math.round(22 * clampScale(cfg.statsBoxScale)), 20);
        int w = lerpInt(minW, targetW, panelVisual);
        int h = lerpInt(minH, targetH, panelVisual);

        if (clampToScreen) {
            int screenW = mc.getWindow().getScaledWidth();
            int screenH = mc.getWindow().getScaledHeight();
            x = Math.max(0, Math.min(x, screenW - w));
            y = Math.max(0, Math.min(y, screenH - h));
        }

        drawAnimatedChips(ctx, mc, x, y, w, h, cfg, chips, panelVisual);
    }

    private static void drawAnimatedChips(DrawContext ctx, MinecraftClient mc, int x, int y, int w, int h, StaffHelperConfig cfg, List<AnimatedStatsChip> chips, float panelVisual) {
        if (chips.isEmpty()) return;
        int outerPad = getOuterPad(cfg);
        int chipH = getChipHeight(mc, cfg);
        int gap = getChipGap(cfg);

        int cursorX = x + outerPad;
        int cursorY = y + outerPad;

        int sx1 = x + 1;
        int sy1 = y + 1;
        int sx2 = x + w - 1;
        int sy2 = y + h - 1;
        if (sx2 > sx1 && sy2 > sy1) {
            ctx.enableScissor(sx1, sy1, sx2, sy2);
        }
        try {
            for (AnimatedStatsChip chip : chips) {
                if (chip.progress <= 0.01f) continue;
                float chipVisual = easeOutCubic(chip.progress) * panelVisual;
                if (chipVisual <= 0.01f) continue;

                int chipW = getChipWidth(mc, cfg, chip.text);
                int offset = Math.round((1.0f - chipVisual) * 8.0f);
                int alpha = Math.max(0, Math.min(255, Math.round(255 * chipVisual)));

                int drawX = cfg.statsHorizontal ? cursorX + offset : x + outerPad + offset;
                int drawY = cursorY;
                drawStatChip(ctx, mc, cfg, drawX, drawY, chipW, chipH, chip.text, alpha, chipVisual);

                if (cfg.statsHorizontal) {
                    cursorX += chipW + gap;
                } else {
                    cursorY += chipH + gap;
                }
            }
        } finally {
            if (sx2 > sx1 && sy2 > sy1) {
                ctx.disableScissor();
            }
        }
    }

    private static void drawStatChip(DrawContext ctx, MinecraftClient mc, StaffHelperConfig cfg, int x, int y, int w, int h, Text text, int alpha, float visual) {
        float accentBoost = -0.14f + (0.28f * visual);
        UiChrome.drawPanel(ctx, x, y, w, h, 7, System.currentTimeMillis(), accentBoost, false, false);
        int textY = y + (h - mc.textRenderer.fontHeight) / 2;
        UiChrome.drawText(ctx, mc.textRenderer, text, x + getChipPadX(cfg), textY, UiChrome.mainTextColor(alpha), false);
    }

    private static void drawPanel(DrawContext ctx, int x, int y, ChipContent content, boolean clampToScreen) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null || mc.getWindow() == null) return;
        StaffHelperConfig cfg = StaffHelperState.CONFIG != null ? StaffHelperState.CONFIG : new StaffHelperConfig();

        List<Text> chips = new ArrayList<>();
        for (ChipItem chip : content.chips()) {
            chips.add(chip.text());
        }

        int w = getPanelWidth(mc, cfg, chips);
        int h = getPanelHeight(mc, cfg, chips);

        if (clampToScreen) {
            int screenW = mc.getWindow().getScaledWidth();
            int screenH = mc.getWindow().getScaledHeight();
            x = Math.max(0, Math.min(x, screenW - w));
            y = Math.max(0, Math.min(y, screenH - h));
        }

        drawStaticChips(ctx, mc, x, y, cfg, chips);
    }

    private static void drawStaticChips(DrawContext ctx, MinecraftClient mc, int x, int y, StaffHelperConfig cfg, List<Text> chips) {
        int outerPad = getOuterPad(cfg);
        int chipH = getChipHeight(mc, cfg);
        int gap = getChipGap(cfg);

        int cursorX = x + outerPad;
        int cursorY = y + outerPad;
        for (Text chip : chips) {
            int chipW = getChipWidth(mc, cfg, chip);
            drawStatChip(ctx, mc, cfg, cursorX, cursorY, chipW, chipH, chip, 248, 1.0f);
            if (cfg.statsHorizontal) {
                cursorX += chipW + gap;
            } else {
                cursorY += chipH + gap;
            }
        }
    }

    public static int getPreviewWidth(MinecraftClient mc) {
        if (mc == null) mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) return 240;

        StaffHelperConfig cfg = StaffHelperState.CONFIG != null ? StaffHelperState.CONFIG : new StaffHelperConfig();
        List<ChipItem> chips = buildChips(cfg, "DontiMonti", "MOD", RolesStore.DEFAULT_ROLE_COLOR, 42, 20.0, 19.9, 19.8, 19.7);
        List<Text> texts = new ArrayList<>();
        for (ChipItem chip : chips) texts.add(chip.text());
        return getPanelWidth(mc, cfg, texts);
    }

    public static int getPreviewHeight() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) return 48;
        StaffHelperConfig cfg = StaffHelperState.CONFIG != null ? StaffHelperState.CONFIG : new StaffHelperConfig();
        List<ChipItem> chips = buildChips(cfg, "DontiMonti", "MOD", RolesStore.DEFAULT_ROLE_COLOR, 42, 20.0, 19.9, 19.8, 19.7);
        List<Text> texts = new ArrayList<>();
        for (ChipItem chip : chips) texts.add(chip.text());
        return getPanelHeight(mc, cfg, texts);
    }

    public static void renderPreview(DrawContext ctx, int x, int y) {
        StaffHelperConfig cfg = StaffHelperState.CONFIG != null ? StaffHelperState.CONFIG : new StaffHelperConfig();
        drawPanel(ctx, x, y, new ChipContent(buildChips(cfg, "DontiMonti", "MOD", RolesStore.DEFAULT_ROLE_COLOR, 42, 20.0, 19.9, 19.8, 19.7)), true);
    }

    private static int getAnimatedPanelWidth(MinecraftClient mc, StaffHelperConfig cfg) {
        return getPanelWidth(mc, cfg, collectVisibleChipTexts(sortedAnimatedChips()));
    }

    private static List<Text> collectVisibleChipTexts(List<AnimatedStatsChip> chips) {
        List<Text> out = new ArrayList<>();
        for (AnimatedStatsChip chip : chips) {
            if (chip.progress <= 0.01f) continue;
            out.add(chip.text);
        }
        return out;
    }

    private static int getPanelWidth(MinecraftClient mc, StaffHelperConfig cfg, List<Text> chips) {
        int content = getContentWidth(mc, cfg, chips);
        return content + (getOuterPad(cfg) * 2);
    }

    private static int getPanelHeight(MinecraftClient mc, StaffHelperConfig cfg, List<Text> chips) {
        int content = getContentHeight(mc, cfg, chips);
        return content + (getOuterPad(cfg) * 2);
    }

    private static int getContentWidth(MinecraftClient mc, StaffHelperConfig cfg, List<Text> chips) {
        if (chips == null || chips.isEmpty()) return Math.max(18, Math.round(26 * clampScale(cfg.statsBoxScale)));
        int gap = getChipGap(cfg);
        if (cfg.statsHorizontal) {
            int width = 0;
            for (int i = 0; i < chips.size(); i++) {
                width += getChipWidth(mc, cfg, chips.get(i));
                if (i < chips.size() - 1) width += gap;
            }
            return width;
        }

        int max = 0;
        for (Text chip : chips) {
            max = Math.max(max, getChipWidth(mc, cfg, chip));
        }
        return max;
    }

    private static int getContentHeight(MinecraftClient mc, StaffHelperConfig cfg, List<Text> chips) {
        int chipH = getChipHeight(mc, cfg);
        if (chips == null || chips.isEmpty()) return chipH;
        int gap = getChipGap(cfg);
        if (cfg.statsHorizontal) {
            return chipH;
        }
        return (chips.size() * chipH) + ((chips.size() - 1) * gap);
    }

    private static int getChipWidth(MinecraftClient mc, StaffHelperConfig cfg, Text text) {
        int textW = mc.textRenderer.getWidth(UiChrome.uiText(text));
        return textW + (getChipPadX(cfg) * 2);
    }

    private static int getChipHeight(MinecraftClient mc, StaffHelperConfig cfg) {
        return mc.textRenderer.fontHeight + (getChipPadY(cfg) * 2);
    }

    private static int getOuterPad(StaffHelperConfig cfg) {
        return 0;
    }

    private static int getChipPadX(StaffHelperConfig cfg) {
        return Math.max(5, Math.round(8 * clampScale(cfg.statsBoxScale)));
    }

    private static int getChipPadY(StaffHelperConfig cfg) {
        return Math.max(2, Math.round(4 * clampScale(cfg.statsBoxScale)));
    }

    private static int getChipGap(StaffHelperConfig cfg) {
        return Math.max(4, Math.round(6 * clampScale(cfg.statsBoxScale)));
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

    private record ChipContent(List<ChipItem> chips) {}
    private record ChipItem(String key, Text text) {}

    private static final class AnimatedStatsChip {
        private Text text = Text.literal("");
        private boolean targetVisible = false;
        private float progress = 0.0f;
        private int order = 0;
    }
}

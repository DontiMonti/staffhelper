package com.dmsh.staffhelper.feature;

import com.dmsh.staffhelper.StaffHelperState;
import com.dmsh.staffhelper.config.StaffHelperConfig;
import com.dmsh.staffhelper.gui.util.MinimalIconRenderer;
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
        int mainRgb = UiChrome.mainTextColor(255) & 0x00FFFFFF;
        int mutedRgb = UiChrome.mutedTextColor(255) & 0x00FFFFFF;
        int accentRgb = UiChrome.accentColor(255) & 0x00FFFFFF;
        int cleanRoleColor = normalizeRoleColor(roleColor);

        chips.add(new ChipItem(
                "nick",
                MinimalIconRenderer.Glyph.PROFILE,
                Text.literal(shownNick),
                mainRgb,
                accentRgb
        ));

        if (cfg.statsShowRole) {
            chips.add(new ChipItem(
                    "role",
                    MinimalIconRenderer.Glyph.TAG,
                    Text.literal(role).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(cleanRoleColor))),
                    mainRgb,
                    cleanRoleColor
            ));
        }

        if (cfg.statsShowPing) {
            int pingRgb = pingColorRgb(ping);
            chips.add(new ChipItem(
                    "ping",
                    MinimalIconRenderer.Glyph.SIGNAL,
                    Text.literal(ping >= 0 ? (ping + " ms") : "?").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(pingRgb))),
                    mutedRgb,
                    pingRgb
            ));
        }

        if (cfg.statsShowTps) {
            if (cfg.statsShowTpsNow) {
                int tpsRgb = tpsColorRgb(tpsNow);
                chips.add(new ChipItem(
                        "tps_now",
                        MinimalIconRenderer.Glyph.TPS,
                        Text.literal(fmt1(tpsNow)).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(tpsRgb)))
                                .append(Text.literal(" now").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(mutedRgb)))),
                        mutedRgb,
                        tpsRgb
                ));
            }
            if (cfg.statsShowTps5m) {
                int tpsRgb = tpsColorRgb(tps5);
                chips.add(new ChipItem(
                        "tps_5m",
                        MinimalIconRenderer.Glyph.TPS,
                        Text.literal(fmt1(tps5)).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(tpsRgb)))
                                .append(Text.literal(" 5m").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(mutedRgb)))),
                        mutedRgb,
                        tpsRgb
                ));
            }
            if (cfg.statsShowTps10m) {
                int tpsRgb = tpsColorRgb(tps10);
                chips.add(new ChipItem(
                        "tps_10m",
                        MinimalIconRenderer.Glyph.TPS,
                        Text.literal(fmt1(tps10)).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(tpsRgb)))
                                .append(Text.literal(" 10m").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(mutedRgb)))),
                        mutedRgb,
                        tpsRgb
                ));
            }
            if (cfg.statsShowTps15m) {
                int tpsRgb = tpsColorRgb(tps15);
                chips.add(new ChipItem(
                        "tps_15m",
                        MinimalIconRenderer.Glyph.TPS,
                        Text.literal(fmt1(tps15)).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(tpsRgb)))
                                .append(Text.literal(" 15m").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(mutedRgb)))),
                        mutedRgb,
                        tpsRgb
                ));
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
            chip.item = item;
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
        List<ChipItem> visibleChips = collectVisibleChipItems(chips);

        int targetW = getPanelWidth(mc, cfg, visibleChips);
        int targetH = getPanelHeight(mc, cfg, visibleChips);
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
        for (AnimatedStatsChip chip : chips) {
            if (chip.progress <= 0.01f) continue;
            float chipVisual = easeOutCubic(chip.progress) * panelVisual;
            if (chipVisual <= 0.01f) continue;

            int chipW = getChipWidth(mc, cfg, chip.item);
            int offset = Math.round((1.0f - chipVisual) * 8.0f);
            int alpha = Math.max(0, Math.min(255, Math.round(255 * chipVisual)));

            int drawX = cfg.statsHorizontal ? cursorX + offset : x + outerPad + offset;
            int drawY = cursorY;
            drawStatChip(ctx, mc, cfg, drawX, drawY, chipW, chipH, chip.item, alpha, chipVisual);

            if (cfg.statsHorizontal) {
                cursorX += chipW + gap;
            } else {
                cursorY += chipH + gap;
            }
        }
    }

    private static void drawStatChip(DrawContext ctx, MinecraftClient mc, StaffHelperConfig cfg, int x, int y, int w, int h, ChipItem chip, int alpha, float visual) {
        float accentBoost = -0.14f + (0.28f * visual);
        UiChrome.drawPanel(ctx, x, y, w, h, 7, System.currentTimeMillis(), accentBoost, false);
        int padX = getChipPadX(cfg);
        int padY = getChipPadY(cfg);
        int iconSize = getChipIconSize(cfg);
        int contentHeight = getChipContentHeight(mc, cfg);
        int iconY = y + padY + Math.max(0, (contentHeight - iconSize) / 2);
        int textX = x + padX + iconSize + getChipIconGap(cfg);
        int textY = y + padY + Math.max(0, (contentHeight - mc.textRenderer.fontHeight) / 2);

        MinimalIconRenderer.draw(ctx, chip.icon(), x + padX, iconY, iconSize, withAlpha(chip.iconColor(), alpha), withAlpha(chip.accentColor(), alpha));
        UiChrome.drawText(ctx, mc.textRenderer, chip.text(), textX, textY, UiChrome.mainTextColor(alpha), false);
    }

    private static void drawPanel(DrawContext ctx, int x, int y, ChipContent content, boolean clampToScreen) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null || mc.getWindow() == null) return;
        StaffHelperConfig cfg = StaffHelperState.CONFIG != null ? StaffHelperState.CONFIG : new StaffHelperConfig();

        int w = getPanelWidth(mc, cfg, content.chips());
        int h = getPanelHeight(mc, cfg, content.chips());

        if (clampToScreen) {
            int screenW = mc.getWindow().getScaledWidth();
            int screenH = mc.getWindow().getScaledHeight();
            x = Math.max(0, Math.min(x, screenW - w));
            y = Math.max(0, Math.min(y, screenH - h));
        }

        drawStaticChips(ctx, mc, x, y, cfg, content.chips());
    }

    private static void drawStaticChips(DrawContext ctx, MinecraftClient mc, int x, int y, StaffHelperConfig cfg, List<ChipItem> chips) {
        int outerPad = getOuterPad(cfg);
        int chipH = getChipHeight(mc, cfg);
        int gap = getChipGap(cfg);

        int cursorX = x + outerPad;
        int cursorY = y + outerPad;
        for (ChipItem chip : chips) {
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
        return getPanelWidth(mc, cfg, chips);
    }

    public static int getPreviewHeight() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) return 48;
        StaffHelperConfig cfg = StaffHelperState.CONFIG != null ? StaffHelperState.CONFIG : new StaffHelperConfig();
        List<ChipItem> chips = buildChips(cfg, "DontiMonti", "MOD", RolesStore.DEFAULT_ROLE_COLOR, 42, 20.0, 19.9, 19.8, 19.7);
        return getPanelHeight(mc, cfg, chips);
    }

    public static void renderPreview(DrawContext ctx, int x, int y) {
        StaffHelperConfig cfg = StaffHelperState.CONFIG != null ? StaffHelperState.CONFIG : new StaffHelperConfig();
        drawPanel(ctx, x, y, new ChipContent(buildChips(cfg, "DontiMonti", "MOD", RolesStore.DEFAULT_ROLE_COLOR, 42, 20.0, 19.9, 19.8, 19.7)), true);
    }

    private static int getAnimatedPanelWidth(MinecraftClient mc, StaffHelperConfig cfg) {
        return getPanelWidth(mc, cfg, collectVisibleChipItems(sortedAnimatedChips()));
    }

    private static List<ChipItem> collectVisibleChipItems(List<AnimatedStatsChip> chips) {
        List<ChipItem> out = new ArrayList<>();
        for (AnimatedStatsChip chip : chips) {
            if (chip.progress <= 0.01f) continue;
            out.add(chip.item);
        }
        return out;
    }

    private static int getPanelWidth(MinecraftClient mc, StaffHelperConfig cfg, List<ChipItem> chips) {
        int content = getContentWidth(mc, cfg, chips);
        return content + (getOuterPad(cfg) * 2);
    }

    private static int getPanelHeight(MinecraftClient mc, StaffHelperConfig cfg, List<ChipItem> chips) {
        int content = getContentHeight(mc, cfg, chips);
        return content + (getOuterPad(cfg) * 2);
    }

    private static int getContentWidth(MinecraftClient mc, StaffHelperConfig cfg, List<ChipItem> chips) {
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
        for (ChipItem chip : chips) {
            max = Math.max(max, getChipWidth(mc, cfg, chip));
        }
        return max;
    }

    private static int getContentHeight(MinecraftClient mc, StaffHelperConfig cfg, List<ChipItem> chips) {
        int chipH = getChipHeight(mc, cfg);
        if (chips == null || chips.isEmpty()) return chipH;
        int gap = getChipGap(cfg);
        if (cfg.statsHorizontal) {
            return chipH;
        }
        return (chips.size() * chipH) + ((chips.size() - 1) * gap);
    }

    private static int getChipWidth(MinecraftClient mc, StaffHelperConfig cfg, ChipItem chip) {
        int textW = mc.textRenderer.getWidth(UiChrome.uiText(chip.text()));
        return textW + (getChipPadX(cfg) * 2) + getChipIconSize(cfg) + getChipIconGap(cfg);
    }

    private static int getChipHeight(MinecraftClient mc, StaffHelperConfig cfg) {
        return getChipContentHeight(mc, cfg) + (getChipPadY(cfg) * 2);
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

    private static int getChipIconSize(StaffHelperConfig cfg) {
        return Math.max(11, Math.round(12 * clampScale(cfg.statsBoxScale)));
    }

    private static int getChipIconGap(StaffHelperConfig cfg) {
        return Math.max(5, Math.round(7 * clampScale(cfg.statsBoxScale)));
    }

    private static int getChipContentHeight(MinecraftClient mc, StaffHelperConfig cfg) {
        return Math.max(mc.textRenderer.fontHeight, getChipIconSize(cfg));
    }

    private static int getChipGap(StaffHelperConfig cfg) {
        return Math.max(4, Math.round(6 * clampScale(cfg.statsBoxScale)));
    }

    private static String fmt1(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static int pingColorRgb(int ping) {
        if (ping < 0) return 0x9A9AA4;
        if (ping <= 70) return 0x7FE39C;
        if (ping <= 130) return 0xF2D66B;
        if (ping <= 200) return 0xF1A95A;
        return 0xF07A7A;
    }

    private static int tpsColorRgb(double tps) {
        if (tps >= 19.5) return 0x7FE39C;
        if (tps >= 18.0) return 0xF2D66B;
        if (tps >= 16.0) return 0xF1A95A;
        return 0xF07A7A;
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

    private static int withAlpha(int rgb, int alpha) {
        int cleanAlpha = Math.max(0, Math.min(255, alpha));
        return (cleanAlpha << 24) | (rgb & 0x00FFFFFF);
    }

    private static float approach(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    private record ChipContent(List<ChipItem> chips) {}
    private record ChipItem(String key, MinimalIconRenderer.Glyph icon, Text text, int iconColor, int accentColor) {}

    private static final class AnimatedStatsChip {
        private ChipItem item = new ChipItem("nick", MinimalIconRenderer.Glyph.PROFILE, Text.literal(""), 0xD9D9E2, 0xD9D9E2);
        private boolean targetVisible = false;
        private float progress = 0.0f;
        private int order = 0;
    }
}

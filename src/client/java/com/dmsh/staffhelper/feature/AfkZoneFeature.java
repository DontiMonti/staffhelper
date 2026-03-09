package com.dmsh.staffhelper.feature;

import com.dmsh.staffhelper.StaffHelperState;
import com.dmsh.staffhelper.gui.util.UiChrome;
import com.dmsh.staffhelper.util.AllowedUsersAccessGate;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AfkZoneFeature {
    private AfkZoneFeature() {}

    private static final Map<String, Set<String>> altsByMain = new HashMap<>();

    private static final Map<String, String> banInfoByNick = new HashMap<>();

    private static final Deque<String> altsCheckQueue = new ArrayDeque<>();
    private static boolean altsCheckRunning = false;
    private static long nextAltsCommandAtMs = 0L;

    private static final long ALTS_CHECK_DELAY_MS = 900L;

    private static long showWidgetUntilMs = 0L;
    private static final Map<String, AnimatedHudRow> animatedHudRows = new LinkedHashMap<>();
    private static float animatedHudPanelProgress = 0.0f;

    private static final LinkedHashMap<String, ViolationEntry> violations = new LinkedHashMap<>();

    private static final Set<String> onlineInZone = new HashSet<>();

    private static String currentScanNick = null;
    private static final Set<String> currentScanAccounts = new LinkedHashSet<>();
    private static long lastScanLineMs = 0L;

    private static String pendingBanNick = null;
    private static String pendingBanId = null;
    private static long pendingBanUntilMs = 0L;

    private static final Pattern P_SCAN_MAIN = Pattern.compile("(?iu)^\\s*сканирование\\s+([A-Za-z0-9_]{3,16})\\b");
    private static final Pattern P_HISTORY_LINE = Pattern.compile("(?iu)^\\s*история\\s+игрока\\b");
    private static final Pattern P_ANY_NICK  = Pattern.compile("@?([A-Za-z0-9_]{3,16})");
    private static final Pattern P_SINGLE_NICK_LINE = Pattern.compile("^\\s*@?([A-Za-z0-9_]{3,16})\\s*$");
    private static final Pattern P_FORMAT_CODE = Pattern.compile("(?i)§[0-9A-FK-ORX]");

    private static final Pattern P_BAN_LINE =
            Pattern.compile("(?iu)заблокировал(?:\\s+чат)?\\s+([A-Za-z0-9_]{3,16})\\s*\\(\\s*[^0-9]*\\s*(\\d+)\\s*\\)");

    private static final Pattern P_REASON_LINE =
            Pattern.compile("(?iu)причина\\s*:\\s*([^\\r\\n]+)");

    public static void init() {
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, receptionTimestamp) ->
                onChatMessage(message.getString())
        );
        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
                onChatMessage(message.getString())
        );

        WorldRenderEvents.AFTER_ENTITIES.register(context ->
                renderZone(context.matrixStack(), context.camera().getPos(), context.consumers())
        );

        HudRenderCallback.EVENT.register(AfkZoneFeature::renderHud);
        ClientTickEvents.END_CLIENT_TICK.register(AfkZoneFeature::tickUpdate);
    }

    public static void copyToClipboardAndClear() {
        if (!AllowedUsersAccessGate.isModAllowed()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;

        LinkedHashSet<String> rows = collectHudRows();
        StringBuilder out = new StringBuilder();
        for (String row : rows) {
            out.append(row).append("\n");
        }
        mc.keyboard.setClipboard(out.toString().trim());

        clearAllTrackingState();
    }

    public static void startAltsCheck() {
        if (!AllowedUsersAccessGate.isModAllowed()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null) return;
        if (StaffHelperState.CONFIG == null) return;
        if (!StaffHelperState.CONFIG.afkZoneEnabled) return;

        showWidgetUntilMs = System.currentTimeMillis() + 1500L;

        String self = mc.player.getGameProfile().getName();
        BlockPos selfPos = mc.player.getBlockPos();
        boolean selfInZone = isInsideZone(selfPos) || isInsideZone(selfPos.down());

        LinkedHashSet<String> targets = new LinkedHashSet<>();
        mc.world.getPlayers().forEach(p -> {
            String name = p.getGameProfile().getName();
            if (name == null) return;
            if (isIgnoredNick(name)) return;

            BlockPos bp = p.getBlockPos();
            if (!(isInsideZone(bp) || isInsideZone(bp.down()))) return;

            if (name.equalsIgnoreCase(self)) return;

            targets.add(name);
        });

        if (targets.isEmpty()) {

            altsCheckQueue.clear();
            altsCheckRunning = false;
            nextAltsCommandAtMs = 0L;
            return;
        }

        altsCheckQueue.clear();
        altsCheckQueue.addAll(targets);
        altsCheckRunning = true;
        nextAltsCommandAtMs = 0L;
    }

    private static void tickAltsCheck(MinecraftClient client) {
        if (!altsCheckRunning) return;

        if (StaffHelperState.CONFIG == null || !StaffHelperState.CONFIG.afkZoneEnabled) {

            altsCheckQueue.clear();
            altsCheckRunning = false;
            nextAltsCommandAtMs = 0L;
            return;
        }

        if (client == null || client.player == null || client.world == null) {
            altsCheckQueue.clear();
            altsCheckRunning = false;
            nextAltsCommandAtMs = 0L;
            return;
        }

        long now = System.currentTimeMillis();

        if (altsCheckQueue.isEmpty()) {
            if (currentScanNick == null) {
                altsCheckRunning = false;
                nextAltsCommandAtMs = 0L;
            }
            return;
        }

        if (currentScanNick != null) {
            if ((now - lastScanLineMs) > 1200L) {

                finalizeScanIfAny();
            } else {
                return;
            }
        }

        if (nextAltsCommandAtMs == 0L) nextAltsCommandAtMs = now;
        if (now < nextAltsCommandAtMs) return;

        String target = altsCheckQueue.pollFirst();
        if (target == null || target.isBlank()) {
            nextAltsCommandAtMs = now + ALTS_CHECK_DELAY_MS;
            return;
        }

        try {
            client.player.networkHandler.sendChatCommand("alts " + target);
        } catch (Throwable t) {

            client.player.networkHandler.sendChatMessage("/alts " + target);
        }

        nextAltsCommandAtMs = now + ALTS_CHECK_DELAY_MS;
    }

    private static void clearAllTrackingState() {

        violations.clear();
        altsByMain.clear();
        banInfoByNick.clear();
        onlineInZone.clear();

        currentScanNick = null;
        currentScanAccounts.clear();
        lastScanLineMs = 0L;

        pendingBanNick = null;
        pendingBanId = null;
        pendingBanUntilMs = 0L;

        altsCheckQueue.clear();
        altsCheckRunning = false;
        nextAltsCommandAtMs = 0L;
        showWidgetUntilMs = 0L;
        animatedHudRows.clear();
        animatedHudPanelProgress = 0.0f;
    }

    public static void clearRuntimeState() {
        clearAllTrackingState();
    }

    private static boolean isIgnoredNick(String nick) {
        if (nick == null || nick.isBlank()) return false;
        if (StaffHelperState.CONFIG == null || StaffHelperState.CONFIG.afkIgnoreNicks == null) return false;
        for (String s : StaffHelperState.CONFIG.afkIgnoreNicks) {
            if (s != null && s.equalsIgnoreCase(nick)) return true;
        }
        return false;
    }

    private static String stripFormattingCodes(String text) {
        if (text == null || text.isEmpty()) return "";
        String normalized = text.replace("В§", "§");
        return P_FORMAT_CODE.matcher(normalized).replaceAll("");
    }

    private static boolean isNickInZoneNow(String nick) {
        if (nick == null || nick.isBlank()) return false;
        for (String onlineNick : onlineInZone) {
            if (onlineNick != null && onlineNick.equalsIgnoreCase(nick)) return true;
        }
        return false;
    }

    private static void tickUpdate(MinecraftClient client) {
        if (!AllowedUsersAccessGate.isModAllowed()) {
            clearAllTrackingState();
            return;
        }
        if (StaffHelperState.CONFIG == null) return;

        tickAltsCheck(client);

        if (currentScanNick != null && (System.currentTimeMillis() - lastScanLineMs) > 1200L) {
            finalizeScanIfAny();
        }

        if (!StaffHelperState.CONFIG.afkZoneEnabled) {
            onlineInZone.clear();
            return;
        }
        if (client.world == null) return;

        onlineInZone.clear();
        client.world.getPlayers().forEach(p -> {
            BlockPos bp = p.getBlockPos();
            String name = p.getGameProfile().getName();
            if (name == null) return;
            if (isIgnoredNick(name)) return;
            if (isInsideZone(bp) || isInsideZone(bp.down())) {
                onlineInZone.add(name);
            }
        });

        for (Map.Entry<String, Set<String>> e : altsByMain.entrySet()) {
            String main = e.getKey();
            Set<String> allRaw = e.getValue();

            LinkedHashSet<String> all = new LinkedHashSet<>();
            for (String n : allRaw) {
                if (n == null) continue;
                if (isIgnoredNick(n)) continue;
                all.add(n);
            }

            int countInZone = 0;
            for (String n : all) if (isNickInZoneNow(n)) countInZone++;

            ViolationEntry ve = violations.get(main);

            if (ve == null) {
                if (countInZone >= 2) {
                    ve = new ViolationEntry(main);
                    violations.put(main, ve);
                } else {
                    continue;
                }
            }

            ve.allAccounts.clear();
            ve.allAccounts.addAll(all);

            ve.inZoneNow.clear();
            for (String n : all) if (isNickInZoneNow(n)) ve.inZoneNow.add(n);

            ve.lastUpdateMs = System.currentTimeMillis();
        }
    }

    private static boolean isInsideZone(BlockPos pos) {
        int x1 = StaffHelperState.CONFIG.afkX1, y1 = StaffHelperState.CONFIG.afkY1, z1 = StaffHelperState.CONFIG.afkZ1;
        int x2 = StaffHelperState.CONFIG.afkX2, y2 = StaffHelperState.CONFIG.afkY2, z2 = StaffHelperState.CONFIG.afkZ2;

        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);

        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    private static void onChatMessage(String msg) {
        if (!AllowedUsersAccessGate.isModAllowed()) return;
        if (msg == null || msg.isBlank()) return;

        String cleanMsg = stripFormattingCodes(msg);

        cleanMsg = cleanMsg.replace("\\n", "\n");

        String[] parts = cleanMsg.split("\\R");
        for (String p : parts) {
            String line = p.trim();
            if (!line.isEmpty()) onChatLine(line);
        }
    }

    private static void onChatLine(String clean) {
        long now = System.currentTimeMillis();

        Matcher mb = P_BAN_LINE.matcher(clean);
        if (mb.find()) {
            pendingBanNick = mb.group(1);
            pendingBanId = mb.group(2);
            pendingBanUntilMs = now + 5000L;
            return;
        }

        if (pendingBanNick != null && pendingBanId != null) {
            if (now > pendingBanUntilMs) {
                pendingBanNick = null;
                pendingBanId = null;
                pendingBanUntilMs = 0L;
            } else {
                Matcher mr = P_REASON_LINE.matcher(clean);
                if (mr.find()) {
                    String reason = mr.group(1).trim();

                    String nick = pendingBanNick;
                    String id = pendingBanId;

                    banInfoByNick.put(nick, reason + " | " + id);

                    System.out.println("[ElytraStaffHelper] BAN PARSED: " + nick + " -> " + reason + " | " + id);

                    pendingBanNick = null;
                    pendingBanId = null;
                    pendingBanUntilMs = 0L;
                    return;
                }
            }
        }

        Matcher start = P_SCAN_MAIN.matcher(clean);
        if (start.find()) {
            finalizeScanIfAny();

            currentScanNick = start.group(1);
            currentScanAccounts.clear();
            currentScanAccounts.add(currentScanNick);

            lastScanLineMs = now;
            return;
        }

        if (currentScanNick != null) {

            if (P_HISTORY_LINE.matcher(clean).find()) {
                finalizeScanIfAny();
                return;
            }

            Matcher singleNickMatcher = P_SINGLE_NICK_LINE.matcher(clean);
            boolean singleNickOnly = singleNickMatcher.matches();
            boolean looksLikeAltsLine = clean.contains(currentScanNick) || clean.contains(",") || singleNickOnly;
            if (!looksLikeAltsLine) return;

            if (singleNickOnly) {
                String nick = singleNickMatcher.group(1);
                if (nick != null && !isIgnoredNick(nick)) {
                    currentScanAccounts.add(nick);
                    lastScanLineMs = now;
                }
                finalizeScanIfAny();
                return;
            }

            Matcher m = P_ANY_NICK.matcher(clean);
            boolean any = false;
            while (m.find()) {
                String nick = m.group(1);
                if (nick != null && !isIgnoredNick(nick)) {
                    currentScanAccounts.add(nick);
                    any = true;
                }
            }
            if (any) lastScanLineMs = now;

            if (any && clean.contains(",")) {
                finalizeScanIfAny();
            }
        }
    }

    private static void finalizeScanIfAny() {
        if (currentScanNick == null) return;
        if (!currentScanAccounts.isEmpty()) {
            LinkedHashSet<String> filtered = new LinkedHashSet<>();
            for (String n : currentScanAccounts) {
                if (n == null) continue;
                if (isIgnoredNick(n)) continue;
                filtered.add(n);
            }
            if (!filtered.isEmpty()) {
                altsByMain.put(currentScanNick, filtered);
            }
        }
        currentScanNick = null;
        currentScanAccounts.clear();
        lastScanLineMs = 0L;
    }

    private static LinkedHashSet<String> collectHudRows() {
        LinkedHashSet<String> rows = new LinkedHashSet<>();

        for (ViolationEntry ve : violations.values()) {
            for (String nick : ve.allAccounts) {
                if (nick == null) continue;
                if (isIgnoredNick(nick)) continue;

                boolean inZone = ve.inZoneNow.contains(nick);
                String ban = banInfoByNick.get(nick);

                if (!inZone && ban == null) continue;

                rows.add(ban == null ? nick : (nick + " " + ban));
            }
        }

        return rows;
    }

    private static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!AllowedUsersAccessGate.isModAllowed()) return;
        if (StaffHelperState.CONFIG == null) return;
        long now = System.currentTimeMillis();
        boolean forceShow = altsCheckRunning || now < showWidgetUntilMs;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) return;

        LinkedHashSet<String> rows = collectHudRows();
        int x = StaffHelperState.CONFIG.afkListX;
        int y = StaffHelperState.CONFIG.afkListY;

        Text title = Text.literal("Multi-account in zone:");
        if (altsCheckRunning) {
            title = Text.literal("⌛ ").formatted(Formatting.GREEN)
                    .append(Text.literal("Multi-account in zone:").formatted(Formatting.WHITE));
        }

        List<String> lines;
        if (rows.isEmpty() && forceShow) {
            lines = List.of("...");
        } else {
            lines = new ArrayList<>(rows);
        }

        syncAnimatedHudRows(lines);
        float visibleRows = updateAnimatedHudRows();
        float targetPanel = lines.isEmpty() ? 0.0f : 1.0f;
        animatedHudPanelProgress = approach(animatedHudPanelProgress, targetPanel, 0.22f);

        if (animatedHudPanelProgress <= 0.01f && visibleRows <= 0.01f) return;
        drawHudBox(ctx, mc, x, y, title);
    }

    private static void drawHudBox(DrawContext ctx, MinecraftClient mc, int x, int y, Text title) {
        float scale = getAfkBoxScale();
        int pad = Math.max(4, Math.round(6 * scale));
        int lineH = Math.max(10, Math.round(10 * scale));
        int titleH = Math.max(12, Math.round(12 * scale));
        float panelVisual = easeOutCubic(animatedHudPanelProgress);

        int w = mc.textRenderer.getWidth(title);
        float visibleRows = 0.0f;
        List<AnimatedHudRow> sorted = sortedAnimatedHudRows();
        for (AnimatedHudRow row : sorted) {
            if (row.progress <= 0.01f) continue;
            visibleRows += row.progress;
            float rowVisual = easeOutCubic(row.progress);
            int weightedWidth = Math.round(mc.textRenderer.getWidth(row.text) * (0.35f + 0.65f * rowVisual));
            w = Math.max(w, weightedWidth);
        }
        w += pad * 2;

        int h = pad + Math.max(1, Math.round(titleH * animatedHudPanelProgress)) + Math.max(1, Math.round(visibleRows * lineH)) + pad;

        UiChrome.drawPanel(ctx, x, y, w, h, 8, System.currentTimeMillis());

        int titleAlpha = Math.max(0, Math.min(255, Math.round(255 * panelVisual)));
        ctx.drawText(mc.textRenderer, title, x + pad, y + pad, (titleAlpha << 24) | 0xFFFFFF, true);

        float yy = y + pad + titleH;
        for (AnimatedHudRow row : sorted) {
            if (row.progress <= 0.01f) continue;
            float rowVisual = easeOutCubic(row.progress) * panelVisual;
            int alpha = Math.max(0, Math.min(255, Math.round(190 * rowVisual)));
            int lineX = x + pad + Math.round((1.0f - rowVisual) * 8.0f);
            ctx.drawText(mc.textRenderer, Text.literal(row.text), lineX, Math.round(yy), (alpha << 24) | 0xBEBEBE, true);
            yy += row.progress * lineH;
        }
    }

    private static void syncAnimatedHudRows(List<String> lines) {
        for (AnimatedHudRow row : animatedHudRows.values()) {
            row.targetVisible = false;
        }
        int order = 0;
        int maxLines = 10;
        for (String line : lines) {
            if (line == null || line.isBlank()) continue;
            if (order >= maxLines) break;
            AnimatedHudRow row = animatedHudRows.computeIfAbsent(line, k -> new AnimatedHudRow());
            row.text = line;
            row.targetVisible = true;
            row.order = order++;
        }
    }

    private static float updateAnimatedHudRows() {
        float sum = 0.0f;
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, AnimatedHudRow> entry : animatedHudRows.entrySet()) {
            AnimatedHudRow row = entry.getValue();
            float target = row.targetVisible ? 1.0f : 0.0f;
            row.progress = approach(row.progress, target, row.targetVisible ? 0.24f : 0.18f);
            if (!row.targetVisible && row.progress <= 0.01f) {
                toRemove.add(entry.getKey());
                continue;
            }
            sum += row.progress;
        }
        for (String key : toRemove) {
            animatedHudRows.remove(key);
        }
        return sum;
    }

    private static List<AnimatedHudRow> sortedAnimatedHudRows() {
        List<AnimatedHudRow> out = new ArrayList<>(animatedHudRows.values());
        out.sort(Comparator.comparingInt(r -> r.order));
        return out;
    }

    private static float approach(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    private static float easeOutCubic(float t) {
        float clamped = Math.max(0.0f, Math.min(1.0f, t));
        float inv = 1.0f - clamped;
        return 1.0f - (inv * inv * inv);
    }

    private static float getAfkBoxScale() {
        if (StaffHelperState.CONFIG == null) return 1.0f;
        float v = StaffHelperState.CONFIG.afkBoxScale;
        if (Float.isNaN(v)) return 1.0f;
        return Math.max(0.6f, Math.min(2.0f, v));
    }

    private static void renderZone(MatrixStack matrices, Vec3d cameraPos, VertexConsumerProvider consumers) {
        if (!AllowedUsersAccessGate.isModAllowed()) return;
        if (StaffHelperState.CONFIG == null) return;
        if (!StaffHelperState.CONFIG.afkZoneEnabled) return;

        boolean outline = StaffHelperState.CONFIG.afkOutlineEnabled;

        boolean fillEnabled = StaffHelperState.CONFIG.afkFillEnabled && outline;

        if (!outline && !fillEnabled) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return;

        int x1 = StaffHelperState.CONFIG.afkX1, y1 = StaffHelperState.CONFIG.afkY1, z1 = StaffHelperState.CONFIG.afkZ1;
        int x2 = StaffHelperState.CONFIG.afkX2, y2 = StaffHelperState.CONFIG.afkY2, z2 = StaffHelperState.CONFIG.afkZ2;

        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);

        Box boxWorld = new Box(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);

        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        VertexConsumerProvider.Immediate immediate =
                (consumers instanceof VertexConsumerProvider.Immediate imm)
                        ? imm
                        : mc.getBufferBuilders().getEntityVertexConsumers();

        if (outline) {
            VertexConsumer lines = immediate.getBuffer(RenderLayer.getLines());
            drawOutlinedBox(matrices, lines, boxWorld, 0.0f, 1.0f, 0.2f, 1.0f);
        }

        if (fillEnabled) {
            VertexConsumer fillVc = immediate.getBuffer(RenderLayer.getDebugFilledBox());
            drawFilledBox(matrices, fillVc, boxWorld, 0.0f, 1.0f, 0.2f, 0.18f);
        }

        immediate.draw();
        matrices.pop();
    }

    private static final class ViolationEntry {
        final String mainNick;
        final Set<String> allAccounts = new LinkedHashSet<>();
        final Set<String> inZoneNow = new LinkedHashSet<>();
        long lastUpdateMs = 0;

        ViolationEntry(String mainNick) { this.mainNick = mainNick; }
    }

    private static final class AnimatedHudRow {
        String text = "";
        boolean targetVisible = false;
        float progress = 0.0f;
        int order = 0;
    }

    private static void drawOutlinedBox(MatrixStack matrices, VertexConsumer vc, Box b,
                                        float r, float g, float bl, float a) {
        float x1 = (float) b.minX, y1 = (float) b.minY, z1 = (float) b.minZ;
        float x2 = (float) b.maxX, y2 = (float) b.maxY, z2 = (float) b.maxZ;

        line(matrices, vc, x1, y1, z1, x2, y1, z1, r, g, bl, a);
        line(matrices, vc, x2, y1, z1, x2, y1, z2, r, g, bl, a);
        line(matrices, vc, x2, y1, z2, x1, y1, z2, r, g, bl, a);
        line(matrices, vc, x1, y1, z2, x1, y1, z1, r, g, bl, a);

        line(matrices, vc, x1, y2, z1, x2, y2, z1, r, g, bl, a);
        line(matrices, vc, x2, y2, z1, x2, y2, z2, r, g, bl, a);
        line(matrices, vc, x2, y2, z2, x1, y2, z2, r, g, bl, a);
        line(matrices, vc, x1, y2, z2, x1, y2, z1, r, g, bl, a);

        line(matrices, vc, x1, y1, z1, x1, y2, z1, r, g, bl, a);
        line(matrices, vc, x2, y1, z1, x2, y2, z1, r, g, bl, a);
        line(matrices, vc, x2, y1, z2, x2, y2, z2, r, g, bl, a);
        line(matrices, vc, x1, y1, z2, x1, y2, z2, r, g, bl, a);
    }

    private static void line(MatrixStack matrices, VertexConsumer vc,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float r, float g, float b, float a) {
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f posMat = entry.getPositionMatrix();

        vc.vertex(posMat, x1, y1, z1).color(r, g, b, a).normal(entry, 0.0f, 1.0f, 0.0f);
        vc.vertex(posMat, x2, y2, z2).color(r, g, b, a).normal(entry, 0.0f, 1.0f, 0.0f);
    }

    private static void drawFilledBox(MatrixStack matrices, VertexConsumer vc, Box b,
                                      float r, float g, float bl, float a) {
        float x1 = (float) b.minX, y1 = (float) b.minY, z1 = (float) b.minZ;
        float x2 = (float) b.maxX, y2 = (float) b.maxY, z2 = (float) b.maxZ;

        quad(matrices, vc, x1, y2, z1,  x2, y2, z1,  x2, y2, z2,  x1, y2, z2, r, g, bl, a);
        quad(matrices, vc, x1, y1, z2,  x2, y1, z2,  x2, y1, z1,  x1, y1, z1, r, g, bl, a);

        quad(matrices, vc, x2, y1, z1,  x2, y1, z2,  x2, y2, z2,  x2, y2, z1, r, g, bl, a);
        quad(matrices, vc, x1, y1, z2,  x1, y1, z1,  x1, y2, z1,  x1, y2, z2, r, g, bl, a);

        quad(matrices, vc, x1, y1, z2,  x2, y1, z2,  x2, y2, z2,  x1, y2, z2, r, g, bl, a);
        quad(matrices, vc, x2, y1, z1,  x1, y1, z1,  x1, y2, z1,  x2, y2, z1, r, g, bl, a);
    }

    private static void quad(MatrixStack matrices, VertexConsumer vc,
                             float ax, float ay, float az,
                             float bx, float by, float bz,
                             float cx, float cy, float cz,
                             float dx, float dy, float dz,
                             float r, float g, float b, float a) {
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f posMat = entry.getPositionMatrix();

        float nx = 0.0f, ny = 1.0f, nz = 0.0f;

        vc.vertex(posMat, ax, ay, az).color(r, g, b, a).normal(entry, nx, ny, nz);
        vc.vertex(posMat, bx, by, bz).color(r, g, b, a).normal(entry, nx, ny, nz);
        vc.vertex(posMat, cx, cy, cz).color(r, g, b, a).normal(entry, nx, ny, nz);

        vc.vertex(posMat, ax, ay, az).color(r, g, b, a).normal(entry, nx, ny, nz);
        vc.vertex(posMat, cx, cy, cz).color(r, g, b, a).normal(entry, nx, ny, nz);
        vc.vertex(posMat, dx, dy, dz).color(r, g, b, a).normal(entry, nx, ny, nz);
    }
}

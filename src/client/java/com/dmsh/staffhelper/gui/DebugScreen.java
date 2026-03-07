package com.dmsh.staffhelper.gui;

import com.dmsh.staffhelper.StaffHelperState;
import com.dmsh.staffhelper.config.StaffHelperConfig;
import com.dmsh.staffhelper.feature.StaffStatsFeature;
import com.dmsh.staffhelper.feature.VanishFeature;
import com.dmsh.staffhelper.gui.util.UiChrome;
import com.dmsh.staffhelper.gui.widget.IntSliderWidget;
import com.dmsh.staffhelper.gui.widget.SoupButtonWidget;
import com.dmsh.staffhelper.util.DebugLogStore;
import com.dmsh.staffhelper.util.NameTagDebugStore;
import com.dmsh.staffhelper.util.NickDecorationsStore;
import com.dmsh.staffhelper.util.RemoteNickDecorationsPoller;
import com.dmsh.staffhelper.util.RemoteRolesPoller;
import com.dmsh.staffhelper.util.RolesStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DebugScreen extends Screen {
    private enum Mode { OVERVIEW, LOGS }

    private Mode mode = Mode.OVERVIEW;

    private ButtonWidget logsBtn;
    private ButtonWidget forceBtn;
    private ButtonWidget backBtn;
    private ButtonWidget closeBtn;
    private IntSliderWidget verticalSlider;
    private IntSliderWidget horizontalSlider;

    private int logsScrollY = 0;
    private int logsScrollX = 0;
    private int logsMaxY = 0;
    private int logsMaxX = 0;

    public DebugScreen() {
        super(Text.literal("StaffHelper DEBUG"));
    }

    @Override
    protected void init() {
        logsBtn = addDrawableChild(new SoupButtonWidget(12, 10, 120, 20, Text.literal("Logs"), b -> {
            mode = Mode.LOGS;
            updateModeVisibility();
        }));

        forceBtn = addDrawableChild(new SoupButtonWidget(12, 10, 140, 20, Text.literal("Force Sync"), b -> {
            RemoteNickDecorationsPoller.forcePollNow(StaffHelperState.CONFIG);
            RemoteRolesPoller.forcePollNow(StaffHelperState.CONFIG);
        }));

        backBtn = addDrawableChild(new SoupButtonWidget(158, 10, 100, 20, Text.literal("Back"), b -> {
            mode = Mode.OVERVIEW;
            updateModeVisibility();
        }));

        closeBtn = addDrawableChild(new SoupButtonWidget(this.width - 92, 10, 80, 20, Text.literal("Close"), b -> close()));

        verticalSlider = addDrawableChild(new IntSliderWidget(
                14, this.height - 52, 280, 20, "Up/Down", 0, 0, 0, value -> logsScrollY = value
        ));
        horizontalSlider = addDrawableChild(new IntSliderWidget(
                304, this.height - 52, 280, 20, "Left/Right", 0, 0, 0, value -> logsScrollX = value
        ));

        updateModeVisibility();
        super.init();
    }

    private void updateModeVisibility() {
        boolean logsMode = mode == Mode.LOGS;
        logsBtn.visible = !logsMode;
        logsBtn.active = !logsMode;
        forceBtn.visible = logsMode;
        forceBtn.active = logsMode;
        backBtn.visible = logsMode;
        backBtn.active = logsMode;
        verticalSlider.visible = logsMode;
        verticalSlider.active = logsMode;
        horizontalSlider.visible = logsMode;
        horizontalSlider.active = logsMode;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mode == Mode.LOGS) {
            if (verticalAmount != 0) {
                logsScrollY -= (int) Math.signum(verticalAmount) * 18;
                logsScrollY = clamp(logsScrollY, 0, logsMaxY);
                verticalSlider.setIntValue(logsScrollY);
            }
            if (horizontalAmount != 0) {
                logsScrollX += (int) Math.signum(horizontalAmount) * 18;
                logsScrollX = clamp(logsScrollX, 0, logsMaxX);
                horizontalSlider.setIntValue(logsScrollX);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xCC000000);
        UiChrome.drawPanel(ctx, 8, 38, this.width - 16, this.height - 46, 10, System.currentTimeMillis(), 0.05f, true, false);

        if (mode == Mode.OVERVIEW) {
            renderOverview(ctx);
        } else {
            renderLogs(ctx);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderOverview(DrawContext ctx) {
        ctx.drawText(this.textRenderer, Text.literal("DEBUG OVERVIEW"), 14, 44, 0xFFFFFFFF, false);
        int x = 14;
        int y = 62;
        int viewH = this.height - 88;
        int lineH = 10;

        List<String> lines = buildStateSnapshot();
        ctx.enableScissor(x, y, this.width - 14, y + viewH);
        int yy = y;
        for (String line : lines) {
            if (yy > y + viewH) break;
            int color = line.startsWith("===") ? 0xFF7FCBFF : 0xFFEAEAEA;
            ctx.drawText(this.textRenderer, Text.literal(line), x, yy, color, false);
            yy += lineH;
        }
        ctx.disableScissor();
    }

    private void renderLogs(DrawContext ctx) {
        ctx.drawText(this.textRenderer, Text.literal("DEBUG LOGS"), 14, 44, 0xFFFFFFFF, false);
        int x = 14;
        int y = 62;
        int right = this.width - 14;
        int bottom = this.height - 58;
        int lineH = 10;
        int viewW = right - x;
        int viewH = bottom - y;

        List<String> lines = new ArrayList<>();
        lines.add("=== REQUEST LOGS ===");
        lines.addAll(DebugLogStore.snapshot());

        logsMaxY = Math.max(0, lines.size() * lineH - viewH);
        int maxLineW = 0;
        for (String line : lines) {
            maxLineW = Math.max(maxLineW, this.textRenderer.getWidth(line));
        }
        logsMaxX = Math.max(0, maxLineW - viewW + 12);

        logsScrollY = clamp(logsScrollY, 0, logsMaxY);
        logsScrollX = clamp(logsScrollX, 0, logsMaxX);

        verticalSlider.setRange(0, logsMaxY);
        horizontalSlider.setRange(0, logsMaxX);
        verticalSlider.setIntValue(logsScrollY);
        horizontalSlider.setIntValue(logsScrollX);

        ctx.enableScissor(x, y, right, bottom);
        int startY = y - logsScrollY;
        int drawX = x - logsScrollX;
        for (int i = 0; i < lines.size(); i++) {
            int yy = startY + i * lineH;
            if (yy + lineH < y || yy > bottom) continue;
            String line = lines.get(i);
            int color = line.startsWith("===") ? 0xFF7FCBFF : 0xFFEAEAEA;
            ctx.drawText(this.textRenderer, Text.literal(line), drawX, yy, color, false);
        }
        ctx.disableScissor();
    }

    private List<String> buildStateSnapshot() {
        List<String> out = new ArrayList<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        StaffHelperConfig cfg = StaffHelperState.CONFIG;

        out.add("=== STATE SNAPSHOT ===");
        out.add("time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        if (mc != null && mc.player != null) {
            out.add("player: " + mc.player.getGameProfile().getName());
            out.add("player uuid: " + mc.player.getUuidAsString());
        } else {
            out.add("player: <none>");
        }

        ServerInfo si = (mc != null) ? mc.getCurrentServerEntry() : null;
        out.add("server: " + (si != null ? si.address : "<singleplayer/offline>"));
        out.add("online tablist count: " + ((mc != null && mc.getNetworkHandler() != null) ? mc.getNetworkHandler().getPlayerList().size() : 0));

        out.add("roles loaded: " + RolesStore.size());
        out.add("decorations loaded: " + NickDecorationsStore.size());
        out.add("vanish enabled: " + VanishFeature.isEnabled());
        out.add("staff stats bans/mutes: " + StaffStatsFeature.getBans() + "/" + StaffStatsFeature.getMutes());
        out.add("");
        out.add("=== NAME TAG DEBUG ===");
        NameTagDebugStore.Snapshot nt = NameTagDebugStore.snapshot();
        out.add("last update: " + nt.time());
        out.add("raw: " + oneLine(nt.raw(), 140));
        out.add("cleaned: " + oneLine(nt.cleaned(), 140));
        out.add("filtered: " + nt.filtered());
        out.add("has server meta: " + nt.hasServerMeta());
        out.add("target nick: " + (nt.nick().isBlank() ? "<none>" : nt.nick()));
        out.add("has decoration: " + nt.hasDecoration());
        out.add("role: " + (nt.role().isBlank() ? "<none>" : nt.role()));
        out.add("last label seen: " + oneLine(nt.lastSeenLabel(), 140));
        out.add("suppressed marker label: " + nt.suppressedMarkerLabel());

        out.add("");
        out.add("=== CONFIG FIELDS ===");

        if (cfg == null) {
            out.add("config: <null>");
            return out;
        }

        Field[] fields = StaffHelperConfig.class.getFields();
        List<Field> sorted = new ArrayList<>(List.of(fields));
        sorted.sort(Comparator.comparing(Field::getName));

        for (Field f : sorted) {
            try {
                Object v = f.get(cfg);
                out.add(f.getName() + " = " + valueToString(v));
            } catch (Exception e) {
                out.add(f.getName() + " = <error: " + e.getClass().getSimpleName() + ">");
            }
        }

        return out;
    }

    private String valueToString(Object v) {
        if (v == null) return "null";
        if (v instanceof List<?> list) {
            int n = list.size();
            if (n == 0) return "[]";
            int take = Math.min(5, n);
            return "size=" + n + " sample=" + list.subList(0, take);
        }
        return String.valueOf(v);
    }

    private static String oneLine(String value, int maxLen) {
        if (value == null || value.isBlank()) return "<empty>";
        String s = value.replace("\r", "\\r").replace("\n", "\\n");
        if (s.length() <= maxLen) return s;
        return s.substring(0, Math.max(0, maxLen - 3)) + "...";
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) return min;
        return Math.min(value, max);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(null);
    }
}

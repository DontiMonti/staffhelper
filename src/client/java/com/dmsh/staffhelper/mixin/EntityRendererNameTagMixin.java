package com.dmsh.staffhelper.mixin;

import com.dmsh.staffhelper.util.AllowedUsersAccessGate;
import com.dmsh.staffhelper.util.NickDecoration;
import com.dmsh.staffhelper.util.NickDecorationsStore;
import com.dmsh.staffhelper.util.NameTagDebugStore;
import com.dmsh.staffhelper.util.RolesStore;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererNameTagMixin<S extends EntityRenderState> {
    private static final Pattern P_RANK_TOKEN =
            Pattern.compile("(?iu)^(?:I|II|III|IV|V|VI|VII|VIII|IX|X|Z)$");

    @Shadow
    protected abstract void renderLabelIfPresent(
            S state,
            Text text,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    );

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/EntityRenderer;renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
                    shift = At.Shift.BEFORE
            ),
            require = 0
    )
    private void staffhelper$renderExtraLabel(
            S state,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            int light,
            CallbackInfo ci
    ) {
        if (!AllowedUsersAccessGate.isModAllowed()) return;
        if (state == null || state.displayName == null) return;

        String source = state.displayName.getString();
        if (source == null || source.isBlank()) return;
        String rawSource = source;

        String cleaned = stripBadServerLines(source);
        boolean filtered = !cleaned.equals(source);
        if (!cleaned.equals(source)) {
            if (cleaned.isBlank()) {
                NameTagDebugStore.update(rawSource, cleaned, true, false, "", false, "");
                state.displayName = Text.empty();
                return;
            }
            state.displayName = Text.literal(cleaned);
            source = cleaned;
        }

        boolean hasServerMeta = hasServerExtraElement(source);
        String nick = findTargetNick(source);
        NickDecoration dec = null;
        String role = "";
        int roleColor = RolesStore.DEFAULT_ROLE_COLOR;
        if (nick != null && !nick.isBlank()) {
            dec = NickDecorationsStore.get().get(nick.toLowerCase(Locale.ROOT));
            String foundRole = RolesStore.getRoleFor(nick);
            role = foundRole == null ? "" : foundRole;
            roleColor = RolesStore.getRoleColorFor(nick);
        }

        NameTagDebugStore.update(rawSource, source, filtered, hasServerMeta, nick, dec != null, role);

        if (hasServerMeta) return;

        if (nick == null || nick.isBlank()) return;

        if (dec == null && (role == null || role.isBlank())) return;

        MutableText extra = Text.empty();
        if (dec != null) {
            extra.append(Text.literal(dec.symbol()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(dec.rgb()))));
        }
        if (role != null && !role.isBlank()) {
            if (dec != null) {
                extra.append(Text.literal(" | ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x8A8F99))));
            }
            extra.append(Text.literal(role).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(roleColor))));
        }

        matrices.push();

        matrices.translate(0.0D, 0.32D, 0.0D);
        this.renderLabelIfPresent(state, extra, matrices, consumers, light);
        matrices.pop();
    }

    private static boolean hasServerExtraElement(String displayName) {
        if (displayName == null || displayName.isBlank()) return false;
        return displayName.contains("|") || displayName.contains("\n") || displayName.contains("\r");
    }

    private static String stripBadServerLines(String displayName) {
        if (displayName == null || displayName.isBlank()) return displayName;

        String normalized = displayName.replace("\\n", "\n");
        String[] lines = normalized.split("\\R");
        if (lines.length <= 1) return displayName;

        ArrayList<String> kept = new ArrayList<>();
        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isEmpty()) continue;

            if (!isBadMarkerLine(line)) {
                kept.add(rawLine);
            }
        }

        if (kept.isEmpty()) return "";
        return String.join("\n", kept);
    }

    private static boolean isBadMarkerLine(String line) {
        String clean = normalizeForMatch(line);
        if (clean.isBlank()) return false;

        if (P_RANK_TOKEN.matcher(clean).matches()) return true;

        String[] parts = clean.split("\\|");
        if (parts.length == 2) {
            String left = parts[0].trim();
            String right = parts[1].trim();
            boolean leftIsMarker = left.isEmpty()
                    || isCrossLikeToken(left)
                    || left.matches("^[^\\p{L}\\p{N}]{1,3}$");
            if (leftIsMarker && P_RANK_TOKEN.matcher(right).matches()) return true;
        }

        String lower = clean.toLowerCase(Locale.ROOT);
        return containsCrossLikeMarker(lower)
                && (lower.contains(" z")
                || lower.endsWith("z")
                || lower.matches(".*\\b(i|ii|iii|iv|v|vi|vii|viii|ix|x)\\b.*"));
    }

    private static String normalizeForMatch(String line) {
        if (line == null) return "";
        String s = line;

        s = s.replaceAll("\\u00A7.", "");
        s = s.replace('\u2502', '|').replace('\u2503', '|').replace('\u00A6', '|');
        s = s.replaceAll("\\s+", " ").trim();

        return s;
    }

    private static boolean isCrossLikeToken(String token) {
        if (token == null) return false;
        String t = token.trim().toLowerCase(Locale.ROOT);
        return t.equals("x")
                || t.equals("\u2718")
                || t.equals("\u2716")
                || t.equals("\u2717")
                || t.equals("\u2573")
                || t.equals("\u00D7");
    }

    private static boolean containsCrossLikeMarker(String s) {
        if (s == null || s.isBlank()) return false;
        return s.contains("\u2718")
                || s.contains("\u2716")
                || s.contains("\u2717")
                || s.contains("\u2573")
                || s.contains("\u00D7")
                || s.matches(".*\\bx\\b.*");
    }

    private static String findTargetNick(String labelText) {
        if (labelText == null || labelText.isBlank()) return null;
        Map<String, NickDecoration> decMap = NickDecorationsStore.get();
        Map<String, RolesStore.RoleInfo> roleMap = RolesStore.snapshot();
        if ((decMap == null || decMap.isEmpty()) && (roleMap == null || roleMap.isEmpty())) return null;

        String best = null;
        int bestPos = Integer.MAX_VALUE;

        Set<String> allNicks = new HashSet<>();
        if (decMap != null) allNicks.addAll(decMap.keySet());
        if (roleMap != null) allNicks.addAll(roleMap.keySet());

        for (String nick : allNicks) {
            if (nick == null || nick.isBlank()) continue;
            int pos = indexOfIgnoreCase(labelText, nick);
            if (pos < 0) continue;
            if (pos < bestPos) {
                bestPos = pos;
                best = nick;
            }
        }

        return best;
    }

    @Inject(
            method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void staffhelper$suppressServerMarkerLabel(
            S state,
            Text text,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (!AllowedUsersAccessGate.isModAllowed()) return;
        if (text == null) return;
        String raw = text.getString();
        boolean suppress = isBadMarkerLine(raw);
        NameTagDebugStore.markRenderLabel(raw, suppress);
        if (suppress) {
            ci.cancel();
        }
    }

    private static int indexOfIgnoreCase(String s, String needle) {
        if (s == null || needle == null) return -1;
        int sLen = s.length();
        int nLen = needle.length();
        if (nLen == 0) return 0;

        for (int i = 0; i + nLen <= sLen; i++) {
            if (s.regionMatches(true, i, needle, 0, nLen)) return i;
        }
        return -1;
    }
}

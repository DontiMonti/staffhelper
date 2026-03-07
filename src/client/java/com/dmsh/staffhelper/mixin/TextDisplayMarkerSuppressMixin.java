package com.dmsh.staffhelper.mixin;

import com.dmsh.staffhelper.util.AllowedUsersAccessGate;
import com.dmsh.staffhelper.util.NameTagDebugStore;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.TextDisplayEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.regex.Pattern;

@Mixin(targets = "net.minecraft.client.render.entity.DisplayEntityRenderer$TextDisplayEntityRenderer")
public class TextDisplayMarkerSuppressMixin {
    private static final Pattern RANK_TOKEN = Pattern.compile("(?iu)^(?:I|II|III|IV|V|VI|VII|VIII|IX|X|Z)$");

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/TextDisplayEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void staffhelper$suppressTextDisplayMarker(
            TextDisplayEntityRenderState state,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            float tickProgress,
            CallbackInfo ci
    ) {
        if (!AllowedUsersAccessGate.isModAllowed()) return;
        String raw = extractRawText(state);
        boolean suppress = isMarkerText(raw);
        if (raw != null && !raw.isBlank()) {
            NameTagDebugStore.markRenderLabel(raw, suppress);
        }
        if (suppress) {
            ci.cancel();
        }
    }

    private static String extractRawText(TextDisplayEntityRenderState state) {
        if (state == null || state.data == null) return null;
        Object data = state.data;
        try {
            for (Method m : data.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (!Text.class.isAssignableFrom(m.getReturnType())) continue;
                Object v = m.invoke(data);
                if (v instanceof Text text) {
                    String s = text.getString();
                    if (s != null && !s.isBlank()) return s;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static boolean isMarkerText(String raw) {
        if (raw == null || raw.isBlank()) return false;
        String normalized = raw.replace("\\n", "\n");
        String[] lines = normalized.split("\\R");
        for (String line : lines) {
            if (isMarkerLine(line)) return true;
        }
        return false;
    }

    private static boolean isMarkerLine(String rawLine) {
        if (rawLine == null) return false;
        String clean = rawLine.replaceAll("\\u00A7.", "");
        clean = clean.replace('\u2502', '|').replace('\u2503', '|').replace('\u00A6', '|');
        clean = clean.replaceAll("\\s+", " ").trim();
        if (clean.isBlank()) return false;

        if (RANK_TOKEN.matcher(clean).matches()) return true;

        String[] parts = clean.split("\\|");
        if (parts.length != 2) return false;

        String left = parts[0].trim();
        String right = parts[1].trim();
        if (!RANK_TOKEN.matcher(right).matches()) return false;

        String leftLower = left.toLowerCase(Locale.ROOT);
        boolean leftLooksMarker = left.isEmpty()
                || leftLower.equals("x")
                || leftLower.equals("\u2718")
                || leftLower.equals("\u2716")
                || leftLower.equals("\u2717")
                || leftLower.equals("\u2573")
                || leftLower.equals("\u00d7")
                || left.matches("^[^\\p{L}\\p{N}]{1,4}$");
        return leftLooksMarker;
    }
}

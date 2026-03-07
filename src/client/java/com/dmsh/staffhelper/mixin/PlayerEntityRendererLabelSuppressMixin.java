package com.dmsh.staffhelper.mixin;

import com.dmsh.staffhelper.util.AllowedUsersAccessGate;
import com.dmsh.staffhelper.util.NameTagDebugStore;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;
import java.util.regex.Pattern;

@Mixin(targets = "net.minecraft.client.render.entity.PlayerEntityRenderer")
public class PlayerEntityRendererLabelSuppressMixin {
    private static final Pattern RANK_TOKEN = Pattern.compile("(?iu)^(?:I|II|III|IV|V|VI|VII|VIII|IX|X|Z)$");

    @Inject(
            method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void staffhelper$suppressMarkerLabelInPlayerRenderer(
            PlayerEntityRenderState state,
            Text text,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (!AllowedUsersAccessGate.isModAllowed()) return;
        if (state != null && state.playerName != null) {
            String rawPlayerName = state.playerName.getString();
            // Vanilla playerName is a close-range scoreboard line (<10 blocks).
            // Disable it to prevent duplicated/garbled extra lines like "✘ | z".
            NameTagDebugStore.markRenderLabel("[PlayerName:scoreboard] " + rawPlayerName, true);
            state.playerName = null;
        }

        if (text == null) return;
        String rawMain = text.getString();
        boolean suppressMain = isMarkerText(rawMain);
        NameTagDebugStore.markRenderLabel("[MainLabel] " + rawMain, suppressMain);
        if (suppressMain) {
            ci.cancel();
        }
    }

    @Inject(
            method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("RETURN"),
            require = 0
    )
    private void staffhelper$stripCloseRangeScoreboardLabel(
            AbstractClientPlayerEntity player,
            PlayerEntityRenderState state,
            float tickDelta,
            CallbackInfo ci
    ) {
        if (!AllowedUsersAccessGate.isModAllowed()) return;
        if (state == null || state.playerName == null) return;
        String raw = state.playerName.getString();
        NameTagDebugStore.markRenderLabel("[PlayerName:update] " + raw, true);
        state.playerName = null;
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
        return left.isEmpty()
                || leftLower.equals("x")
                || leftLower.equals("\u2718")
                || leftLower.equals("\u2716")
                || leftLower.equals("\u2717")
                || leftLower.equals("\u2573")
                || leftLower.equals("\u00d7")
                || left.matches("^[^\\p{L}\\p{N}]{1,4}$");
    }
}

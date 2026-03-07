package com.dmsh.staffhelper.mixin;

import com.dmsh.staffhelper.util.AllowedUsersAccessGate;
import com.dmsh.staffhelper.util.NameTagDebugStore;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;
import java.util.regex.Pattern;

@Mixin(TextRenderer.class)
public class TextRendererMarkerSuppressMixin {
    private static final Pattern RANK_TOKEN = Pattern.compile("(?iu)^(?:I|II|III|IV|V|VI|VII|VIII|IX|X|Z)$");

    @Inject(
            method = "draw(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void staffhelper$suppressMarkerStringDraw(
            String text,
            float x,
            float y,
            int color,
            boolean shadow,
            Matrix4f matrix,
            VertexConsumerProvider vertexConsumers,
            TextRenderer.TextLayerType layerType,
            int backgroundColor,
            int light,
            CallbackInfo ci
    ) {
        if (!AllowedUsersAccessGate.isModAllowed()) return;
        boolean suppress = isMarkerText(text);
        if (looksRelevant(text)) {
            NameTagDebugStore.markRenderLabel("[TextRenderer:String] " + text, suppress);
        }
        if (suppress) ci.cancel();
    }

    @Inject(
            method = "draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void staffhelper$suppressMarkerTextDraw(
            Text text,
            float x,
            float y,
            int color,
            boolean shadow,
            Matrix4f matrix,
            VertexConsumerProvider vertexConsumers,
            TextRenderer.TextLayerType layerType,
            int backgroundColor,
            int light,
            CallbackInfo ci
    ) {
        if (!AllowedUsersAccessGate.isModAllowed()) return;
        String raw = text == null ? null : text.getString();
        boolean suppress = isMarkerText(raw);
        if (looksRelevant(raw)) {
            NameTagDebugStore.markRenderLabel("[TextRenderer:Text] " + raw, suppress);
        }
        if (suppress) ci.cancel();
    }

    @Inject(
            method = "draw(Lnet/minecraft/text/OrderedText;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void staffhelper$suppressMarkerOrderedDraw(
            OrderedText text,
            float x,
            float y,
            int color,
            boolean shadow,
            Matrix4f matrix,
            VertexConsumerProvider vertexConsumers,
            TextRenderer.TextLayerType layerType,
            int backgroundColor,
            int light,
            CallbackInfo ci
    ) {
        if (!AllowedUsersAccessGate.isModAllowed()) return;
        String raw = orderedTextToString(text);
        boolean suppress = isMarkerText(raw);
        if (looksRelevant(raw)) {
            NameTagDebugStore.markRenderLabel("[TextRenderer:Ordered] " + raw, suppress);
        }
        if (suppress) ci.cancel();
    }

    @Inject(
            method = "drawWithOutline(Lnet/minecraft/text/OrderedText;FFIILorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void staffhelper$suppressMarkerOutlineDraw(
            OrderedText text,
            float x,
            float y,
            int color,
            int outlineColor,
            Matrix4f matrix,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (!AllowedUsersAccessGate.isModAllowed()) return;
        String raw = orderedTextToString(text);
        boolean suppress = isMarkerText(raw);
        if (looksRelevant(raw)) {
            NameTagDebugStore.markRenderLabel("[TextRenderer:Outline] " + raw, suppress);
        }
        if (suppress) ci.cancel();
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

    private static boolean looksRelevant(String raw) {
        if (raw == null || raw.isBlank()) return false;
        String s = raw.toLowerCase(Locale.ROOT);
        return s.contains("|")
                || s.contains("\u2718")
                || s.contains("\u2716")
                || s.contains("\u2717")
                || s.contains("\u2573")
                || s.contains("\u00d7");
    }

    private static String orderedTextToString(OrderedText text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }
}

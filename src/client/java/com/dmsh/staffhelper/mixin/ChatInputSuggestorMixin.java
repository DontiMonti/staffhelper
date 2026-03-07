package com.dmsh.staffhelper.mixin;

import com.dmsh.staffhelper.feature.CommandBuilderFeature;
import net.minecraft.client.MinecraftClient;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Mixin(value = ChatInputSuggestor.class, priority = 1500)
public abstract class ChatInputSuggestorMixin {
    @Shadow @Final TextFieldWidget textField;
    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow private boolean completingSuggestions;
    @Shadow public abstract void show(boolean narrateFirstSuggestion);

    @Inject(method = "refresh", at = @At("TAIL"))
    private void staffhelper$injectCustomSuggestions(CallbackInfo ci) {
        if (this.completingSuggestions) return;

        String text = this.textField.getText();
        int cursor = this.textField.getCursor();
        if (text == null || cursor < 0 || cursor > text.length()) return;

        CommandBuilderFeature.SuggestData data = CommandBuilderFeature.getSuggestData(text, cursor);
        if (data == null || data.suggestions().isEmpty()) return;

        if (this.pendingSuggestions == null) {
            this.pendingSuggestions = CompletableFuture.completedFuture(buildSuggestions(text, data));
            this.show(false);
            return;
        }

        this.pendingSuggestions = this.pendingSuggestions.thenApply(existing -> mergeSuggestions(text, existing, data));
        this.pendingSuggestions.thenRun(() -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null) mc.execute(() -> this.show(false));
        });
    }

    private static Suggestions mergeSuggestions(String text, Suggestions base, CommandBuilderFeature.SuggestData data) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<Suggestion> merged = new ArrayList<>();

        if (base != null) {
            for (Suggestion s : base.getList()) {
                String key = suggestionKey(s.getText());
                if (seen.add(key)) merged.add(s);
            }
        }

        StringRange range = StringRange.between(data.start(), data.end());
        for (String value : data.suggestions()) {
            if (value == null || value.isBlank()) continue;
            String key = suggestionKey(value);
            if (seen.add(key)) merged.add(new Suggestion(range, value));
        }

        if (merged.isEmpty()) {
            return base != null ? base : Suggestions.empty().join();
        }
        return Suggestions.create(text, merged);
    }

    private static Suggestions buildSuggestions(String text, CommandBuilderFeature.SuggestData data) {
        StringRange range = StringRange.between(data.start(), data.end());
        List<Suggestion> list = new ArrayList<>();
        for (String value : data.suggestions()) {
            if (value == null || value.isBlank()) continue;
            list.add(new Suggestion(range, value));
        }
        if (list.isEmpty()) return Suggestions.empty().join();
        return Suggestions.create(text, list);
    }

    private static String suggestionKey(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}

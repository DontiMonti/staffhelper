package com.dmsh.staffhelper.mixin;

import com.dmsh.staffhelper.feature.CommandBuilderFeature;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Pseudo
@Mixin(targets = "io.github.darkkronicle.advancedchatbox.chat.ChatSuggestor", remap = false)
public abstract class AdvancedChatBoxSuggestorMixin {
    @Shadow private CompletableFuture<?> pendingSuggestions;
    @Shadow @Final private TextFieldWidget textField;

    @Inject(
            method = "updateCommandSuggestions(Ljava/lang/Runnable;)V",
            at = @At(
                    value = "FIELD",
                    target = "Lio/github/darkkronicle/advancedchatbox/chat/ChatSuggestor;pendingSuggestions:Ljava/util/concurrent/CompletableFuture;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER
            ),
            remap = false
    )
    private void staffhelper$injectCommandBuilderSuggestions(Runnable onDone, CallbackInfo ci) {
        if (this.textField == null) return;

        String text = this.textField.getText();
        int cursor = this.textField.getCursor();
        if (text == null || cursor < 0 || cursor > text.length()) return;
        if (!text.startsWith("/")) return;

        CommandBuilderFeature.SuggestData data = CommandBuilderFeature.getSuggestData(text, cursor);
        if (data == null || data.suggestions().isEmpty()) return;

        if (this.pendingSuggestions == null) return;
        this.pendingSuggestions = this.pendingSuggestions.thenApply(existing -> mergeSuggestions(existing, text, data));
    }

    private static Object mergeSuggestions(Object existingAdvanced, String text, CommandBuilderFeature.SuggestData data) {
        StringRange ourRange = StringRange.between(data.start(), data.end());

        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<Suggestion> merged = new ArrayList<>();

        Object existingRangeObj = invokeGetter(existingAdvanced, "getRange");
        if (existingAdvanced != null) {
            Object existingListObj = invokeGetter(existingAdvanced, "getSuggestions");
            if (existingListObj instanceof List<?> existingList) {
                for (Object o : existingList) {
                    if (!(o instanceof Suggestion s)) continue;
                    if (seen.add(suggestionKey(s.getText()))) {
                        merged.add(s);
                    }
                }
            }
        }

        for (String raw : data.suggestions()) {
            if (raw == null || raw.isBlank()) continue;
            if (seen.add(suggestionKey(raw))) {
                merged.add(new Suggestion(ourRange, raw));
            }
        }

        if (merged.isEmpty()) return existingAdvanced;

        StringRange targetRange = (existingRangeObj instanceof StringRange sr) ? sr : ourRange;
        Suggestions brigadierMerged = Suggestions.create(text, merged);
        Object advancedMerged = toAdvancedSuggestions(brigadierMerged);
        if (advancedMerged != null) return advancedMerged;

        // Fallback: try constructing AdvancedSuggestions(range, list) directly.
        try {
            Class<?> clazz = Class.forName("io.github.darkkronicle.advancedchatbox.chat.AdvancedSuggestions");
            return clazz.getConstructor(StringRange.class, List.class).newInstance(targetRange, merged);
        } catch (Throwable ignored) {
            return existingAdvanced;
        }
    }

    private static String suggestionKey(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static Object invokeGetter(Object target, String methodName) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(methodName);
            return m.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object toAdvancedSuggestions(Suggestions suggestions) {
        try {
            Class<?> clazz = Class.forName("io.github.darkkronicle.advancedchatbox.chat.AdvancedSuggestions");
            Method fromSuggestions = clazz.getMethod("fromSuggestions", Suggestions.class);
            return fromSuggestions.invoke(null, suggestions);
        } catch (Throwable ignored) {
            return null;
        }
    }
}

package com.dmsh.staffhelper.feature;

import com.dmsh.staffhelper.StaffHelperState;
import com.dmsh.staffhelper.config.StaffHelperConfig;
import com.dmsh.staffhelper.util.DebugLogStore;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandBuilderFeature {
    private CommandBuilderFeature() {}
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{[a-zA-Z0-9_]+\\}");

    public static void init() {
        ClientSendMessageEvents.MODIFY_COMMAND.register(CommandBuilderFeature::onModifyCommand);
    }

    private static String onModifyCommand(String command) {
        StaffHelperConfig cfg = StaffHelperState.CONFIG;
        if (cfg == null || cfg.commandBuilders == null || cfg.commandBuilders.isEmpty()) return command;

        String fullInput = "/" + command;
        String inputCmd = firstToken(fullInput);
        boolean hadCommandBuilderCandidate = false;
        for (StaffHelperConfig.CommandBuilderEntry entry : cfg.commandBuilders) {
            if (entry == null) continue;
            String alias = safe(entry.alias);
            String execute = safe(entry.execute);
            if (alias.isBlank() || execute.isBlank()) continue;
            String aliasCmd = firstToken(alias);
            if (!equalsCommandTokenIgnoreCase(aliasCmd, inputCmd)) continue;

            hadCommandBuilderCandidate = true;
            DebugLogStore.add("[CB] TRY alias=\"" + alias + "\" execute=\"" + execute + "\" input=\"" + fullInput + "\"");

            String[] effectiveAliasTokens = buildEffectiveAliasTokens(entry);
            MatchResult match = matchAlias(effectiveAliasTokens, fullInput);
            if (!match.matched) continue;

            String out = applyTemplate(execute, match.values).trim();
            if (out.isEmpty()) return command;
            if (out.startsWith("/")) out = out.substring(1);
            DebugLogStore.add("[CB] SEND alias=\"" + alias + "\" -> \"/" + out + "\"");
            return out;
        }

        if (hadCommandBuilderCandidate) {
            DebugLogStore.add("[CB] SKIP no matched pattern for input=\"" + fullInput + "\"");
        }
        return command;
    }

    public static SuggestData getSuggestData(String text, int cursor) {
        if (text == null || cursor < 0 || cursor > text.length()) return null;
        if (!text.startsWith("/")) return null;

        StaffHelperConfig cfg = StaffHelperState.CONFIG;
        if (cfg == null || cfg.commandBuilders == null || cfg.commandBuilders.isEmpty()) return null;

        String prefixPart = text.substring(0, cursor);
        String trimmed = prefixPart.trim();
        if (trimmed.isEmpty()) return null;

        boolean endsWithSpace = prefixPart.endsWith(" ");
        String[] tokens = splitNonEmpty(trimmed);
        if (tokens.length == 0) return null;

        String currentPrefix = endsWithSpace ? "" : tokens[tokens.length - 1];
        int tokenIndex = endsWithSpace ? tokens.length : tokens.length - 1;
        int replaceStart = endsWithSpace ? cursor : startOfCurrentWord(prefixPart, cursor);

        List<String> suggestions = new ArrayList<>();

        if (tokenIndex == 0) {
            String normalizedPrefix = normalizeTypedCommandPrefix(currentPrefix);
            for (StaffHelperConfig.CommandBuilderEntry entry : cfg.commandBuilders) {
                if (entry == null) continue;
                String aliasCmd = normalizeForSuggestion(firstToken(safe(entry.alias)));
                if (aliasCmd.isBlank()) continue;
                if (startsWithIgnoreCase(aliasCmd, normalizedPrefix)) suggestions.add(aliasCmd);
            }
        } else {
            String commandToken = tokens[0];
            for (StaffHelperConfig.CommandBuilderEntry entry : cfg.commandBuilders) {
                if (entry == null) continue;
                String[] aliasTokens = buildEffectiveAliasTokens(entry);
                if (aliasTokens.length == 0) continue;
                if (!equalsCommandTokenIgnoreCase(aliasTokens[0], commandToken)) continue;

                int aliasArgIndex = tokenIndex - 1;
                if (aliasArgIndex < 0 || aliasArgIndex >= (aliasTokens.length - 1)) continue;
                String spec = aliasTokens[aliasArgIndex + 1];

                if ("{playername}".equalsIgnoreCase(spec)) {
                    suggestions.addAll(playerNameSuggestions(currentPrefix));
                } else if ("{time}".equalsIgnoreCase(spec)) {
                    if (entry.hasExecuteToken("{time}")) {
                        suggestions.addAll(csvSuggestions(entry.timeOptions, currentPrefix));
                    }
                } else if ("{reason}".equalsIgnoreCase(spec)) {
                    if (entry.hasExecuteToken("{reason}")) {
                        suggestions.addAll(csvSuggestions(entry.reasonOptions, currentPrefix));
                    }
                } else if (startsWithIgnoreCase(spec, currentPrefix)) {
                    suggestions.add(spec);
                }
            }
        }

        LinkedHashSet<String> dedup = new LinkedHashSet<>();
        for (String s : suggestions) {
            if (s != null && !s.isBlank()) dedup.add(s);
        }

        if (dedup.isEmpty()) return null;
        return new SuggestData(replaceStart, cursor, new ArrayList<>(dedup));
    }

    public static boolean isCommandBuilderContext(String text, int cursor) {
        if (text == null || cursor < 0 || cursor > text.length()) return false;
        if (!text.startsWith("/")) return false;
        StaffHelperConfig cfg = StaffHelperState.CONFIG;
        if (cfg == null || cfg.commandBuilders == null || cfg.commandBuilders.isEmpty()) return false;

        String prefixPart = text.substring(0, cursor);
        String trimmed = prefixPart.trim();
        if (trimmed.isEmpty()) return false;

        String[] tokens = splitNonEmpty(trimmed);
        if (tokens.length == 0) return false;

        String cmd = tokens[0];
        if ("/".equals(cmd)) return false;
        for (StaffHelperConfig.CommandBuilderEntry entry : cfg.commandBuilders) {
            if (entry == null) continue;
            String aliasCmd = firstToken(safe(entry.alias));
            if (aliasCmd.isBlank()) continue;
            if (equalsCommandTokenIgnoreCase(aliasCmd, cmd)) return true;
        }
        return false;
    }

    public static boolean applyTabCompletion(TextFieldWidget chatField) {
        if (chatField == null) return false;
        String text = chatField.getText();
        if (text == null || !text.startsWith("/")) return false;
        if (chatField.getCursor() != text.length()) return false;

        SuggestData data = getSuggestData(text, chatField.getCursor());
        if (data == null || data.suggestions().isEmpty()) return false;

        String chosen = data.suggestions().getFirst();
        String replaced = text.substring(0, data.start()) + chosen + " ";
        chatField.setText(replaced);
        chatField.setCursorToEnd(false);
        return true;
    }

    private static int startOfCurrentWord(String s, int cursor) {
        int i = Math.max(0, Math.min(cursor, s.length()));
        while (i > 0) {
            char c = s.charAt(i - 1);
            if (Character.isWhitespace(c)) break;
            i--;
        }
        return i;
    }

    private static List<String> playerNameSuggestions(String prefix) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getNetworkHandler() == null) return Collections.emptyList();

        List<String> names = new ArrayList<>();
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            if (entry == null || entry.getProfile() == null) continue;
            String name = entry.getProfile().getName();
            if (name == null || name.isBlank()) continue;
            if (startsWithIgnoreCase(name, prefix)) names.add(name);
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private static List<String> csvSuggestions(String csv, String prefix) {
        List<String> out = new ArrayList<>();
        if (csv == null || csv.isBlank()) return out;
        for (String part : csv.split(",")) {
            String value = part.trim();
            if (value.isEmpty()) continue;
            if (startsWithIgnoreCase(value, prefix)) out.add(value);
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    private static MatchResult matchAlias(String[] aliasTokens, String input) {
        String[] inputTokens = splitNonEmpty(input);
        if (aliasTokens.length == 0 || inputTokens.length == 0) return MatchResult.no();
        if (!equalsCommandTokenIgnoreCase(aliasTokens[0], inputTokens[0])) return MatchResult.no();

        Map<String, String> values = new HashMap<>();
        int i = 1;
        for (; i < aliasTokens.length; i++) {
            String token = aliasTokens[i];
            boolean last = i == aliasTokens.length - 1;
            boolean placeholder = isPlaceholder(token);

            if (!placeholder) {
                if (i >= inputTokens.length) return MatchResult.no();
                if (!equalsIgnoreCase(token, inputTokens[i])) return MatchResult.no();
                continue;
            }

            if (last) {
                String value = joinFrom(inputTokens, i);
                values.put(token.toLowerCase(Locale.ROOT), value);
                return MatchResult.yes(values);
            }

            if (i >= inputTokens.length) {
                values.put(token.toLowerCase(Locale.ROOT), "");
                continue;
            }
            values.put(token.toLowerCase(Locale.ROOT), inputTokens[i]);
        }

        if (inputTokens.length > aliasTokens.length) return MatchResult.no();
        return MatchResult.yes(values);
    }

    private static String applyTemplate(String template, Map<String, String> values) {
        String out = template;
        for (Map.Entry<String, String> e : values.entrySet()) {
            String key = e.getKey();
            String value = e.getValue() == null ? "" : e.getValue();
            out = out.replace(key, value);
            out = out.replace(key.toUpperCase(Locale.ROOT), value);
        }
        return out;
    }

    private static boolean isPlaceholder(String token) {
        return token.startsWith("{") && token.endsWith("}");
    }

    private static String joinFrom(String[] arr, int from) {
        if (from >= arr.length) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < arr.length; i++) {
            if (i > from) sb.append(' ');
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    private static String firstToken(String text) {
        String[] t = splitNonEmpty(text);
        return t.length == 0 ? "" : t[0];
    }

    private static String[] splitNonEmpty(String text) {
        if (text == null) return new String[0];
        String t = text.trim();
        if (t.isEmpty()) return new String[0];
        return t.split("\\s+");
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String[] buildEffectiveAliasTokens(StaffHelperConfig.CommandBuilderEntry entry) {
        String[] aliasTokens = splitNonEmpty(safe(entry.alias));
        if (aliasTokens.length == 0) return aliasTokens;

        List<String> out = new ArrayList<>(List.of(aliasTokens));
        String execute = safe(entry.execute);

        LinkedHashSet<String> placeholders = new LinkedHashSet<>();
        for (String t : aliasTokens) {
            if (isPlaceholder(t)) placeholders.add(t.toLowerCase(Locale.ROOT));
        }

        Matcher m = PLACEHOLDER_PATTERN.matcher(execute);
        while (m.find()) {
            String ph = m.group();
            if (ph == null || ph.isBlank()) continue;
            String norm = ph.toLowerCase(Locale.ROOT);
            if (placeholders.contains(norm)) continue;
            placeholders.add(norm);
            out.add(norm);
        }

        return out.toArray(new String[0]);
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    private static boolean startsWithIgnoreCase(String value, String prefix) {
        if (value == null || prefix == null) return false;
        if (prefix.isEmpty()) return true;
        if (prefix.length() > value.length()) return false;
        return value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static boolean equalsCommandTokenIgnoreCase(String a, String b) {
        String na = normalizeCommandToken(a);
        String nb = normalizeCommandToken(b);
        if (na.isEmpty() || nb.isEmpty()) return false;
        return na.equalsIgnoreCase(nb);
    }

    private static String normalizeCommandToken(String token) {
        String s = safe(token);
        while (s.startsWith("/")) {
            s = s.substring(1).trim();
        }
        return s;
    }

    private static String normalizeForSuggestion(String token) {
        String core = normalizeCommandToken(token);
        if (core.isEmpty()) return "";
        return "/" + core;
    }

    private static String normalizeTypedCommandPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return "/";
        return prefix.startsWith("/") ? prefix : ("/" + prefix);
    }

    public record SuggestData(int start, int end, List<String> suggestions) {}

    private static final class MatchResult {
        final boolean matched;
        final Map<String, String> values;

        private MatchResult(boolean matched, Map<String, String> values) {
            this.matched = matched;
            this.values = values;
        }

        static MatchResult no() {
            return new MatchResult(false, Map.of());
        }

        static MatchResult yes(Map<String, String> values) {
            return new MatchResult(true, values);
        }
    }
}

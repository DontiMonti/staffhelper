package com.dmsh.staffhelper.util;

import net.minecraft.text.*;

import java.util.*;

public final class NameDecorations {
    private NameDecorations() {}

    private static Map<String, NickDecoration> currentMap() {
        Map<String, NickDecoration> m = NickDecorationsStore.get();
        return (m == null) ? Collections.emptyMap() : m;
    }

    public static Text withDecorationIfTarget(String profileName, Text base) {
        if (profileName == null || base == null) return base;

        NickDecoration d = findDecoration(profileName);
        if (d == null) return base;

        if (hasTrailingSymbol(base.getString(), d.symbol())) return base;

        return Text.empty().append(base).append(buildSymbolText(d, Style.EMPTY));
    }

    public static Text applyStarsToChat(Text in) {
        if (in == null) return null;

        String whole = in.getString();
        if (whole == null || whole.isEmpty()) return in;

        Map<String, NickDecoration> map = currentMap();

        boolean any = false;
        for (String nick : map.keySet()) {
            if (indexOfIgnoreCase(whole, nick, 0) >= 0) {
                any = true;
                break;
            }
        }
        if (!any) return in;

        MutableText out = Text.empty();
        in.visit((style, str) -> {
            appendWithSymbols(out, str, style, map);
            return Optional.empty();
        }, Style.EMPTY);

        return out;
    }

    private static NickDecoration findDecoration(String profileName) {
        if (profileName == null) return null;
        return currentMap().get(profileName.toLowerCase(Locale.ROOT));
    }

    private static boolean isNameChar(char c) {
        return (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9')
                || c == '_';
    }

    private static boolean hasSymbolAfter(String s, int idx, String symbol) {
        if (s == null || symbol == null || symbol.isEmpty()) return false;
        if (idx >= s.length()) return false;
        if (s.startsWith(symbol, idx)) return true;
        return s.startsWith(" " + symbol, idx);
    }

    private static boolean hasTrailingSymbol(String baseText, String symbol) {
        if (baseText == null || symbol == null || symbol.isBlank()) return false;
        String s = baseText.stripTrailing();
        return s.endsWith(symbol) || s.endsWith(" " + symbol);
    }

    private static void appendLiteral(MutableText out, String s, Style style) {
        if (s == null || s.isEmpty()) return;
        out.append(Text.literal(s).setStyle(style));
    }

    private static Text buildSymbolText(NickDecoration d, Style baseStyle) {
        TextColor color = TextColor.fromRgb(d.rgb());
        Style s = baseStyle.withColor(color);
        return Text.literal(" " + d.symbol()).setStyle(s);
    }

    private static void appendWithSymbols(MutableText out, String s, Style style, Map<String, NickDecoration> map) {
        if (s == null || s.isEmpty()) return;

        int i = 0;
        while (i < s.length()) {
            int bestPos = -1;
            String bestNick = null;
            NickDecoration bestDec = null;

            for (Map.Entry<String, NickDecoration> e : map.entrySet()) {
                String nick = e.getKey();
                int p = indexOfIgnoreCase(s, nick, i);
                if (p >= 0 && (bestPos < 0 || p < bestPos)) {
                    bestPos = p;
                    bestNick = nick;
                    bestDec = e.getValue();
                }
            }

            if (bestPos < 0 || bestNick == null || bestDec == null) {
                appendLiteral(out, s.substring(i), style);
                return;
            }

            int len = bestNick.length();

            boolean leftOk = bestPos == 0 || !isNameChar(s.charAt(bestPos - 1));
            boolean rightOk = (bestPos + len) >= s.length() || !isNameChar(s.charAt(bestPos + len));
            if (!leftOk || !rightOk) {
                appendLiteral(out, s.substring(i, bestPos + 1), style);
                i = bestPos + 1;
                continue;
            }

            if (bestPos > i) {
                appendLiteral(out, s.substring(i, bestPos), style);
            }

            appendLiteral(out, s.substring(bestPos, bestPos + len), style);

            int after = bestPos + len;
            if (!hasSymbolAfter(s, after, bestDec.symbol())) {
                out.append(buildSymbolText(bestDec, style));
            }

            i = after;
        }
    }

    private static int indexOfIgnoreCase(String s, String needle, int fromIndex) {
        if (s == null || needle == null) return -1;
        int sLen = s.length();
        int nLen = needle.length();
        if (nLen == 0) return Math.max(0, fromIndex);

        for (int i = Math.max(0, fromIndex); i + nLen <= sLen; i++) {
            if (s.regionMatches(true, i, needle, 0, nLen)) return i;
        }
        return -1;
    }
}

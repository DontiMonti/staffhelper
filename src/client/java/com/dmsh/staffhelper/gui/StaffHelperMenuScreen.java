package com.dmsh.staffhelper.gui;

import com.dmsh.staffhelper.StaffHelperState;
import com.dmsh.staffhelper.config.StaffHelperConfig;
import com.dmsh.staffhelper.feature.NickSearchFeature;
import com.dmsh.staffhelper.gui.util.GuiRenderUtils;
import com.dmsh.staffhelper.gui.util.UiChrome;
import com.dmsh.staffhelper.gui.widget.CenteredTextFieldWidget;
import com.dmsh.staffhelper.gui.widget.IntSliderWidget;
import com.dmsh.staffhelper.gui.widget.SoupIntSliderWidget;
import com.dmsh.staffhelper.gui.widget.SoupButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class StaffHelperMenuScreen extends Screen {

    private enum Tab { NICKSEARCH, AFKZONE, COMMANDBUILDER, MODULES, APPEARANCE }
    private Tab tab = Tab.NICKSEARCH;
    private static final int CUSTOM_DIALOG_W = 420;
    private static final int CUSTOM_DIALOG_H = 266;

    private float openProgress = 0f;
    private int openingOffsetY = 0;
    private static final int OPEN_FROM_BOTTOM_PX = 64;
    private float tabTransitionProgress = 1.0f;
    private int appliedWidgetOffsetY = 0;
    private boolean closing = false;

    private final int panelW = 620;
    private final int panelH = 380;

    private TextFieldWidget addInput;
    private TextFieldWidget searchInput;

    // AFK fields
    private TextFieldWidget pos1X, pos1Y, pos1Z;
    private TextFieldWidget pos2X, pos2Y, pos2Z;

    // AFK ignore list UI
    private TextFieldWidget afkIgnoreInput;
    private ButtonWidget afkIgnoreAddBtn;
    private int afkIgnoreScroll = 0;

    private SoupButtonWidget tabNickBtn;
    private SoupButtonWidget tabAfkBtn;
    private SoupButtonWidget tabCommandBuilderBtn;
    private SoupButtonWidget tabModulesBtn;
    private SoupButtonWidget tabAppearanceBtn;

    private ButtonWidget addBtn;
    private ButtonWidget clearBtn;
    private ButtonWidget hudEditorBtn;
    private ButtonWidget uiSheenToggleBtn;
    private ButtonWidget toggleBtn;
    private ButtonWidget closeBtn;
    private ButtonWidget afkRenderToggleBtn;
    private ButtonWidget afkApplyBtn;
    private ButtonWidget statsSectionBtn;
    private ButtonWidget statsEnableBtn;
    private ButtonWidget statsLayoutBtn;
    private ButtonWidget statsRoleBtn;
    private ButtonWidget statsPingBtn;
    private ButtonWidget statsTpsBtn;
    private ButtonWidget statsNowBtn;
    private ButtonWidget stats5mBtn;
    private ButtonWidget stats10mBtn;
    private ButtonWidget stats15mBtn;
    private ButtonWidget autoBoxSectionBtn;
    private SoupButtonWidget autoBoxBox1Btn;
    private SoupButtonWidget autoBoxBox2Btn;
    private SoupButtonWidget themeBlueBtn;
    private SoupButtonWidget themeRedBtn;
    private SoupButtonWidget themePurpleBtn;
    private SoupButtonWidget themeOrangeBtn;
    private SoupButtonWidget themeGreenBtn;
    private SoupButtonWidget themeBrightPurpleBtn;
    private SoupButtonWidget themePinkBtn;
    private SoupButtonWidget themeCustomBtn;

    private IntSliderWidget customColor1R;
    private IntSliderWidget customColor1G;
    private IntSliderWidget customColor1B;
    private IntSliderWidget customColor2R;
    private IntSliderWidget customColor2G;
    private IntSliderWidget customColor2B;
    private SoupButtonWidget customThemeApplyBtn;
    private SoupButtonWidget customThemeCancelBtn;
    private boolean customThemeDialogOpen = false;
    private String customThemeBeforeOpen = "BLUE";
    private int customColor1BeforeOpen = 0x2D4A73;
    private int customColor2BeforeOpen = 0x5F8FD6;
    private boolean statsExpanded = false;
    private boolean autoBoxExpanded = false;
    private float statsExpandProgress = 0.0f;
    private float autoBoxExpandProgress = 0.0f;
    private int modulesScroll = 0;

    private int modulesListX = 0;
    private int modulesListY = 0;
    private int modulesListW = 300;
    private int modulesListH = 220;

    private int scroll = 0;

    // Command Builder tab UI
    private ButtonWidget commandBuilderAddBtn;
    private final List<CommandBuilderUiEntry> commandBuilderUiEntries = new ArrayList<>();
    private int commandBuilderScroll = 0;
    private int commandBuilderListX = 0;
    private int commandBuilderListY = 0;
    private int commandBuilderListW = 0;
    private int commandBuilderListH = 0;
    private final Map<TextFieldWidget, Float> animatedTextFieldProgress = new IdentityHashMap<>();
    private final Map<TextFieldWidget, Boolean> animatedTextFieldTargets = new IdentityHashMap<>();

    private static Text tr(String key, Object... args) {
        return Text.translatable(key, args);
    }

    private static String ts(String key, Object... args) {
        return tr(key, args).getString();
    }

    public StaffHelperMenuScreen() {
        super(tr("screen.staffhelper.menu.title"));
    }

    @Override
    protected void init() {
        int x0 = (this.width - panelW) / 2;
        int y0 = (this.height - panelH) / 2;

        int pad = 16;
        int tabsY = y0 + 10;

        // Tabs
        int tabX = x0 + pad;
        tabNickBtn = addDrawableChild(new SoupButtonWidget(tabX, tabsY, 125, 20, Text.literal("NickSearch"), b -> {
            switchTab(Tab.NICKSEARCH);
        }));
        tabX += 125 + 8;

        tabAfkBtn = addDrawableChild(new SoupButtonWidget(tabX, tabsY, 105, 20, Text.literal("AFK Zone"), b -> {
            switchTab(Tab.AFKZONE);
        }));
        tabX += 105 + 8;

        tabCommandBuilderBtn = addDrawableChild(new SoupButtonWidget(tabX, tabsY, 145, 20, Text.literal("ComandBuilder"), b -> {
            switchTab(Tab.COMMANDBUILDER);
        }));
        tabX += 145 + 8;

        tabModulesBtn = addDrawableChild(new SoupButtonWidget(tabX, tabsY, 90, 20, Text.literal("Modules"), b -> {
            switchTab(Tab.MODULES);
        }));
        tabX += 90 + 8;

        tabAppearanceBtn = addDrawableChild(new SoupButtonWidget(tabX, tabsY, 100, 20, Text.literal("Appearance"), b -> {
            switchTab(Tab.APPEARANCE);
        }));

        // Close
        closeBtn = addDrawableChild(new SoupButtonWidget(
                x0 + panelW - pad - 110,
                y0 + panelH - pad - 20,
                110,
                20,
                tr("gui.staffhelper.button.close"),
                b -> beginCloseAnimation()
        ));

        // layout rows for Nick tab
        int headerY = tabsY + 28;
        int blockY = headerY + 34;
        int searchRowY = blockY + 14 + 20 + 28;

        // Toggle (NickSearch)
        toggleBtn = addDrawableChild(new SoupButtonWidget(
                x0 + pad + 360 + 8 + 90 + 8,
                blockY + 14,
                90,
                20,
                toggleText(),
                b -> {
                    StaffHelperState.CONFIG.nickSearchEnabled = !StaffHelperState.CONFIG.nickSearchEnabled;
                    StaffHelperState.CONFIG.save();
                    b.setMessage(toggleText());
                }
        ));

        // Add input
        addInput = new CenteredTextFieldWidget(this.textRenderer,
                x0 + pad,
                blockY + 14,
                360,
                20,
                Text.literal(""));
        addInput.setMaxLength(64);
        addInput.setDrawsBackground(false);
        addDrawableChild(addInput);

        // Add button
        addBtn = addDrawableChild(new SoupButtonWidget(x0 + pad + 360 + 8, blockY + 14, 90, 20, tr("gui.staffhelper.button.add"), b -> {
            String ptn = addInput.getText().trim();
            if (!ptn.isEmpty()) {
                StaffHelperState.CONFIG.nickPatterns.add(ptn);
                StaffHelperState.CONFIG.save();
                addInput.setText("");
                clampScroll();
            }
        }));

        // Search input
        searchInput = new CenteredTextFieldWidget(this.textRenderer,
                x0 + pad,
                searchRowY + 14,
                458,
                20,
                Text.literal(""));
        searchInput.setMaxLength(64);
        searchInput.setDrawsBackground(false);
        addDrawableChild(searchInput);

        // Clear button
        clearBtn = addDrawableChild(new SoupButtonWidget(x0 + pad + 458 + 8, searchRowY + 14, 90, 20, tr("gui.staffhelper.button.clear"), b -> {
            searchInput.setText("");
            scroll = 0;
        }));

        // HUD editor button
        hudEditorBtn = addDrawableChild(new SoupButtonWidget(
                x0 + 16,
                y0 + 78,
                120,
                20,
                tr("gui.staffhelper.button.edit_hud"),
                b -> {
                    if (tab == Tab.APPEARANCE) MinecraftClient.getInstance().setScreen(new HudEditorScreen());
                }
        ));
        uiSheenToggleBtn = addDrawableChild(new SoupButtonWidget(
                x0 + 144,
                y0 + 78,
                170,
                20,
                uiSheenText(),
                b -> {
                    StaffHelperState.CONFIG.uiSheenAnimationEnabled = !StaffHelperState.CONFIG.uiSheenAnimationEnabled;
                    StaffHelperState.CONFIG.save();
                    b.setMessage(uiSheenText());
                }
        ));

        // ===== AFK ZONE TAB UI =====
        int afkBaseY = y0 + 110;

        pos1X = new CenteredTextFieldWidget(this.textRenderer, x0 + pad + 60, afkBaseY + 20, 90, 20, Text.literal(""));
        pos1Y = new CenteredTextFieldWidget(this.textRenderer, x0 + pad + 160, afkBaseY + 20, 90, 20, Text.literal(""));
        pos1Z = new CenteredTextFieldWidget(this.textRenderer, x0 + pad + 260, afkBaseY + 20, 90, 20, Text.literal(""));
        pos2X = new CenteredTextFieldWidget(this.textRenderer, x0 + pad + 60, afkBaseY + 90, 90, 20, Text.literal(""));
        pos2Y = new CenteredTextFieldWidget(this.textRenderer, x0 + pad + 160, afkBaseY + 90, 90, 20, Text.literal(""));
        pos2Z = new CenteredTextFieldWidget(this.textRenderer, x0 + pad + 260, afkBaseY + 90, 90, 20, Text.literal(""));

        for (TextFieldWidget tf : new TextFieldWidget[]{pos1X, pos1Y, pos1Z, pos2X, pos2Y, pos2Z}) {
            tf.setMaxLength(12);
            tf.setDrawsBackground(false);
            addDrawableChild(tf);
        }

        reloadAfkFieldsFromConfig();

        afkRenderToggleBtn = addDrawableChild(new SoupButtonWidget(x0 + pad, afkBaseY + 140, 110, 20, afkRenderText(), b -> {
            // cycle: OFF -> OUTLINE -> BOTH -> OFF
            int mode = getAfkRenderMode();
            mode = switch (mode) {
                case 0 -> 1;      // OFF -> OUTLINE
                case 1 -> 3;      // OUTLINE -> BOTH
                case 2 -> 3;      // (safety) FILL -> BOTH
                default -> 0;     // BOTH -> OFF
            };
            setAfkRenderMode(mode);
            StaffHelperState.CONFIG.save();
            b.setMessage(afkRenderText());
        }));

        afkApplyBtn = addDrawableChild(new SoupButtonWidget(x0 + pad, afkBaseY + 170, 110, 20, tr("gui.staffhelper.button.apply"), b -> {
            StaffHelperState.CONFIG.afkX1 = parseInt(pos1X.getText(), StaffHelperState.CONFIG.afkX1);
            StaffHelperState.CONFIG.afkY1 = parseInt(pos1Y.getText(), StaffHelperState.CONFIG.afkY1);
            StaffHelperState.CONFIG.afkZ1 = parseInt(pos1Z.getText(), StaffHelperState.CONFIG.afkZ1);

            StaffHelperState.CONFIG.afkX2 = parseInt(pos2X.getText(), StaffHelperState.CONFIG.afkX2);
            StaffHelperState.CONFIG.afkY2 = parseInt(pos2Y.getText(), StaffHelperState.CONFIG.afkY2);
            StaffHelperState.CONFIG.afkZ2 = parseInt(pos2Z.getText(), StaffHelperState.CONFIG.afkZ2);

            StaffHelperState.CONFIG.save();
            reloadAfkFieldsFromConfig();
        }));

        // ---- AFK ignore list (right column) ----
        int ignoreX = x0 + pad + 370;
        int ignoreY = afkBaseY + 34;

        afkIgnoreInput = new CenteredTextFieldWidget(this.textRenderer,
                ignoreX,
                ignoreY,
                130,
                20,
                Text.literal(""));
        afkIgnoreInput.setMaxLength(16);
        afkIgnoreInput.setDrawsBackground(false);
        addDrawableChild(afkIgnoreInput);

        afkIgnoreAddBtn = addDrawableChild(new SoupButtonWidget(ignoreX + 130 + 8, ignoreY, 80, 20, tr("gui.staffhelper.button.add"), b -> {
            String nick = (afkIgnoreInput != null) ? afkIgnoreInput.getText().trim() : "";
            if (nick.isEmpty()) return;
            if (nick.length() < 3 || nick.length() > 16) return;

            boolean exists = false;
            for (String s : StaffHelperState.CONFIG.afkIgnoreNicks) {
                if (s != null && s.equalsIgnoreCase(nick)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                StaffHelperState.CONFIG.afkIgnoreNicks.add(nick);
                StaffHelperState.CONFIG.save();
            }
            afkIgnoreInput.setText("");
            clampIgnoreScroll();
        }));

        // ===== COMMAND BUILDER TAB UI =====
        initCommandBuilderUi(x0, y0, pad);

        // ===== MODULES TAB UI =====
        int modulesX = x0 + pad;
        int modulesY = y0 + 96;
        int modulesRightX = modulesX + 304;
        modulesListX = modulesX;
        modulesListY = modulesY;
        modulesListW = panelW - pad * 2;
        modulesListH = panelH - 112;
        int rowY = modulesY + 26;

        statsSectionBtn = addDrawableChild(new SoupButtonWidget(modulesX, modulesY, 280, 20, statsSectionText(), b -> {
            statsExpanded = !statsExpanded;
            b.setMessage(statsSectionText());
            updateTabVisibility();
        }));

        statsEnableBtn = addDrawableChild(new SoupButtonWidget(modulesX + 14, rowY, 170, 20, statsEnabledText(), b -> {
            StaffHelperState.CONFIG.statsEnabled = !StaffHelperState.CONFIG.statsEnabled;
            StaffHelperState.CONFIG.save();
            b.setMessage(statsEnabledText());
        }));
        rowY += 24;
        statsLayoutBtn = addDrawableChild(new SoupButtonWidget(modulesX + 14, rowY, 170, 20, statsLayoutText(), b -> {
            StaffHelperState.CONFIG.statsHorizontal = !StaffHelperState.CONFIG.statsHorizontal;
            StaffHelperState.CONFIG.save();
            b.setMessage(statsLayoutText());
        }));
        rowY += 24;
        statsRoleBtn = addDrawableChild(new SoupButtonWidget(modulesX + 14, rowY, 170, 20, statsRoleText(), b -> {
            StaffHelperState.CONFIG.statsShowRole = !StaffHelperState.CONFIG.statsShowRole;
            StaffHelperState.CONFIG.save();
            b.setMessage(statsRoleText());
        }));
        rowY += 24;
        statsPingBtn = addDrawableChild(new SoupButtonWidget(modulesX + 14, rowY, 170, 20, statsPingText(), b -> {
            StaffHelperState.CONFIG.statsShowPing = !StaffHelperState.CONFIG.statsShowPing;
            StaffHelperState.CONFIG.save();
            b.setMessage(statsPingText());
        }));
        rowY += 24;
        statsTpsBtn = addDrawableChild(new SoupButtonWidget(modulesX + 14, rowY, 170, 20, statsTpsText(), b -> {
            StaffHelperState.CONFIG.statsShowTps = !StaffHelperState.CONFIG.statsShowTps;
            StaffHelperState.CONFIG.save();
            b.setMessage(statsTpsText());
            updateTabVisibility();
        }));
        rowY += 24;
        statsNowBtn = addDrawableChild(new SoupButtonWidget(modulesX + 34, rowY, 140, 20, statsNowText(), b -> {
            StaffHelperState.CONFIG.statsShowTpsNow = !StaffHelperState.CONFIG.statsShowTpsNow;
            StaffHelperState.CONFIG.save();
            b.setMessage(statsNowText());
        }));
        rowY += 24;
        stats5mBtn = addDrawableChild(new SoupButtonWidget(modulesX + 34, rowY, 140, 20, stats5mText(), b -> {
            StaffHelperState.CONFIG.statsShowTps5m = !StaffHelperState.CONFIG.statsShowTps5m;
            StaffHelperState.CONFIG.save();
            b.setMessage(stats5mText());
        }));
        rowY += 24;
        stats10mBtn = addDrawableChild(new SoupButtonWidget(modulesX + 34, rowY, 140, 20, stats10mText(), b -> {
            StaffHelperState.CONFIG.statsShowTps10m = !StaffHelperState.CONFIG.statsShowTps10m;
            StaffHelperState.CONFIG.save();
            b.setMessage(stats10mText());
        }));
        rowY += 24;
        stats15mBtn = addDrawableChild(new SoupButtonWidget(modulesX + 34, rowY, 140, 20, stats15mText(), b -> {
            StaffHelperState.CONFIG.statsShowTps15m = !StaffHelperState.CONFIG.statsShowTps15m;
            StaffHelperState.CONFIG.save();
            b.setMessage(stats15mText());
        }));
        rowY += 24;

        autoBoxSectionBtn = addDrawableChild(new SoupButtonWidget(modulesRightX, rowY, 280, 20, autoBoxSectionText(), b -> {
            autoBoxExpanded = !autoBoxExpanded;
            b.setMessage(autoBoxSectionText());
            updateTabVisibility();
        }));
        rowY += 24;

        autoBoxBox1Btn = addDrawableChild(new SoupButtonWidget(modulesRightX + 14, rowY, 126, 20, Text.literal("Box#1"), b -> {
            setAutoBoxSelection(1);
        }));
        autoBoxBox2Btn = addDrawableChild(new SoupButtonWidget(modulesRightX + 154, rowY, 126, 20, Text.literal("Box#2"), b -> {
            setAutoBoxSelection(2);
        }));
        refreshAutoBoxButtonsState();
        statsExpandProgress = statsExpanded ? 1.0f : 0.0f;
        autoBoxExpandProgress = autoBoxExpanded ? 1.0f : 0.0f;
        applyModulesLayout();

        // ===== APPEARANCE TAB UI =====
        int appearanceX = x0 + pad;
        int appearanceY = y0 + 126;
        int themePresetW = 141;
        int themePresetGap = 8;
        int row2Y = appearanceY + 30;

        themeBlueBtn = addDrawableChild(new SoupButtonWidget(appearanceX, appearanceY, themePresetW, 20, tr("gui.staffhelper.theme.blue"), b -> setTheme("BLUE")));
        themeRedBtn = addDrawableChild(new SoupButtonWidget(appearanceX + (themePresetW + themePresetGap), appearanceY, themePresetW, 20, tr("gui.staffhelper.theme.red"), b -> setTheme("RED")));
        themePurpleBtn = addDrawableChild(new SoupButtonWidget(appearanceX + (themePresetW + themePresetGap) * 2, appearanceY, themePresetW, 20, tr("gui.staffhelper.theme.purple"), b -> setTheme("PURPLE")));
        themeOrangeBtn = addDrawableChild(new SoupButtonWidget(appearanceX + (themePresetW + themePresetGap) * 3, appearanceY, themePresetW, 20, tr("gui.staffhelper.theme.orange"), b -> setTheme("ORANGE")));
        themeGreenBtn = addDrawableChild(new SoupButtonWidget(appearanceX, row2Y, themePresetW, 20, tr("gui.staffhelper.theme.green"), b -> setTheme("GREEN")));
        themeBrightPurpleBtn = addDrawableChild(new SoupButtonWidget(appearanceX + (themePresetW + themePresetGap), row2Y, themePresetW, 20, tr("gui.staffhelper.theme.bright_purple"), b -> setTheme("BRIGHT_PURPLE")));
        themePinkBtn = addDrawableChild(new SoupButtonWidget(appearanceX + (themePresetW + themePresetGap) * 2, row2Y, themePresetW, 20, tr("gui.staffhelper.theme.pink"), b -> setTheme("PINK")));
        themeCustomBtn = addDrawableChild(new SoupButtonWidget(appearanceX + (themePresetW + themePresetGap) * 3, row2Y, themePresetW, 20, tr("gui.staffhelper.theme.custom"), b -> openCustomThemeDialog()));

        initCustomThemeDialog(x0, y0);
        refreshThemeButtonsState();

        updateTabVisibility();
        setOpeningOffset(OPEN_FROM_BOTTOM_PX);
        super.init();
    }

    private void reloadAfkFieldsFromConfig() {
        pos1X.setText(Integer.toString(StaffHelperState.CONFIG.afkX1));
        pos1Y.setText(Integer.toString(StaffHelperState.CONFIG.afkY1));
        pos1Z.setText(Integer.toString(StaffHelperState.CONFIG.afkZ1));
        pos2X.setText(Integer.toString(StaffHelperState.CONFIG.afkX2));
        pos2Y.setText(Integer.toString(StaffHelperState.CONFIG.afkY2));
        pos2Z.setText(Integer.toString(StaffHelperState.CONFIG.afkZ2));
    }

    private int parseInt(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return fallback; }
    }

    private void initCommandBuilderUi(int x0, int y0, int pad) {
        if (StaffHelperState.CONFIG.commandBuilders == null) {
            StaffHelperState.CONFIG.commandBuilders = new ArrayList<>();
        }

        commandBuilderListX = x0 + pad;
        commandBuilderListY = y0 + 100;
        commandBuilderListW = panelW - pad * 2;
        commandBuilderListH = panelH - 148;

        commandBuilderAddBtn = addDrawableChild(new SoupButtonWidget(
                commandBuilderListX + commandBuilderListW - 28,
                commandBuilderListY + 6,
                20,
                20,
                Text.literal("+"),
                b -> {
                    StaffHelperConfig.CommandBuilderEntry entry = new StaffHelperConfig.CommandBuilderEntry();
                    entry.name = "New command";
                    entry.alias = "/test123 {playername}";
                    entry.execute = "/ban {playername} {time} {reason}";
                    entry.expanded = true;
                    StaffHelperState.CONFIG.commandBuilders.add(entry);
                    StaffHelperState.CONFIG.save();
                    rebuildCommandBuilderWidgets();
                    clampCommandBuilderScroll();
                }
        ));

        rebuildCommandBuilderWidgets();
        clampCommandBuilderScroll();
        applyCommandBuilderLayout();
    }

    private void rebuildCommandBuilderWidgets() {
        for (CommandBuilderUiEntry ui : commandBuilderUiEntries) {
            this.remove(ui.expandButton);
            this.remove(ui.deleteButton);
            this.remove(ui.nameField);
            this.remove(ui.aliasField);
            this.remove(ui.executeField);
            this.remove(ui.timeOptionsField);
            this.remove(ui.reasonOptionsField);
        }
        commandBuilderUiEntries.clear();

        List<StaffHelperConfig.CommandBuilderEntry> list = StaffHelperState.CONFIG.commandBuilders;
        for (StaffHelperConfig.CommandBuilderEntry entry : list) {
            if (entry == null) continue;
            CommandBuilderUiEntry ui = new CommandBuilderUiEntry(entry);

            ui.expandButton = addDrawableChild(new SoupButtonWidget(0, 0, 20, 20, Text.literal(entry.expanded ? "v" : ">"), b -> {
                entry.expanded = !entry.expanded;
                b.setMessage(Text.literal(entry.expanded ? "v" : ">"));
                StaffHelperState.CONFIG.save();
                clampCommandBuilderScroll();
                applyCommandBuilderLayout();
                updateTabVisibility();
            }));

            ui.deleteButton = addDrawableChild(new SoupButtonWidget(0, 0, 20, 20, Text.literal("x"), b -> {
                StaffHelperState.CONFIG.commandBuilders.remove(entry);
                StaffHelperState.CONFIG.save();
                rebuildCommandBuilderWidgets();
                clampCommandBuilderScroll();
                applyCommandBuilderLayout();
                updateTabVisibility();
            }));

            ui.nameField = new CenteredTextFieldWidget(this.textRenderer, 0, 0, 220, 20, Text.literal(""));
            ui.nameField.setDrawsBackground(false);
            ui.nameField.setMaxLength(64);
            ui.nameField.setText(entry.name);
            ui.nameField.setChangedListener(v -> {
                entry.name = v;
                StaffHelperState.CONFIG.save();
            });
            addDrawableChild(ui.nameField);

            ui.aliasField = new CenteredTextFieldWidget(this.textRenderer, 0, 0, 380, 20, Text.literal(""));
            ui.aliasField.setDrawsBackground(false);
            ui.aliasField.setMaxLength(256);
            ui.aliasField.setSuggestion("/test123 {playername} {time} {reason}");
            ui.aliasField.setText(entry.alias);
            ui.aliasField.setChangedListener(v -> {
                entry.alias = v;
                StaffHelperState.CONFIG.save();
                clampCommandBuilderScroll();
                applyCommandBuilderLayout();
                updateTabVisibility();
            });
            addDrawableChild(ui.aliasField);

            ui.executeField = new CenteredTextFieldWidget(this.textRenderer, 0, 0, 380, 20, Text.literal(""));
            ui.executeField.setDrawsBackground(false);
            ui.executeField.setMaxLength(256);
            ui.executeField.setSuggestion("/ban {playername} {time} {reason}");
            ui.executeField.setText(entry.execute);
            ui.executeField.setChangedListener(v -> {
                entry.execute = v;
                StaffHelperState.CONFIG.save();
            });
            addDrawableChild(ui.executeField);

            ui.timeOptionsField = new CenteredTextFieldWidget(this.textRenderer, 0, 0, 380, 20, Text.literal(""));
            ui.timeOptionsField.setDrawsBackground(false);
            ui.timeOptionsField.setMaxLength(256);
            ui.timeOptionsField.setSuggestion("30m, 1h, 2h");
            ui.timeOptionsField.setText(entry.timeOptions);
            ui.timeOptionsField.setChangedListener(v -> {
                entry.timeOptions = v;
                StaffHelperState.CONFIG.save();
            });
            addDrawableChild(ui.timeOptionsField);

            ui.reasonOptionsField = new CenteredTextFieldWidget(this.textRenderer, 0, 0, 380, 20, Text.literal(""));
            ui.reasonOptionsField.setDrawsBackground(false);
            ui.reasonOptionsField.setMaxLength(256);
            ui.reasonOptionsField.setSuggestion("Cheats, Grief, Spam");
            ui.reasonOptionsField.setText(entry.reasonOptions);
            ui.reasonOptionsField.setChangedListener(v -> {
                entry.reasonOptions = v;
                StaffHelperState.CONFIG.save();
            });
            addDrawableChild(ui.reasonOptionsField);

            commandBuilderUiEntries.add(ui);
        }
    }

    private void applyCommandBuilderLayout() {
        int y = commandBuilderListY + 32 - commandBuilderScroll;
        int rowW = commandBuilderListW - 14;

        for (CommandBuilderUiEntry ui : commandBuilderUiEntries) {
            ui.rowX = commandBuilderListX + 6;
            ui.rowY = y;
            ui.rowW = rowW;

            int nameY = y + 6;
            ui.expandButton.setX(ui.rowX + 6);
            ui.expandButton.setY(nameY);

            ui.deleteButton.setX(ui.rowX + ui.rowW - 26);
            ui.deleteButton.setY(nameY);

            ui.nameField.setX(ui.rowX + 32);
            ui.nameField.setY(nameY);
            ui.nameField.setWidth(ui.rowW - 70);
            ui.nameField.setHeight(20);

            int rowH = 32;
            if (ui.entry.expanded) {
                int fieldY = y + 34;
                ui.aliasField.setX(ui.rowX + 78);
                ui.aliasField.setY(fieldY);
                ui.aliasField.setWidth(ui.rowW - 88);
                ui.aliasField.setHeight(20);
                fieldY += 26;

                ui.executeField.setX(ui.rowX + 78);
                ui.executeField.setY(fieldY);
                ui.executeField.setWidth(ui.rowW - 88);
                ui.executeField.setHeight(20);
                fieldY += 26;

                if (ui.entry.hasExecuteToken("{time}")) {
                    ui.timeOptionsField.setX(ui.rowX + 78);
                    ui.timeOptionsField.setY(fieldY);
                    ui.timeOptionsField.setWidth(ui.rowW - 88);
                    ui.timeOptionsField.setHeight(20);
                    fieldY += 26;
                }

                if (ui.entry.hasExecuteToken("{reason}")) {
                    ui.reasonOptionsField.setX(ui.rowX + 78);
                    ui.reasonOptionsField.setY(fieldY);
                    ui.reasonOptionsField.setWidth(ui.rowW - 88);
                    ui.reasonOptionsField.setHeight(20);
                    fieldY += 26;
                }

                rowH = Math.max(32, fieldY - y + 4);
            }

            ui.rowH = rowH;
            y += rowH + 8;
        }
    }

    private int commandBuilderContentHeight() {
        int total = 0;
        for (CommandBuilderUiEntry ui : commandBuilderUiEntries) {
            total += ui.rowH + 8;
        }
        return total;
    }

    private void clampCommandBuilderScroll() {
        applyCommandBuilderLayout();
        int maxScroll = Math.max(0, commandBuilderContentHeight() - (commandBuilderListH - 36));
        if (commandBuilderScroll < 0) commandBuilderScroll = 0;
        if (commandBuilderScroll > maxScroll) commandBuilderScroll = maxScroll;
        applyCommandBuilderLayout();
    }

    private boolean commandBuilderInViewport(CommandBuilderUiEntry ui) {
        int top = commandBuilderViewportTop();
        int bottom = commandBuilderViewportBottom();
        int y1 = ui.rowY;
        int y2 = y1 + ui.rowH;
        return y2 >= top && y1 <= bottom;
    }

    private int commandBuilderViewportTop() {
        return commandBuilderListY + 30;
    }

    private int commandBuilderViewportBottom() {
        return commandBuilderListY + commandBuilderListH - 8;
    }

    private boolean commandBuilderWidgetInViewport(int y, int h) {
        int top = commandBuilderViewportTop();
        int bottom = commandBuilderViewportBottom();
        return (y + h) >= top && y <= bottom;
    }

    private boolean commandBuilderWidgetInViewport(TextFieldWidget tf) {
        if (tf == null) return false;
        return commandBuilderWidgetInViewport(tf.getY(), tf.getHeight());
    }

    private boolean commandBuilderWidgetInViewport(ButtonWidget b) {
        if (b == null) return false;
        return commandBuilderWidgetInViewport(b.getY(), b.getHeight());
    }

    private Text afkFillText() {
        return tr("gui.staffhelper.afk.fill", onOff(StaffHelperState.CONFIG.afkFillEnabled));
    }

    private Text afkOutlineText() {
        return tr("gui.staffhelper.afk.outline", onOff(StaffHelperState.CONFIG.afkOutlineEnabled));
    }

    

    // ===== AFK zone render mode (combined button) =====
    // mode bits: 1 = outline, 2 = fill
    private int getAfkRenderMode() {
        if (StaffHelperState.CONFIG == null) return 0;
        int m = 0;
        if (StaffHelperState.CONFIG.afkOutlineEnabled) m |= 1;
        if (StaffHelperState.CONFIG.afkFillEnabled) m |= 2;
        return m;
    }

    private void setAfkRenderMode(int mode) {
        if (StaffHelperState.CONFIG == null) return;
        boolean outline = (mode & 1) != 0;
        boolean fill = (mode & 2) != 0;
        // ?????????????? ???? ???????????????? fill ?????? outline (?????????? "?????????? ??????????" ?????? ?????????????????????? ??????????????)
        if (fill) outline = true;
        StaffHelperState.CONFIG.afkOutlineEnabled = outline;
        StaffHelperState.CONFIG.afkFillEnabled = fill;
    }

    private Text afkRenderText() {
        int mode = getAfkRenderMode();
        String state = switch (mode) {
            case 1 -> ts("gui.staffhelper.afk.render_state.out");
            case 2 -> ts("gui.staffhelper.afk.render_state.both"); // safety: fill-only treated as BOTH
            case 3 -> ts("gui.staffhelper.afk.render_state.both");
            default -> ts("gui.staffhelper.afk.render_state.off");
        };
        return tr("gui.staffhelper.afk.render", state);
    }

    private Text toggleText() {
        return tr("gui.staffhelper.nicksearch.toggle", onOff(StaffHelperState.CONFIG.nickSearchEnabled));
    }

    private String onOff(boolean v) {
        return ts(v ? "gui.staffhelper.state.on" : "gui.staffhelper.state.off");
    }

    private Text statsSectionText() {
        return tr("gui.staffhelper.stats.section", statsExpanded ? "v" : ">");
    }

    private Text autoBoxSectionText() {
        return tr("gui.staffhelper.autobox.section", autoBoxExpanded ? "v" : ">", autoBoxSelectionName());
    }

    private Text statsEnabledText() { return tr("gui.staffhelper.stats.widget", onOff(StaffHelperState.CONFIG.statsEnabled)); }
    private Text statsLayoutText() { return tr("gui.staffhelper.stats.layout", StaffHelperState.CONFIG.statsHorizontal ? ts("gui.staffhelper.layout.horizontal") : ts("gui.staffhelper.layout.vertical")); }
    private Text statsRoleText() { return tr("gui.staffhelper.stats.show_role", onOff(StaffHelperState.CONFIG.statsShowRole)); }
    private Text statsPingText() { return tr("gui.staffhelper.stats.show_ping", onOff(StaffHelperState.CONFIG.statsShowPing)); }
    private Text statsTpsText() { return tr("gui.staffhelper.stats.show_tps", onOff(StaffHelperState.CONFIG.statsShowTps)); }
    private Text statsNowText() { return tr("gui.staffhelper.stats.tps_now", onOff(StaffHelperState.CONFIG.statsShowTpsNow)); }
    private Text stats5mText() { return tr("gui.staffhelper.stats.tps_5m", onOff(StaffHelperState.CONFIG.statsShowTps5m)); }
    private Text stats10mText() { return tr("gui.staffhelper.stats.tps_10m", onOff(StaffHelperState.CONFIG.statsShowTps10m)); }
    private Text stats15mText() { return tr("gui.staffhelper.stats.tps_15m", onOff(StaffHelperState.CONFIG.statsShowTps15m)); }
    private Text uiSheenText() { return tr("gui.staffhelper.sheen", onOff(StaffHelperState.CONFIG.uiSheenAnimationEnabled)); }

    private String autoBoxSelectionName() {
        int selection = getAutoBoxSelection();
        return switch (selection) {
            case 1 -> ts("gui.staffhelper.autobox.selection.box1");
            case 2 -> ts("gui.staffhelper.autobox.selection.box2");
            default -> ts("gui.staffhelper.autobox.selection.off");
        };
    }

    private int getAutoBoxSelection() {
        if (StaffHelperState.CONFIG == null) return 0;
        int v = StaffHelperState.CONFIG.autoBoxSelection;
        if (v < 0 || v > 2) return 0;
        return v;
    }

    private void setAutoBoxSelection(int value) {
        if (StaffHelperState.CONFIG == null) return;
        StaffHelperState.CONFIG.autoBoxSelection = getAutoBoxSelection() == value ? 0 : value;
        StaffHelperState.CONFIG.save();
        refreshAutoBoxButtonsState();
        if (autoBoxSectionBtn != null) autoBoxSectionBtn.setMessage(autoBoxSectionText());
    }

    private void refreshAutoBoxButtonsState() {
        int selection = getAutoBoxSelection();
        if (autoBoxBox1Btn != null) autoBoxBox1Btn.setLockedPressed(selection == 1);
        if (autoBoxBox2Btn != null) autoBoxBox2Btn.setLockedPressed(selection == 2);
    }

    private void setTheme(String theme) {
        StaffHelperState.CONFIG.uiTheme = theme;
        StaffHelperState.CONFIG.save();
        refreshThemeButtonsState();
    }

    private void initCustomThemeDialog(int x0, int y0) {
        int w = CUSTOM_DIALOG_W;
        int h = CUSTOM_DIALOG_H;
        int dialogX = x0 + (panelW - w) / 2;
        int dialogY = y0 + (panelH - h) / 2;

        int leftX = dialogX + 18;
        int rightX = dialogX + 222;
        int sliderW = 180;
        int topY = dialogY + 72;
        int rowStep = 27;

        customColor1R = addDrawableChild(new SoupIntSliderWidget(leftX, topY, sliderW, 20, ts("gui.staffhelper.custom_theme.slider.color1_r"), 0, 255, 45, v -> onCustomSliderChanged()));
        customColor1G = addDrawableChild(new SoupIntSliderWidget(leftX, topY + rowStep, sliderW, 20, ts("gui.staffhelper.custom_theme.slider.color1_g"), 0, 255, 74, v -> onCustomSliderChanged()));
        customColor1B = addDrawableChild(new SoupIntSliderWidget(leftX, topY + rowStep * 2, sliderW, 20, ts("gui.staffhelper.custom_theme.slider.color1_b"), 0, 255, 115, v -> onCustomSliderChanged()));

        customColor2R = addDrawableChild(new SoupIntSliderWidget(rightX, topY, sliderW, 20, ts("gui.staffhelper.custom_theme.slider.color2_r"), 0, 255, 95, v -> onCustomSliderChanged()));
        customColor2G = addDrawableChild(new SoupIntSliderWidget(rightX, topY + rowStep, sliderW, 20, ts("gui.staffhelper.custom_theme.slider.color2_g"), 0, 255, 143, v -> onCustomSliderChanged()));
        customColor2B = addDrawableChild(new SoupIntSliderWidget(rightX, topY + rowStep * 2, sliderW, 20, ts("gui.staffhelper.custom_theme.slider.color2_b"), 0, 255, 214, v -> onCustomSliderChanged()));

        customThemeApplyBtn = addDrawableChild(new SoupButtonWidget(dialogX + w - 196, dialogY + h - 28, 88, 20, tr("gui.staffhelper.button.apply"), b -> applyCustomThemeDialog()));
        customThemeCancelBtn = addDrawableChild(new SoupButtonWidget(dialogX + w - 100, dialogY + h - 28, 88, 20, tr("gui.staffhelper.button.cancel"), b -> closeCustomThemeDialog(true)));

        setCustomDialogWidgetsVisible(false);
    }

    private void onCustomSliderChanged() {
        if (!customThemeDialogOpen) return;
        int c1 = sliderRgb(customColor1R, customColor1G, customColor1B);
        int c2 = sliderRgb(customColor2R, customColor2G, customColor2B);
        StaffHelperState.CONFIG.uiCustomColor1 = c1;
        StaffHelperState.CONFIG.uiCustomColor2 = c2;
    }

    private void openCustomThemeDialog() {
        customThemeBeforeOpen = currentTheme();
        customColor1BeforeOpen = StaffHelperState.CONFIG.uiCustomColor1;
        customColor2BeforeOpen = StaffHelperState.CONFIG.uiCustomColor2;

        setTheme("CUSTOM");

        int c1 = clampRgb(StaffHelperState.CONFIG.uiCustomColor1);
        int c2 = clampRgb(StaffHelperState.CONFIG.uiCustomColor2);
        customColor1R.setIntValue((c1 >> 16) & 0xFF);
        customColor1G.setIntValue((c1 >> 8) & 0xFF);
        customColor1B.setIntValue(c1 & 0xFF);
        customColor2R.setIntValue((c2 >> 16) & 0xFF);
        customColor2G.setIntValue((c2 >> 8) & 0xFF);
        customColor2B.setIntValue(c2 & 0xFF);
        onCustomSliderChanged();

        customThemeDialogOpen = true;
        setCustomDialogWidgetsVisible(true);
        setDialogBackdropControlsVisible(false);
    }

    private void applyCustomThemeDialog() {
        onCustomSliderChanged();
        StaffHelperState.CONFIG.uiTheme = "CUSTOM";
        StaffHelperState.CONFIG.save();
        closeCustomThemeDialog(false);
    }

    private void closeCustomThemeDialog(boolean restorePrevious) {
        if (restorePrevious) {
            StaffHelperState.CONFIG.uiTheme = customThemeBeforeOpen;
            StaffHelperState.CONFIG.uiCustomColor1 = customColor1BeforeOpen;
            StaffHelperState.CONFIG.uiCustomColor2 = customColor2BeforeOpen;
            StaffHelperState.CONFIG.save();
        }
        customThemeDialogOpen = false;
        setCustomDialogWidgetsVisible(false);
        setDialogBackdropControlsVisible(true);
        updateTabVisibility();
        refreshThemeButtonsState();
    }

    private void setCustomDialogWidgetsVisible(boolean visible) {
        setVisibleActive(customColor1R, visible);
        setVisibleActive(customColor1G, visible);
        setVisibleActive(customColor1B, visible);
        setVisibleActive(customColor2R, visible);
        setVisibleActive(customColor2G, visible);
        setVisibleActive(customColor2B, visible);
        setVisibleActive(customThemeApplyBtn, visible);
        setVisibleActive(customThemeCancelBtn, visible);
    }

    private void setDialogBackdropControlsVisible(boolean visible) {
        setVisibleActive(tabNickBtn, visible);
        setVisibleActive(tabAfkBtn, visible);
        setVisibleActive(tabCommandBuilderBtn, visible);
        setVisibleActive(tabModulesBtn, visible);
        setVisibleActive(tabAppearanceBtn, visible);
        setVisibleActive(closeBtn, visible);
        setVisibleActive(hudEditorBtn, visible);
        setVisibleActive(uiSheenToggleBtn, visible);
        setVisibleActive(themeBlueBtn, visible);
        setVisibleActive(themeRedBtn, visible);
        setVisibleActive(themePurpleBtn, visible);
        setVisibleActive(themeOrangeBtn, visible);
        setVisibleActive(themeGreenBtn, visible);
        setVisibleActive(themeBrightPurpleBtn, visible);
        setVisibleActive(themePinkBtn, visible);
        setVisibleActive(themeCustomBtn, visible);
    }

    private static int sliderRgb(IntSliderWidget r, IntSliderWidget g, IntSliderWidget b) {
        return ((r.getIntValue() & 0xFF) << 16) | ((g.getIntValue() & 0xFF) << 8) | (b.getIntValue() & 0xFF);
    }

    private static int clampRgb(int c) {
        if (c < 0) return 0;
        if (c > 0xFFFFFF) return 0xFFFFFF;
        return c;
    }

    private static String currentTheme() {
        if (StaffHelperState.CONFIG == null || StaffHelperState.CONFIG.uiTheme == null) return "BLUE";
        return StaffHelperState.CONFIG.uiTheme.trim().toUpperCase();
    }

    private void refreshThemeButtonsState() {
        String theme = currentTheme();
        setThemeButtonPressed(themeBlueBtn, "BLUE", theme);
        setThemeButtonPressed(themeRedBtn, "RED", theme);
        setThemeButtonPressed(themePurpleBtn, "PURPLE", theme);
        setThemeButtonPressed(themeOrangeBtn, "ORANGE", theme);
        setThemeButtonPressed(themeGreenBtn, "GREEN", theme);
        setThemeButtonPressed(themeBrightPurpleBtn, "BRIGHT_PURPLE", theme);
        setThemeButtonPressed(themePinkBtn, "PINK", theme);
        setThemeButtonPressed(themeCustomBtn, "CUSTOM", theme);
    }

    private static void setThemeButtonPressed(SoupButtonWidget button, String buttonTheme, String activeTheme) {
        if (button == null) return;
        button.setLockedPressed(buttonTheme.equalsIgnoreCase(activeTheme));
    }

    private void switchTab(Tab newTab) {
        if (newTab == null || newTab == tab) return;
        tab = newTab;
        tabTransitionProgress = 0.0f;
        updateTabVisibility();
    }

    private void refreshTabButtonsState() {
        if (tabNickBtn != null) tabNickBtn.setLockedPressed(tab == Tab.NICKSEARCH);
        if (tabAfkBtn != null) tabAfkBtn.setLockedPressed(tab == Tab.AFKZONE);
        if (tabCommandBuilderBtn != null) tabCommandBuilderBtn.setLockedPressed(tab == Tab.COMMANDBUILDER);
        if (tabModulesBtn != null) tabModulesBtn.setLockedPressed(tab == Tab.MODULES);
        if (tabAppearanceBtn != null) tabAppearanceBtn.setLockedPressed(tab == Tab.APPEARANCE);
    }

    private void setVisibleActive(TextFieldWidget tf, boolean v) {
        if (tf == null) return;
        tf.visible = v;
        tf.active = v;
    }

    private void setVisibleActiveAnimated(TextFieldWidget tf, boolean v) {
        if (tf == null) return;
        float current = animatedTextFieldProgress.getOrDefault(tf, v ? 1.0f : 0.0f);
        animatedTextFieldProgress.putIfAbsent(tf, current);
        animatedTextFieldTargets.put(tf, v);

        if (v) tf.visible = true;
        if (!v && current <= 0.02f) tf.visible = false;
        tf.active = v && current >= 0.92f;
    }

    private void setVisibleActive(IntSliderWidget slider, boolean v) {
        if (slider == null) return;
        slider.visible = v;
        slider.active = v;
    }

    private void setVisibleActive(ButtonWidget b, boolean v) {
        if (b == null) return;
        b.visible = v;
        b.active = v;
        b.setAlpha(v ? 1.0f : 0.0f);
    }

    private void setVisibleActive(ButtonWidget b, boolean visible, boolean active) {
        if (b == null) return;
        b.visible = visible;
        b.active = visible && active;
        b.setAlpha(visible ? 1.0f : 0.0f);
    }

    private void setModuleButtonState(ButtonWidget b, boolean visible, boolean active, float alpha) {
        if (b == null) return;
        float a = Math.max(0.0f, Math.min(1.0f, alpha));
        b.visible = visible;
        b.active = visible && active;
        b.setAlpha(a);
    }

    private void setOpeningOffset(int newOffset) {
        int clamped = Math.max(0, newOffset);
        openingOffsetY = clamped;
        applyWidgetOffsets();
    }

    private int getUiOffsetY() {
        return openingOffsetY;
    }

    private void applyWidgetOffsets() {
        int target = getUiOffsetY();
        int dy = target - appliedWidgetOffsetY;
        if (dy == 0) return;
        appliedWidgetOffsetY = target;
        shiftAllWidgetsBy(dy);
    }

    private void shiftAllWidgetsBy(int dy) {
        if (dy == 0) return;
        for (var child : this.children()) {
            if (child instanceof ClickableWidget clickable) {
                clickable.setY(clickable.getY() + dy);
            }
        }
        modulesListY += dy;
        commandBuilderListY += dy;
    }

    private static float easeOutCubic(float t) {
        float clamped = Math.max(0.0f, Math.min(1.0f, t));
        float inv = 1.0f - clamped;
        return 1.0f - (inv * inv * inv);
    }

    private static int fadeArgb(int argb, float factor) {
        float f = Math.max(0.0f, Math.min(1.0f, factor));
        int a = (argb >>> 24) & 0xFF;
        int outA = Math.max(0, Math.min(255, Math.round(a * f)));
        return (outA << 24) | (argb & 0x00FFFFFF);
    }

    private int tabTextColor(int argb) {
        return fadeArgb(argb, easeOutCubic(tabTransitionProgress));
    }

    private void tickTextFieldAnimations() {
        for (Map.Entry<TextFieldWidget, Float> entry : animatedTextFieldProgress.entrySet()) {
            TextFieldWidget tf = entry.getKey();
            if (tf == null) continue;

            boolean targetVisible = animatedTextFieldTargets.getOrDefault(tf, tf.visible);
            float current = entry.getValue();
            float target = targetVisible ? 1.0f : 0.0f;
            current = current + (target - current) * 0.28f;
            if (Math.abs(target - current) < 0.01f) current = target;
            entry.setValue(current);

            tf.visible = current > 0.02f;
            tf.active = targetVisible && current >= 0.92f;

            int alpha = Math.max(0, Math.min(255, Math.round(255 * current)));
            int textColor = (alpha << 24) | 0xEAEAEA;
            tf.setEditableColor(textColor);
            tf.setUneditableColor(textColor);
        }
    }

    private float getTextFieldAnimation(TextFieldWidget tf) {
        if (tf == null) return 0.0f;
        return animatedTextFieldProgress.getOrDefault(tf, tf.visible ? 1.0f : 0.0f);
    }

    private void drawTextFieldPanel(DrawContext ctx, TextFieldWidget tf, int radius) {
        if (tf == null) return;
        float anim = getTextFieldAnimation(tf);
        if (anim <= 0.02f) return;

        UiChrome.drawPanel(ctx, tf.getX(), tf.getY(), tf.getWidth(), tf.getHeight(), radius, System.currentTimeMillis(), -0.15f, false);
        if (anim < 0.999f) {
            int fadeMask = Math.max(0, Math.min(220, Math.round((1.0f - anim) * 180.0f)));
            ctx.fill(tf.getX() + 1, tf.getY() + 1, tf.getX() + tf.getWidth() - 1, tf.getY() + tf.getHeight() - 1, fadeMask << 24);
        }
    }

    private void updateTabVisibility() {
        refreshTabButtonsState();
        boolean nick = (tab == Tab.NICKSEARCH);
        boolean afk = (tab == Tab.AFKZONE);
        boolean commandBuilder = (tab == Tab.COMMANDBUILDER);
        boolean modules = (tab == Tab.MODULES);
        boolean appearance = (tab == Tab.APPEARANCE);

        // Nick tab
        setVisibleActiveAnimated(addInput, nick);
        setVisibleActive(addBtn, nick);
        setVisibleActiveAnimated(searchInput, nick);
        setVisibleActive(clearBtn, nick);
        setVisibleActive(toggleBtn, nick);

        // AFK tab
        setVisibleActiveAnimated(pos1X, afk);
        setVisibleActiveAnimated(pos1Y, afk);
        setVisibleActiveAnimated(pos1Z, afk);
        setVisibleActiveAnimated(pos2X, afk);
        setVisibleActiveAnimated(pos2Y, afk);
        setVisibleActiveAnimated(pos2Z, afk);
        setVisibleActive(afkRenderToggleBtn, afk);
        
        setVisibleActive(afkApplyBtn, afk);

        setVisibleActiveAnimated(afkIgnoreInput, afk);
        setVisibleActive(afkIgnoreAddBtn, afk);

        // CommandBuilder tab
        setVisibleActive(commandBuilderAddBtn, commandBuilder);
        for (CommandBuilderUiEntry ui : commandBuilderUiEntries) {
            boolean visibleRow = commandBuilder && commandBuilderInViewport(ui);
            setVisibleActive(ui.expandButton, visibleRow && commandBuilderWidgetInViewport(ui.expandButton));
            setVisibleActive(ui.deleteButton, visibleRow && commandBuilderWidgetInViewport(ui.deleteButton));
            setVisibleActive(ui.nameField, visibleRow && commandBuilderWidgetInViewport(ui.nameField));

            boolean showExpanded = visibleRow && ui.entry.expanded;
            setVisibleActive(ui.aliasField, showExpanded && commandBuilderWidgetInViewport(ui.aliasField));
            setVisibleActive(ui.executeField, showExpanded && commandBuilderWidgetInViewport(ui.executeField));
            setVisibleActive(ui.timeOptionsField, showExpanded && ui.entry.hasExecuteToken("{time}") && commandBuilderWidgetInViewport(ui.timeOptionsField));
            setVisibleActive(ui.reasonOptionsField, showExpanded && ui.entry.hasExecuteToken("{reason}") && commandBuilderWidgetInViewport(ui.reasonOptionsField));
        }

        // Modules tab
        setVisibleActive(statsSectionBtn, modules && moduleInViewport(statsSectionBtn));
        float statsVisual = modules ? easeInOutCubic(statsExpandProgress) : 0.0f;
        boolean statsDetailsVisible = modules && statsVisual > 0.08f;
        boolean statsDetailsActive = modules && statsExpanded && statsVisual >= 0.985f;
        float statsAlpha = statsVisual;
        setModuleButtonState(statsEnableBtn, statsDetailsVisible && moduleInViewport(statsEnableBtn), statsDetailsActive && moduleInViewport(statsEnableBtn), statsAlpha);
        setModuleButtonState(statsLayoutBtn, statsDetailsVisible && moduleInViewport(statsLayoutBtn), statsDetailsActive && moduleInViewport(statsLayoutBtn), statsAlpha);
        setModuleButtonState(statsRoleBtn, statsDetailsVisible && moduleInViewport(statsRoleBtn), statsDetailsActive && moduleInViewport(statsRoleBtn), statsAlpha);
        setModuleButtonState(statsPingBtn, statsDetailsVisible && moduleInViewport(statsPingBtn), statsDetailsActive && moduleInViewport(statsPingBtn), statsAlpha);
        setModuleButtonState(statsTpsBtn, statsDetailsVisible && moduleInViewport(statsTpsBtn), statsDetailsActive && moduleInViewport(statsTpsBtn), statsAlpha);

        boolean statsShowTps = StaffHelperState.CONFIG != null && StaffHelperState.CONFIG.statsShowTps;
        boolean showTpsWindows = statsDetailsVisible && statsShowTps;
        boolean activeTpsWindows = statsDetailsActive && statsShowTps;
        setModuleButtonState(statsNowBtn, showTpsWindows && moduleInViewport(statsNowBtn), activeTpsWindows && moduleInViewport(statsNowBtn), statsAlpha);
        setModuleButtonState(stats5mBtn, showTpsWindows && moduleInViewport(stats5mBtn), activeTpsWindows && moduleInViewport(stats5mBtn), statsAlpha);
        setModuleButtonState(stats10mBtn, showTpsWindows && moduleInViewport(stats10mBtn), activeTpsWindows && moduleInViewport(stats10mBtn), statsAlpha);
        setModuleButtonState(stats15mBtn, showTpsWindows && moduleInViewport(stats15mBtn), activeTpsWindows && moduleInViewport(stats15mBtn), statsAlpha);
        setVisibleActive(autoBoxSectionBtn, modules && moduleInViewport(autoBoxSectionBtn));

        float autoBoxVisual = modules ? easeInOutCubic(autoBoxExpandProgress) : 0.0f;
        boolean autoBoxDetailsVisible = modules && autoBoxVisual > 0.08f;
        boolean autoBoxDetailsActive = modules && autoBoxExpanded && autoBoxVisual >= 0.985f;
        float autoBoxAlpha = autoBoxVisual;
        setModuleButtonState(autoBoxBox1Btn, autoBoxDetailsVisible && moduleInViewport(autoBoxBox1Btn), autoBoxDetailsActive && moduleInViewport(autoBoxBox1Btn), autoBoxAlpha);
        setModuleButtonState(autoBoxBox2Btn, autoBoxDetailsVisible && moduleInViewport(autoBoxBox2Btn), autoBoxDetailsActive && moduleInViewport(autoBoxBox2Btn), autoBoxAlpha);

        // Appearance tab
        setVisibleActive(hudEditorBtn, appearance);
        setVisibleActive(uiSheenToggleBtn, appearance);
        setVisibleActive(themeBlueBtn, appearance);
        setVisibleActive(themeRedBtn, appearance);
        setVisibleActive(themePurpleBtn, appearance);
        setVisibleActive(themeOrangeBtn, appearance);
        setVisibleActive(themeGreenBtn, appearance);
        setVisibleActive(themeBrightPurpleBtn, appearance);
        setVisibleActive(themePinkBtn, appearance);
        setVisibleActive(themeCustomBtn, appearance);

        if (!nick) scroll = 0;
        if (!afk) afkIgnoreScroll = 0;
        if (!commandBuilder) commandBuilderScroll = 0;
    }

    @Override
    public void tick() {
        tickTextFieldAnimations();
        statsExpandProgress = animateProgress(statsExpandProgress, statsExpanded ? 1.0f : 0.0f, 0.16f);
        autoBoxExpandProgress = animateProgress(autoBoxExpandProgress, autoBoxExpanded ? 1.0f : 0.0f, 0.16f);
        if (tab == Tab.NICKSEARCH) clampScroll();
        if (tab == Tab.COMMANDBUILDER) {
            clampCommandBuilderScroll();
            applyCommandBuilderLayout();
            updateTabVisibility();
        }
        if (tab == Tab.MODULES) {
            clampModulesScroll();
            applyModulesLayout();
            updateTabVisibility();
        }
        super.tick();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (customThemeDialogOpen) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        // Resize ALL text boxes using mouse wheel when hovered.
        // (Some MC versions do not forward scroll events to TextFieldWidget reliably,
        // so we handle it at the screen level.)
        if (verticalAmount != 0 && tab != Tab.COMMANDBUILDER) {
            for (var child : this.children()) {
                if (child instanceof net.minecraft.client.gui.widget.TextFieldWidget tf) {
                    if (tf.visible && tf.active && tf.isMouseOver(mouseX, mouseY)) {
                        int dw = (verticalAmount > 0) ? 10 : -10;
                        int dh = (verticalAmount > 0) ? 2 : -2;

                        int newW = tf.getWidth() + dw;
                        int newH = tf.getHeight() + dh;

                        newW = Math.max(80, Math.min(620, newW));
                        newH = Math.max(14, Math.min(34, newH));

                        tf.setWidth(newW);
                        tf.setHeight(newH);
                        return true;
                    }
                }
            }
        }

        if (tab == Tab.NICKSEARCH) {
            int x0 = (this.width - panelW) / 2;
            int y0 = (this.height - panelH) / 2 + getUiOffsetY();

            int pad = 16;
            int listX = x0 + pad;
            int listY = y0 + 200;
            int listW = panelW - pad * 2;
            int listH = panelH - 200 - pad - 30;

            if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
                scroll -= (int) Math.signum(verticalAmount) * 18;
                clampScroll();
                return true;
            }

            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        if (tab == Tab.AFKZONE) {
            int x0 = (this.width - panelW) / 2;
            int y0 = (this.height - panelH) / 2 + getUiOffsetY();
            int pad = 16;
            int afkBaseY = y0 + 110;

            int boxX = x0 + pad + 370;
            int boxY = afkBaseY + 68;
            int boxW = 218;
            int boxH = 120;

            if (mouseX >= boxX && mouseX <= boxX + boxW && mouseY >= boxY && mouseY <= boxY + boxH) {
                afkIgnoreScroll -= (int) Math.signum(verticalAmount) * 18;
                clampIgnoreScroll();
                return true;
            }
        }

        if (tab == Tab.COMMANDBUILDER) {
            int x = commandBuilderListX;
            int y = commandBuilderListY + 30;
            int w = commandBuilderListW;
            int h = commandBuilderListH - 36;
            if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                commandBuilderScroll -= (int) Math.signum(verticalAmount) * 18;
                clampCommandBuilderScroll();
                applyCommandBuilderLayout();
                updateTabVisibility();
                return true;
            }
        }

        if (tab == Tab.MODULES) {
            int x = modulesListX;
            int y = modulesListY;
            int w = modulesListW;
            int h = modulesListH;
            if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                modulesScroll -= (int) Math.signum(verticalAmount) * 18;
                clampModulesScroll();
                applyModulesLayout();
                updateTabVisibility();
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (customThemeDialogOpen) {
            boolean handled = super.mouseClicked(mouseX, mouseY, button);
            if (handled) return true;
            if (button == 0 && !isInsideCustomThemeDialog(mouseX, mouseY)) {
                closeCustomThemeDialog(true);
            }
            return true;
        }

        if (tab == Tab.NICKSEARCH && button == 0) {
            int x0 = (this.width - panelW) / 2;
            int y0 = (this.height - panelH) / 2 + getUiOffsetY();

            int pad = 16;
            int listX = x0 + pad;
            int listY = y0 + 200;
            int listW = panelW - pad * 2;
            int listH = panelH - 200 - pad - 30;

            if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
                int rowH = 18;
                int localY = (int) (mouseY - listY) + scroll;
                int idx = localY / rowH;

                List<String> list = filteredList();
                if (idx >= 0 && idx < list.size()) {
                    String value = list.get(idx);

                    int crossSize = 12;
                    int crossPadRight = 8;
                    int crossX = listX + listW - crossPadRight - crossSize;
                    int rowTop = (listY - scroll) + idx * rowH;
                    int crossY = rowTop + (rowH - crossSize) / 2;

                    if (mouseX >= crossX && mouseX <= crossX + crossSize && mouseY >= crossY && mouseY <= crossY + crossSize) {
                        boolean removed = StaffHelperState.CONFIG.nickPatterns.remove(value);
                        if (removed) {
                            StaffHelperState.CONFIG.save();
                            clampScroll();
                        }
                        return true;
                    }
                }
            }
        }

        // AFK ignore list remove (x)
        if (tab == Tab.AFKZONE && button == 0) {
            int x0 = (this.width - panelW) / 2;
            int y0 = (this.height - panelH) / 2 + getUiOffsetY();
            int pad = 16;
            int afkBaseY = y0 + 110;

            int boxX = x0 + pad + 370;
            int boxY = afkBaseY + 68;
            int boxW = 218;
            int boxH = 120;

            if (mouseX >= boxX && mouseX <= boxX + boxW && mouseY >= boxY && mouseY <= boxY + boxH) {
                int rowH = 18;
                int localY = (int) (mouseY - boxY) + afkIgnoreScroll;
                int idx = localY / rowH;

                List<String> list = new ArrayList<>(StaffHelperState.CONFIG.afkIgnoreNicks);
                list.sort(String.CASE_INSENSITIVE_ORDER);

                if (idx >= 0 && idx < list.size()) {
                    String value = list.get(idx);

                    int crossSize = 12;
                    int crossPadRight = 8;
                    int crossX = boxX + boxW - crossPadRight - crossSize;
                    int rowTop = (boxY - afkIgnoreScroll) + idx * rowH;
                    int crossY = rowTop + (rowH - crossSize) / 2;

                    if (mouseX >= crossX && mouseX <= crossX + crossSize && mouseY >= crossY && mouseY <= crossY + crossSize) {
                        boolean removed = StaffHelperState.CONFIG.afkIgnoreNicks.removeIf(s -> s != null && s.equalsIgnoreCase(value));
                        if (removed) {
                            StaffHelperState.CONFIG.save();
                            clampIgnoreScroll();
                        }
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (closing) {
            openProgress = Math.max(0f, openProgress - delta * 0.42f);
            if (openProgress <= 0.01f) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null) mc.setScreen(null);
                return;
            }
        } else {
            openProgress = Math.min(1f, openProgress + delta * 0.26f);
        }

        tabTransitionProgress = Math.min(1.0f, tabTransitionProgress + delta * 0.70f);

        float p = easeOutCubic(openProgress);
        int targetOffset = Math.round((1.0f - p) * OPEN_FROM_BOTTOM_PX);
        setOpeningOffset(targetOffset);

        int overlayAlpha = (int) (92 * p);
        ctx.fill(0, 0, this.width, this.height, (overlayAlpha << 24));

        int x0 = (this.width - panelW) / 2;
        int y0 = (this.height - panelH) / 2 + getUiOffsetY();

        UiChrome.drawPanel(ctx, x0, y0, panelW, panelH, 12, System.currentTimeMillis(), 0.10f, true, false);

        // subtle top separator
        ctx.fill(x0 + 12, y0 + 44, x0 + panelW - 12, y0 + 45, ((int)(0x80 * p) << 24) | 0x2A2F3A);

        int tabsY = y0 + 10;
        int headerY = tabsY + 28;

        ctx.drawText(this.textRenderer, tr("screen.staffhelper.menu.title"), x0 + 16, headerY, 0xFFFFFFFF, false);
        ctx.drawText(this.textRenderer, tr("screen.staffhelper.menu.subtitle"), x0 + 16, headerY + 14, 0xFFBEBEBE, false);

        renderTabContent(ctx, x0, y0, mouseX, mouseY);
        if (customThemeDialogOpen) {
            renderCustomThemeDialog(ctx, x0, y0);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderTabContent(DrawContext ctx, int x0, int y0, int mouseX, int mouseY) {
        int textMain = tabTextColor(0xFFFFFFFF);
        int textSub = tabTextColor(0xFFBEBEBE);
        int textAccent = tabTextColor(0xFF6FB3FF);
        if (tab == Tab.NICKSEARCH) {
            int pad = 16;
            int tabsY = y0 + 10;
            int headerY = tabsY + 28;
            int blockY = headerY + 34;
            int searchRowY = blockY + 14 + 20 + 28;

            ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.nick.add_pattern"), x0 + pad, blockY + 0, textSub, false);
            ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.nick.search"), x0 + pad, searchRowY + 0, textSub, false);

            // Textfield backgrounds (Soup-like)
            if (addInput != null) {
                drawTextFieldPanel(ctx, addInput, 8);
            }

            if (searchInput != null) {
                drawTextFieldPanel(ctx, searchInput, 8);
            }

            ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.nick.patterns_list"), x0 + pad, y0 + 182, textSub, false);

            int listX = x0 + pad;
            int listY = y0 + 200;
            int listW = panelW - pad * 2;
            int listH = panelH - 200 - pad - 30;

            UiChrome.drawPanel(ctx, listX, listY, listW, listH, 10, System.currentTimeMillis(), -0.10f, true);

            List<String> list = filteredList();
            int rowH = 18;

            ctx.enableScissor(listX, listY, listX + listW, listY + listH);

            int startY = listY - scroll;
            for (int i = 0; i < list.size(); i++) {
                int yy = startY + i * rowH;
                if (yy + rowH < listY || yy > listY + listH) continue;

                String value = list.get(i);

                int rowBg = (i % 2 == 0) ? 0x8012141B : 0x80101116;
                ctx.fill(listX + 2, yy, listX + listW - 2, yy + rowH, rowBg);
                ctx.drawText(this.textRenderer, Text.literal(value), listX + 6, yy + 5, 0xFFEAEAEA, false);

                int crossSize = 12;
                int crossPadRight = 8;
                int crossX = listX + listW - crossPadRight - crossSize;
                int crossY = yy + (rowH - crossSize) / 2;

                boolean hoverCross = (mouseX >= crossX && mouseX <= crossX + crossSize && mouseY >= crossY && mouseY <= crossY + crossSize);
                int crossBg = hoverCross ? 0xCC1C202A : 0xB015171E;
                GuiRenderUtils.roundedRect(ctx, crossX, crossY, crossX + crossSize, crossY + crossSize, 4, crossBg);
                GuiRenderUtils.roundedOutline(ctx, crossX, crossY, crossX + crossSize, crossY + crossSize, 4, 1, hoverCross ? 0xFF3A4252 : 0xFF2A2F3A);
                ctx.drawText(this.textRenderer, Text.literal("x"), crossX + 4, crossY + 2, 0xFFFFFFFF, false);
            }

            ctx.disableScissor();

        } else if (tab == Tab.AFKZONE) {
            int pad = 16;
            int baseY = y0 + 110;

            ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.afk.title"), x0 + pad, baseY - 10, textMain, false);
            ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.afk.pos1"), x0 + pad, baseY + 6, textSub, false);
            ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.afk.pos2"), x0 + pad, baseY + 76, textSub, false);

            // Coordinate field backgrounds (derive from widget bounds so resizing works)
            int r = 8;
            // These fields are regular TextFieldWidget in some builds; keep it generic.
            TextFieldWidget[] coords = new TextFieldWidget[]{pos1X, pos1Y, pos1Z, pos2X, pos2Y, pos2Z};
            for (TextFieldWidget tf : coords) {
                if (tf == null) continue;
                drawTextFieldPanel(ctx, tf, r);
            }

            ctx.drawText(this.textRenderer,
                    tr("gui.staffhelper.tab.afk.current",
                            StaffHelperState.CONFIG.afkX1, StaffHelperState.CONFIG.afkY1, StaffHelperState.CONFIG.afkZ1,
                            StaffHelperState.CONFIG.afkX2, StaffHelperState.CONFIG.afkY2, StaffHelperState.CONFIG.afkZ2),
                    x0 + pad, baseY + 210, textSub, false);

            ctx.drawText(this.textRenderer,
                    tr("gui.staffhelper.tab.afk.tip"),
                    x0 + pad, baseY + 230, textAccent, false);

            // ---- ignore list (right column) ----
            int boxX = x0 + pad + 370;
            int boxY = baseY + 68;
            int boxW = 218;
            int boxH = 120;

            ctx.drawText(this.textRenderer,
                    tr("gui.staffhelper.tab.afk.ignored_nicks"),
                    boxX, baseY + 6, textSub, false);
            ctx.drawText(this.textRenderer,
                    tr("gui.staffhelper.tab.afk.ignored_desc"),
                    boxX, baseY + 18, textAccent, false);

            // Ignore input background (derive from widget bounds so resizing works)
            if (afkIgnoreInput != null) {
                drawTextFieldPanel(ctx, afkIgnoreInput, 8);
            }

            UiChrome.drawPanel(ctx, boxX, boxY, boxW, boxH, 10, System.currentTimeMillis(), -0.10f, true);

            List<String> list = new ArrayList<>(StaffHelperState.CONFIG.afkIgnoreNicks);
            list.sort(String.CASE_INSENSITIVE_ORDER);
            int rowH = 18;

            ctx.enableScissor(boxX, boxY, boxX + boxW, boxY + boxH);

            int startY = boxY - afkIgnoreScroll;
            for (int i = 0; i < list.size(); i++) {
                int yy = startY + i * rowH;
                if (yy + rowH < boxY || yy > boxY + boxH) continue;

                String value = list.get(i);

                int rowBg = (i % 2 == 0) ? 0x8012141B : 0x80101116;
                ctx.fill(boxX + 2, yy, boxX + boxW - 2, yy + rowH, rowBg);
                ctx.drawText(this.textRenderer, Text.literal(value), boxX + 6, yy + 5, 0xFFEAEAEA, false);

                int crossSize = 12;
                int crossPadRight = 8;
                int crossX = boxX + boxW - crossPadRight - crossSize;
                int crossY = yy + (rowH - crossSize) / 2;

                boolean hoverCross = (mouseX >= crossX && mouseX <= crossX + crossSize && mouseY >= crossY && mouseY <= crossY + crossSize);
                int crossBg = hoverCross ? 0xCC1C202A : 0xB015171E;
                GuiRenderUtils.roundedRect(ctx, crossX, crossY, crossX + crossSize, crossY + crossSize, 4, crossBg);
                GuiRenderUtils.roundedOutline(ctx, crossX, crossY, crossX + crossSize, crossY + crossSize, 4, 1, hoverCross ? 0xFF3A4252 : 0xFF2A2F3A);
                ctx.drawText(this.textRenderer, Text.literal("x"), crossX + 4, crossY + 2, 0xFFFFFFFF, false);
            }

            ctx.disableScissor();

        } else if (tab == Tab.COMMANDBUILDER) {
            renderCommandBuilderTab(ctx, x0, y0);
        } else if (tab == Tab.MODULES) {
        } else if (tab == Tab.APPEARANCE) {
            int appearanceX = x0 + 16;
            int appearanceY = y0 + 126;
            ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.appearance.theme_presets"), appearanceX, appearanceY - 14, textSub, false);
        }
    }

    private void renderCustomThemeDialog(DrawContext ctx, int panelX, int panelY) {
        int w = CUSTOM_DIALOG_W;
        int h = CUSTOM_DIALOG_H;
        int x = panelX + (panelW - w) / 2;
        int y = panelY + (panelH - h) / 2;

        ctx.fill(0, 0, this.width, this.height, 0x7A000000);
        UiChrome.drawPanel(ctx, x, y, w, h, 10, System.currentTimeMillis(), 0.22f, true, false);
        ctx.fill(x + 12, y + 40, x + w - 12, y + 41, 0x882A2F3A);
        UiChrome.drawPanel(ctx, x + 12, y + 50, 192, 156, 8, System.currentTimeMillis(), -0.08f, false, false);
        UiChrome.drawPanel(ctx, x + 216, y + 50, 192, 156, 8, System.currentTimeMillis(), -0.08f, false, false);

        int c1 = sliderRgb(customColor1R, customColor1G, customColor1B);
        int c2 = sliderRgb(customColor2R, customColor2G, customColor2B);

        ctx.drawText(this.textRenderer, tr("gui.staffhelper.custom_theme.title"), x + 14, y + 12, 0xFFFFFFFF, false);
        ctx.drawText(this.textRenderer, tr("gui.staffhelper.custom_theme.subtitle"), x + 14, y + 26, 0xFFBEBEBE, false);
        ctx.drawText(this.textRenderer, tr("gui.staffhelper.custom_theme.color1"), x + 20, y + 56, 0xFFD7DEE9, false);
        ctx.drawText(this.textRenderer, tr("gui.staffhelper.custom_theme.color2"), x + 224, y + 56, 0xFFD7DEE9, false);

        drawColorSwatch(ctx, x + 18, y + 172, 180, 20, c1, "#" + hexColor(c1));
        drawColorSwatch(ctx, x + 222, y + 172, 180, 20, c2, "#" + hexColor(c2));
    }

    private void drawColorSwatch(DrawContext ctx, int x, int y, int w, int h, int rgb, String label) {
        int fill = 0xFF000000 | clampRgb(rgb);
        GuiRenderUtils.roundedRect(ctx, x, y, x + w, y + h, 6, fill);
        GuiRenderUtils.roundedOutline(ctx, x, y, x + w, y + h, 6, 1, 0xAAFFFFFF);
        ctx.drawText(this.textRenderer, Text.literal(label), x + 6, y + 5, 0xFFFFFFFF, false);
    }

    private static String hexColor(int rgb) {
        int c = clampRgb(rgb);
        String s = Integer.toHexString(c).toUpperCase();
        while (s.length() < 6) s = "0" + s;
        return s;
    }

    private boolean isInsideCustomThemeDialog(double mouseX, double mouseY) {
        int x0 = (this.width - panelW) / 2;
        int y0 = (this.height - panelH) / 2 + getUiOffsetY();
        int w = CUSTOM_DIALOG_W;
        int h = CUSTOM_DIALOG_H;
        int x = x0 + (panelW - w) / 2;
        int y = y0 + (panelH - h) / 2;
        return mouseX >= x && mouseX <= (x + w) && mouseY >= y && mouseY <= (y + h);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (customThemeDialogOpen && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closeCustomThemeDialog(true);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        beginCloseAnimation();
    }

    private void beginCloseAnimation() {
        if (closing) return;
        closing = true;
    }

    private void renderCommandBuilderTab(DrawContext ctx, int x0, int y0) {
        int pad = 16;
        int textMain = tabTextColor(0xFFFFFFFF);
        int textSub = tabTextColor(0xFFBEBEBE);
        int textAccent = tabTextColor(0xFF6FB3FF);
        ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.commandbuilder.title"), x0 + pad, y0 + 78, textMain, false);
        ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.commandbuilder.subtitle"), x0 + pad, y0 + 92, textSub, false);

        UiChrome.drawPanel(ctx, commandBuilderListX, commandBuilderListY, commandBuilderListW, commandBuilderListH, 10, System.currentTimeMillis(), -0.10f, true);

        int listTop = commandBuilderViewportTop();
        int listBottom = commandBuilderViewportBottom();

        ctx.enableScissor(commandBuilderListX + 4, listTop, commandBuilderListX + commandBuilderListW - 4, listBottom);
        for (CommandBuilderUiEntry ui : commandBuilderUiEntries) {
            if (!commandBuilderInViewport(ui)) continue;
            UiChrome.drawPanel(ctx, ui.rowX, ui.rowY, ui.rowW, ui.rowH, 8, System.currentTimeMillis(), -0.15f, false);

            ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.commandbuilder.name"), ui.rowX + 32, ui.rowY - 6 + 12, textSub, false);

            if (ui.entry.expanded) {
                if (commandBuilderWidgetInViewport(ui.aliasField)) {
                    ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.commandbuilder.alias"), ui.rowX + 10, ui.aliasField.getY() + 6, textSub, false);
                }
                if (commandBuilderWidgetInViewport(ui.executeField)) {
                    ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.commandbuilder.execute"), ui.rowX + 10, ui.executeField.getY() + 6, textSub, false);
                }

                if (ui.entry.hasExecuteToken("{time}") && commandBuilderWidgetInViewport(ui.timeOptionsField)) {
                    ctx.drawText(this.textRenderer, Text.literal("{time}"), ui.rowX + 10, ui.timeOptionsField.getY() + 6, textAccent, false);
                }
                if (ui.entry.hasExecuteToken("{reason}") && commandBuilderWidgetInViewport(ui.reasonOptionsField)) {
                    ctx.drawText(this.textRenderer, Text.literal("{reason}"), ui.rowX + 10, ui.reasonOptionsField.getY() + 6, textAccent, false);
                }

                if (commandBuilderWidgetInViewport(ui.aliasField)) {
                    UiChrome.drawPanel(ctx, ui.aliasField.getX(), ui.aliasField.getY(), ui.aliasField.getWidth(), ui.aliasField.getHeight(), 8, System.currentTimeMillis(), -0.15f, false);
                }
                if (commandBuilderWidgetInViewport(ui.executeField)) {
                    UiChrome.drawPanel(ctx, ui.executeField.getX(), ui.executeField.getY(), ui.executeField.getWidth(), ui.executeField.getHeight(), 8, System.currentTimeMillis(), -0.15f, false);
                }
                if (ui.entry.hasExecuteToken("{time}") && commandBuilderWidgetInViewport(ui.timeOptionsField)) {
                    UiChrome.drawPanel(ctx, ui.timeOptionsField.getX(), ui.timeOptionsField.getY(), ui.timeOptionsField.getWidth(), ui.timeOptionsField.getHeight(), 8, System.currentTimeMillis(), -0.15f, false);
                }
                if (ui.entry.hasExecuteToken("{reason}") && commandBuilderWidgetInViewport(ui.reasonOptionsField)) {
                    UiChrome.drawPanel(ctx, ui.reasonOptionsField.getX(), ui.reasonOptionsField.getY(), ui.reasonOptionsField.getWidth(), ui.reasonOptionsField.getHeight(), 8, System.currentTimeMillis(), -0.15f, false);
                }
            }

            if (commandBuilderWidgetInViewport(ui.nameField)) {
                UiChrome.drawPanel(ctx, ui.nameField.getX(), ui.nameField.getY(), ui.nameField.getWidth(), ui.nameField.getHeight(), 8, System.currentTimeMillis(), -0.15f, false);
            }
        }
        ctx.disableScissor();

        if (commandBuilderUiEntries.isEmpty()) {
            ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.commandbuilder.empty"), commandBuilderListX + 10, commandBuilderListY + 40, textSub, false);
        }
    }

    private List<String> filteredList() {
        String q = (searchInput != null) ? searchInput.getText().trim().toLowerCase() : "";
        List<String> all = StaffHelperState.CONFIG.nickPatterns;

        if (q.isEmpty()) return new ArrayList<>(all);

        List<String> out = new ArrayList<>();
        for (String s : all) {
            if (s.toLowerCase().contains(q)) out.add(s);
        }
        return out;
    }

    private void clampScroll() {
        List<String> list = filteredList();
        int rowH = 18;

        int pad = 16;
        int listH = panelH - 200 - pad - 30;

        int contentH = list.size() * rowH;
        int maxScroll = Math.max(0, contentH - listH);

        if (scroll < 0) scroll = 0;
        if (scroll > maxScroll) scroll = maxScroll;
    }

    private void clampIgnoreScroll() {
        if (StaffHelperState.CONFIG == null) return;
        List<String> list = new ArrayList<>(StaffHelperState.CONFIG.afkIgnoreNicks);
        int rowH = 18;

        int contentH = list.size() * rowH;
        int boxH = 120;

        int maxScroll = Math.max(0, contentH - boxH);
        if (afkIgnoreScroll < 0) afkIgnoreScroll = 0;
        if (afkIgnoreScroll > maxScroll) afkIgnoreScroll = maxScroll;
    }

    private void applyModulesLayout() {
        int topY = modulesListY - modulesScroll;
        if (statsSectionBtn != null) statsSectionBtn.setY(topY);

        float statsVisual = easeInOutCubic(statsExpandProgress);
        int statsOffsetY = Math.round((1.0f - statsVisual) * 10.0f);
        int statsRowY = topY + 24 + statsOffsetY;
        if (statsEnableBtn != null) statsEnableBtn.setY(statsRowY);
        statsRowY += 24;
        if (statsLayoutBtn != null) statsLayoutBtn.setY(statsRowY);
        statsRowY += 24;
        if (statsRoleBtn != null) statsRoleBtn.setY(statsRowY);
        statsRowY += 24;
        if (statsPingBtn != null) statsPingBtn.setY(statsRowY);
        statsRowY += 24;
        if (statsTpsBtn != null) statsTpsBtn.setY(statsRowY);
        statsRowY += 24;
        if (statsNowBtn != null) statsNowBtn.setY(statsRowY);
        statsRowY += 24;
        if (stats5mBtn != null) stats5mBtn.setY(statsRowY);
        statsRowY += 24;
        if (stats10mBtn != null) stats10mBtn.setY(statsRowY);
        statsRowY += 24;
        if (stats15mBtn != null) stats15mBtn.setY(statsRowY);

        int autoBoxY = topY;
        if (autoBoxSectionBtn != null) {
            autoBoxSectionBtn.setY(autoBoxY);
            autoBoxSectionBtn.setMessage(autoBoxSectionText());
        }

        float autoBoxVisual = easeInOutCubic(autoBoxExpandProgress);
        int choiceY = autoBoxY + 24 + Math.round((1.0f - autoBoxVisual) * 10.0f);
        if (autoBoxBox1Btn != null) autoBoxBox1Btn.setY(choiceY);
        if (autoBoxBox2Btn != null) autoBoxBox2Btn.setY(choiceY);
    }

    private void clampModulesScroll() {
        float statsVisual = easeInOutCubic(statsExpandProgress);
        float autoBoxVisual = easeInOutCubic(autoBoxExpandProgress);
        float leftColumn = 24.0f + (24.0f * 5.0f * statsVisual);
        if (StaffHelperState.CONFIG != null && StaffHelperState.CONFIG.statsShowTps) {
            leftColumn += 24.0f * 4.0f * statsVisual;
        }
        float rightColumn = 24.0f + (24.0f * autoBoxVisual);
        int content = Math.round(Math.max(leftColumn, rightColumn));
        int maxScroll = Math.max(0, content - modulesListH);
        if (modulesScroll < 0) modulesScroll = 0;
        if (modulesScroll > maxScroll) modulesScroll = maxScroll;
    }

    private static float animateProgress(float current, float target, float speed) {
        float out = current + (target - current) * speed;
        if (Math.abs(target - out) < 0.001f) return target;
        return Math.max(0.0f, Math.min(1.0f, out));
    }

    private static float easeInOutCubic(float t) {
        float clamped = Math.max(0.0f, Math.min(1.0f, t));
        if (clamped < 0.5f) {
            return 4.0f * clamped * clamped * clamped;
        }
        float inv = (-2.0f * clamped) + 2.0f;
        return 1.0f - ((inv * inv * inv) * 0.5f);
    }

    private boolean moduleInViewport(ButtonWidget b) {
        if (b == null) return false;
        int y1 = b.getY();
        int y2 = y1 + b.getHeight();
        int top = modulesListY;
        int bottom = modulesListY + modulesListH;
        return y2 >= top && y1 <= bottom;
    }

    private static class CommandBuilderUiEntry {
        private final StaffHelperConfig.CommandBuilderEntry entry;
        private ButtonWidget expandButton;
        private ButtonWidget deleteButton;
        private TextFieldWidget nameField;
        private TextFieldWidget aliasField;
        private TextFieldWidget executeField;
        private TextFieldWidget timeOptionsField;
        private TextFieldWidget reasonOptionsField;
        private int rowX;
        private int rowY;
        private int rowW;
        private int rowH;

        private CommandBuilderUiEntry(StaffHelperConfig.CommandBuilderEntry entry) {
            this.entry = entry;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // HUD Editor Screen (?????????????? NickSearch + AFK List)
    public static class HudEditorScreen extends Screen {
        private enum DragTarget { NONE, STATS, NICK, AFK_LIST, VANISH }

        private DragTarget dragging = DragTarget.NONE;
        private int offX = 0;
        private int offY = 0;

        public HudEditorScreen() {
            super(StaffHelperMenuScreen.tr("screen.staffhelper.hud_editor.title"));
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

            // 0) STATS widget hitbox
            int xStats = StaffHelperState.CONFIG.statsWidgetX;
            int yStats = StaffHelperState.CONFIG.statsWidgetY;
            int wStats = com.dmsh.staffhelper.feature.StatsHudFeature.getPreviewWidth(MinecraftClient.getInstance());
            int hStats = com.dmsh.staffhelper.feature.StatsHudFeature.getPreviewHeight();

            if (mouseX >= xStats && mouseX <= xStats + wStats && mouseY >= yStats && mouseY <= yStats + hStats) {
                dragging = DragTarget.STATS;
                offX = (int) mouseX - xStats;
                offY = (int) mouseY - yStats;
                return true;
            }

            // 1) NickSearch widget hitbox
            int wNick = NickSearchFeature.getWidgetWidthPreview();
            int hNick = NickSearchFeature.getWidgetHeightPreview(5);
            int xNick = StaffHelperState.CONFIG.nickWidgetX;
            int yNick = StaffHelperState.CONFIG.nickWidgetY;

            if (mouseX >= xNick && mouseX <= xNick + wNick && mouseY >= yNick && mouseY <= yNick + hNick) {
                dragging = DragTarget.NICK;
                offX = (int) mouseX - xNick;
                offY = (int) mouseY - yNick;
                return true;
            }

            // 2) Vanish widget hitbox
            int xVanish = StaffHelperState.CONFIG.vanishWidgetX;
            int yVanish = StaffHelperState.CONFIG.vanishWidgetY;
            int wVanish = com.dmsh.staffhelper.feature.VanishFeature.getPreviewWidth(this.textRenderer);
            int hVanish = com.dmsh.staffhelper.feature.VanishFeature.getPreviewHeight();

            if (mouseX >= xVanish && mouseX <= xVanish + wVanish && mouseY >= yVanish && mouseY <= yVanish + hVanish) {
                dragging = DragTarget.VANISH;
                offX = (int) mouseX - xVanish;
                offY = (int) mouseY - yVanish;
                return true;
            }

            // 3) AFK list hitbox (?????????????????????? ?? ?????????????????? HUD-??????????????)
            int xAfk = StaffHelperState.CONFIG.afkListX;
            int yAfk = StaffHelperState.CONFIG.afkListY;

            float afkScale = clampScale(StaffHelperState.CONFIG.afkBoxScale);
            int pad = Math.max(4, Math.round(6 * afkScale));
            int lineH = Math.max(10, Math.round(10 * afkScale));
            int titleH = Math.max(12, Math.round(12 * afkScale));
            Text title = StaffHelperMenuScreen.tr("gui.staffhelper.hud.afk.title");
            // ?? ???????????? ???????????? ???????????? ????????????, ?????????? ???????????? ???????????????? ?? ??????, ?????? ???????????? ?? ????????
            String l1 = "Nick Test | 1234";
            String l2 = "OtherNick";

            int wAfk = this.textRenderer.getWidth(title);
            wAfk = Math.max(wAfk, this.textRenderer.getWidth(l1));
            wAfk = Math.max(wAfk, this.textRenderer.getWidth(l2));
            wAfk += pad * 2;

            int hAfk = pad + titleH + (2 * lineH) + pad;

            if (mouseX >= xAfk && mouseX <= xAfk + wAfk && mouseY >= yAfk && mouseY <= yAfk + hAfk) {
                dragging = DragTarget.AFK_LIST;
                offX = (int) mouseX - xAfk;
                offY = (int) mouseY - yAfk;
                return true;
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
            if (dragging == DragTarget.NONE) return super.mouseDragged(mouseX, mouseY, button, dx, dy);

            MinecraftClient mc = MinecraftClient.getInstance();
            int screenW = mc.getWindow().getScaledWidth();
            int screenH = mc.getWindow().getScaledHeight();

            if (dragging == DragTarget.NICK) {
                int w = NickSearchFeature.getWidgetWidthPreview();
                int h = NickSearchFeature.getWidgetHeightPreview(5);

                int nx = (int) mouseX - offX;
                int ny = (int) mouseY - offY;

                nx = Math.max(0, Math.min(nx, screenW - w));
                ny = Math.max(0, Math.min(ny, screenH - h));

                StaffHelperState.CONFIG.nickWidgetX = nx;
                StaffHelperState.CONFIG.nickWidgetY = ny;
                return true;
            }

            if (dragging == DragTarget.STATS) {
                int w = com.dmsh.staffhelper.feature.StatsHudFeature.getPreviewWidth(mc);
                int h = com.dmsh.staffhelper.feature.StatsHudFeature.getPreviewHeight();

                int nx = (int) mouseX - offX;
                int ny = (int) mouseY - offY;

                nx = Math.max(0, Math.min(nx, screenW - w));
                ny = Math.max(0, Math.min(ny, screenH - h));

                StaffHelperState.CONFIG.statsWidgetX = nx;
                StaffHelperState.CONFIG.statsWidgetY = ny;
                return true;
            }

            if (dragging == DragTarget.VANISH) {
                int w = com.dmsh.staffhelper.feature.VanishFeature.getPreviewWidth(this.textRenderer);
                int h = com.dmsh.staffhelper.feature.VanishFeature.getPreviewHeight();

                int nx = (int) mouseX - offX;
                int ny = (int) mouseY - offY;

                nx = Math.max(0, Math.min(nx, screenW - w));
                ny = Math.max(0, Math.min(ny, screenH - h));

                StaffHelperState.CONFIG.vanishWidgetX = nx;
                StaffHelperState.CONFIG.vanishWidgetY = ny;
                return true;
            }

            if (dragging == DragTarget.AFK_LIST) {
                float afkScale = clampScale(StaffHelperState.CONFIG.afkBoxScale);
                int pad = Math.max(4, Math.round(6 * afkScale));
                int lineH = Math.max(10, Math.round(10 * afkScale));
                int titleH = Math.max(12, Math.round(12 * afkScale));
                Text title = StaffHelperMenuScreen.tr("gui.staffhelper.hud.afk.title");
                String l1 = "Nick Test | 1234";
                String l2 = "OtherNick";

                int w = this.textRenderer.getWidth(title);
                w = Math.max(w, this.textRenderer.getWidth(l1));
                w = Math.max(w, this.textRenderer.getWidth(l2));
                w += pad * 2;

                int h = pad + titleH + (2 * lineH) + pad;

                int nx = (int) mouseX - offX;
                int ny = (int) mouseY - offY;

                nx = Math.max(0, Math.min(nx, screenW - w));
                ny = Math.max(0, Math.min(ny, screenH - h));

                StaffHelperState.CONFIG.afkListX = nx;
                StaffHelperState.CONFIG.afkListY = ny;
                return true;
            }

            return super.mouseDragged(mouseX, mouseY, button, dx, dy);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            if (verticalAmount == 0 || StaffHelperState.CONFIG == null) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            float step = verticalAmount > 0 ? 0.05f : -0.05f;

            int xStats = StaffHelperState.CONFIG.statsWidgetX;
            int yStats = StaffHelperState.CONFIG.statsWidgetY;
            int wStats = com.dmsh.staffhelper.feature.StatsHudFeature.getPreviewWidth(MinecraftClient.getInstance());
            int hStats = com.dmsh.staffhelper.feature.StatsHudFeature.getPreviewHeight();
            if (mouseX >= xStats && mouseX <= xStats + wStats && mouseY >= yStats && mouseY <= yStats + hStats) {
                StaffHelperState.CONFIG.statsBoxScale = clampScale(StaffHelperState.CONFIG.statsBoxScale + step);
                StaffHelperState.CONFIG.save();
                return true;
            }

            int xNick = StaffHelperState.CONFIG.nickWidgetX;
            int yNick = StaffHelperState.CONFIG.nickWidgetY;
            int wNick = NickSearchFeature.getWidgetWidthPreview();
            int hNick = NickSearchFeature.getWidgetHeightPreview(5);
            if (mouseX >= xNick && mouseX <= xNick + wNick && mouseY >= yNick && mouseY <= yNick + hNick) {
                StaffHelperState.CONFIG.nickBoxScale = clampScale(StaffHelperState.CONFIG.nickBoxScale + step);
                StaffHelperState.CONFIG.save();
                return true;
            }

            int xVanish = StaffHelperState.CONFIG.vanishWidgetX;
            int yVanish = StaffHelperState.CONFIG.vanishWidgetY;
            int wVanish = com.dmsh.staffhelper.feature.VanishFeature.getPreviewWidth(this.textRenderer);
            int hVanish = com.dmsh.staffhelper.feature.VanishFeature.getPreviewHeight();
            if (mouseX >= xVanish && mouseX <= xVanish + wVanish && mouseY >= yVanish && mouseY <= yVanish + hVanish) {
                StaffHelperState.CONFIG.vanishBoxScale = clampScale(StaffHelperState.CONFIG.vanishBoxScale + step);
                StaffHelperState.CONFIG.save();
                return true;
            }

            float afkScale = clampScale(StaffHelperState.CONFIG.afkBoxScale);
            int pad = Math.max(4, Math.round(6 * afkScale));
            int lineH = Math.max(10, Math.round(10 * afkScale));
            int titleH = Math.max(12, Math.round(12 * afkScale));
            Text title = StaffHelperMenuScreen.tr("gui.staffhelper.hud.afk.title");
            String l1 = "Nick Test | 1234";
            String l2 = "OtherNick";
            int wAfk = this.textRenderer.getWidth(title);
            wAfk = Math.max(wAfk, this.textRenderer.getWidth(l1));
            wAfk = Math.max(wAfk, this.textRenderer.getWidth(l2));
            wAfk += pad * 2;
            int hAfk = pad + titleH + (2 * lineH) + pad;
            int xAfk = StaffHelperState.CONFIG.afkListX;
            int yAfk = StaffHelperState.CONFIG.afkListY;
            if (mouseX >= xAfk && mouseX <= xAfk + wAfk && mouseY >= yAfk && mouseY <= yAfk + hAfk) {
                StaffHelperState.CONFIG.afkBoxScale = clampScale(StaffHelperState.CONFIG.afkBoxScale + step);
                StaffHelperState.CONFIG.save();
                return true;
            }

            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (dragging != DragTarget.NONE) {
                dragging = DragTarget.NONE;
                StaffHelperState.CONFIG.save();
                return true;
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            // ??????????: ???? renderBackground -> ?????????? blur crash
            ctx.fill(0, 0, this.width, this.height, 0xAA000000);

            ctx.drawText(this.textRenderer, StaffHelperMenuScreen.tr("screen.staffhelper.hud_editor.tip"), 10, 10, 0xFFFFFFFF, false);

            // STATS preview
            com.dmsh.staffhelper.feature.StatsHudFeature.renderPreview(ctx,
                    StaffHelperState.CONFIG.statsWidgetX,
                    StaffHelperState.CONFIG.statsWidgetY);

            // NickSearch preview
            NickSearchFeature.renderWidgetPreview(ctx,
                    StaffHelperState.CONFIG.nickWidgetX,
                    StaffHelperState.CONFIG.nickWidgetY);

            // Vanish preview
            com.dmsh.staffhelper.feature.VanishFeature.renderPreview(ctx,
                    StaffHelperState.CONFIG.vanishWidgetX,
                    StaffHelperState.CONFIG.vanishWidgetY);

            // AFK list preview (?????????? ?????? ?? NickSearch: ?????????????? + ????????????)
            int x = StaffHelperState.CONFIG.afkListX;
            int y = StaffHelperState.CONFIG.afkListY;

            float afkScale = clampScale(StaffHelperState.CONFIG.afkBoxScale);
            int pad = Math.max(4, Math.round(6 * afkScale));
            int lineH = Math.max(10, Math.round(10 * afkScale));
            int titleH = Math.max(12, Math.round(12 * afkScale));
            Text title = StaffHelperMenuScreen.tr("gui.staffhelper.hud.afk.title");
            String l1 = "Nick Test | 1234";
            String l2 = "OtherNick";

            int w = this.textRenderer.getWidth(title);
            w = Math.max(w, this.textRenderer.getWidth(l1));
            w = Math.max(w, this.textRenderer.getWidth(l2));
            w += pad * 2;

            int h = pad + titleH + (2 * lineH) + pad;

            UiChrome.drawPanel(ctx, x, y, w, h, 8, System.currentTimeMillis());
            ctx.drawText(this.textRenderer, title, x + pad, y + pad, 0xFFFFFFFF, false);

            int yy = y + pad + titleH;
            ctx.drawText(this.textRenderer, Text.literal(l1), x + pad, yy, 0xFFBEBEBE, false);
            yy += lineH;
            ctx.drawText(this.textRenderer, Text.literal(l2), x + pad, yy, 0xFFBEBEBE, false);

            super.render(ctx, mouseX, mouseY, delta);
        }

        @Override
        public boolean shouldPause() {
            return false;
        }

        private static float clampScale(float v) {
            if (Float.isNaN(v)) return 1.0f;
            return Math.max(0.6f, Math.min(2.0f, v));
        }
    }
}


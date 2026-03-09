package com.dmsh.staffhelper.gui;

import com.dmsh.staffhelper.StaffHelperState;
import com.dmsh.staffhelper.config.StaffHelperConfig;
import com.dmsh.staffhelper.feature.NickSearchFeature;
import com.dmsh.staffhelper.gui.util.GuiRenderUtils;
import com.dmsh.staffhelper.gui.util.ModernGui;
import com.dmsh.staffhelper.gui.util.UiChrome;
import com.dmsh.staffhelper.gui.widget.CenteredTextFieldWidget;
import com.dmsh.staffhelper.gui.widget.IconTabButtonWidget;
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
    private static final int CUSTOM_DIALOG_W = 436;
    private static final int CUSTOM_DIALOG_H = 304;
    private static final int SIDEBAR_X_OFFSET = 6;
    private static final int SIDEBAR_W = 30;
    private static final int SIDEBAR_ICON_SIZE = 20;
    private static final int PANEL_BASE_SHIFT_Y = -18;
    private static final float MENU_BASE_SCALE = 0.92f;
    private static final float MENU_MIN_SCALE = 0.74f;
    private static final int MENU_MARGIN = 20;
    private static final int CONTENT_PAD = SIDEBAR_X_OFFSET + SIDEBAR_W + 10;
    private static final int RIGHT_COLUMN_X_OFFSET = 350;
    private static final int RIGHT_COLUMN_W = 218;
    private static final int MODULE_COLUMN_W = 266;
    private static final int MODULE_COLUMN_GAP = 14;
    private static final int APPEARANCE_PRESET_W = 136;
    private static final int APPEARANCE_PRESET_GAP = 6;
    private static final int MENU_TITLE_Y = 14;
    private static final float MENU_TITLE_SCALE = 1.24f;
    private static final int NICK_LIST_TOP_Y = 220;
    private static final int NICK_LIST_BOTTOM_PAD = 44;
    private static final int NICK_PATTERN_LIST_W = RIGHT_COLUMN_X_OFFSET - 8;

    private float openProgress = 0f;
    private int openingOffsetY = 0;
    private static final int OPEN_FROM_BOTTOM_PX = 64;
    private float tabTransitionProgress = 1.0f;
    private int appliedWidgetOffsetY = 0;
    private boolean closing = false;

    private final int panelW = 620;
    private final int panelH = 380;
    private float menuScale = MENU_BASE_SCALE;

    private TextFieldWidget addInput;
    private TextFieldWidget searchInput;
    private TextFieldWidget nickIgnoreInput;
    private ButtonWidget nickIgnoreAddBtn;
    private int nickIgnoreScroll = 0;

    private TextFieldWidget pos1X, pos1Y, pos1Z;
    private TextFieldWidget pos2X, pos2Y, pos2Z;

    private TextFieldWidget afkIgnoreInput;
    private ButtonWidget afkIgnoreAddBtn;
    private int afkIgnoreScroll = 0;

    private IconTabButtonWidget tabNickBtn;
    private IconTabButtonWidget tabAfkBtn;
    private IconTabButtonWidget tabCommandBuilderBtn;
    private IconTabButtonWidget tabModulesBtn;
    private IconTabButtonWidget tabAppearanceBtn;

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

    private TextFieldWidget customHexInput;
    private SoupButtonWidget customStopAddBtn;
    private SoupButtonWidget customStopRemoveBtn;
    private SoupButtonWidget customThemeApplyBtn;
    private SoupButtonWidget customThemeCancelBtn;
    private boolean customThemeDialogOpen = false;
    private boolean customHexEditInternal = false;
    private String customThemeBeforeOpen = "BLUE";
    private int customColor1BeforeOpen = 0x2D4A73;
    private int customColor2BeforeOpen = 0x5F8FD6;
    private final List<StaffHelperConfig.UiGradientStop> customGradientDraft = new ArrayList<>();
    private final List<StaffHelperConfig.UiGradientStop> customGradientBeforeOpen = new ArrayList<>();
    private int customSelectedStopIndex = 0;
    private boolean customDraggingSv = false;
    private boolean customDraggingHue = false;
    private int customDraggingStopIndex = -1;
    private float customPickerHue = 0.0f;
    private float customPickerSat = 0.0f;
    private float customPickerVal = 0.0f;
    private float customGradientAngleDeg = 90.0f;
    private float customGradientAngleBeforeOpen = 90.0f;
    private boolean customDraggingAngle = false;
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
        return UiChrome.uiText(Text.translatable(key, args));
    }

    private static String ts(String key, Object... args) {
        return tr(key, args).getString();
    }

    public StaffHelperMenuScreen() {
        super(tr("screen.staffhelper.menu.title"));
    }

    private void recalculateMenuScale() {
        float fitW = (this.width - (MENU_MARGIN * 2.0f)) / (float) panelW;
        float fitH = (this.height - (MENU_MARGIN * 2.0f)) / (float) panelH;
        float fit = Math.min(1.0f, Math.min(fitW, fitH));
        if (Float.isNaN(fit) || fit <= 0.0f) fit = MENU_BASE_SCALE;
        menuScale = Math.max(MENU_MIN_SCALE, Math.min(MENU_BASE_SCALE, fit));
    }

    private int panelBaseX() {
        return (this.width - panelW) / 2;
    }

    private int panelBaseY() {
        return ((this.height - panelH) / 2) + PANEL_BASE_SHIFT_Y;
    }

    private float panelScaleCenterX() {
        return panelBaseX() + (panelW / 2.0f);
    }

    private float panelScaleCenterY() {
        return panelBaseY() + getUiOffsetY() + (panelH / 2.0f);
    }

    private void pushMenuScale(DrawContext ctx) {
        float scale = Math.max(MENU_MIN_SCALE, Math.min(1.0f, menuScale));
        float cx = panelScaleCenterX();
        float cy = panelScaleCenterY();
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(cx, cy);
        ctx.getMatrices().scale(scale, scale);
        ctx.getMatrices().translate(-cx, -cy);
    }

    private void popMenuScale(DrawContext ctx) {
        ctx.getMatrices().popMatrix();
    }

    private double toMenuSpaceX(double mouseX) {
        float scale = Math.max(MENU_MIN_SCALE, Math.min(1.0f, menuScale));
        if (Math.abs(scale - 1.0f) <= 0.0001f) return mouseX;
        float cx = panelScaleCenterX();
        return cx + ((mouseX - cx) / scale);
    }

    private double toMenuSpaceY(double mouseY) {
        float scale = Math.max(MENU_MIN_SCALE, Math.min(1.0f, menuScale));
        if (Math.abs(scale - 1.0f) <= 0.0001f) return mouseY;
        float cy = panelScaleCenterY();
        return cy + ((mouseY - cy) / scale);
    }

    @Override
    protected void init() {
        recalculateMenuScale();
        int x0 = panelBaseX();
        int y0 = panelBaseY();

        int pad = CONTENT_PAD;
        int sidebarX = x0 + SIDEBAR_X_OFFSET;
        int sidebarTopY = y0 + 58;
        int tabX = sidebarX + ((SIDEBAR_W - SIDEBAR_ICON_SIZE) / 2);
        int tabY = sidebarTopY + 8;
        int tabGap = 7;

        tabNickBtn = addDrawableChild(new IconTabButtonWidget(tabX, tabY, SIDEBAR_ICON_SIZE, SIDEBAR_ICON_SIZE, IconTabButtonWidget.IconType.NICKSEARCH, b -> {
            switchTab(Tab.NICKSEARCH);
        }));
        tabY += SIDEBAR_ICON_SIZE + tabGap;

        tabAfkBtn = addDrawableChild(new IconTabButtonWidget(tabX, tabY, SIDEBAR_ICON_SIZE, SIDEBAR_ICON_SIZE, IconTabButtonWidget.IconType.AFKZONE, b -> {
            switchTab(Tab.AFKZONE);
        }));
        tabY += SIDEBAR_ICON_SIZE + tabGap;

        tabCommandBuilderBtn = addDrawableChild(new IconTabButtonWidget(tabX, tabY, SIDEBAR_ICON_SIZE, SIDEBAR_ICON_SIZE, IconTabButtonWidget.IconType.COMMANDBUILDER, b -> {
            switchTab(Tab.COMMANDBUILDER);
        }));
        tabY += SIDEBAR_ICON_SIZE + tabGap;

        tabModulesBtn = addDrawableChild(new IconTabButtonWidget(tabX, tabY, SIDEBAR_ICON_SIZE, SIDEBAR_ICON_SIZE, IconTabButtonWidget.IconType.MODULES, b -> {
            switchTab(Tab.MODULES);
        }));
        tabY += SIDEBAR_ICON_SIZE + tabGap;

        tabAppearanceBtn = addDrawableChild(new IconTabButtonWidget(tabX, tabY, SIDEBAR_ICON_SIZE, SIDEBAR_ICON_SIZE, IconTabButtonWidget.IconType.APPEARANCE, b -> {
            switchTab(Tab.APPEARANCE);
        }));

        closeBtn = addDrawableChild(new SoupButtonWidget(
                x0 + panelW - 16 - 110,
                y0 + panelH - 16 - 20,
                110,
                20,
                tr("gui.staffhelper.button.close"),
                b -> beginCloseAnimation()
        ));

        int headerY = y0 + 38;
        int blockY = headerY + 34;
        int searchRowY = blockY + 14 + 20 + 28;

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

        addInput = new CenteredTextFieldWidget(this.textRenderer,
                x0 + pad,
                blockY + 14,
                360,
                20,
                Text.literal(""));
        addInput.setMaxLength(64);
        addInput.setDrawsBackground(false);
        addDrawableChild(addInput);

        addBtn = addDrawableChild(new SoupButtonWidget(x0 + pad + 360 + 8, blockY + 14, 90, 20, tr("gui.staffhelper.button.add"), b -> {
            String ptn = addInput.getText().trim();
            if (!ptn.isEmpty()) {
                StaffHelperState.CONFIG.nickPatterns.add(ptn);
                StaffHelperState.CONFIG.save();
                addInput.setText("");
                clampScroll();
            }
        }));

        searchInput = new CenteredTextFieldWidget(this.textRenderer,
                x0 + pad,
                searchRowY + 14,
                458,
                20,
                Text.literal(""));
        searchInput.setMaxLength(64);
        searchInput.setDrawsBackground(false);
        addDrawableChild(searchInput);

        clearBtn = addDrawableChild(new SoupButtonWidget(x0 + pad + 458 + 8, searchRowY + 14, 90, 20, tr("gui.staffhelper.button.clear"), b -> {
            searchInput.setText("");
            scroll = 0;
        }));

        int nickIgnoreX = x0 + pad + RIGHT_COLUMN_X_OFFSET;
        int nickIgnoreInputY = y0 + 196;
        nickIgnoreInput = new CenteredTextFieldWidget(this.textRenderer,
                nickIgnoreX,
                nickIgnoreInputY,
                130,
                20,
                Text.literal(""));
        nickIgnoreInput.setMaxLength(16);
        nickIgnoreInput.setDrawsBackground(false);
        addDrawableChild(nickIgnoreInput);

        nickIgnoreAddBtn = addDrawableChild(new SoupButtonWidget(nickIgnoreX + 130 + 8, nickIgnoreInputY, 80, 20, tr("gui.staffhelper.button.add"), b -> {
            String nick = (nickIgnoreInput != null) ? nickIgnoreInput.getText().trim() : "";
            if (nick.isEmpty()) return;
            if (nick.length() < 3 || nick.length() > 16) return;

            boolean exists = false;
            for (String s : StaffHelperState.CONFIG.nickIgnoreNicks) {
                if (s != null && s.equalsIgnoreCase(nick)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                StaffHelperState.CONFIG.nickIgnoreNicks.add(nick);
                StaffHelperState.CONFIG.save();
            }
            nickIgnoreInput.setText("");
            clampNickIgnoreScroll();
        }));

        hudEditorBtn = addDrawableChild(new SoupButtonWidget(
                x0 + CONTENT_PAD,
                y0 + 78,
                120,
                20,
                tr("gui.staffhelper.button.edit_hud"),
                b -> {
                    if (tab == Tab.APPEARANCE) MinecraftClient.getInstance().setScreen(new HudEditorScreen());
                }
        ));
        uiSheenToggleBtn = addDrawableChild(new SoupButtonWidget(
                x0 + CONTENT_PAD + 120 + 8,
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

        int afkBaseY = y0 + 110;

        pos1X = new CenteredTextFieldWidget(this.textRenderer, x0 + pad, afkBaseY + 20, 90, 20, Text.literal(""));
        pos1Y = new CenteredTextFieldWidget(this.textRenderer, x0 + pad + 100, afkBaseY + 20, 90, 20, Text.literal(""));
        pos1Z = new CenteredTextFieldWidget(this.textRenderer, x0 + pad + 200, afkBaseY + 20, 90, 20, Text.literal(""));
        pos2X = new CenteredTextFieldWidget(this.textRenderer, x0 + pad, afkBaseY + 90, 90, 20, Text.literal(""));
        pos2Y = new CenteredTextFieldWidget(this.textRenderer, x0 + pad + 100, afkBaseY + 90, 90, 20, Text.literal(""));
        pos2Z = new CenteredTextFieldWidget(this.textRenderer, x0 + pad + 200, afkBaseY + 90, 90, 20, Text.literal(""));

        for (TextFieldWidget tf : new TextFieldWidget[]{pos1X, pos1Y, pos1Z, pos2X, pos2Y, pos2Z}) {
            tf.setMaxLength(12);
            tf.setDrawsBackground(false);
            addDrawableChild(tf);
        }

        reloadAfkFieldsFromConfig();

        afkRenderToggleBtn = addDrawableChild(new SoupButtonWidget(x0 + pad, afkBaseY + 140, 124, 22, afkRenderText(), b -> {

            int mode = getAfkRenderMode();
            mode = switch (mode) {
                case 0 -> 1;
                case 1 -> 3;
                case 2 -> 3;
                default -> 0;
            };
            setAfkRenderMode(mode);
            StaffHelperState.CONFIG.save();
            b.setMessage(afkRenderText());
        }));

        afkApplyBtn = addDrawableChild(new SoupButtonWidget(x0 + pad, afkBaseY + 170, 124, 22, tr("gui.staffhelper.button.apply"), b -> {
            StaffHelperState.CONFIG.afkX1 = parseInt(pos1X.getText(), StaffHelperState.CONFIG.afkX1);
            StaffHelperState.CONFIG.afkY1 = parseInt(pos1Y.getText(), StaffHelperState.CONFIG.afkY1);
            StaffHelperState.CONFIG.afkZ1 = parseInt(pos1Z.getText(), StaffHelperState.CONFIG.afkZ1);

            StaffHelperState.CONFIG.afkX2 = parseInt(pos2X.getText(), StaffHelperState.CONFIG.afkX2);
            StaffHelperState.CONFIG.afkY2 = parseInt(pos2Y.getText(), StaffHelperState.CONFIG.afkY2);
            StaffHelperState.CONFIG.afkZ2 = parseInt(pos2Z.getText(), StaffHelperState.CONFIG.afkZ2);

            StaffHelperState.CONFIG.save();
            reloadAfkFieldsFromConfig();
        }));

        int ignoreX = x0 + pad + RIGHT_COLUMN_X_OFFSET;
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

        initCommandBuilderUi(x0, y0, pad);

        int modulesX = x0 + pad;
        int modulesY = y0 + 96;
        int modulesRightX = modulesX + MODULE_COLUMN_W + MODULE_COLUMN_GAP;
        modulesListX = modulesX;
        modulesListY = modulesY;
        modulesListW = panelW - pad * 2;
        modulesListH = panelH - 112;
        int rowY = modulesY + 26;

        statsSectionBtn = addDrawableChild(new SoupButtonWidget(modulesX, modulesY, MODULE_COLUMN_W, 20, statsSectionText(), b -> {
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

        autoBoxSectionBtn = addDrawableChild(new SoupButtonWidget(modulesRightX, rowY, MODULE_COLUMN_W, 20, autoBoxSectionText(), b -> {
            autoBoxExpanded = !autoBoxExpanded;
            b.setMessage(autoBoxSectionText());
            updateTabVisibility();
        }));
        rowY += 24;

        int autoBoxChoiceW = 111;
        autoBoxBox1Btn = addDrawableChild(new SoupButtonWidget(modulesRightX + 14, rowY, autoBoxChoiceW, 20, Text.literal("Box#1"), b -> {
            setAutoBoxSelection(1);
        }));
        autoBoxBox2Btn = addDrawableChild(new SoupButtonWidget(modulesRightX + 14 + autoBoxChoiceW + 8, rowY, autoBoxChoiceW, 20, Text.literal("Box#2"), b -> {
            setAutoBoxSelection(2);
        }));
        refreshAutoBoxButtonsState();
        statsExpandProgress = statsExpanded ? 1.0f : 0.0f;
        autoBoxExpandProgress = autoBoxExpanded ? 1.0f : 0.0f;
        applyModulesLayout();

        int appearanceX = x0 + pad;
        int appearanceY = y0 + 126;
        int themePresetW = APPEARANCE_PRESET_W;
        int themePresetGap = APPEARANCE_PRESET_GAP;
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

        if (fill) outline = true;
        StaffHelperState.CONFIG.afkOutlineEnabled = outline;
        StaffHelperState.CONFIG.afkFillEnabled = fill;
    }

    private Text afkRenderText() {
        int mode = getAfkRenderMode();
        String state = switch (mode) {
            case 1 -> ts("gui.staffhelper.afk.render_state.out");
            case 2 -> ts("gui.staffhelper.afk.render_state.both");
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

        customHexInput = new CenteredTextFieldWidget(this.textRenderer, dialogX + 14, dialogY + 258, 126, 20, Text.empty());
        customHexInput.setDrawsBackground(false);
        customHexInput.setMaxLength(7);
        customHexInput.setSuggestion("#RRGGBB");
        customHexInput.setChangedListener(this::onCustomHexChanged);
        addDrawableChild(customHexInput);

        customStopAddBtn = addDrawableChild(new SoupButtonWidget(
                dialogX + 214,
                dialogY + 220,
                100,
                20,
                tr("gui.staffhelper.custom_theme.add_color"),
                b -> addCustomGradientStop()
        ));
        customStopRemoveBtn = addDrawableChild(new SoupButtonWidget(
                dialogX + 322,
                dialogY + 220,
                100,
                20,
                tr("gui.staffhelper.custom_theme.remove_color"),
                b -> removeCustomGradientStop()
        ));

        customThemeApplyBtn = addDrawableChild(new SoupButtonWidget(dialogX + w - 196, dialogY + h - 28, 88, 20, tr("gui.staffhelper.button.apply"), b -> applyCustomThemeDialog()));
        customThemeCancelBtn = addDrawableChild(new SoupButtonWidget(dialogX + w - 100, dialogY + h - 28, 88, 20, tr("gui.staffhelper.button.cancel"), b -> closeCustomThemeDialog(true)));

        setCustomDialogWidgetsVisible(false);
    }

    private void openCustomThemeDialog() {
        customThemeBeforeOpen = currentTheme();
        customColor1BeforeOpen = StaffHelperState.CONFIG.uiCustomColor1;
        customColor2BeforeOpen = StaffHelperState.CONFIG.uiCustomColor2;
        customGradientAngleBeforeOpen = normalizeAngleDeg(StaffHelperState.CONFIG.uiCustomGradientAngle);
        customGradientBeforeOpen.clear();
        customGradientBeforeOpen.addAll(copyStops(StaffHelperState.CONFIG.uiCustomGradientStops));

        customGradientDraft.clear();
        customGradientDraft.addAll(copyStops(StaffHelperState.CONFIG.uiCustomGradientStops));
        normalizeDraftStops();
        customSelectedStopIndex = Math.max(0, Math.min(customSelectedStopIndex, customGradientDraft.size() - 1));
        customGradientAngleDeg = normalizeAngleDeg(StaffHelperState.CONFIG.uiCustomGradientAngle);
        syncPickerFromSelectedStop();
        syncHexFieldFromSelectedStop();
        applyDraftToConfig();
        StaffHelperState.CONFIG.uiTheme = "CUSTOM";
        refreshThemeButtonsState();

        customThemeDialogOpen = true;
        setCustomDialogWidgetsVisible(true);
        setDialogBackdropControlsVisible(false);
    }

    private void applyCustomThemeDialog() {
        applyDraftToConfig();
        StaffHelperState.CONFIG.uiTheme = "CUSTOM";
        StaffHelperState.CONFIG.save();
        closeCustomThemeDialog(false);
    }

    private void closeCustomThemeDialog(boolean restorePrevious) {
        if (restorePrevious) {
            StaffHelperState.CONFIG.uiTheme = customThemeBeforeOpen;
            StaffHelperState.CONFIG.uiCustomColor1 = customColor1BeforeOpen;
            StaffHelperState.CONFIG.uiCustomColor2 = customColor2BeforeOpen;
            StaffHelperState.CONFIG.uiCustomGradientAngle = customGradientAngleBeforeOpen;
            StaffHelperState.CONFIG.uiCustomGradientStops = copyStops(customGradientBeforeOpen);
            StaffHelperState.CONFIG.save();
        }
        customDraggingSv = false;
        customDraggingHue = false;
        customDraggingAngle = false;
        customDraggingStopIndex = -1;
        customThemeDialogOpen = false;
        setCustomDialogWidgetsVisible(false);
        setDialogBackdropControlsVisible(true);
        updateTabVisibility();
        refreshThemeButtonsState();
    }

    private void setCustomDialogWidgetsVisible(boolean visible) {
        setVisibleActive(customHexInput, visible);
        setVisibleActive(customStopAddBtn, visible);
        setVisibleActive(customStopRemoveBtn, visible);
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

    private void addCustomGradientStop() {
        normalizeDraftStops();
        if (customGradientDraft.size() >= 10) return;

        StaffHelperConfig.UiGradientStop left = selectedStop();
        if (left == null) return;

        int leftIndex = customSelectedStopIndex;
        int rightIndex = Math.min(customGradientDraft.size() - 1, leftIndex + 1);
        StaffHelperConfig.UiGradientStop right = customGradientDraft.get(rightIndex);
        if (right == left && leftIndex > 0) {
            right = left;
            left = customGradientDraft.get(leftIndex - 1);
        }

        float newPos = clamp01((left.position + right.position) * 0.5f);
        if (Math.abs(right.position - left.position) < 0.015f) {
            newPos = clamp01(left.position + 0.06f);
        }
        int newColor = ModernGui.lerpColor(0xFF000000 | clampRgb(left.color), 0xFF000000 | clampRgb(right.color), 0.5f) & 0xFFFFFF;
        StaffHelperConfig.UiGradientStop created = new StaffHelperConfig.UiGradientStop(newPos, newColor);
        customGradientDraft.add(created);
        sortStopsKeepingSelection(created);
        syncPickerFromSelectedStop();
        syncHexFieldFromSelectedStop();
        applyDraftToConfig();
    }

    private void removeCustomGradientStop() {
        normalizeDraftStops();
        if (customGradientDraft.size() <= 2) return;
        if (customSelectedStopIndex < 0 || customSelectedStopIndex >= customGradientDraft.size()) {
            customSelectedStopIndex = 0;
        }
        customGradientDraft.remove(customSelectedStopIndex);
        customSelectedStopIndex = Math.max(0, Math.min(customSelectedStopIndex, customGradientDraft.size() - 1));
        syncPickerFromSelectedStop();
        syncHexFieldFromSelectedStop();
        applyDraftToConfig();
    }

    private void normalizeDraftStops() {
        if (customGradientDraft.isEmpty()) {
            customGradientDraft.add(new StaffHelperConfig.UiGradientStop(0.0f, clampRgb(StaffHelperState.CONFIG.uiCustomColor1)));
            customGradientDraft.add(new StaffHelperConfig.UiGradientStop(1.0f, clampRgb(StaffHelperState.CONFIG.uiCustomColor2)));
        }
        for (StaffHelperConfig.UiGradientStop stop : customGradientDraft) {
            if (stop == null) continue;
            stop.position = clamp01(stop.position);
            stop.color = clampRgb(stop.color);
        }
        customGradientDraft.removeIf(s -> s == null);
        if (customGradientDraft.isEmpty()) {
            customGradientDraft.add(new StaffHelperConfig.UiGradientStop(0.0f, 0x2D4A73));
            customGradientDraft.add(new StaffHelperConfig.UiGradientStop(1.0f, 0x5F8FD6));
        }
        customGradientDraft.sort((a, b) -> Float.compare(a.position, b.position));
        if (customGradientDraft.size() == 1) {
            StaffHelperConfig.UiGradientStop only = customGradientDraft.get(0);
            customGradientDraft.add(new StaffHelperConfig.UiGradientStop(only.position < 0.5f ? 1.0f : 0.0f, only.color));
            customGradientDraft.sort((a, b) -> Float.compare(a.position, b.position));
        }
        customSelectedStopIndex = Math.max(0, Math.min(customSelectedStopIndex, customGradientDraft.size() - 1));
    }

    private void sortStopsKeepingSelection(StaffHelperConfig.UiGradientStop selectedRef) {
        customGradientDraft.sort((a, b) -> Float.compare(a.position, b.position));
        int idx = customGradientDraft.indexOf(selectedRef);
        customSelectedStopIndex = idx < 0 ? 0 : idx;
    }

    private StaffHelperConfig.UiGradientStop selectedStop() {
        if (customGradientDraft.isEmpty()) return null;
        customSelectedStopIndex = Math.max(0, Math.min(customSelectedStopIndex, customGradientDraft.size() - 1));
        return customGradientDraft.get(customSelectedStopIndex);
    }

    private void applyDraftToConfig() {
        normalizeDraftStops();
        StaffHelperState.CONFIG.uiCustomGradientStops = copyStops(customGradientDraft);
        StaffHelperState.CONFIG.uiCustomGradientAngle = normalizeAngleDeg(customGradientAngleDeg);
        StaffHelperConfig.UiGradientStop first = customGradientDraft.get(0);
        StaffHelperConfig.UiGradientStop last = customGradientDraft.get(customGradientDraft.size() - 1);
        StaffHelperState.CONFIG.uiCustomColor1 = clampRgb(first.color);
        StaffHelperState.CONFIG.uiCustomColor2 = clampRgb(last.color);
    }

    private void onCustomHexChanged(String value) {
        if (!customThemeDialogOpen || customHexEditInternal) return;
        String raw = value == null ? "" : value.trim();
        if (raw.isEmpty()) return;
        String hex = raw.startsWith("#") ? raw.substring(1) : raw;
        if (!hex.matches("(?i)[0-9a-f]{6}")) return;

        StaffHelperConfig.UiGradientStop selected = selectedStop();
        if (selected == null) return;
        selected.color = Integer.parseInt(hex, 16) & 0xFFFFFF;
        syncPickerFromSelectedStop();
        applyDraftToConfig();
    }

    private void syncHexFieldFromSelectedStop() {
        StaffHelperConfig.UiGradientStop selected = selectedStop();
        if (selected == null || customHexInput == null) return;
        customHexEditInternal = true;
        customHexInput.setText("#" + hexColor(selected.color));
        customHexEditInternal = false;
    }

    private void syncPickerFromSelectedStop() {
        StaffHelperConfig.UiGradientStop selected = selectedStop();
        if (selected == null) return;
        float[] hsv = rgbToHsv(clampRgb(selected.color));
        customPickerHue = hsv[0];
        customPickerSat = hsv[1];
        customPickerVal = hsv[2];
    }

    private void applyPickerToSelectedStop() {
        StaffHelperConfig.UiGradientStop selected = selectedStop();
        if (selected == null) return;
        selected.color = hsvToRgb(customPickerHue, customPickerSat, customPickerVal);
        syncHexFieldFromSelectedStop();
        applyDraftToConfig();
    }

    private static int hsvToRgb(float h, float s, float v) {
        float hue = clamp01(h);
        float sat = clamp01(s);
        float val = clamp01(v);

        if (sat <= 0.0001f) {
            int gray = Math.round(val * 255.0f);
            return ((gray & 0xFF) << 16) | ((gray & 0xFF) << 8) | (gray & 0xFF);
        }

        float hh = (hue % 1.0f) * 6.0f;
        int sector = (int) Math.floor(hh);
        float frac = hh - sector;
        float p = val * (1.0f - sat);
        float q = val * (1.0f - (sat * frac));
        float t = val * (1.0f - (sat * (1.0f - frac)));

        float r;
        float g;
        float b;
        switch (sector) {
            case 0 -> {
                r = val; g = t; b = p;
            }
            case 1 -> {
                r = q; g = val; b = p;
            }
            case 2 -> {
                r = p; g = val; b = t;
            }
            case 3 -> {
                r = p; g = q; b = val;
            }
            case 4 -> {
                r = t; g = p; b = val;
            }
            default -> {
                r = val; g = p; b = q;
            }
        }
        int ir = Math.round(r * 255.0f);
        int ig = Math.round(g * 255.0f);
        int ib = Math.round(b * 255.0f);
        return ((ir & 0xFF) << 16) | ((ig & 0xFF) << 8) | (ib & 0xFF);
    }

    private static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float h;
        if (delta <= 0.0001f) {
            h = 0.0f;
        } else if (max == r) {
            h = ((g - b) / delta) / 6.0f;
            if (h < 0.0f) h += 1.0f;
        } else if (max == g) {
            h = (((b - r) / delta) + 2.0f) / 6.0f;
        } else {
            h = (((r - g) / delta) + 4.0f) / 6.0f;
        }

        float s = max <= 0.0001f ? 0.0f : (delta / max);
        float v = max;
        return new float[]{clamp01(h), clamp01(s), clamp01(v)};
    }

    private static float clamp01(float value) {
        if (Float.isNaN(value)) return 0.0f;
        if (value < 0.0f) return 0.0f;
        return Math.min(1.0f, value);
    }

    private static float normalizeAngleDeg(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return 90.0f;
        float out = value % 360.0f;
        if (out < 0.0f) out += 360.0f;
        return out;
    }

    private static List<StaffHelperConfig.UiGradientStop> copyStops(List<StaffHelperConfig.UiGradientStop> source) {
        List<StaffHelperConfig.UiGradientStop> out = new ArrayList<>();
        if (source == null) return out;
        for (StaffHelperConfig.UiGradientStop stop : source) {
            if (stop == null) continue;
            out.add(new StaffHelperConfig.UiGradientStop(clamp01(stop.position), clampRgb(stop.color)));
        }
        return out;
    }

    private int customDialogX() {
        int x0 = panelBaseX();
        return x0 + (panelW - CUSTOM_DIALOG_W) / 2;
    }

    private int customDialogY() {
        int y0 = panelBaseY() + getUiOffsetY();
        return y0 + (panelH - CUSTOM_DIALOG_H) / 2;
    }

    private int customPickerX() { return customDialogX() + 14; }
    private int customPickerY() { return customDialogY() + 52; }
    private int customPickerSize() { return 186; }
    private int customHueX() { return customDialogX() + 14; }
    private int customHueY() { return customDialogY() + 242; }
    private int customHueW() { return 186; }
    private int customHueH() { return 12; }
    private int customGradientX() { return customDialogX() + 214; }
    private int customGradientY() { return customDialogY() + 76; }
    private int customGradientW() { return 208; }
    private int customGradientH() { return 16; }
    private int customAngleCenterX() { return customDialogX() + 317; }
    private int customAngleCenterY() { return customDialogY() + 184; }
    private int customAnglePreviewSize() { return 72; }
    private int customAngleRingRadius() { return 24; }
    private int customAngleRingHit() { return 8; }

    private boolean handleCustomThemePointerPressed(double mouseX, double mouseY) {
        normalizeDraftStops();

        int gradientX = customGradientX();
        int gradientY = customGradientY();
        int gradientW = customGradientW();
        int gradientH = customGradientH();

        for (int i = 0; i < customGradientDraft.size(); i++) {
            StaffHelperConfig.UiGradientStop stop = customGradientDraft.get(i);
            int handleX = gradientX + Math.round(clamp01(stop.position) * (gradientW - 1));
            int handleY = gradientY + (gradientH / 2);
            if (Math.abs(mouseX - handleX) <= 7 && Math.abs(mouseY - handleY) <= 8) {
                customSelectedStopIndex = i;
                customDraggingStopIndex = i;
                syncPickerFromSelectedStop();
                syncHexFieldFromSelectedStop();
                return true;
            }
        }

        if (mouseX >= gradientX && mouseX <= gradientX + gradientW && mouseY >= gradientY && mouseY <= gradientY + gradientH) {
            customSelectedStopIndex = nearestStopIndex(mouseX);
            customDraggingStopIndex = customSelectedStopIndex;
            moveSelectedStopToMouseX(mouseX);
            syncHexFieldFromSelectedStop();
            return true;
        }

        int svX = customPickerX();
        int svY = customPickerY();
        int svS = customPickerSize();
        if (mouseX >= svX && mouseX <= svX + svS && mouseY >= svY && mouseY <= svY + svS) {
            customDraggingSv = true;
            updateSvFromMouse(mouseX, mouseY);
            return true;
        }

        int hueX = customHueX();
        int hueY = customHueY();
        int hueW = customHueW();
        int hueH = customHueH();
        if (mouseX >= hueX && mouseX <= hueX + hueW && mouseY >= hueY && mouseY <= hueY + hueH) {
            customDraggingHue = true;
            updateHueFromMouse(mouseX);
            return true;
        }

        if (isInsideCustomAngleRing(mouseX, mouseY)) {
            customDraggingAngle = true;
            updateAngleFromMouse(mouseX, mouseY);
            return true;
        }

        return false;
    }

    private boolean handleCustomThemePointerDragged(double mouseX, double mouseY) {
        if (customDraggingSv) {
            updateSvFromMouse(mouseX, mouseY);
            return true;
        }
        if (customDraggingHue) {
            updateHueFromMouse(mouseX);
            return true;
        }
        if (customDraggingStopIndex >= 0) {
            moveSelectedStopToMouseX(mouseX);
            return true;
        }
        if (customDraggingAngle) {
            updateAngleFromMouse(mouseX, mouseY);
            return true;
        }
        return false;
    }

    private void handleCustomThemePointerReleased() {
        customDraggingSv = false;
        customDraggingHue = false;
        customDraggingAngle = false;
        customDraggingStopIndex = -1;
    }

    private void updateSvFromMouse(double mouseX, double mouseY) {
        int svX = customPickerX();
        int svY = customPickerY();
        int svS = customPickerSize();
        customPickerSat = clamp01((float) ((mouseX - svX) / (double) Math.max(1, svS - 1)));
        customPickerVal = clamp01((float) (1.0 - ((mouseY - svY) / (double) Math.max(1, svS - 1))));
        applyPickerToSelectedStop();
    }

    private void updateHueFromMouse(double mouseX) {
        int hueX = customHueX();
        int hueW = customHueW();
        customPickerHue = clamp01((float) ((mouseX - hueX) / (double) Math.max(1, hueW - 1)));
        applyPickerToSelectedStop();
    }

    private boolean isInsideCustomAngleRing(double mouseX, double mouseY) {
        int cx = customAngleCenterX();
        int cy = customAngleCenterY();
        int radius = customAngleRingRadius();
        int hit = customAngleRingHit();
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt((dx * dx) + (dy * dy));
        return dist >= (radius - hit) && dist <= (radius + hit);
    }

    private void updateAngleFromMouse(double mouseX, double mouseY) {
        int cx = customAngleCenterX();
        int cy = customAngleCenterY();
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        if (Math.abs(dx) <= 0.001 && Math.abs(dy) <= 0.001) return;
        customGradientAngleDeg = normalizeAngleDeg((float) Math.toDegrees(Math.atan2(dy, dx)));
        applyDraftToConfig();
    }

    private int nearestStopIndex(double mouseX) {
        if (customGradientDraft.isEmpty()) return 0;
        int gradientX = customGradientX();
        int gradientW = customGradientW();
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < customGradientDraft.size(); i++) {
            StaffHelperConfig.UiGradientStop stop = customGradientDraft.get(i);
            int x = gradientX + Math.round(clamp01(stop.position) * (gradientW - 1));
            double dist = Math.abs(mouseX - x);
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return best;
    }

    private void moveSelectedStopToMouseX(double mouseX) {
        if (customGradientDraft.isEmpty()) return;
        if (customDraggingStopIndex < 0 || customDraggingStopIndex >= customGradientDraft.size()) return;

        int gradientX = customGradientX();
        int gradientW = customGradientW();
        float pos = clamp01((float) ((mouseX - gradientX) / (double) Math.max(1, gradientW - 1)));

        StaffHelperConfig.UiGradientStop selected = customGradientDraft.get(customDraggingStopIndex);
        selected.position = pos;
        sortStopsKeepingSelection(selected);
        customDraggingStopIndex = customSelectedStopIndex;
        applyDraftToConfig();
    }

    private static int sampleGradientColor(List<StaffHelperConfig.UiGradientStop> stops, float t) {
        if (stops == null || stops.isEmpty()) return 0x2D4A73;
        float pos = clamp01(t);
        StaffHelperConfig.UiGradientStop first = stops.get(0);
        if (pos <= first.position) return clampRgb(first.color);
        StaffHelperConfig.UiGradientStop last = stops.get(stops.size() - 1);
        if (pos >= last.position) return clampRgb(last.color);

        for (int i = 0; i < stops.size() - 1; i++) {
            StaffHelperConfig.UiGradientStop a = stops.get(i);
            StaffHelperConfig.UiGradientStop b = stops.get(i + 1);
            if (pos < a.position || pos > b.position) continue;
            float span = Math.max(0.0001f, b.position - a.position);
            float local = clamp01((pos - a.position) / span);
            return (ModernGui.lerpColor(0xFF000000 | clampRgb(a.color), 0xFF000000 | clampRgb(b.color), local) & 0xFFFFFF);
        }
        return clampRgb(last.color);
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

    private int listRowColor(int index, boolean hovered) {
        int base = (index % 2 == 0) ? 0x66252730 : 0x5522232B;
        if (!hovered) return base;
        return ModernGui.lerpColor(base, UiChrome.accentColor(92), 0.50f);
    }

    private void drawDeleteChip(DrawContext ctx, int x, int y, int size, boolean hovered) {
        int baseFill = 0xA6232730;
        int fill = hovered ? ModernGui.lerpColor(baseFill, UiChrome.accentColor(112), 0.62f) : baseFill;
        int outline = hovered ? UiChrome.accentColor(210) : UiChrome.outlineColor(196);
        int text = hovered ? UiChrome.mainTextColor(255) : UiChrome.mutedTextColor(236);

        GuiRenderUtils.roundedRect(ctx, x, y, x + size, y + size, 4, fill);
        GuiRenderUtils.roundedOutline(ctx, x, y, x + size, y + size, 4, 1, outline);
        UiChrome.drawText(ctx, this.textRenderer, UiChrome.uiLiteral("x"), x + 4, y + 2, text, false);
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

        setVisibleActiveAnimated(addInput, nick);
        setVisibleActive(addBtn, nick);
        setVisibleActiveAnimated(searchInput, nick);
        setVisibleActive(clearBtn, nick);
        setVisibleActiveAnimated(nickIgnoreInput, nick);
        setVisibleActive(nickIgnoreAddBtn, nick);
        setVisibleActive(toggleBtn, nick);

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

        if (!nick) {
            scroll = 0;
            nickIgnoreScroll = 0;
        }
        if (!afk) afkIgnoreScroll = 0;
        if (!commandBuilder) commandBuilderScroll = 0;
    }

    @Override
    public void tick() {
        tickTextFieldAnimations();
        statsExpandProgress = animateProgress(statsExpandProgress, statsExpanded ? 1.0f : 0.0f, 0.16f);
        autoBoxExpandProgress = animateProgress(autoBoxExpandProgress, autoBoxExpanded ? 1.0f : 0.0f, 0.16f);
        if (tab == Tab.NICKSEARCH) {
            clampScroll();
            clampNickIgnoreScroll();
        }
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
        double uiMouseX = toMenuSpaceX(mouseX);
        double uiMouseY = toMenuSpaceY(mouseY);

        if (customThemeDialogOpen) {
            return super.mouseScrolled(uiMouseX, uiMouseY, horizontalAmount, verticalAmount);
        }

        if (verticalAmount != 0 && tab != Tab.COMMANDBUILDER) {
            for (var child : this.children()) {
                if (child instanceof net.minecraft.client.gui.widget.TextFieldWidget tf) {
                    if (tf.visible && tf.active && tf.isMouseOver(uiMouseX, uiMouseY)) {
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
            int x0 = panelBaseX();
            int y0 = panelBaseY() + getUiOffsetY();

            int pad = CONTENT_PAD;
            int patternListX = x0 + pad;
            int patternListY = y0 + NICK_LIST_TOP_Y;
            int patternListW = NICK_PATTERN_LIST_W;
            int patternListH = panelH - NICK_LIST_TOP_Y - NICK_LIST_BOTTOM_PAD;
            if (uiMouseX >= patternListX && uiMouseX <= patternListX + patternListW && uiMouseY >= patternListY && uiMouseY <= patternListY + patternListH) {
                scroll -= (int) Math.signum(verticalAmount) * 18;
                clampScroll();
                return true;
            }

            int ignoreBoxX = x0 + pad + RIGHT_COLUMN_X_OFFSET;
            int ignoreBoxY = y0 + NICK_LIST_TOP_Y;
            int ignoreBoxW = RIGHT_COLUMN_W;
            int ignoreBoxH = patternListH;
            if (uiMouseX >= ignoreBoxX && uiMouseX <= ignoreBoxX + ignoreBoxW && uiMouseY >= ignoreBoxY && uiMouseY <= ignoreBoxY + ignoreBoxH) {
                nickIgnoreScroll -= (int) Math.signum(verticalAmount) * 18;
                clampNickIgnoreScroll();
                return true;
            }

            return super.mouseScrolled(uiMouseX, uiMouseY, horizontalAmount, verticalAmount);
        }

        if (tab == Tab.AFKZONE) {
            int x0 = panelBaseX();
            int y0 = panelBaseY() + getUiOffsetY();
            int pad = CONTENT_PAD;
            int afkBaseY = y0 + 110;

            int boxX = x0 + pad + RIGHT_COLUMN_X_OFFSET;
            int boxY = afkBaseY + 68;
            int boxW = RIGHT_COLUMN_W;
            int boxH = 120;

            if (uiMouseX >= boxX && uiMouseX <= boxX + boxW && uiMouseY >= boxY && uiMouseY <= boxY + boxH) {
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
            if (uiMouseX >= x && uiMouseX <= x + w && uiMouseY >= y && uiMouseY <= y + h) {
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
            if (uiMouseX >= x && uiMouseX <= x + w && uiMouseY >= y && uiMouseY <= y + h) {
                modulesScroll -= (int) Math.signum(verticalAmount) * 18;
                clampModulesScroll();
                applyModulesLayout();
                updateTabVisibility();
                return true;
            }
        }

        return super.mouseScrolled(uiMouseX, uiMouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double uiMouseX = toMenuSpaceX(mouseX);
        double uiMouseY = toMenuSpaceY(mouseY);

        if (customThemeDialogOpen) {
            boolean handled = super.mouseClicked(uiMouseX, uiMouseY, button);
            if (handled) return true;
            if (button == 0) {
                if (handleCustomThemePointerPressed(uiMouseX, uiMouseY)) return true;
                if (!isInsideCustomThemeDialog(uiMouseX, uiMouseY)) {
                    closeCustomThemeDialog(true);
                }
            }
            return true;
        }

        if (tab == Tab.NICKSEARCH && button == 0) {
            int x0 = panelBaseX();
            int y0 = panelBaseY() + getUiOffsetY();

            int pad = CONTENT_PAD;
            int listX = x0 + pad;
            int listY = y0 + NICK_LIST_TOP_Y;
            int listW = NICK_PATTERN_LIST_W;
            int listH = panelH - NICK_LIST_TOP_Y - NICK_LIST_BOTTOM_PAD;

            if (uiMouseX >= listX && uiMouseX <= listX + listW && uiMouseY >= listY && uiMouseY <= listY + listH) {
                int rowH = 18;
                int localY = (int) (uiMouseY - listY) + scroll;
                int idx = localY / rowH;

                List<String> list = filteredList();
                if (idx >= 0 && idx < list.size()) {
                    String value = list.get(idx);

                    int crossSize = 12;
                    int crossPadRight = 8;
                    int crossX = listX + listW - crossPadRight - crossSize;
                    int rowTop = (listY - scroll) + idx * rowH;
                    int crossY = rowTop + (rowH - crossSize) / 2;

                    if (uiMouseX >= crossX && uiMouseX <= crossX + crossSize && uiMouseY >= crossY && uiMouseY <= crossY + crossSize) {
                        boolean removed = StaffHelperState.CONFIG.nickPatterns.remove(value);
                        if (removed) {
                            StaffHelperState.CONFIG.save();
                            clampScroll();
                        }
                        return true;
                    }
                }
            }

            int ignoreBoxX = x0 + pad + RIGHT_COLUMN_X_OFFSET;
            int ignoreBoxY = y0 + NICK_LIST_TOP_Y;
            int ignoreBoxW = RIGHT_COLUMN_W;
            int ignoreBoxH = listH;
            if (uiMouseX >= ignoreBoxX && uiMouseX <= ignoreBoxX + ignoreBoxW && uiMouseY >= ignoreBoxY && uiMouseY <= ignoreBoxY + ignoreBoxH) {
                int rowH = 18;
                int localY = (int) (uiMouseY - ignoreBoxY) + nickIgnoreScroll;
                int idx = localY / rowH;
                List<String> list = sortedNickIgnoreList();
                if (idx >= 0 && idx < list.size()) {
                    String value = list.get(idx);

                    int crossSize = 12;
                    int crossPadRight = 8;
                    int crossX = ignoreBoxX + ignoreBoxW - crossPadRight - crossSize;
                    int rowTop = (ignoreBoxY - nickIgnoreScroll) + idx * rowH;
                    int crossY = rowTop + (rowH - crossSize) / 2;
                    if (uiMouseX >= crossX && uiMouseX <= crossX + crossSize && uiMouseY >= crossY && uiMouseY <= crossY + crossSize) {
                        boolean removed = StaffHelperState.CONFIG.nickIgnoreNicks.removeIf(s -> s != null && s.equalsIgnoreCase(value));
                        if (removed) {
                            StaffHelperState.CONFIG.save();
                            clampNickIgnoreScroll();
                        }
                        return true;
                    }
                }
            }
        }

        if (tab == Tab.AFKZONE && button == 0) {
            int x0 = panelBaseX();
            int y0 = panelBaseY() + getUiOffsetY();
            int pad = CONTENT_PAD;
            int afkBaseY = y0 + 110;

            int boxX = x0 + pad + RIGHT_COLUMN_X_OFFSET;
            int boxY = afkBaseY + 68;
            int boxW = RIGHT_COLUMN_W;
            int boxH = 120;

            if (uiMouseX >= boxX && uiMouseX <= boxX + boxW && uiMouseY >= boxY && uiMouseY <= boxY + boxH) {
                int rowH = 18;
                int localY = (int) (uiMouseY - boxY) + afkIgnoreScroll;
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

                    if (uiMouseX >= crossX && uiMouseX <= crossX + crossSize && uiMouseY >= crossY && uiMouseY <= crossY + crossSize) {
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

        return super.mouseClicked(uiMouseX, uiMouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        double uiMouseX = toMenuSpaceX(mouseX);
        double uiMouseY = toMenuSpaceY(mouseY);
        if (customThemeDialogOpen) {
            if (button == 0 && handleCustomThemePointerDragged(uiMouseX, uiMouseY)) return true;
            return super.mouseDragged(uiMouseX, uiMouseY, button, deltaX, deltaY);
        }
        return super.mouseDragged(uiMouseX, uiMouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double uiMouseX = toMenuSpaceX(mouseX);
        double uiMouseY = toMenuSpaceY(mouseY);
        if (customThemeDialogOpen) {
            if (button == 0) handleCustomThemePointerReleased();
            super.mouseReleased(uiMouseX, uiMouseY, button);
            return true;
        }
        return super.mouseReleased(uiMouseX, uiMouseY, button);
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

        int x0 = panelBaseX();
        int y0 = panelBaseY() + getUiOffsetY();
        int uiMouseX = (int) Math.round(toMenuSpaceX(mouseX));
        int uiMouseY = (int) Math.round(toMenuSpaceY(mouseY));
        long now = System.currentTimeMillis();

        pushMenuScale(ctx);
        UiChrome.drawPanel(ctx, x0, y0, panelW, panelH, 12, now, 0.10f, true, false);

        UiChrome.drawPanel(ctx, x0 + SIDEBAR_X_OFFSET, y0 + 58, SIDEBAR_W, panelH - 72, 8, now, -0.30f, false, false);
        ctx.fill(x0 + SIDEBAR_X_OFFSET + SIDEBAR_W - 1, y0 + 54, x0 + SIDEBAR_X_OFFSET + SIDEBAR_W, y0 + panelH - 12, tabTextColor(UiChrome.outlineColor(106)));

        int titleX = x0 + CONTENT_PAD;
        int titleY = y0 + MENU_TITLE_Y;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(titleX, titleY);
        ctx.getMatrices().scale(MENU_TITLE_SCALE, MENU_TITLE_SCALE);
        ctx.drawText(this.textRenderer, tr("screen.staffhelper.menu.title"), 0, 0, tabTextColor(UiChrome.mainTextColor(252)), false);
        ctx.getMatrices().popMatrix();

        renderTabContent(ctx, x0, y0, uiMouseX, uiMouseY);
        if (customThemeDialogOpen) {
            renderCustomThemeDialog(ctx);
        }

        super.render(ctx, uiMouseX, uiMouseY, delta);
        popMenuScale(ctx);
    }

    private void renderTabContent(DrawContext ctx, int x0, int y0, int mouseX, int mouseY) {
        int textMain = tabTextColor(UiChrome.mainTextColor(255));
        int textSub = tabTextColor(UiChrome.mutedTextColor(246));
        int textAccent = tabTextColor(UiChrome.accentColor(255));
        if (tab == Tab.NICKSEARCH) {
            int pad = CONTENT_PAD;
            int tabsY = y0 + 10;
            int headerY = tabsY + 28;
            int blockY = headerY + 34;
            int searchRowY = blockY + 14 + 20 + 28;

            ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.nick.add_pattern"), x0 + pad, blockY + 0, textSub, false);
            ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.nick.search"), x0 + pad, searchRowY + 0, textSub, false);

            if (addInput != null) {
                drawTextFieldPanel(ctx, addInput, 8);
            }

            if (searchInput != null) {
                drawTextFieldPanel(ctx, searchInput, 8);
            }
            if (nickIgnoreInput != null) {
                drawTextFieldPanel(ctx, nickIgnoreInput, 8);
            }

            ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.nick.patterns_list"), x0 + pad, y0 + 182, textSub, false);
            ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.nick.ignored_nicks"), x0 + pad + RIGHT_COLUMN_X_OFFSET, y0 + 182, textSub, false);

            int listX = x0 + pad;
            int listY = y0 + NICK_LIST_TOP_Y;
            int listW = NICK_PATTERN_LIST_W;
            int listH = panelH - NICK_LIST_TOP_Y - NICK_LIST_BOTTOM_PAD;

            UiChrome.drawPanel(ctx, listX, listY, listW, listH, 10, System.currentTimeMillis(), -0.10f, true);

            List<String> list = filteredList();
            int rowH = 18;

            ctx.enableScissor(listX, listY, listX + listW, listY + listH);

            int startY = listY - scroll;
            for (int i = 0; i < list.size(); i++) {
                int yy = startY + i * rowH;
                if (yy + rowH < listY || yy > listY + listH) continue;

                String value = list.get(i);

                boolean hoverRow = mouseY >= yy && mouseY <= yy + rowH && mouseX >= listX + 2 && mouseX <= listX + listW - 2;
                int rowBg = listRowColor(i, hoverRow);
                GuiRenderUtils.roundedRect(ctx, listX + 2, yy, listX + listW - 2, yy + rowH, 4, rowBg);
                UiChrome.drawText(ctx, this.textRenderer, UiChrome.uiLiteral(value), listX + 6, yy + 5, UiChrome.mainTextColor(246), false);

                int crossSize = 12;
                int crossPadRight = 8;
                int crossX = listX + listW - crossPadRight - crossSize;
                int crossY = yy + (rowH - crossSize) / 2;

                boolean hoverCross = (mouseX >= crossX && mouseX <= crossX + crossSize && mouseY >= crossY && mouseY <= crossY + crossSize);
                drawDeleteChip(ctx, crossX, crossY, crossSize, hoverCross);
            }

            ctx.disableScissor();

            int ignoreBoxX = x0 + pad + RIGHT_COLUMN_X_OFFSET;
            int ignoreBoxY = y0 + NICK_LIST_TOP_Y;
            int ignoreBoxW = RIGHT_COLUMN_W;
            int ignoreBoxH = listH;
            UiChrome.drawPanel(ctx, ignoreBoxX, ignoreBoxY, ignoreBoxW, ignoreBoxH, 10, System.currentTimeMillis(), -0.10f, true);

            List<String> ignoreList = sortedNickIgnoreList();
            int ignoreRowH = 18;

            ctx.enableScissor(ignoreBoxX, ignoreBoxY, ignoreBoxX + ignoreBoxW, ignoreBoxY + ignoreBoxH);
            int ignoreStartY = ignoreBoxY - nickIgnoreScroll;
            for (int i = 0; i < ignoreList.size(); i++) {
                int yy = ignoreStartY + i * ignoreRowH;
                if (yy + ignoreRowH < ignoreBoxY || yy > ignoreBoxY + ignoreBoxH) continue;

                String value = ignoreList.get(i);
                boolean hoverRow = mouseY >= yy && mouseY <= yy + ignoreRowH && mouseX >= ignoreBoxX + 2 && mouseX <= ignoreBoxX + ignoreBoxW - 2;
                int rowBg = listRowColor(i, hoverRow);
                GuiRenderUtils.roundedRect(ctx, ignoreBoxX + 2, yy, ignoreBoxX + ignoreBoxW - 2, yy + ignoreRowH, 4, rowBg);
                UiChrome.drawText(ctx, this.textRenderer, UiChrome.uiLiteral(value), ignoreBoxX + 6, yy + 5, UiChrome.mainTextColor(246), false);

                int crossSize = 12;
                int crossPadRight = 8;
                int crossX = ignoreBoxX + ignoreBoxW - crossPadRight - crossSize;
                int crossY = yy + (ignoreRowH - crossSize) / 2;
                boolean hoverCross = (mouseX >= crossX && mouseX <= crossX + crossSize && mouseY >= crossY && mouseY <= crossY + crossSize);
                drawDeleteChip(ctx, crossX, crossY, crossSize, hoverCross);
            }
            ctx.disableScissor();

        } else if (tab == Tab.AFKZONE) {
            int pad = CONTENT_PAD;
            int baseY = y0 + 110;

            ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.afk.title"), x0 + pad, baseY - 10, textMain, false);
            ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.afk.pos1"), x0 + pad, baseY + 6, textSub, false);
            ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.afk.pos2"), x0 + pad, baseY + 76, textSub, false);

            int r = 8;

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

            int boxX = x0 + pad + RIGHT_COLUMN_X_OFFSET;
            int boxY = baseY + 68;
            int boxW = RIGHT_COLUMN_W;
            int boxH = 120;

            ctx.drawText(this.textRenderer,
                    tr("gui.staffhelper.tab.afk.ignored_nicks"),
                    boxX, baseY + 6, textSub, false);
            ctx.drawText(this.textRenderer,
                    tr("gui.staffhelper.tab.afk.ignored_desc"),
                    boxX, baseY + 18, textAccent, false);

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

                boolean hoverRow = mouseY >= yy && mouseY <= yy + rowH && mouseX >= boxX + 2 && mouseX <= boxX + boxW - 2;
                int rowBg = listRowColor(i, hoverRow);
                GuiRenderUtils.roundedRect(ctx, boxX + 2, yy, boxX + boxW - 2, yy + rowH, 4, rowBg);
                UiChrome.drawText(ctx, this.textRenderer, UiChrome.uiLiteral(value), boxX + 6, yy + 5, UiChrome.mainTextColor(246), false);

                int crossSize = 12;
                int crossPadRight = 8;
                int crossX = boxX + boxW - crossPadRight - crossSize;
                int crossY = yy + (rowH - crossSize) / 2;

                boolean hoverCross = (mouseX >= crossX && mouseX <= crossX + crossSize && mouseY >= crossY && mouseY <= crossY + crossSize);
                drawDeleteChip(ctx, crossX, crossY, crossSize, hoverCross);
            }

            ctx.disableScissor();

        } else if (tab == Tab.COMMANDBUILDER) {
            renderCommandBuilderTab(ctx, x0, y0);
        } else if (tab == Tab.MODULES) {
        } else if (tab == Tab.APPEARANCE) {
            int appearanceX = x0 + CONTENT_PAD;
            int appearanceY = y0 + 126;
            ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.appearance.theme_presets"), appearanceX, appearanceY - 14, textSub, false);
        }
    }

    private void renderCustomThemeDialog(DrawContext ctx) {
        normalizeDraftStops();
        int x = customDialogX();
        int y = customDialogY();
        int w = CUSTOM_DIALOG_W;
        int h = CUSTOM_DIALOG_H;
        long now = System.currentTimeMillis();

        StaffHelperConfig.UiGradientStop selected = selectedStop();
        int selectedColor = selected != null ? clampRgb(selected.color) : hsvToRgb(customPickerHue, customPickerSat, customPickerVal);

        int svX = customPickerX();
        int svY = customPickerY();
        int svS = customPickerSize();
        int hueX = customHueX();
        int hueY = customHueY();
        int hueW = customHueW();
        int hueH = customHueH();
        int gradientX = customGradientX();
        int gradientY = customGradientY();
        int gradientW = customGradientW();
        int gradientH = customGradientH();
        if (customStopAddBtn != null) customStopAddBtn.active = customGradientDraft.size() < 10;
        if (customStopRemoveBtn != null) customStopRemoveBtn.active = customGradientDraft.size() > 2;

        ctx.fill(0, 0, this.width, this.height, 0x7A000000);
        UiChrome.drawPanel(ctx, x, y, w, h, 10, now, 0.22f, true, false);
        ctx.fill(x + 12, y + 40, x + w - 12, y + 41, 0x882A2F3A);
        UiChrome.drawPanel(ctx, x + 12, y + 48, 194, 206, 8, now, -0.06f, false, false);
        UiChrome.drawPanel(ctx, x + 212, y + 48, 210, 192, 8, now, -0.06f, false, false);

        UiChrome.drawText(ctx, this.textRenderer, UiChrome.uiLiteral("Custom Theme"), x + 14, y + 12, UiChrome.mainTextColor(255), false);
        UiChrome.drawText(ctx, this.textRenderer, UiChrome.uiLiteral("HSV picker + editable gradient stops"), x + 14, y + 26, UiChrome.mutedTextColor(232), false);
        UiChrome.drawText(ctx, this.textRenderer, UiChrome.uiLiteral("Color picker"), x + 16, y + 52, UiChrome.mainTextColor(248), false);
        UiChrome.drawText(ctx, this.textRenderer, UiChrome.uiLiteral("Gradient stops"), x + 216, y + 52, UiChrome.mainTextColor(248), false);

        int hueColor = 0xFF000000 | hsvToRgb(customPickerHue, 1.0f, 1.0f);
        GuiRenderUtils.roundedRect(ctx, svX, svY, svX + svS, svY + svS, 8, hueColor);
        for (int i = 0; i < svS; i++) {
            float t = i / (float) Math.max(1, svS - 1);
            int alpha = Math.round((1.0f - t) * 255.0f);
            ctx.fill(svX + i, svY, svX + i + 1, svY + svS, (alpha << 24) | 0x00FFFFFF);
        }
        for (int i = 0; i < svS; i++) {
            float t = i / (float) Math.max(1, svS - 1);
            int alpha = Math.round(t * 255.0f);
            ctx.fill(svX, svY + i, svX + svS, svY + i + 1, (alpha << 24));
        }
        GuiRenderUtils.roundedOutline(ctx, svX, svY, svX + svS, svY + svS, 8, 1, UiChrome.outlineColor(180));

        int svMarkerX = svX + Math.round(customPickerSat * (svS - 1));
        int svMarkerY = svY + Math.round((1.0f - customPickerVal) * (svS - 1));
        GuiRenderUtils.roundedOutline(ctx, svMarkerX - 5, svMarkerY - 5, svMarkerX + 5, svMarkerY + 5, 5, 1, UiChrome.mainTextColor(255));

        for (int i = 0; i < hueW; i++) {
            float t = i / (float) Math.max(1, hueW - 1);
            int rgb = hsvToRgb(t, 1.0f, 1.0f);
            ctx.fill(hueX + i, hueY, hueX + i + 1, hueY + hueH, 0xFF000000 | rgb);
        }
        GuiRenderUtils.roundedOutline(ctx, hueX, hueY, hueX + hueW, hueY + hueH, 4, 1, UiChrome.outlineColor(180));
        int hueMarkerX = hueX + Math.round(customPickerHue * (hueW - 1));
        GuiRenderUtils.roundedRect(ctx, hueMarkerX - 1, hueY - 2, hueMarkerX + 2, hueY + hueH + 2, 1, UiChrome.mainTextColor(255));

        drawTextFieldPanel(ctx, customHexInput, 6);
        drawColorSwatch(ctx, x + 146, y + 258, 58, 20, selectedColor, "");

        for (int i = 0; i < gradientW; i++) {
            float t = i / (float) Math.max(1, gradientW - 1);
            int rgb = sampleGradientColor(customGradientDraft, t);
            ctx.fill(gradientX + i, gradientY, gradientX + i + 1, gradientY + gradientH, 0xFF000000 | rgb);
        }
        GuiRenderUtils.roundedOutline(ctx, gradientX, gradientY, gradientX + gradientW, gradientY + gradientH, 5, 1, UiChrome.outlineColor(186));

        for (int i = 0; i < customGradientDraft.size(); i++) {
            StaffHelperConfig.UiGradientStop stop = customGradientDraft.get(i);
            int handleX = gradientX + Math.round(clamp01(stop.position) * (gradientW - 1));
            int handleY = gradientY + (gradientH / 2);
            int size = (i == customSelectedStopIndex) ? 10 : 8;
            int fill = 0xFF000000 | clampRgb(stop.color);
            int border = (i == customSelectedStopIndex) ? UiChrome.accentColor(255) : UiChrome.outlineColor(204);
            GuiRenderUtils.roundedRect(ctx, handleX - (size / 2), handleY - (size / 2), handleX + (size / 2), handleY + (size / 2), 3, fill);
            GuiRenderUtils.roundedOutline(ctx, handleX - (size / 2), handleY - (size / 2), handleX + (size / 2), handleY + (size / 2), 3, 1, border);
        }

        String selectedHex = "#" + hexColor(selectedColor);
        int selectedPos = selected != null ? Math.round(clamp01(selected.position) * 100.0f) : 0;
        UiChrome.drawText(ctx, this.textRenderer, UiChrome.uiLiteral("Selected: " + selectedHex), x + 216, y + 102, UiChrome.mainTextColor(246), false);
        UiChrome.drawText(ctx, this.textRenderer, UiChrome.uiLiteral("Position: " + selectedPos + "%"), x + 216, y + 116, UiChrome.mutedTextColor(236), false);
        UiChrome.drawText(ctx, this.textRenderer, UiChrome.uiLiteral("Stops: " + customGradientDraft.size()), x + 216, y + 130, UiChrome.mutedTextColor(236), false);
        UiChrome.drawText(ctx, this.textRenderer, UiChrome.uiLiteral("Drag markers on bar to reorder blend"), x + 216, y + 152, UiChrome.accentColor(230), false);
    }

    private void drawColorSwatch(DrawContext ctx, int x, int y, int w, int h, int rgb, String label) {
        int fill = 0xFF000000 | clampRgb(rgb);
        GuiRenderUtils.roundedRect(ctx, x, y, x + w, y + h, 6, fill);
        GuiRenderUtils.roundedOutline(ctx, x, y, x + w, y + h, 6, 1, UiChrome.outlineColor(192));
        if (label != null && !label.isEmpty()) {
            UiChrome.drawText(ctx, this.textRenderer, UiChrome.uiLiteral(label), x + 6, y + 5, UiChrome.mainTextColor(255), false);
        }
    }

    private static String hexColor(int rgb) {
        int c = clampRgb(rgb);
        String s = Integer.toHexString(c).toUpperCase();
        while (s.length() < 6) s = "0" + s;
        return s;
    }

    private boolean isInsideCustomThemeDialog(double mouseX, double mouseY) {
        int x = customDialogX();
        int y = customDialogY();
        int w = CUSTOM_DIALOG_W;
        int h = CUSTOM_DIALOG_H;
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
        int pad = CONTENT_PAD;
        int textMain = tabTextColor(UiChrome.mainTextColor(255));
        int textSub = tabTextColor(UiChrome.mutedTextColor(246));
        int textAccent = tabTextColor(UiChrome.accentColor(255));
        ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.commandbuilder.title"), x0 + pad, y0 + 74, textMain, false);
        ctx.drawText(this.textRenderer, tr("gui.staffhelper.tab.commandbuilder.subtitle"), x0 + pad, y0 + 88, textSub, false);

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
                    UiChrome.drawText(ctx, this.textRenderer, UiChrome.uiLiteral("{time}"), ui.rowX + 10, ui.timeOptionsField.getY() + 6, textAccent, false);
                }
                if (ui.entry.hasExecuteToken("{reason}") && commandBuilderWidgetInViewport(ui.reasonOptionsField)) {
                    UiChrome.drawText(ctx, this.textRenderer, UiChrome.uiLiteral("{reason}"), ui.rowX + 10, ui.reasonOptionsField.getY() + 6, textAccent, false);
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

    private List<String> sortedNickIgnoreList() {
        List<String> out = new ArrayList<>();
        if (StaffHelperState.CONFIG == null || StaffHelperState.CONFIG.nickIgnoreNicks == null) return out;
        out.addAll(StaffHelperState.CONFIG.nickIgnoreNicks);
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    private void clampScroll() {
        List<String> list = filteredList();
        int rowH = 18;
        int listH = panelH - NICK_LIST_TOP_Y - NICK_LIST_BOTTOM_PAD;

        int contentH = list.size() * rowH;
        int maxScroll = Math.max(0, contentH - listH);

        if (scroll < 0) scroll = 0;
        if (scroll > maxScroll) scroll = maxScroll;
    }

    private void clampNickIgnoreScroll() {
        List<String> list = sortedNickIgnoreList();
        int rowH = 18;
        int boxH = panelH - NICK_LIST_TOP_Y - NICK_LIST_BOTTOM_PAD;
        int contentH = list.size() * rowH;
        int maxScroll = Math.max(0, contentH - boxH);
        if (nickIgnoreScroll < 0) nickIgnoreScroll = 0;
        if (nickIgnoreScroll > maxScroll) nickIgnoreScroll = maxScroll;
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

            int xAfk = StaffHelperState.CONFIG.afkListX;
            int yAfk = StaffHelperState.CONFIG.afkListY;

            float afkScale = clampScale(StaffHelperState.CONFIG.afkBoxScale);
            int pad = Math.max(4, Math.round(6 * afkScale));
            int lineH = Math.max(10, Math.round(10 * afkScale));
            int headerH = Math.max(14, Math.round(16 * afkScale));
            int contentTopPad = Math.max(3, Math.round(4 * afkScale));
            Text title = StaffHelperMenuScreen.tr("gui.staffhelper.hud.afk.title");

            String l1 = "Nick Test | 1234";
            String l2 = "OtherNick";

            int wAfk = this.textRenderer.getWidth(title);
            wAfk = Math.max(wAfk, this.textRenderer.getWidth(l1));
            wAfk = Math.max(wAfk, this.textRenderer.getWidth(l2));
            wAfk += pad * 2;

            int hAfk = headerH + contentTopPad + (2 * lineH) + pad;

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
                int headerH = Math.max(14, Math.round(16 * afkScale));
                int contentTopPad = Math.max(3, Math.round(4 * afkScale));
                Text title = StaffHelperMenuScreen.tr("gui.staffhelper.hud.afk.title");
                String l1 = "Nick Test | 1234";
                String l2 = "OtherNick";

                int w = this.textRenderer.getWidth(title);
                w = Math.max(w, this.textRenderer.getWidth(l1));
                w = Math.max(w, this.textRenderer.getWidth(l2));
                w += pad * 2;

                int h = headerH + contentTopPad + (2 * lineH) + pad;

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
            int headerH = Math.max(14, Math.round(16 * afkScale));
            int contentTopPad = Math.max(3, Math.round(4 * afkScale));
            Text title = StaffHelperMenuScreen.tr("gui.staffhelper.hud.afk.title");
            String l1 = "Nick Test | 1234";
            String l2 = "OtherNick";
            int wAfk = this.textRenderer.getWidth(title);
            wAfk = Math.max(wAfk, this.textRenderer.getWidth(l1));
            wAfk = Math.max(wAfk, this.textRenderer.getWidth(l2));
            wAfk += pad * 2;
            int hAfk = headerH + contentTopPad + (2 * lineH) + pad;
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

            ctx.fill(0, 0, this.width, this.height, 0xAA000000);

            ctx.drawText(this.textRenderer, StaffHelperMenuScreen.tr("screen.staffhelper.hud_editor.tip"), 10, 10, 0xFFFFFFFF, false);

            com.dmsh.staffhelper.feature.StatsHudFeature.renderPreview(ctx,
                    StaffHelperState.CONFIG.statsWidgetX,
                    StaffHelperState.CONFIG.statsWidgetY);

            NickSearchFeature.renderWidgetPreview(ctx,
                    StaffHelperState.CONFIG.nickWidgetX,
                    StaffHelperState.CONFIG.nickWidgetY);

            com.dmsh.staffhelper.feature.VanishFeature.renderPreview(ctx,
                    StaffHelperState.CONFIG.vanishWidgetX,
                    StaffHelperState.CONFIG.vanishWidgetY);

            int x = StaffHelperState.CONFIG.afkListX;
            int y = StaffHelperState.CONFIG.afkListY;

            float afkScale = clampScale(StaffHelperState.CONFIG.afkBoxScale);
            int pad = Math.max(4, Math.round(6 * afkScale));
            int lineH = Math.max(10, Math.round(10 * afkScale));
            int headerH = Math.max(14, Math.round(16 * afkScale));
            int contentTopPad = Math.max(3, Math.round(4 * afkScale));
            Text title = StaffHelperMenuScreen.tr("gui.staffhelper.hud.afk.title");
            String l1 = "Nick Test | 1234";
            String l2 = "OtherNick";

            int w = this.textRenderer.getWidth(title);
            w = Math.max(w, this.textRenderer.getWidth(l1));
            w = Math.max(w, this.textRenderer.getWidth(l2));
            w += pad * 2;

            int h = headerH + contentTopPad + (2 * lineH) + pad;

            UiChrome.drawHudPanel(ctx, x, y, w, h, 8, headerH, System.currentTimeMillis(), 0.10f, true);
            int titleY = y + Math.max(2, (headerH - this.textRenderer.fontHeight) / 2);
            ctx.drawText(this.textRenderer, title, x + pad, titleY, UiChrome.mainTextColor(255), false);

            int yy = y + headerH + contentTopPad;
            UiChrome.drawText(ctx, this.textRenderer, UiChrome.uiLiteral(l1), x + pad, yy, UiChrome.mutedTextColor(238), false);
            yy += lineH;
            UiChrome.drawText(ctx, this.textRenderer, UiChrome.uiLiteral(l2), x + pad, yy, UiChrome.mutedTextColor(238), false);

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


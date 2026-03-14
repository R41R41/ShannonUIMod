package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.client.util.InputUtil;

public class ShannonUIScreen extends Screen {
    private final UIRenderer.UIState uiState = new UIRenderer.UIState();

    public ShannonUIScreen() {
        super(Text.literal("Shannon UI"));
    }

    @Override
    protected void init() {
        // タブごとのスクロール量を復元
        int selectedTab = ShannonUIModClient.getSelectedTab();
        uiState.selectedTab = selectedTab;
        uiState.scrollOffset = ShannonUIModClient.getTabScrollOffset(selectedTab);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        MinecraftClient mc = MinecraftClient.getInstance();
        // レイアウト計算
        int textureSize = 6;
        int windowWidth = (13 * textureSize + 2);
        int windowHeight = textureSize * 2 + 2;
        UIRenderer.updatePanelLayout(mc, uiState, windowWidth, windowHeight, textureSize);
        // 最新のタスクツリー状態を取得
        com.shannon.network.packet.TaskTreeState latestState = ShannonUIModClient.getTaskTreeState();
        com.shannon.network.packet.InventoryState inventoryState = ShannonUIModClient.getInventoryState();
        com.shannon.network.packet.ConstantSkillsState constantSkillsState = ShannonUIModClient
                .getConstantSkillsState();
        com.shannon.network.packet.ChatState chatState = ShannonUIModClient.getChatState();
        UIRenderer.renderUI(context, mc, uiState.lastPanelX, uiState.lastPanelY, uiState.lastUiWidth,
                uiState.lastUiHeight, uiState, latestState, inventoryState, constantSkillsState, chatState,
                uiState.lastUiHeight,
                com.shannon.ShannonUIModClient.UIMode.SCREEN);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        char chr = (char) charInput.codepoint();
        int modifiers = charInput.modifiers();
        // チャットタブ
        if (uiState.selectedTab == 4) {
            ChatUIRenderer.handleCharTyped(chr, modifiers);
            return true;
        }
        // デバッグタブ（検索フォーカス中）
        if (uiState.selectedTab == 5 && DebugUIRenderer.isSearchFocused()) {
            DebugUIRenderer.handleCharTyped(chr, modifiers);
            return true;
        }
        // 設定タブ（数値入力中）
        if (uiState.selectedTab == 6 && SettingsUIRenderer.isEditing()) {
            SettingsUIRenderer.handleCharTyped(chr, modifiers);
            return true;
        }
        return super.charTyped(charInput);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        InputUtil.Key pressedKey = InputUtil.fromKeyCode(keyInput);
        String pressedKeyTranslation = pressedKey.getTranslationKey();

        // 特殊キーの処理（全タブ共通）
        if (ShannonUIModClient.getToggleDisplayUIKey().getBoundKeyTranslationKey().equals(pressedKeyTranslation)) {
            ShannonUIModClient.updateUIMode(true, MinecraftClient.getInstance());
            return true;
        }
        if (ShannonUIModClient.getToggleHUDAndScreenUIKey().getBoundKeyTranslationKey().equals(pressedKeyTranslation)) {
            ShannonUIModClient.updateUIMode(false, MinecraftClient.getInstance());
            return true;
        }
        if (ShannonUIModClient.getTabSwitchNextKey().getBoundKeyTranslationKey().equals(pressedKeyTranslation)) {
            int selectedTab = ShannonUIModClient.getSelectedTab();
            ShannonUIModClient.setTabScrollOffset(selectedTab, uiState.scrollOffset);
            selectedTab = (selectedTab + 1) % 7;
            ShannonUIModClient.setSelectedTab(selectedTab);
            uiState.selectedTab = selectedTab;
            uiState.scrollOffset = ShannonUIModClient.getTabScrollOffset(selectedTab);
            return true;
        }
        // ESCキーが押されたら、UIを非表示にする
        if (keyInput.isEscape()) {
            ShannonUIModClient.updateUIMode(false, MinecraftClient.getInstance());
            return true;
        }

        // チャットタブでのキー入力処理（特殊キー処理後）
        if (uiState.selectedTab == 4) {
            ChatUIRenderer.handleKeyPress(keyInput.key(), keyInput.scancode(), keyInput.modifiers());
            return true;
        }

        // デバッグタブ（検索フォーカス中）
        if (uiState.selectedTab == 5 && DebugUIRenderer.isSearchFocused()) {
            DebugUIRenderer.handleKeyPress(keyInput.key(), keyInput.scancode(), keyInput.modifiers());
            return true;
        }

        // 設定タブ（数値入力中）
        if (uiState.selectedTab == 6 && SettingsUIRenderer.isEditing()) {
            SettingsUIRenderer.handleKeyPress(keyInput.key(), keyInput.scancode(), keyInput.modifiers());
            return true;
        }

        return super.keyPressed(keyInput);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        UIRenderer.handleScroll(verticalAmount, uiState, uiState.lastUiHeight);
        ShannonUIModClient.setTabScrollOffset(uiState.selectedTab, uiState.scrollOffset);
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int button = click.buttonInfo().button();
        InputUtil.Key pressedKey = InputUtil.Type.MOUSE.createFromCode(button);
        String pressedKeyTranslation = pressedKey.getTranslationKey();

        if (ShannonUIModClient.getToggleDisplayUIKey().getBoundKeyTranslationKey().equals(pressedKeyTranslation)) {
            ShannonUIModClient.updateUIMode(true, MinecraftClient.getInstance());
            return true;
        }
        if (ShannonUIModClient.getToggleHUDAndScreenUIKey().getBoundKeyTranslationKey().equals(pressedKeyTranslation)) {
            ShannonUIModClient.updateUIMode(false, MinecraftClient.getInstance());
            return true;
        }
        if (ShannonUIModClient.getTabSwitchNextKey().getBoundKeyTranslationKey().equals(pressedKeyTranslation)) {
            int selectedTab = ShannonUIModClient.getSelectedTab();
            ShannonUIModClient.setTabScrollOffset(selectedTab, uiState.scrollOffset);
            selectedTab = (selectedTab + 1) % 7;
            ShannonUIModClient.setSelectedTab(selectedTab);
            uiState.selectedTab = selectedTab;
            uiState.scrollOffset = ShannonUIModClient.getTabScrollOffset(selectedTab);
            return true;
        }
        return super.mouseClicked(click, doubled);
    }
}

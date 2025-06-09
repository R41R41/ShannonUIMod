package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.util.InputUtil;

public class ShannonUIScreen extends Screen {
    private final UIRenderer.UIState uiState = new UIRenderer.UIState();
    private ShannonUIModClient.UIMode uiMode = ShannonUIModClient.UIMode.HIDDEN;
    private com.shannon.network.packet.TaskTreeState taskTreeState = null;

    public ShannonUIScreen() {
        super(Text.literal("Shannon UI"));
    }

    @Override
    protected void init() {
        // タブごとのスクロール量を復元
        uiState.scrollOffset = ShannonUIModClient.getTabScrollOffset(uiState.selectedTab);
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
        // UI描画
        UIRenderer.renderUI(context, mc, uiState.lastPanelX, uiState.lastPanelY, uiState.lastUiWidth,
                uiState.lastUiHeight, uiState, latestState, uiState.lastUiHeight);
        // ShannonUIModClientのselectedTabやscrollOffsetと同期（必要なら）
        // 例: ShannonUIModClient.setSelectedTab(uiState.selectedTab);
        // 例: ShannonUIModClient.setScrollOffset(uiState.scrollOffset);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        InputUtil.Key pressedKey = InputUtil.fromKeyCode(keyCode, scanCode);
        String pressedKeyTranslation = pressedKey.getTranslationKey();
        if (ShannonUIModClient.getToggleHUDUIKey().getBoundKeyTranslationKey().equals(pressedKeyTranslation)) {
            ShannonUIModClient.updateUIMode(true, MinecraftClient.getInstance());
            return true;
        }
        if (ShannonUIModClient.getToggleScreenUIKey().getBoundKeyTranslationKey().equals(pressedKeyTranslation)) {
            ShannonUIModClient.updateUIMode(false, MinecraftClient.getInstance());
            return true;
        }
        if (ShannonUIModClient.getTabSwitchNextKey().getBoundKeyTranslationKey().equals(pressedKeyTranslation)) {
            ShannonUIModClient.setTabScrollOffset(uiState.selectedTab, uiState.scrollOffset);
            uiState.selectedTab = (uiState.selectedTab + 1) % 3;
            uiState.scrollOffset = ShannonUIModClient.getTabScrollOffset(uiState.selectedTab);
            return true;
        }
        if (ShannonUIModClient.getTabSwitchPrevKey().getBoundKeyTranslationKey().equals(pressedKeyTranslation)) {
            ShannonUIModClient.setTabScrollOffset(uiState.selectedTab, uiState.scrollOffset);
            uiState.selectedTab = (uiState.selectedTab - 1 + 3) % 3;
            uiState.scrollOffset = ShannonUIModClient.getTabScrollOffset(uiState.selectedTab);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        UIRenderer.handleScroll(verticalAmount, uiState, uiState.lastUiHeight);
        ShannonUIModClient.setTabScrollOffset(uiState.selectedTab, uiState.scrollOffset);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        InputUtil.Key pressedKey = InputUtil.Type.MOUSE.createFromCode(button);
        String pressedKeyTranslation = pressedKey.getTranslationKey();

        if (ShannonUIModClient.getToggleHUDUIKey().getBoundKeyTranslationKey().equals(pressedKeyTranslation)) {
            ShannonUIModClient.updateUIMode(true, MinecraftClient.getInstance());
            return true;
        }
        if (ShannonUIModClient.getToggleScreenUIKey().getBoundKeyTranslationKey().equals(pressedKeyTranslation)) {
            ShannonUIModClient.updateUIMode(false, MinecraftClient.getInstance());
            return true;
        }
        if (ShannonUIModClient.getTabSwitchNextKey().getBoundKeyTranslationKey().equals(pressedKeyTranslation)) {
            ShannonUIModClient.setTabScrollOffset(uiState.selectedTab, uiState.scrollOffset);
            uiState.selectedTab = (uiState.selectedTab + 1) % 3;
            uiState.scrollOffset = ShannonUIModClient.getTabScrollOffset(uiState.selectedTab);
            return true;
        }
        if (ShannonUIModClient.getTabSwitchPrevKey().getBoundKeyTranslationKey().equals(pressedKeyTranslation)) {
            ShannonUIModClient.setTabScrollOffset(uiState.selectedTab, uiState.scrollOffset);
            uiState.selectedTab = (uiState.selectedTab - 1 + 3) % 3;
            uiState.scrollOffset = ShannonUIModClient.getTabScrollOffset(uiState.selectedTab);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
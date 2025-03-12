package com.sh4nnon.shannonuimod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraft.world.entity.player.Player;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import org.lwjgl.glfw.GLFW;
import java.util.Random;

@Mod(ShannonUIMod.MOD_ID)
public class ShannonUIMod {
    public static final String MOD_ID = "shannonuimod";
    private static boolean showLargeRectangle = false;
    private static long showRectangleStartTime = 0;
    private static final long SHOW_RECTANGLE_DURATION = 3000; // 3秒間表示
    private static boolean autoHideRectangle = true; // 自動的に非表示にするかどうか

    // Botの情報（実際のmineflayerと連携する場合は、ここを実際のデータに置き換える）
    private static class BotInfo {
        private static final Random random = new Random();
        private static float health = 20.0f; // 最大20
        private static int food = 20; // 最大20
        private static String botName = "MineflayerBot";

        // 実際のmineflayerと連携する場合は、このメソッドを実際のデータ取得に置き換える
        public static void updateInfo() {
            // デモ用にランダムに値を変動させる
            if (random.nextInt(100) < 10) { // 10%の確率で変化
                health = Math.max(0, Math.min(20, health + random.nextFloat() * 2 - 1));
                food = Math.max(0, Math.min(20, food + random.nextInt(3) - 1));
            }
        }
    }

    public ShannonUIMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        System.out.println("ShannonUIMod: commonSetup");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        System.out.println("ShannonUIMod: clientSetup");
        // プレイヤーイベントハンドラを登録
        MinecraftForge.EVENT_BUS.register(new PlayerEventHandler());
        // ボットステータスオーバーレイを登録
        MinecraftForge.EVENT_BUS.register(BotStatusOverlay.class);
        // キー入力ハンドラを登録
        MinecraftForge.EVENT_BUS.register(KeyHandler.class);
        System.out.println("ShannonUIMod: Event handlers registered");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    // プレイヤーイベントを処理するクラス
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class PlayerEventHandler {
        private static final String WELCOME_MESSAGE = "§c§lShannonUIMod is active!"; // 赤色で太字
        private static boolean hasShownMessage = false;

        // プレイヤーがログインしたときのイベント
        @SubscribeEvent
        public void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
            System.out.println("ShannonUIMod: Player logged in");
            hasShownMessage = false; // リセット
        }

        // クライアントティックごとにメッセージを表示（一度だけ）
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && !hasShownMessage) {
                    // 少し待ってからメッセージを表示（ワールド読み込み完了後）
                    if (mc.player.tickCount > 20) {
                        // タイトル表示
                        mc.player.displayClientMessage(Component.literal(WELCOME_MESSAGE), false);

                        // チャットメッセージも追加
                        mc.gui.getChat().addMessage(Component.literal("ShannonUIMod: MODが正常に読み込まれました！"));
                        mc.gui.getChat().addMessage(Component.literal("ShannonUIMod: このMODはUIをカスタマイズします。"));
                        mc.gui.getChat().addMessage(Component.literal("ShannonUIMod: 画面右下にBotのステータスが表示されます。"));
                        mc.gui.getChat().addMessage(Component.literal("ShannonUIMod: Uキーを押すと詳細情報の表示/非表示を切り替えられます。"));

                        System.out.println("ShannonUIMod: Displayed welcome message");
                        hasShownMessage = true;
                    }
                }
            }

            // 大きな長方形の表示時間を管理（自動非表示が有効な場合）
            if (autoHideRectangle && showLargeRectangle
                    && System.currentTimeMillis() - showRectangleStartTime > SHOW_RECTANGLE_DURATION) {
                showLargeRectangle = false;
                System.out.println("ShannonUIMod: Hide large rectangle (auto)");
            }

            // Botの情報を更新
            if (event.phase == TickEvent.Phase.END) {
                BotInfo.updateInfo();
            }
        }
    }

    // キー入力を処理するクラス
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class KeyHandler {
        private static boolean wasUKeyPressed = false;

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.screen == null) {
                // Uキーが押されたかチェック (GLFW.GLFW_KEY_U = 85)
                if (event.getKey() == GLFW.GLFW_KEY_U && event.getAction() == GLFW.GLFW_PRESS) {
                    // Uキーが押された
                    showLargeRectangle = !showLargeRectangle; // 表示状態を切り替え

                    if (showLargeRectangle) {
                        // 長方形を表示
                        showRectangleStartTime = System.currentTimeMillis();

                        // サウンドを再生
                        mc.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);

                        System.out.println("ShannonUIMod: U key pressed! Show large rectangle");
                    } else {
                        // 長方形を非表示
                        System.out.println("ShannonUIMod: U key pressed! Hide large rectangle");
                    }
                }
            }
        }
    }

    // Botステータスとオーバーレイを表示するクラス
    public static class BotStatusOverlay {
        public static final int PANEL_WIDTH = 100; // パネルの幅
        public static final int PANEL_HEIGHT = 50; // パネルの高さ
        private static final int PANEL_COLOR = 0xAA000000; // 半透明の黒色 (ARGB)
        private static final int PANEL_BORDER_COLOR = 0xFFFFFFFF; // 白色の枠線 (ARGB)

        private static final int HEALTH_BAR_COLOR = 0xFFFF0000; // 赤色 (ARGB)
        private static final int FOOD_BAR_COLOR = 0xFFFFAA00; // オレンジ色 (ARGB)

        private static final int LARGE_RECT_WIDTH = 200; // 大きな長方形の幅
        private static final int LARGE_RECT_HEIGHT = 150; // 大きな長方形の高さ
        private static final int LARGE_RECT_COLOR = 0xCC000000; // 半透明の黒色 (ARGB)
        private static final int LARGE_RECT_BORDER_COLOR = 0xFFFFFFFF; // 白色の枠線 (ARGB)

        @SubscribeEvent
        public static void onRenderGui(CustomizeGuiOverlayEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null)
                return;

            try {
                GuiGraphics guiGraphics = event.getGuiGraphics();
                int screenWidth = event.getWindow().getGuiScaledWidth();
                int screenHeight = event.getWindow().getGuiScaledHeight();

                RenderSystem.enableBlend(); // 透明度を有効化

                // パネルの位置（右下から10ピクセル内側）
                int panelX = screenWidth - PANEL_WIDTH - 10;
                int panelY = screenHeight - PANEL_HEIGHT - 10;

                // パネルの背景を描画
                guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, PANEL_COLOR);

                // パネルの枠線を描画
                guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 1, PANEL_BORDER_COLOR); // 上辺
                guiGraphics.fill(panelX, panelY, panelX + 1, panelY + PANEL_HEIGHT, PANEL_BORDER_COLOR); // 左辺
                guiGraphics.fill(panelX, panelY + PANEL_HEIGHT - 1, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT,
                        PANEL_BORDER_COLOR); // 下辺
                guiGraphics.fill(panelX + PANEL_WIDTH - 1, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT,
                        PANEL_BORDER_COLOR); // 右辺

                // Botの名前を描画
                guiGraphics.drawString(mc.font, BotInfo.botName, panelX + 5, panelY + 5, 0xFFFFFFFF);

                // 体力バーの背景
                guiGraphics.fill(panelX + 5, panelY + 20, panelX + PANEL_WIDTH - 5, panelY + 25, 0x80FFFFFF);

                // 体力バー
                int healthWidth = (int) ((PANEL_WIDTH - 10) * (BotInfo.health / 20.0f));
                guiGraphics.fill(panelX + 5, panelY + 20, panelX + 5 + healthWidth, panelY + 25, HEALTH_BAR_COLOR);

                // 体力テキスト
                String healthText = String.format("HP: %.1f/20", BotInfo.health);
                guiGraphics.drawString(mc.font, healthText, panelX + 5, panelY + 30, 0xFFFFFFFF);

                // 空腹度バーの背景
                guiGraphics.fill(panelX + 5, panelY + 40, panelX + PANEL_WIDTH - 5, panelY + 45, 0x80FFFFFF);

                // 空腹度バー
                int foodWidth = (int) ((PANEL_WIDTH - 10) * (BotInfo.food / 20.0f));
                guiGraphics.fill(panelX + 5, panelY + 40, panelX + 5 + foodWidth, panelY + 45, FOOD_BAR_COLOR);

                // 空腹度テキスト
                String foodText = String.format("Food: %d/20", BotInfo.food);
                guiGraphics.drawString(mc.font, foodText, panelX + 5, panelY + 50, 0xFFFFFFFF);

                // 大きな長方形を表示（Uキーが押された場合）
                if (showLargeRectangle) {
                    // 画面中央に大きな長方形を描画
                    int rectX = (screenWidth - LARGE_RECT_WIDTH) / 2;
                    int rectY = (screenHeight - LARGE_RECT_HEIGHT) / 2;

                    // 長方形の背景を描画
                    guiGraphics.fill(rectX, rectY, rectX + LARGE_RECT_WIDTH, rectY + LARGE_RECT_HEIGHT,
                            LARGE_RECT_COLOR);

                    // 長方形の枠線を描画
                    guiGraphics.fill(rectX, rectY, rectX + LARGE_RECT_WIDTH, rectY + 1, LARGE_RECT_BORDER_COLOR); // 上辺
                    guiGraphics.fill(rectX, rectY, rectX + 1, rectY + LARGE_RECT_HEIGHT, LARGE_RECT_BORDER_COLOR); // 左辺
                    guiGraphics.fill(rectX, rectY + LARGE_RECT_HEIGHT - 1, rectX + LARGE_RECT_WIDTH,
                            rectY + LARGE_RECT_HEIGHT, LARGE_RECT_BORDER_COLOR); // 下辺
                    guiGraphics.fill(rectX + LARGE_RECT_WIDTH - 1, rectY, rectX + LARGE_RECT_WIDTH,
                            rectY + LARGE_RECT_HEIGHT, LARGE_RECT_BORDER_COLOR); // 右辺

                    // タイトルを描画
                    guiGraphics.drawCenteredString(mc.font, "Mineflayer Bot Status", screenWidth / 2, rectY + 10,
                            0xFFFFFFFF);

                    // 詳細情報を描画
                    guiGraphics.drawString(mc.font, "Bot Name: " + BotInfo.botName, rectX + 10, rectY + 30, 0xFFFFFFFF);
                    guiGraphics.drawString(mc.font, "Health: " + String.format("%.1f/20", BotInfo.health), rectX + 10,
                            rectY + 50, 0xFFFFFFFF);
                    guiGraphics.drawString(mc.font, "Food: " + BotInfo.food + "/20", rectX + 10, rectY + 70,
                            0xFFFFFFFF);

                    // 体力バーの背景
                    guiGraphics.fill(rectX + 10, rectY + 55, rectX + LARGE_RECT_WIDTH - 10, rectY + 60, 0x80FFFFFF);

                    // 体力バー
                    int detailedHealthWidth = (int) ((LARGE_RECT_WIDTH - 20) * (BotInfo.health / 20.0f));
                    guiGraphics.fill(rectX + 10, rectY + 55, rectX + 10 + detailedHealthWidth, rectY + 60,
                            HEALTH_BAR_COLOR);

                    // 空腹度バーの背景
                    guiGraphics.fill(rectX + 10, rectY + 75, rectX + LARGE_RECT_WIDTH - 10, rectY + 80, 0x80FFFFFF);

                    // 空腹度バー
                    int detailedFoodWidth = (int) ((LARGE_RECT_WIDTH - 20) * (BotInfo.food / 20.0f));
                    guiGraphics.fill(rectX + 10, rectY + 75, rectX + 10 + detailedFoodWidth, rectY + 80,
                            FOOD_BAR_COLOR);

                    // 追加情報
                    guiGraphics.drawString(mc.font, "Bot Status: Active", rectX + 10, rectY + 100, 0xFFFFFFFF);
                    guiGraphics.drawString(mc.font, "Server: " + mc.getCurrentServer().name, rectX + 10, rectY + 120,
                            0xFFFFFFFF);

                    // 操作方法
                    guiGraphics.drawCenteredString(mc.font, "Uキーで閉じる", screenWidth / 2, rectY + LARGE_RECT_HEIGHT - 15,
                            0xFFFFFFFF);
                }

                RenderSystem.disableBlend(); // 透明度を無効化

                // デバッグ情報をコンソールに出力
                if (mc.player.tickCount % 300 == 0) { // 300ティックごとに出力
                    System.out.println("ShannonUIMod: Rendering bot status panel");
                }
            } catch (Exception e) {
                System.err.println("Error rendering overlay: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // バックアップ方法：ゲームティックごとにコンソールにメッセージを出力
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class TickHandler {
        private static int tickCounter = 0;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    tickCounter++;
                    if (tickCounter % 200 == 0) { // 200ティックごとに出力
                        System.out.println("ShannonUIMod: Client tick " + tickCounter);
                    }
                }
            }
        }
    }
}

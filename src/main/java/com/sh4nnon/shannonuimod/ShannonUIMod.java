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
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

@Mod(ShannonUIMod.MOD_ID)
public class ShannonUIMod {
    public static final String MOD_ID = "shannonuimod";
    private static boolean showLargeRectangle = false;
    private static long showRectangleStartTime = 0;
    private static final long SHOW_RECTANGLE_DURATION = 3000; // 3秒間表示
    private static boolean autoHideRectangle = true; // 自動的に非表示にするかどうか

    // Botの情報（実際のmineflayerと連携する場合は、ここを実際のデータに置き換える）
    private static class BotInfo {
        private static float health = 20.0f; // 最大20
        private static int food = 20; // 最大20
        private static String botName = "R41R41";

        // 実際のプレイヤーの情報を取得するメソッド
        public static void updateInfo() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                // プレイヤーの体力と空腹度を取得
                health = mc.player.getHealth(); // プレイヤーの現在の体力
                food = mc.player.getFoodData().getFoodLevel(); // プレイヤーの現在の空腹度
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
        MinecraftForge.EVENT_BUS.register(new PlayerEventHandler());
        MinecraftForge.EVENT_BUS.register(BotStatusOverlay.class);
        MinecraftForge.EVENT_BUS.register(KeyHandler.class);
        System.out.println("ShannonUIMod: Event handlers registered");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class PlayerEventHandler {
        private static final String WELCOME_MESSAGE = "§c§lShannonUIMod is active!"; // 赤色で太字
        private static boolean hasShownMessage = false;

        @SubscribeEvent
        public void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
            System.out.println("ShannonUIMod: Player logged in");
            hasShownMessage = false; // リセット
        }

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && !hasShownMessage) {
                    if (mc.player.tickCount > 20) {
                        mc.player.displayClientMessage(Component.literal(WELCOME_MESSAGE), false);
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

    public static class BotStatusOverlay {
        private static final int LARGE_RECT_WIDTH = 200;
        private static final int LARGE_RECT_HEIGHT = 150;
        private static final int LARGE_RECT_COLOR = 0xCC000000;
        private static final int LARGE_RECT_BORDER_COLOR = 0xFFFFFFFF;

        private static final ResourceLocation ICONS = ResourceLocation.parse("minecraft:textures/gui/icons.png");
        private static final ResourceLocation SHANNON_ICON = ResourceLocation
                .parse("shannonuimod:textures/ui/shannon.png");

        private static final ResourceLocation HEART_FULL = ResourceLocation.parse(
                "minecraft:textures/gui/sprites/hud/heart/full.png");
        private static final ResourceLocation HEART_HALF = ResourceLocation.parse(
                "minecraft:textures/gui/sprites/hud/heart/half.png");
        private static final ResourceLocation HEART_EMPTY = ResourceLocation.parse(
                "minecraft:textures/gui/sprites/hud/heart/container.png");

        // 食料アイコンも同様に
        private static final ResourceLocation FOOD_FULL = ResourceLocation.parse(
                "minecraft:textures/gui/sprites/hud/food_full.png");
        private static final ResourceLocation FOOD_HALF = ResourceLocation.parse(
                "minecraft:textures/gui/sprites/hud/food_half.png");
        private static final ResourceLocation FOOD_EMPTY = ResourceLocation.parse(
                "minecraft:textures/gui/sprites/hud/food_empty.png");

        @SubscribeEvent
        public static void onRenderGui(CustomizeGuiOverlayEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null)
                return;

            try {
                GuiGraphics guiGraphics = event.getGuiGraphics();
                int screenWidth = event.getWindow().getGuiScaledWidth();
                int screenHeight = event.getWindow().getGuiScaledHeight();

                RenderSystem.enableBlend();

                RenderSystem.setShaderTexture(0, ICONS);

                int hudSize = 6;
                int hudPadding = 10;
                int hudGap = 2;
                int heartX = screenWidth - hudSize * 10 - hudPadding;
                int heartY = screenHeight - hudSize * 2 - hudPadding - hudGap;

                int fullHearts = (int) (BotInfo.health / 2);
                boolean hasHalfHeart = BotInfo.health % 2 != 0;

                for (int i = 0; i < 10; i++) {
                    int currentHeartX = heartX + i * hudSize;
                    try {
                        RenderSystem.setShaderTexture(0, HEART_EMPTY);
                        guiGraphics.blit(HEART_EMPTY, currentHeartX, heartY, 0, 0, hudSize, hudSize, hudSize,
                                hudSize);

                        if (i < fullHearts) {
                            RenderSystem.setShaderTexture(0, HEART_FULL);
                            guiGraphics.blit(HEART_FULL, currentHeartX, heartY, 0, 0, hudSize, hudSize, hudSize,
                                    hudSize);
                        } else if (i == fullHearts && hasHalfHeart) {
                            RenderSystem.setShaderTexture(0, HEART_HALF);
                            guiGraphics.blit(HEART_HALF, currentHeartX, heartY, 0, 0, hudSize, hudSize, hudSize,
                                    hudSize);
                        }
                    } catch (Exception e) {
                        System.err.println("ShannonUIMod: Error drawing heart at index " + i + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                int foodX = screenWidth - hudSize * 10 - hudPadding;
                int foodY = screenHeight - hudSize * 1 - hudPadding;

                int fullFood = BotInfo.food / 2;
                boolean hasHalfFood = BotInfo.food % 2 != 0;

                for (int i = 0; i < 10; i++) {
                    int currentFoodX = foodX + (9 - i) * hudSize;
                    try {
                        RenderSystem.setShaderTexture(0, FOOD_EMPTY);
                        guiGraphics.blit(FOOD_EMPTY, currentFoodX, foodY, 0, 0, hudSize, hudSize, hudSize,
                                hudSize);

                        if (i < fullFood) {
                            RenderSystem.setShaderTexture(0, FOOD_FULL);
                            guiGraphics.blit(FOOD_FULL, currentFoodX, foodY, 0, 0, hudSize, hudSize, hudSize,
                                    hudSize);
                        } else if (i == fullFood && hasHalfFood) {
                            RenderSystem.setShaderTexture(0, FOOD_HALF);
                            guiGraphics.blit(FOOD_HALF, currentFoodX, foodY, 0, 0, hudSize, hudSize, hudSize,
                                    hudSize);
                        }
                    } catch (Exception e) {
                        System.err.println("ShannonUIMod: Error drawing food at index " + i + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                try {
                    int shannonIconSize = hudSize * 2 + hudGap;
                    int shannonIconX = screenWidth - hudSize * 10 - hudPadding - shannonIconSize - hudGap;
                    int shannonIconY = screenHeight - hudSize * 2 - hudPadding - hudGap;

                    RenderSystem.setShaderTexture(0, SHANNON_ICON);

                    guiGraphics.blit(SHANNON_ICON, shannonIconX, shannonIconY, 0, 0, shannonIconSize, shannonIconSize,
                            shannonIconSize, shannonIconSize);
                } catch (Exception e) {
                    System.err.println("ShannonUIMod: Error drawing Shannon icon: " + e.getMessage());
                    e.printStackTrace();
                }

                if (showLargeRectangle) {
                    int rectX = (screenWidth - LARGE_RECT_WIDTH) / 2;
                    int rectY = (screenHeight - LARGE_RECT_HEIGHT) / 2;

                    guiGraphics.fill(rectX, rectY, rectX + LARGE_RECT_WIDTH, rectY + LARGE_RECT_HEIGHT,
                            LARGE_RECT_COLOR);

                    guiGraphics.fill(rectX, rectY, rectX + LARGE_RECT_WIDTH, rectY + 1, LARGE_RECT_BORDER_COLOR); // 上辺
                    guiGraphics.fill(rectX, rectY, rectX + 1, rectY + LARGE_RECT_HEIGHT, LARGE_RECT_BORDER_COLOR); // 左辺
                    guiGraphics.fill(rectX, rectY + LARGE_RECT_HEIGHT - 1, rectX + LARGE_RECT_WIDTH,
                            rectY + LARGE_RECT_HEIGHT, LARGE_RECT_BORDER_COLOR); // 下辺
                    guiGraphics.fill(rectX + LARGE_RECT_WIDTH - 1, rectY, rectX + LARGE_RECT_WIDTH,
                            rectY + LARGE_RECT_HEIGHT, LARGE_RECT_BORDER_COLOR); // 右辺

                    guiGraphics.drawCenteredString(mc.font, "Mineflayer Bot Status", screenWidth / 2, rectY + 10,
                            0xFFFFFFFF);

                    guiGraphics.drawString(mc.font, "Bot Name: " + BotInfo.botName, rectX + 10, rectY + 30, 0xFFFFFFFF);

                    guiGraphics.drawString(mc.font, "Health: " + String.format("%.1f/20", BotInfo.health), rectX + 10,
                            rectY + 50, 0xFFFFFFFF);

                    int detailedHeartX = rectX + 10;
                    int detailedHeartY = rectY + 65;
                    int detailedHeartSize = 9;

                    for (int i = 0; i < 10; i++) {
                        int currentDetailedHeartX = detailedHeartX + i * (detailedHeartSize + 1);
                        try {
                            RenderSystem.setShaderTexture(0, HEART_EMPTY);
                            guiGraphics.blit(HEART_EMPTY, currentDetailedHeartX, detailedHeartY, 0, 0, 9, 9, 9, 9);

                            if (i < fullHearts) {
                                RenderSystem.setShaderTexture(0, HEART_FULL);
                                guiGraphics.blit(HEART_FULL, currentDetailedHeartX, detailedHeartY, 0, 0, 9, 9, 9, 9);
                            } else if (i == fullHearts && hasHalfHeart) {
                                RenderSystem.setShaderTexture(0, HEART_HALF);
                                guiGraphics.blit(HEART_HALF, currentDetailedHeartX, detailedHeartY, 0, 0, 9, 9, 9, 9);
                            }
                        } catch (Exception e) {
                            if (i < fullHearts) {
                                guiGraphics.fill(currentDetailedHeartX, detailedHeartY,
                                        currentDetailedHeartX + detailedHeartSize, detailedHeartY + detailedHeartSize,
                                        0xFFFF0000);
                            } else if (i == fullHearts && hasHalfHeart) {
                                guiGraphics.fill(currentDetailedHeartX, detailedHeartY,
                                        currentDetailedHeartX + detailedHeartSize, detailedHeartY + detailedHeartSize,
                                        0xFFFF8080);
                            } else {
                                guiGraphics.fill(currentDetailedHeartX, detailedHeartY,
                                        currentDetailedHeartX + detailedHeartSize, detailedHeartY + detailedHeartSize,
                                        0xFF808080);
                            }
                            System.err.println(
                                    "ShannonUIMod: Error drawing detailed heart at index " + i + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    guiGraphics.drawString(mc.font, "Food: " + BotInfo.food + "/20", rectX + 10, rectY + 85,
                            0xFFFFFFFF);

                    int detailedFoodX = rectX + 10;
                    int detailedFoodY = rectY + 100;
                    int detailedFoodSize = 9;

                    for (int i = 0; i < 10; i++) {
                        int currentDetailedFoodX = detailedFoodX + i * (detailedFoodSize + 1);
                        try {
                            RenderSystem.setShaderTexture(0, ICONS);
                            guiGraphics.blit(ICONS, currentDetailedFoodX, detailedFoodY, 16, 27, 9, 9);

                            if (i < fullFood) {
                                guiGraphics.blit(ICONS, currentDetailedFoodX, detailedFoodY, 52, 27, 9, 9);
                            } else if (i == fullFood && hasHalfFood) {
                                guiGraphics.blit(ICONS, currentDetailedFoodX, detailedFoodY, 61, 27, 9, 9);
                            }
                        } catch (Exception e) {
                            if (i < fullFood) {
                                guiGraphics.fill(currentDetailedFoodX, detailedFoodY,
                                        currentDetailedFoodX + detailedFoodSize, detailedFoodY + detailedFoodSize,
                                        0xFFA05000);
                            } else if (i == fullFood && hasHalfFood) {
                                guiGraphics.fill(currentDetailedFoodX, detailedFoodY,
                                        currentDetailedFoodX + detailedFoodSize, detailedFoodY + detailedFoodSize,
                                        0xFFC08040);
                            } else {
                                guiGraphics.fill(currentDetailedFoodX, detailedFoodY,
                                        currentDetailedFoodX + detailedFoodSize, detailedFoodY + detailedFoodSize,
                                        0xFF808080);
                            }
                            System.err.println(
                                    "ShannonUIMod: Error drawing detailed food at index " + i + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    guiGraphics.drawString(mc.font, "Bot Status: Active", rectX + 10, rectY + 120, 0xFFFFFFFF);

                    if (mc.getCurrentServer() != null) {
                        guiGraphics.drawString(mc.font, "Server: " + mc.getCurrentServer().name, rectX + 10,
                                rectY + 135, 0xFFFFFFFF);
                    }

                    guiGraphics.drawCenteredString(mc.font, "Uキーで閉じる", screenWidth / 2, rectY + LARGE_RECT_HEIGHT - 15,
                            0xFFFFFFFF);
                }

                RenderSystem.disableBlend();

                if (mc.player.tickCount % 300 == 0) {
                    System.out.println("ShannonUIMod: Rendering bot status panel");
                }
            } catch (Exception e) {
                System.err.println("Error rendering overlay: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

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

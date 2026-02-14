package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import com.shannon.network.packet.ConstantSkillsState;
import com.shannon.network.packet.ConstantSkillClickPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Identifier;

import java.util.*;

/**
 * 常時スキルUIレンダラー
 * スキルをカテゴリ別にグルーピングし、折りたたみ対応
 */
public class ConstantSkillsUIRenderer {
    private static final float SCALE = RenderUtils.SCALE;
    private static final int LINE_HEIGHT = 10;
    private static final Identifier STATUS_TRUE = Identifier.of("shannonuimod", "textures/status_true.png");
    private static final Identifier STATUS_FALSE = Identifier.of("shannonuimod", "textures/status_false.png");
    private static boolean wasMousePressed = false;

    // 折りたたまれたカテゴリ
    private static final Set<String> collapsedCategories = new HashSet<>();

    // カテゴリ定義（表示順序を保持）
    private static final String[] CATEGORY_ORDER = {
            "生存", "回避", "視線", "移動", "検知", "その他"
    };

    /**
     * スキル名からカテゴリを判定
     */
    private static String getSkillCategory(String skillName) {
        if (skillName.contains("avoid") || skillName.contains("run-from"))
            return "回避";
        if (skillName.contains("face") || skillName.contains("looking-at"))
            return "視線";
        if (skillName.equals("auto-eat") || skillName.equals("auto-sleep") || skillName.equals("auto-swim"))
            return "生存";
        if (skillName.equals("auto-follow") || skillName.contains("pick-up"))
            return "移動";
        if (skillName.contains("detect") || skillName.equals("auto-update-state"))
            return "検知";
        return "その他";
    }

    public static void renderConstantSkills(DrawContext context, MinecraftClient mc, int x, int y, int uiWidth,
            int uiHeight, UIRenderer.UIState state, ConstantSkillsState constantSkillsState, int mouseX, int mouseY,
            boolean mouseClicked) {
        context.getMatrices().pushMatrix();
        try {
            if (constantSkillsState == null || constantSkillsState.skills == null
                    || constantSkillsState.skills.isEmpty()) {
                int drawXEmpty = (int) (4 / SCALE);
                int drawYEmpty = (int) (4 / SCALE);
                context.getMatrices().translate(x, y);
                context.getMatrices().scale(SCALE, SCALE);
                context.drawTextWithShadow(mc.textRenderer, Text.literal("常時スキル"), drawXEmpty, drawYEmpty,
                        RenderUtils.COLOR_HEADER);
                context.drawTextWithShadow(mc.textRenderer, Text.literal("読み込み中..."), drawXEmpty,
                        drawYEmpty + LINE_HEIGHT * 2, RenderUtils.COLOR_MUTED);
                context.drawTextWithShadow(mc.textRenderer, Text.literal("(バックエンド接続を確認)"), drawXEmpty,
                        drawYEmpty + LINE_HEIGHT * 4, 0x666666);
                state.contentHeight = uiHeight;
                return;
            }

            context.getMatrices().translate(x, y);
            context.getMatrices().scale(SCALE, SCALE);

            int scaledMouseX = (int) ((mouseX + 4) / SCALE);
            int scaledMouseY = (int) ((mouseY + 4) / SCALE);
            int scaledUiWidth = (int) (uiWidth / SCALE);
            int scaledUiHeight = (int) (uiHeight / SCALE);

            int line = 0;
            int drawX = (int) (4 / SCALE);
            int drawY = (int) (4 / SCALE);
            int yOffset = (int) (-state.scrollOffset / SCALE);
            int maxTextWidth = scaledUiWidth - 24;
            int startY = drawY + yOffset;

            // ツールチップ用
            String tooltipDesc = null;
            int tooltipX = 0, tooltipY = 0;
            java.util.List<OrderedText> tooltipLines = null;

            // スキルをカテゴリ別にグループ化
            LinkedHashMap<String, List<ConstantSkillsState.ConstantSkill>> grouped = new LinkedHashMap<>();
            for (String cat : CATEGORY_ORDER) {
                grouped.put(cat, new ArrayList<>());
            }
            for (ConstantSkillsState.ConstantSkill skill : constantSkillsState.skills) {
                String cat = getSkillCategory(skill.skillName);
                grouped.computeIfAbsent(cat, k -> new ArrayList<>()).add(skill);
            }

            // カテゴリごとに描画
            for (Map.Entry<String, List<ConstantSkillsState.ConstantSkill>> entry : grouped.entrySet()) {
                String category = entry.getKey();
                List<ConstantSkillsState.ConstantSkill> skills = entry.getValue();
                if (skills.isEmpty())
                    continue;

                boolean isCollapsed = collapsedCategories.contains(category);

                // カテゴリ内のON数をカウント
                long onCount = skills.stream().filter(s -> s.status).count();
                String collapseIcon = isCollapsed ? "> " : "v ";
                String categoryText = collapseIcon + category + " (" + onCount + "/" + skills.size() + ")";

                int catY = startY + line * LINE_HEIGHT;

                // カテゴリヘッダー背景
                if (catY >= 0 && catY + LINE_HEIGHT <= scaledUiHeight) {
                    int headerBg = onCount == skills.size() ? 0xFF2A4A2A : 0xFF3A3A3A;
                    context.fill(drawX - 2, catY - 2, scaledUiWidth - 8, catY + LINE_HEIGHT, headerBg);
                    context.drawTextWithShadow(mc.textRenderer, Text.literal(categoryText), drawX, catY,
                            RenderUtils.COLOR_CATEGORY);
                }

                // カテゴリヘッダーのクリック判定
                int catRectY1 = catY - 2 - yOffset;
                int catRectY2 = catRectY1 + LINE_HEIGHT + 2;
                if (mouseClicked && !wasMousePressed
                        && scaledMouseX >= drawX - 2 && scaledMouseX <= scaledUiWidth - 8
                        && scaledMouseY >= catRectY1 && scaledMouseY <= catRectY2) {
                    if (isCollapsed) {
                        collapsedCategories.remove(category);
                    } else {
                        collapsedCategories.add(category);
                    }
                }

                line++;

                // 展開時のみスキルを表示
                if (!isCollapsed) {
                    for (ConstantSkillsState.ConstantSkill skill : skills) {
                        String lineText = skill.skillName;
                        int statusY = startY + line * LINE_HEIGHT;

                        // ステータスアイコン
                        if (statusY >= 0 && statusY + 10 <= scaledUiHeight) {
                            Identifier statusIcon = skill.status ? STATUS_TRUE : STATUS_FALSE;
                            context.drawTexture(RenderPipelines.GUI_TEXTURED, statusIcon,
                                    drawX + 8, statusY, 0, 0, 8, 8, 8, 8);
                        }

                        for (OrderedText wrapped : RenderUtils.wrapText(mc, lineText, maxTextWidth - 12)) {
                            int textY = startY + line * LINE_HEIGHT;
                            int rectX1 = drawX + 20;
                            int rectY1 = textY - 2 - yOffset;
                            int rectX2 = rectX1 + scaledUiWidth - 48;
                            int rectY2 = rectY1 + LINE_HEIGHT;
                            boolean hovered = (scaledMouseX >= rectX1 && scaledMouseX <= rectX2
                                    && scaledMouseY >= rectY1 && scaledMouseY <= rectY2);

                            if (textY >= 0 && textY + 10 <= scaledUiHeight) {
                                if (hovered) {
                                    context.fill(rectX1 - 1, rectY1 + yOffset, scaledUiWidth - 8,
                                            rectY2 + yOffset, 0xFFFFFFFF);
                                    context.drawText(mc.textRenderer, wrapped, drawX + 20, textY, 0xFF000000, false);

                                    if (mouseClicked && !wasMousePressed) {
                                        ClientPlayNetworking
                                                .send(new ConstantSkillClickPacket(skill.skillName, !skill.status));
                                    }

                                    // ツールチップ情報を保存
                                    if (skill.description != null && !skill.description.isEmpty()) {
                                        tooltipDesc = skill.description;
                                        tooltipX = rectX1;
                                        tooltipY = textY + 12;
                                        tooltipLines = RenderUtils.wrapText(mc, skill.description, scaledUiWidth - 32);
                                    }
                                } else {
                                    context.drawText(mc.textRenderer, wrapped, drawX + 20, textY, 0xFFFFFFFF, false);
                                }
                            }
                            line++;
                        }
                    }
                }

                // カテゴリ間のセパレーター
                int sepY = startY + line * LINE_HEIGHT - 2;
                if (sepY >= 0 && sepY <= scaledUiHeight) {
                    context.fill(drawX, sepY, scaledUiWidth - 8, sepY + 1, RenderUtils.COLOR_SEPARATOR);
                }
                line++;
            }

            state.contentHeight = (int) ((line + 1) * LINE_HEIGHT * SCALE) + 8;
            if (state.contentHeight <= uiHeight) {
                state.scrollOffset = 0;
                ShannonUIModClient.setTabScrollOffset(state.selectedTab, 0);
            }

            // ツールチップを最前面に描画
            if (tooltipDesc != null && tooltipLines != null) {
                int tooltipH = tooltipLines.size() * 12 + 6;
                context.fill(tooltipX - 1, tooltipY - 3, scaledUiWidth - 8, tooltipY + tooltipH - 6, 0xF0000000);
                int descY = tooltipY;
                for (OrderedText descLine : tooltipLines) {
                    context.drawTextWithShadow(mc.textRenderer, descLine, tooltipX, descY, 0xFFFFFF);
                    descY += 12;
                }
            }
        } finally {
            wasMousePressed = mouseClicked;
            context.getMatrices().popMatrix();
        }
    }
}

package com.shannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import com.shannon.network.packet.ConstantSkillsState;
import com.shannon.network.packet.ConstantSkillClickPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.*;

/**
 * 常時スキルUIレンダラー
 * スキルをカテゴリ別にグルーピングし、折りたたみ対応
 */
public class ConstantSkillsUIRenderer {
    private static final float SCALE = RenderUtils.SCALE;
    private static final int LINE_HEIGHT = 10;
    private static boolean wasMousePressed = false;

    // 折りたたまれたカテゴリ
    private static final Set<String> collapsedCategories = new HashSet<>();

    // フラッシュアニメーション: スキル名 → フラッシュ終了時刻(ms)
    private static final java.util.HashMap<String, Long> flashTimers = new java.util.HashMap<>();
    private static final long FLASH_DURATION_MS = 300;

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
        context.enableScissor(x, y, x + uiWidth, y + uiHeight);
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
            int rightEdge = scaledUiWidth - 16;
            int maxTextWidth = rightEdge - drawX;
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
                int catRectY1 = catY - 2 - yOffset;
                int catRectY2 = catRectY1 + LINE_HEIGHT + 2;
                boolean catHovered = scaledMouseX >= drawX - 2 && scaledMouseX <= rightEdge
                        && scaledMouseY >= catRectY1 && scaledMouseY <= catRectY2;

                // カテゴリヘッダー背景（アクセントバー＋ミニプログレスバー付き）
                if (catY >= 0 && catY + LINE_HEIGHT <= scaledUiHeight) {
                    int headerBg = catHovered
                            ? (onCount == skills.size() ? 0xFF3A6A3A : 0xFF4A4A4A)
                            : (onCount == skills.size() ? 0xFF2A4A2A : 0xFF3A3A3A);
                    context.fill(drawX - 2, catY - 2, rightEdge, catY + LINE_HEIGHT, headerBg);
                    // 左アクセントバー
                    int accentColor = onCount == skills.size() ? 0xFF44CC44 : (onCount > 0 ? 0xFFCC8844 : 0xFF555555);
                    context.fill(drawX - 2, catY - 2, drawX + 1, catY + LINE_HEIGHT, accentColor);
                    // テキスト
                    int catTextColor = catHovered ? 0xFFFFFFFF : RenderUtils.COLOR_CATEGORY;
                    context.drawTextWithShadow(mc.textRenderer, Text.literal(categoryText), drawX + 4, catY, catTextColor);
                }

                // カテゴリヘッダーのクリック判定（折りたたみ/展開）
                if (mouseClicked && !wasMousePressed && catHovered) {
                    if (isCollapsed) {
                        collapsedCategories.remove(category);
                    } else {
                        collapsedCategories.add(category);
                    }
                }

                line++;

                // 展開時のみスキルを表示（トグルスイッチ付き）
                if (!isCollapsed) {
                    for (ConstantSkillsState.ConstantSkill skill : skills) {
                        int statusY = startY + line * LINE_HEIGHT + 2;
                        int rowY1 = statusY - 2 - yOffset;
                        int rowY2 = rowY1 + LINE_HEIGHT;
                        boolean rowHovered = scaledMouseX >= drawX && scaledMouseX <= rightEdge
                                && scaledMouseY >= rowY1 && scaledMouseY <= rowY2;

                        if (statusY >= 0 && statusY + 10 <= scaledUiHeight) {
                            // ホバー背景
                            if (rowHovered) {
                                context.fill(drawX - 2, statusY - 2, rightEdge,
                                        statusY + LINE_HEIGHT - 2, 0x33FFFFFF);
                            }

                            // フラッシュエフェクト
                            long flashEnd = flashTimers.getOrDefault(skill.skillName, 0L);
                            if (System.currentTimeMillis() < flashEnd) {
                                context.fill(drawX + 2, statusY - 1, drawX + 24, statusY + 9, 0x66FFFFFF);
                            }

                            // トグルスイッチ
                            RenderUtils.drawToggleSwitch(context, drawX + 4, statusY, skill.status, rowHovered);

                            // スキル名
                            int textColor = rowHovered ? 0xFFFFFF55 : 0xFFFFFFFF;
                            context.drawTextWithShadow(mc.textRenderer, Text.literal(skill.skillName),
                                    drawX + 28, statusY, textColor);

                            // クリック処理
                            if (rowHovered && mouseClicked && !wasMousePressed) {
                                ClientPlayNetworking
                                        .send(new ConstantSkillClickPacket(skill.skillName, !skill.status));
                                flashTimers.put(skill.skillName,
                                        System.currentTimeMillis() + FLASH_DURATION_MS);
                            }

                            // ツールチップ
                            if (rowHovered && skill.description != null && !skill.description.isEmpty()) {
                                tooltipDesc = skill.description;
                                tooltipX = drawX + 28;
                                tooltipY = statusY + 12;
                                tooltipLines = RenderUtils.wrapText(mc, skill.description, maxTextWidth);
                            }
                        }
                        line++;
                    }
                }

                // カテゴリ間の余白
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
                context.fill(tooltipX - 1, tooltipY - 3, rightEdge, tooltipY + tooltipH - 6, 0xF0000000);
                int descY = tooltipY;
                for (OrderedText descLine : tooltipLines) {
                    context.drawTextWithShadow(mc.textRenderer, descLine, tooltipX, descY, 0xFFFFFFFF);
                    descY += 12;
                }
            }
        } finally {
            wasMousePressed = mouseClicked;
            context.disableScissor();
            context.getMatrices().popMatrix();
        }
    }
}

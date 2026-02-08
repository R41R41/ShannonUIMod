package com.shannon.util;

import com.shannon.network.packet.AdvancementsState;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 進捗データ収集ユーティリティ
 * サーバースレッドで実行すること
 */
public class AdvancementCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdvancementCollector.class);

    // バニラカテゴリの表示名マッピング
    private static final Map<String, String> CATEGORY_DISPLAY_NAMES = new LinkedHashMap<>();
    static {
        CATEGORY_DISPLAY_NAMES.put("minecraft:story", "Minecraft（ストーリー）");
        CATEGORY_DISPLAY_NAMES.put("minecraft:adventure", "冒険");
        CATEGORY_DISPLAY_NAMES.put("minecraft:husbandry", "農業");
        CATEGORY_DISPLAY_NAMES.put("minecraft:nether", "ネザー");
        CATEGORY_DISPLAY_NAMES.put("minecraft:end", "ジ・エンド");
    }

    /**
     * 指定プレイヤーの全進捗データを収集（カテゴリ別に整理済み）
     * サーバースレッドで実行すること
     */
    public static AdvancementsState collect(MinecraftServer server, String playerName) {
        AdvancementsState state = new AdvancementsState();
        state.updatedAt = System.currentTimeMillis();

        // プレイヤーを検索
        ServerPlayerEntity targetPlayer = null;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            String name = player.getName().getString();
            if (name.equals(playerName) || name.contains(playerName)) {
                targetPlayer = player;
                break;
            }
        }

        if (targetPlayer == null) {
            state.playerName = playerName + " (not found)";
            return state;
        }

        state.playerName = targetPlayer.getName().getString();
        PlayerAdvancementTracker tracker = targetPlayer.getAdvancementTracker();
        ServerAdvancementLoader advancementLoader = server.getAdvancementLoader();
        Collection<AdvancementEntry> allAdvancements = advancementLoader.getAdvancements();
        int logCount = 0; // デバッグ用: 最初の数件だけログ出力

        // カテゴリ別に仮データを収集
        Map<String, List<AdvancementsState.Advancement>> categoryMap = new LinkedHashMap<>();
        Map<String, int[]> categoryCounts = new LinkedHashMap<>(); // [completed, total]

        for (AdvancementEntry entry : allAdvancements) {
            String advancementId = entry.id().toString();

            // レシピ進捗をスキップ
            if (isRecipeAdvancement(advancementId)) continue;

            // 表示がない進捗をスキップ
            Optional<AdvancementDisplay> display = entry.value().display();
            if (display.isEmpty()) continue;

            // カテゴリを取得
            String categoryId = getCategory(advancementId);
            if (categoryId == null) continue;

            // 進捗状況
            AdvancementProgress progress = tracker.getProgress(entry);
            boolean done = progress != null && progress.isDone();

            int obtainedCount = 0;
            int totalCount = 0;
            if (progress != null) {
                for (String ignored : progress.getObtainedCriteria()) obtainedCount++;
                totalCount = obtainedCount;
                for (String ignored : progress.getUnobtainedCriteria()) totalCount++;
            }

            // Advancement データ作成（TextをJSON形式で保存→クライアント側で言語に合わせて表示）
            AdvancementsState.Advancement adv = new AdvancementsState.Advancement();
            AdvancementDisplay d = display.get();
            adv.title = serializeText(d.getTitle(), server);
            adv.description = serializeText(d.getDescription(), server);
            if (logCount < 3) {
                LOGGER.info("[Advancements] {} -> title='{}', contentType={}",
                        advancementId, adv.title,
                        d.getTitle().getContent().getClass().getSimpleName());
                logCount++;
            }
            adv.done = done;
            adv.progress = (totalCount > 1 && !done) ? obtainedCount + "/" + totalCount : "";

            categoryMap.computeIfAbsent(categoryId, k -> new ArrayList<>()).add(adv);
            int[] counts = categoryCounts.computeIfAbsent(categoryId, k -> new int[]{0, 0});
            if (done) counts[0]++;
            counts[1]++;
        }

        // カテゴリをソート（バニラ優先、データパックはアルファベット順）
        List<String> sortedCategories = new ArrayList<>(categoryMap.keySet());
        sortedCategories.sort((a, b) -> {
            boolean aVanilla = CATEGORY_DISPLAY_NAMES.containsKey(a);
            boolean bVanilla = CATEGORY_DISPLAY_NAMES.containsKey(b);
            if (aVanilla && !bVanilla) return -1;
            if (!aVanilla && bVanilla) return 1;
            if (aVanilla) {
                // バニラ同士は定義順
                List<String> order = new ArrayList<>(CATEGORY_DISPLAY_NAMES.keySet());
                return Integer.compare(order.indexOf(a), order.indexOf(b));
            }
            return a.compareTo(b);
        });

        // カテゴリデータを構築
        state.categories = new ArrayList<>();
        for (String categoryId : sortedCategories) {
            AdvancementsState.Category cat = new AdvancementsState.Category();
            cat.categoryId = categoryId;
            cat.displayName = CATEGORY_DISPLAY_NAMES.getOrDefault(categoryId, categoryId);
            int[] counts = categoryCounts.getOrDefault(categoryId, new int[]{0, 0});
            cat.completed = counts[0];
            cat.total = counts[1];
            // 完了済みを先に、未完了を後に
            List<AdvancementsState.Advancement> advs = categoryMap.get(categoryId);
            advs.sort((a, b) -> Boolean.compare(b.done, a.done));
            cat.advancements = advs;
            state.categories.add(cat);
        }

        return state;
    }

    /**
     * TextをJSON形式またはプレーンテキストの文字列に変換
     * バニラ進捗（"advancements."で始まるキー）はクライアント側で言語解決させる
     * データパック/MODの進捗はサーバー側で解決済みテキストを送る（クライアントに翻訳がないため）
     */
    private static String serializeText(Text text, MinecraftServer server) {
        if (text.getContent() instanceof TranslatableTextContent translatable) {
            String key = translatable.getKey();
            // バニラの進捗キーはクライアント側の言語ファイルに存在する
            if (key.startsWith("advancements.")) {
                return "{\"translate\":\"" + key + "\"}";
            }
            // データパック/MODのキーはサーバー側で解決（クライアントに翻訳がない）
            return text.getString();
        }

        // リテラルテキスト等はそのまま文字列化
        return text.getString();
    }

    private static boolean isRecipeAdvancement(String advancementId) {
        int colonIndex = advancementId.indexOf(':');
        if (colonIndex < 0) return false;
        String path = advancementId.substring(colonIndex + 1);
        return path.startsWith("recipes/");
    }

    private static String getCategory(String advancementId) {
        int colonIndex = advancementId.indexOf(':');
        if (colonIndex < 0) return null;
        String namespace = advancementId.substring(0, colonIndex);
        String path = advancementId.substring(colonIndex + 1);
        int slashIndex = path.indexOf('/');
        if (slashIndex > 0) {
            return namespace + ":" + path.substring(0, slashIndex);
        }
        return namespace;
    }
}

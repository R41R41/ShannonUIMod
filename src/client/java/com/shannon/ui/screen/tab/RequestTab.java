package com.shannon.ui.screen.tab;

import com.shannon.ShannonUIMod;
import com.shannon.ui.ClientConfig;
import com.shannon.ui.gfx.Gui;
import com.shannon.ui.gfx.Palette;
import com.shannon.ui.net.BotPhrases;
import com.shannon.ui.net.ClientActions;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Asking the bot for things without typing: saved requests on the left, and on the right an item
 * search, a count and the sentence that will be sent.
 */
public class RequestTab extends ShannonTab {
    private static final Identifier ICON = Identifier.of(ShannonUIMod.MOD_ID, "textures/request.png");
    private static final int LIST_W = 104;
    private static final int ROW = 14;
    private static final int SLOT = 18;
    private static final int COLUMNS = 12;
    private static final int ROWS = 3;
    private static final int[] COUNTS = {1, 8, 16, 32, 64};

    /** Every item with its name in the player's language, built once per opening. */
    private record Entry(Item item, String id, Text name, String search) {
    }

    /** Survive switching tabs and reopening, like the other tabs' selections. */
    private static String query = "";
    private static String selectedId = "minecraft:oak_log";
    private static int count = 32;

    private final List<Entry> all = new ArrayList<>();
    private List<Entry> matches = List.of();
    private int resultScroll;
    private int favoriteScroll;
    private TextFieldWidget search;
    private final List<ButtonWidget> countButtons = new ArrayList<>();

    @Override
    public Identifier icon() {
        return ICON;
    }

    @Override
    public Text title() {
        return Text.translatable("shannonuimod.tab.request");
    }

    @Override
    protected void build() {
        if (all.isEmpty()) {
            for (Item item : Registries.ITEM) {
                if (item == Items.AIR) {
                    continue;
                }
                String id = Registries.ITEM.getId(item).toString();
                Text name = new ItemStack(item).getName();
                all.add(new Entry(item, id, name, (name.getString() + " " + id).toLowerCase(Locale.ROOT)));
            }
        }
        int rightX = x + LIST_W + 6;
        int rightW = w - LIST_W - 6;
        search = add(new TextFieldWidget(client.textRenderer, rightX + 1, y + 11, rightW - 2, 16,
                Text.translatable("shannonuimod.request.search")));
        search.setMaxLength(64);
        search.setPlaceholder(Text.translatable("shannonuimod.request.search").formatted(Formatting.DARK_GRAY));
        search.setText(query);
        search.setChangedListener(text -> {
            query = text;
            resultScroll = 0;
            filter();
        });
        filter();

        countButtons.clear();
        int countY = y + 104;
        int buttonW = (rightW - (COUNTS.length - 1) * 2) / COUNTS.length;
        for (int i = 0; i < COUNTS.length; i++) {
            int value = COUNTS[i];
            ButtonWidget button = ButtonWidget.builder(Text.translatable("shannonuimod.request.count", value), b -> {
                count = value;
                updateCounts();
            }).dimensions(rightX + i * (buttonW + 2), countY, buttonW, 18).build();
            countButtons.add(add(button));
        }
        updateCounts();

        int buttonY = y + h - 20;
        int sendW = 90;
        add(ButtonWidget.builder(Text.translatable("shannonuimod.request.send"), button -> send(sentence()))
                .dimensions(x + w - sendW, buttonY, sendW, 20).build());
        add(ButtonWidget.builder(Text.translatable("shannonuimod.request.save"), button -> save())
                .dimensions(x + w - sendW - 4 - 60, buttonY, 60, 20)
                .tooltip(Tooltip.of(Text.translatable("shannonuimod.request.save.tip")))
                .build());
    }

    private void filter() {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            matches = all;
            return;
        }
        List<Entry> found = new ArrayList<>();
        for (Entry entry : all) {
            if (entry.search().contains(needle)) {
                found.add(entry);
            }
        }
        matches = found;
    }

    private void updateCounts() {
        for (int i = 0; i < countButtons.size(); i++) {
            countButtons.get(i).active = COUNTS[i] != count;
        }
    }

    private Entry selected() {
        for (Entry entry : all) {
            if (entry.id().equals(selectedId)) {
                return entry;
            }
        }
        return null;
    }

    private String sentence() {
        Entry entry = selected();
        return entry == null ? null : BotPhrases.collect(entry.name().getString(), count);
    }

    private void send(String text) {
        if (text == null || !ClientActions.chat(text)) {
            return;
        }
        if (client.player != null) {
            client.player.sendMessage(Text.translatable("shannonuimod.command.sent", text), true);
        }
        screen.close();
    }

    private void save() {
        String text = sentence();
        ClientConfig config = shannon.config();
        if (text == null || config.favorites.contains(text)) {
            return;
        }
        config.favorites.add(0, text);
        while (config.favorites.size() > ClientConfig.MAX_FAVORITES) {
            config.favorites.remove(config.favorites.size() - 1);
        }
        config.save();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderFavorites(context, mouseX, mouseY);
        int rightX = x + LIST_W + 6;
        int rightW = w - LIST_W - 6;
        Gui.label(context, Text.translatable("shannonuimod.request.what"), rightX + 1, y);
        renderResults(context, mouseX, mouseY, rightX, y + 31);

        Entry entry = selected();
        Gui.label(context, entry == null ? Text.translatable("shannonuimod.request.pick")
                : Text.translatable("shannonuimod.request.how_many", entry.name()), rightX + 1, y + 93);

        int sentenceY = y + 126;
        Gui.inset(context, rightX, sentenceY, rightW, 30, Palette.SLOT);
        String text = sentence();
        if (text != null) {
            Gui.paragraph(context, Text.translatable("shannonuimod.request.quote", text), rightX + 4, sentenceY + 5,
                    rightW - 8, Palette.WHITE, 2);
        }
    }

    private void renderFavorites(DrawContext context, int mouseX, int mouseY) {
        Gui.label(context, Text.translatable("shannonuimod.request.saved"), x + 1, y);
        int top = y + 11;
        int height = h - 11;
        Gui.inset(context, x, top, LIST_W, height, Palette.SLOT);
        List<String> favorites = shannon.config().favorites;
        if (favorites.isEmpty()) {
            Gui.paragraph(context, Text.translatable("shannonuimod.request.saved.none"), x + 5, top + 5,
                    LIST_W - 10, Palette.WHITE, 4);
            return;
        }
        int visible = (height - 4) / ROW;
        favoriteScroll = Math.max(0, Math.min(favoriteScroll, favorites.size() - visible));
        for (int i = 0; i < Math.min(visible, favorites.size() - favoriteScroll); i++) {
            String favorite = favorites.get(i + favoriteScroll);
            int rowY = top + 2 + i * ROW;
            boolean hovered = Gui.inside(mouseX, mouseY, x + 2, rowY, LIST_W - 4, ROW);
            if (hovered) {
                context.fill(x + 2, rowY, x + LIST_W - 2, rowY + ROW, 0x20FFFFFF);
                Gui.outline(context, x + 2, rowY, LIST_W - 4, ROW, Palette.WHITE);
                screen.tooltip(List.of(Text.literal(favorite),
                        Text.translatable("shannonuimod.request.saved.send").formatted(Formatting.GRAY),
                        Text.translatable("shannonuimod.request.saved.remove").formatted(Formatting.GRAY)));
            }
            Gui.text(context, Gui.fit(favorite, LIST_W - 10), x + 5, rowY + 3, Palette.WHITE);
        }
    }

    private void renderResults(DrawContext context, int mouseX, int mouseY, int left, int top) {
        int maxScroll = Math.max(0, (matches.size() + COLUMNS - 1) / COLUMNS - ROWS);
        resultScroll = Math.max(0, Math.min(resultScroll, maxScroll));
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int sx = left + column * SLOT;
                int sy = top + row * SLOT;
                Gui.slot(context, sx, sy);
                int index = (row + resultScroll) * COLUMNS + column;
                if (index >= matches.size()) {
                    continue;
                }
                Entry entry = matches.get(index);
                if (entry.id().equals(selectedId)) {
                    Gui.outline(context, sx, sy, SLOT, SLOT, Palette.WHITE);
                }
                context.drawItem(new ItemStack(entry.item()), sx + 1, sy + 1);
                if (Gui.inside(mouseX, mouseY, sx, sy, SLOT, SLOT)) {
                    context.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, Palette.SLOT_HOVER);
                    screen.tooltip(List.of(entry.name()));
                }
            }
        }
        if (matches.isEmpty()) {
            Gui.text(context, Text.translatable("shannonuimod.request.none"), left + 4, top + 5, Palette.WHITE);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int top = y + 13;
        List<String> favorites = shannon.config().favorites;
        if (Gui.inside(mouseX, mouseY, x, top, LIST_W, h - 13)) {
            int index = (int) ((mouseY - top) / ROW) + favoriteScroll;
            if (index >= 0 && index < favorites.size()) {
                if (button == 1) {
                    favorites.remove(index);
                    shannon.config().save();
                } else {
                    send(favorites.get(index));
                }
                return true;
            }
            return false;
        }
        int left = x + LIST_W + 6;
        int resultsTop = y + 31;
        if (Gui.inside(mouseX, mouseY, left, resultsTop, COLUMNS * SLOT, ROWS * SLOT)) {
            int column = (int) ((mouseX - left) / SLOT);
            int row = (int) ((mouseY - resultsTop) / SLOT);
            int index = (row + resultScroll) * COLUMNS + column;
            if (index < matches.size()) {
                selectedId = matches.get(index).id();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX < x + LIST_W) {
            favoriteScroll -= (int) Math.signum(amount);
        } else {
            resultScroll -= (int) Math.signum(amount);
        }
        return true;
    }
}

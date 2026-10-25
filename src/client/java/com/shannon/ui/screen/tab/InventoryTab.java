package com.shannon.ui.screen.tab;

import com.shannon.ShannonUIMod;
import com.shannon.model.BotVitals;
import com.shannon.model.InventoryState;
import com.shannon.sync.StateChannels;
import com.shannon.ui.gfx.Gui;
import com.shannon.ui.gfx.Palette;
import com.shannon.ui.hud.BotLocator;
import com.shannon.ui.net.ClientActions;
import com.shannon.ui.screen.ShannonScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * The bot's inventory, laid out like the player's own, with a column about how the bot is doing.
 * Clicking a stack asks the bot to drop it for the player.
 */
public class InventoryTab extends ShannonTab {
    private static final Identifier ICON = Identifier.of(ShannonUIMod.MOD_ID, "textures/inventory.png");
    private static final int SLOT = 18;
    /** The armour column and the model beside it. */
    private static final int ARMOR_H = 72;
    /** Three rows, the gap and the hotbar. */
    private static final int GRID_H = 3 * SLOT + 4 + SLOT;
    /** A worn-out tool's slot: vanilla's red, faint. */
    private static final int WORN_SLOT = 0x60FF3030;

    @Override
    public Identifier icon() {
        return ICON;
    }

    @Override
    public Text title() {
        return Text.translatable("shannonuimod.tab.inventory");
    }

    private InventoryState inventory() {
        return shannon.store().get(StateChannels.INVENTORY);
    }

    /** Where each clickable stack is drawn this frame, rebuilt every render. */
    private record Spot(int x, int y, InventoryState.Stack stack) {
    }

    private final List<Spot> spots = new ArrayList<>();

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        spots.clear();
        InventoryState inventory = inventory();
        if (inventory == null) {
            Gui.label(context, Text.translatable("shannonuimod.inventory.none"), x + 1, y + 2);
            return;
        }
        // Armour and model on top, the 36 slots below. A short screen (GUI scale 3 in a 720p window) first loses the top gap,
        // then the "Items" label, so the hotbar never runs past the panel.
        int free = h - ARMOR_H - GRID_H;
        boolean itemsLabel = free >= 16;
        int top = y + Math.max(0, Math.min(10, free - (itemsLabel ? 16 : 4)));
        InventoryState.Stack[] armor = {inventory.head, inventory.chest, inventory.legs, inventory.feet};
        for (int i = 0; i < armor.length; i++) {
            slot(context, x, top + i * SLOT, armor[i]);
        }
        // Where the player model would be.
        Gui.inset(context, x + 22, top, 52, ARMOR_H, 0xFF000000);
        Gui.face(context, x + 32, top + 16, 32);
        Text name = Text.literal(shannon.botName());
        Gui.text(context, Gui.fit(shannon.botName(), 50), x + 48 - Math.min(50, Gui.width(name)) / 2, top + 58, Palette.GRAY);

        Gui.label(context, Text.translatable("shannonuimod.inventory.offhand"), x + 79, top + 36);
        slot(context, x + 78, top + 46, inventory.offHand);

        int gridY = top + ARMOR_H + 4;
        if (itemsLabel) {
            Gui.label(context, Text.translatable("shannonuimod.inventory.items"), x + 1, gridY);
            gridY += 10;
        }
        for (int i = 9; i < 36; i++) {
            int col = (i - 9) % 9;
            int row = (i - 9) / 9;
            slot(context, x + col * SLOT, gridY + row * SLOT, stackAt(inventory, i));
        }
        int hotbarY = gridY + 3 * SLOT + 4;
        for (int i = 0; i < 9; i++) {
            slot(context, x + i * SLOT, hotbarY, stackAt(inventory, i));
            if (i == inventory.selectedSlot) {
                Gui.outline(context, x + i * SLOT - 1, hotbarY - 1, SLOT + 2, SLOT + 2, Palette.WHITE);
            }
        }

        renderVitals(context, x + 172, y, w - 172);

        for (Spot spot : spots) {
            if (spot.stack() != null && Gui.inside(mouseX, mouseY, spot.x(), spot.y(), SLOT, SLOT)) {
                context.fill(spot.x() + 1, spot.y() + 1, spot.x() + SLOT - 1, spot.y() + SLOT - 1, Palette.SLOT_HOVER);
                screen.tooltip(tooltip(spot.stack()));
            }
        }
    }

    private static InventoryState.Stack stackAt(InventoryState inventory, int index) {
        return inventory.main != null && index < inventory.main.size() ? inventory.main.get(index) : null;
    }

    private void slot(DrawContext context, int sx, int sy, InventoryState.Stack stack) {
        Gui.slot(context, sx, sy);
        if (stack != null && stack.item != null) {
            if (stack.maxDamage > 0 && stack.durability() <= Gui.WORN) {
                context.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, WORN_SLOT);
            }
            Gui.item(context, stack, sx + 1, sy + 1);
        }
        spots.add(new Spot(sx, sy, stack));
    }

    private List<Text> tooltip(InventoryState.Stack stack) {
        ItemStack item = Gui.stack(stack);
        List<Text> lines = new ArrayList<>();
        lines.add(item.isEmpty() ? Text.literal(stack.item) : item.getName().copy().formatted(stack.enchanted ? Formatting.AQUA : Formatting.WHITE));
        if (stack.maxDamage > 0) {
            lines.add(Gui.durability(stack));
            if (stack.durability() <= Gui.WORN) {
                lines.add(Text.translatable("shannonuimod.inventory.worn").formatted(Formatting.RED));
            }
        }
        if (stack.count > 1) {
            lines.add(Text.translatable("shannonuimod.inventory.count", stack.count).formatted(Formatting.GRAY));
        }
        lines.add(Text.translatable("shannonuimod.inventory.take_one").formatted(Formatting.AQUA));
        if (stack.count > 1) {
            lines.add(Text.translatable("shannonuimod.inventory.take_all").formatted(Formatting.AQUA));
        }
        return lines;
    }

    private void renderVitals(DrawContext context, int left, int top, int width) {
        Gui.label(context, Text.translatable("shannonuimod.inventory.vitals"), left + 1, top);
        int wellH = h - 10;
        Gui.inset(context, left, top + 10, width, wellH, Palette.SLOT);
        BotVitals vitals = shannon.store().get(StateChannels.VITALS);
        int rowX = left + 6;
        int rowW = width - 12;
        int cursor = top + 16;
        if (vitals == null || !vitals.online) {
            Gui.paragraph(context, Text.translatable("shannonuimod.inventory.offline"), rowX, cursor, rowW, Palette.WHITE, 3);
            return;
        }
        Gui.text(context, Text.translatable("shannonuimod.inventory.health"), rowX, cursor, Palette.WHITE);
        cursor += 10;
        Gui.hearts(context, rowX, cursor, vitals.health, vitals.maxHealth);
        cursor += 13;
        Gui.text(context, Text.translatable("shannonuimod.inventory.food"), rowX, cursor, Palette.WHITE);
        cursor += 10;
        Gui.food(context, rowX, cursor, vitals.food);
        cursor += 14;
        cursor = row(context, rowX, rowW, cursor, Text.translatable("shannonuimod.inventory.position"),
                Text.literal((int) vitals.x + ", " + (int) vitals.y + ", " + (int) vitals.z));
        if (vitals.biome != null) {
            Identifier biome = Identifier.tryParse(vitals.biome);
            Text biomeName = biome == null ? Text.literal(vitals.biome)
                    : Text.translatable("biome." + biome.getNamespace() + "." + biome.getPath());
            cursor = row(context, rowX, rowW, cursor, Text.translatable("shannonuimod.inventory.biome"), biomeName);
        }
        BotLocator where = BotLocator.locate(client, shannon.botName(), vitals);
        cursor = row(context, rowX, rowW, cursor, Text.translatable("shannonuimod.inventory.distance"),
                where == null ? Text.translatable("shannonuimod.inventory.far") : Text.literal(Math.round(where.distance()) + "m"));
        if (vitals.held != null) {
            ItemStack held = Gui.stack(vitals.held);
            cursor = row(context, rowX, rowW, cursor, Text.translatable("shannonuimod.inventory.holding"),
                    held.isEmpty() ? Text.literal(vitals.held.item) : held.getName());
            if (vitals.held.maxDamage > 0) {
                row(context, rowX, rowW, cursor, Text.translatable("shannonuimod.inventory.wear"), Gui.durabilityValue(vitals.held));
            }
        } else if (vitals.mainHand != null) {
            ItemStack held = Gui.stack(vitals.mainHand, 1);
            row(context, rowX, rowW, cursor, Text.translatable("shannonuimod.inventory.holding"),
                    held.isEmpty() ? Text.literal(vitals.mainHand) : held.getName());
        }
    }

    private static int row(DrawContext context, int x, int width, int y, Text label, Text value) {
        Gui.text(context, label, x, y, Palette.WHITE);
        int room = width - Gui.width(label) - 6;
        Text fitted = Gui.width(value) <= room ? value : Gui.fit(value.getString(), room);
        Gui.text(context, fitted, x + width - Gui.width(fitted), y, Palette.WHITE);
        return y + 12;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Spot spot : spots) {
            if (spot.stack() != null && spot.stack().item != null
                    && Gui.inside(mouseX, mouseY, spot.x(), spot.y(), SLOT, SLOT)) {
                int count = ShannonScreen.shiftDown() ? spot.stack().count : 1;
                ClientActions.giveItem(spot.stack().item, count);
                return true;
            }
        }
        return false;
    }
}

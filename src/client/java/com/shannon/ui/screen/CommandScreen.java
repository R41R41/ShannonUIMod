package com.shannon.ui.screen;

import com.shannon.sync.BotCommand;
import com.shannon.ui.ShannonClient;
import com.shannon.ui.gfx.Gui;
import com.shannon.ui.gfx.Icons;
import com.shannon.ui.gfx.Palette;
import com.shannon.ui.gfx.PixelIcon;
import com.shannon.ui.input.KeyBindings;
import com.shannon.ui.net.ClientActions;
import com.shannon.ui.screen.tab.InventoryTab;
import com.shannon.ui.state.BotStatus;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The command switcher, modelled on vanilla's game mode switcher (F3 + F4).
 *
 * <p>Hold the key, point at an order and let go; or tap the key and click. The arrow keys, the
 * number keys and the key itself also move the selection, and Enter confirms.
 */
public class CommandScreen extends OverlayScreen {
    private static final int SLOT = 26;
    private static final int SPACING = 30;
    /** A press held at least this long makes releasing the key confirm the selection. */
    private static final long HOLD_MS = 250;

    private record Option(String key, PixelIcon icon, Runnable action) {
    }

    private final KeyBinding trigger;
    private final long openedAt = System.currentTimeMillis();
    private final List<Option> options;
    private int selected;
    /** False while the key that opened this screen is still held down from that press. */
    private boolean triggerReleased;
    private int lastMouseX = -1;
    private int lastMouseY = -1;

    public CommandScreen(KeyBinding trigger) {
        super(Text.translatable("shannonuimod.commands.title"));
        this.trigger = trigger;
        this.options = List.of(
                new Option("stop", Icons.CMD_STOP, () -> ClientActions.command(BotCommand.STOP)),
                new Option("follow", Icons.CMD_FOLLOW, () -> ClientActions.command(BotCommand.FOLLOW)),
                new Option("come", Icons.CMD_COME, () -> ClientActions.command(BotCommand.COME)),
                new Option("resume", Icons.CMD_RESUME, () -> ClientActions.command(BotCommand.RESUME)),
                new Option("cancel", Icons.CMD_CANCEL, () -> ClientActions.command(BotCommand.CANCEL)),
                new Option("bag", Icons.CMD_BAG, null));
        BotStatus status = ShannonClient.get().store().status();
        this.selected = status == BotStatus.WAITING || status == BotStatus.ERROR ? 3 : 1;
    }

    private int rowX() {
        return (width - (options.size() * SPACING - (SPACING - SLOT))) / 2;
    }

    private int rowY() {
        return height / 2 - 30;
    }

    private int optionAt(double mouseX, double mouseY) {
        for (int i = 0; i < options.size(); i++) {
            if (Gui.inside(mouseX, mouseY, rowX() + i * SPACING, rowY(), SLOT, SLOT)) {
                return i;
            }
        }
        return -1;
    }

    private void confirm() {
        Option option = options.get(selected);
        if (option.action() == null) {
            client.setScreen(new ShannonScreen(InventoryTab.class));
            return;
        }
        option.action().run();
        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        int triggerKey = KeyBindings.keyCode(trigger);
        if (!triggerReleased && (triggerKey < 0
                || GLFW.glfwGetKey(client.getWindow().getHandle(), triggerKey) != GLFW.GLFW_PRESS)) {
            // Released before this screen could see it: a quick tap.
            triggerReleased = true;
        }
        if (mouseX != lastMouseX || mouseY != lastMouseY) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            int hovered = optionAt(mouseX, mouseY);
            if (hovered >= 0) {
                selected = hovered;
            }
        }
        Option option = options.get(selected);
        Text label = Text.translatable("shannonuimod.command." + option.key());
        Text description = Text.translatable("shannonuimod.command." + option.key() + ".desc");
        int centerX = width / 2;

        // One box holds the name, the description and the row, so the text stays readable over
        // bright skies and name tags.
        int rowW = options.size() * SPACING - (SPACING - SLOT) + 12;
        int boxW = Math.max(rowW, Math.max(Gui.width(label), Gui.width(description)) + 16);
        int boxX = centerX - boxW / 2;
        int boxY = rowY() - 34;
        int boxH = rowY() + SLOT + 6 - boxY;
        context.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0x8C000000);
        Gui.outline(context, boxX, boxY, boxW, boxH, 0xFF4A4A4A);
        Gui.text(context, label, centerX - Gui.width(label) / 2, rowY() - 28, Palette.WHITE);
        Gui.text(context, description, centerX - Gui.width(description) / 2, rowY() - 17, Palette.GRAY);
        for (int i = 0; i < options.size(); i++) {
            int x = rowX() + i * SPACING;
            int y = rowY();
            context.fill(x, y, x + SLOT, y + SLOT, 0x73000000);
            Gui.outline(context, x, y, SLOT, SLOT, i == selected ? Palette.WHITE : 0xFF6B6B6B);
            if (i == selected) {
                context.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0x1FFFFFFF);
            }
            options.get(i).icon().draw(context, x + 7, y + 7);
            Text number = Text.literal(String.valueOf(i + 1));
            Gui.text(context, number, x + SLOT - Gui.width(number) - 1, y + SLOT - 8, Palette.DARK_GRAY);
        }

        renderHints(context, centerX, rowY() + SLOT + 12);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    /**
     * The keys that work right now: while the opening key is held, letting go chooses; after a
     * quick tap the menu stays open and Enter or a click chooses.
     */
    private void renderHints(DrawContext context, int centerX, int y) {
        Text keyName = KeyBindings.keyName(trigger);
        Text next = Text.translatable("shannonuimod.commands.next");
        int width = Gui.keyHintWidth(keyName, next) + 10;
        Text enter = Text.translatable("key.keyboard.enter");
        Text choose = Text.translatable("shannonuimod.commands.choose");
        Text escape = Text.translatable("key.keyboard.escape");
        Text close = Text.translatable("shannonuimod.talk.close");
        Text release = Text.translatable("shannonuimod.commands.release");
        if (triggerReleased) {
            width += Gui.keyHintWidth(enter, choose) + 10 + Gui.keyHintWidth(escape, close);
        } else {
            width += Gui.width(release);
        }
        int x = centerX - width / 2;
        context.fill(x - 4, y - 2, x + width + 4, y + 14, 0x80000000);
        x += Gui.keyHint(context, keyName, next, x, y) + 10;
        if (triggerReleased) {
            x += Gui.keyHint(context, enter, choose, x, y) + 10;
            Gui.keyHint(context, escape, close, x, y);
        } else {
            Gui.text(context, release, x, y + 2, Palette.GRAY);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int index = optionAt(click.x(), click.y());
        if (index >= 0) {
            selected = index;
            confirm();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (key == KeyBindings.keyCode(trigger)) {
            // Key repeat while still holding the opening press must not spin the selection.
            if (triggerReleased) {
                selected = (selected + 1) % options.size();
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_TAB) {
            selected = (selected + 1) % options.size();
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT) {
            selected = (selected + options.size() - 1) % options.size();
            return true;
        }
        if (key >= GLFW.GLFW_KEY_1 && key < GLFW.GLFW_KEY_1 + options.size()) {
            selected = key - GLFW.GLFW_KEY_1;
            confirm();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            confirm();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean keyReleased(KeyInput input) {
        if (input.key() == KeyBindings.keyCode(trigger) && !triggerReleased) {
            triggerReleased = true;
            if (System.currentTimeMillis() - openedAt >= HOLD_MS) {
                confirm();
            }
            return true;
        }
        return super.keyReleased(input);
    }
}

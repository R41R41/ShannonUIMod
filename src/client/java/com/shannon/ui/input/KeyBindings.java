package com.shannon.ui.input;

import com.shannon.ShannonUIMod;
import com.shannon.sync.Actions;
import com.shannon.ui.ShannonClient;
import com.shannon.ui.net.ClientActions;
import com.shannon.ui.screen.CommandScreen;
import com.shannon.ui.screen.QuickChatScreen;
import com.shannon.ui.screen.ShannonScreen;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * The mod's key bindings, all rebindable under Controls.
 *
 * <p>Keys act only while no screen is open, so typing in another mod's screen never triggers them.
 */
public final class KeyBindings {
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of(ShannonUIMod.MOD_ID, "category"));

    private KeyBinding talk;
    private KeyBinding commands;
    private KeyBinding details;
    private KeyBinding pushToTalk;
    private KeyBinding voiceMode;
    private KeyBinding toggleHud;
    private boolean talking;

    public void register() {
        talk = bind("talk", GLFW.GLFW_KEY_Y);
        commands = bind("commands", GLFW.GLFW_KEY_G);
        details = bind("details", GLFW.GLFW_KEY_U);
        pushToTalk = bind("push_to_talk", GLFW.GLFW_KEY_B);
        voiceMode = bind("voice_mode", GLFW.GLFW_KEY_UNKNOWN);
        toggleHud = bind("toggle_hud", GLFW.GLFW_KEY_UNKNOWN);
    }

    private static KeyBinding bind(String name, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + ShannonUIMod.MOD_ID + "." + name, InputUtil.Type.KEYSYM, key, CATEGORY));
    }

    public void tick(MinecraftClient client, ShannonClient shannon) {
        if (client.player == null) {
            return;
        }
        if (client.currentScreen == null) {
            if (talk.wasPressed()) {
                client.setScreen(new QuickChatScreen());
            } else if (commands.wasPressed()) {
                client.setScreen(new CommandScreen(commands));
            } else if (details.wasPressed()) {
                client.setScreen(new ShannonScreen());
            }
            while (voiceMode.wasPressed()) {
                ClientActions.send(Actions.VOICE_MODE, new Actions.Empty());
            }
            while (toggleHud.wasPressed()) {
                shannon.config().showCard = !shannon.config().showCard;
                shannon.config().save();
            }
        }
        boolean held = client.currentScreen == null && pushToTalk.isPressed();
        if (held != talking) {
            talking = held;
            ClientActions.send(Actions.VOICE_PTT, new Actions.VoicePtt(held));
        }
    }

    public KeyBinding talk() {
        return talk;
    }

    public KeyBinding commands() {
        return commands;
    }

    public KeyBinding details() {
        return details;
    }

    public KeyBinding pushToTalk() {
        return pushToTalk;
    }

    /** The name of the key bound to {@code binding}, as the Controls screen shows it. */
    public static Text keyName(KeyBinding binding) {
        return binding.getBoundKeyLocalizedText();
    }

    /** The GLFW key code bound to {@code binding}, or -1 when it is a mouse button or unbound. */
    public static int keyCode(KeyBinding binding) {
        InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(binding);
        return key.getCategory() == InputUtil.Type.KEYSYM ? key.getCode() : -1;
    }
}

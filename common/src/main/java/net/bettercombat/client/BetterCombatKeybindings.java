package net.bettercombat.client;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class BetterCombatKeybindings {
    public static KeyBinding feintKeyBinding;
    public static KeyBinding toggleMineKeyBinding;
    public static KeyBinding dashKeyBinding;
    public static List<KeyBinding> all;

    static {
        feintKeyBinding = new KeyBinding(
                "keybinds.bettercombat.feint",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(),
                "Better Combat");

        toggleMineKeyBinding = new KeyBinding(
                "keybinds.bettercombat.toggle_mine_with_weapons",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(),
                "Better Combat");
        dashKeyBinding = new KeyBinding(
                "keybinds.bettercombat.dash",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                "Better Combat");
        all = List.of(feintKeyBinding, toggleMineKeyBinding,dashKeyBinding);
    }
}

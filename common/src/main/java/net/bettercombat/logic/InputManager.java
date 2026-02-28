package net.bettercombat.logic;

import net.bettercombat.network.Packets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

import static net.bettercombat.client.BetterCombatKeybindings.*;

public final class InputManager {
    public static final Logger LOGGER = LogManager.getLogger(InputManager.class);

    private static int lastMask = 0;
    private static long lastSent = 0L;
    private static final long SEND_INTERVAL_MS = 150;


    public static enum Bit {
        W_BIT("forwardKey"),
        A_BIT("leftKey"),
        S_BIT("rightKey"),
        D_BIT("backKey"),
        RIGHT_CLICK("useKey"),
        DASH("dashKeyBinding"),
        ATTACK_BIT("attackKey"),
        JUMP_KEY("jumpKey"),
        SPRINT_KEY("sprintKey"),
        INVENTORY_KEY("inventoryKey"),
        SWAP_HANDS_KEY("swapHandsKey"),
        SNEAK_KEY("sneakKey"),
        PICK_ITEM_KEY("pickItemKey"),
        CHAT_KEY("chatKey"),
        PLAYER_LIST_KEY("playerListKey"),
        COMMAND_KEY("commandKey"),
        SOCIAL_INTERACTIONS_KEY("socialInteractionsKey"),
        SCREENSHOT_KEY("screenshotKey"),
        TOGGLE_PERSPECT_KEY("togglePerspectiveKey"),
        SMOOTH_CAMERA_KEY("smoothCameraKey"),
        FULLSCREEN_KEY("fullscreenKey"),
        SPECTATOR_OUTLINES_KEY("spectatorOutlinesKey"),
        ADVANCEMENT_KEY("advancementsKey"),
        SAVE_TOOLBAR_ACTIVATOR_KEY("saveToolbarActivatorKey"),
        LOAD_TOOLBAR_ACTIVATOR_KEY("loadToolbarActivatorKey"),
        FEINT_KEY("feintKeyBinding"),
        TOGGLE_MINE_KEY("toggleMineKeyBinding"),
        DROP_KEY("dropKey");

        private String name;

        Bit(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static List<Bit> findBitsByNames(String[] names) {
            List<String> target = Arrays.asList(names);
            List<Bit> result = new ArrayList<>();

            for (Bit bit : Bit.values()) {
                if (target.contains(bit.getName())) {
                    result.add(bit);
                }
            }

            return result;
        }
    }

    private static boolean isPressed(int mask, Bit bit) {
        return (mask & (1 << bit.ordinal())) != 0;
    }

    public static boolean isPressed(int mask, String[] buttons) {
        for (Bit button : Bit.findBitsByNames(buttons)) {
            if (!isPressed(mask, button)) {
                return false;
            }
        }
        return true;
    }


    public static boolean forwardKey(int mask) {
        return isPressed(mask, Bit.W_BIT);
    }

    public static boolean leftKey(int mask) {
        return isPressed(mask, Bit.A_BIT);
    }

    public static boolean rightKey(int mask) {
        return isPressed(mask, Bit.S_BIT);
    }

    public static boolean backKey(int mask) {
        return isPressed(mask, Bit.D_BIT);
    }

    public static boolean dashKey(int mask) {
        return isPressed(mask, Bit.DASH);
    }

    public static boolean rightClick(int mask) {
        return isPressed(mask, Bit.RIGHT_CLICK);
    }



    public static boolean notMoved(int mask) {
        return !isPressed(mask, Bit.W_BIT) && !isPressed(mask, Bit.A_BIT) && !isPressed(mask, Bit.S_BIT) && !isPressed(mask, Bit.D_BIT);
    }

    public static int generateMask(MinecraftClient mc) {
        int mask = 0;
        if (mc.options.forwardKey.isPressed())    mask |= (1 << 0); // W
        if (mc.options.leftKey.isPressed())  mask |= (1 << 1); // A
        if (mc.options.backKey.isPressed())  mask |= (1 << 2); // S
        if (mc.options.rightKey.isPressed()) mask |= (1 << 3); // D
        if (mc.options.useKey.isPressed()) mask |= (1 << 4); // Right Click
        if (dashKeyBinding.isPressed()) mask |= (1 << 5); // Dash
        if (mc.options.attackKey.isPressed()) mask |= (1 << 6); // LeftClick
        if (mc.options.jumpKey.isPressed()) mask |= (1 << 7);
        if (mc.options.sprintKey.isPressed()) mask |= (1 << 8);
        if (mc.options.inventoryKey.isPressed()) mask |= (1 << 9);
        if (mc.options.swapHandsKey.isPressed()) mask |= (1 << 10);
        if (mc.options.sneakKey.isPressed()) mask |= (1 << 11);
        if (mc.options.pickItemKey.isPressed()) mask |= (1 << 12);
        if (mc.options.chatKey.isPressed()) mask |= (1 << 13);
        if (mc.options.playerListKey.isPressed()) mask |= (1 << 14);
        if (mc.options.commandKey.isPressed()) mask |= (1 << 15);
        if (mc.options.socialInteractionsKey.isPressed()) mask |= (1 << 16);
        if (mc.options.screenshotKey.isPressed()) mask |= (1 << 17);
        if (mc.options.togglePerspectiveKey.isPressed()) mask |= (1 << 18);
        if (mc.options.smoothCameraKey.isPressed()) mask |= (1 << 19);
        if (mc.options.fullscreenKey.isPressed()) mask |= (1 << 20);
        if (mc.options.spectatorOutlinesKey.isPressed()) mask |= (1 << 21);
        if (mc.options.advancementsKey.isPressed()) mask |= (1 << 22);
        if (mc.options.saveToolbarActivatorKey.isPressed()) mask |= (1 << 23);
        if (mc.options.loadToolbarActivatorKey.isPressed()) mask |= (1 << 24);
        if (feintKeyBinding.isPressed()) mask |= (1 << 25);
        if (toggleMineKeyBinding.isPressed()) mask |= (1 << 26);
        if (mc.options.dropKey.isPressed()) mask |= (1 << 27);
        return mask;
    }

    public static void sendUpdateToServer() {
        int mask = generateMask(MinecraftClient.getInstance());
        long now = System.currentTimeMillis();
        boolean changed = (mask != lastMask);
        boolean timeElapsed = (now - lastSent) >= SEND_INTERVAL_MS;

        if (changed || timeElapsed) {

            ClientPlayNetworking.send(
                    Packets.C2S_KeyInput.ID,
                    new Packets.C2S_KeyInput(mask, now).write()
            );
            var state = PlayerInputState.get(MinecraftClient.getInstance().player.getUuid());
            state.setMask(mask);
        }
    }


    public static String asString(int mask) {
        var vector = "up";
        if (InputManager.backKey(mask)) {
            return vector = "right";
        } else if (InputManager.leftKey(mask)) {
            return vector = "left";
        } else if (InputManager.forwardKey(mask)) {
            return vector = "up";
        } else if (InputManager.rightKey(mask)) {
            return vector = "butt";
        }
        return vector;
    }
}

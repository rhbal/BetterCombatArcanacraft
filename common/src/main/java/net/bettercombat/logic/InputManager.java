package net.bettercombat.logic;

import net.bettercombat.network.Packets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public final class InputManager {
    private static int lastMask = 0;
    private static long lastSent = 0L;
    private static final long SEND_INTERVAL_MS = 150;


    public static final int W_BIT = 0;
    public static final int A_BIT = 1;
    public static final int S_BIT = 2;
    public static final int D_BIT = 3;
    public static final int RIGHT_CLICK = 4;

    public static boolean isPressed(int mask, int bit) {
        return (mask & (1 << bit)) != 0;
    }

    public static boolean w(int mask)     { return isPressed(mask, W_BIT); }
    public static boolean a(int mask)     { return isPressed(mask, A_BIT); }
    public static boolean s(int mask)     { return isPressed(mask, S_BIT); }
    public static boolean d(int mask)     { return isPressed(mask, D_BIT); }
    public static boolean rightClick(int mask) { return isPressed(mask, RIGHT_CLICK); }
    public static boolean notMoved(int mask) {return !isPressed(mask, W_BIT) && !isPressed(mask, A_BIT) && !isPressed(mask, S_BIT) && !isPressed(mask, D_BIT);}
    public static boolean pressedOnly(int mask, int bit) { return isPressed(mask, bit) && Integer.bitCount(mask) == 1;}
    public static int generateMask(MinecraftClient mc) {
        int mask = 0;
        if (mc.options.forwardKey.isPressed())    mask |= (1 << 0); // W
        if (mc.options.leftKey.isPressed())  mask |= (1 << 1); // A
        if (mc.options.backKey.isPressed())  mask |= (1 << 2); // S
        if (mc.options.rightKey.isPressed()) mask |= (1 << 3); // D
        if (mc.options.useKey.isPressed()) mask |= (1 << 4); // Right Click
        return mask;
    }

    public static void sendUpdateToServer(){
        int mask = generateMask(MinecraftClient.getInstance());
        long now = System.currentTimeMillis();
        boolean changed = (mask != lastMask);
        boolean timeElapsed = (now - lastSent) >= SEND_INTERVAL_MS;

        if (changed || timeElapsed) {

            ClientPlayNetworking.send(
                    Packets.C2S_KeyInput.ID,
                    new Packets.C2S_KeyInput(mask, now).write()
            );
        }
    }
}

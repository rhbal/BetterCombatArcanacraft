package net.bettercombat.logic;

public final class InputBits {
    public static final int W_BIT = 0;
    public static final int A_BIT = 1;
    public static final int S_BIT = 2;
    public static final int D_BIT = 3;
    public static final int SHIFT_BIT = 4;
    public static final int SPRINT_BIT = 5;
    public static final int JUMP_BIT = 6;

    public static boolean isPressed(int mask, int bit) {
        return (mask & (1 << bit)) != 0;
    }

    public static boolean w(int mask)     { return isPressed(mask, W_BIT); }
    public static boolean a(int mask)     { return isPressed(mask, A_BIT); }
    public static boolean s(int mask)     { return isPressed(mask, S_BIT); }
    public static boolean d(int mask)     { return isPressed(mask, D_BIT); }
    public static boolean shift(int mask) { return isPressed(mask, SHIFT_BIT); }
    public static boolean sprint(int mask){ return isPressed(mask, SPRINT_BIT); }
    public static boolean jump(int mask)  { return isPressed(mask, JUMP_BIT); }
}

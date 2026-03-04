package net.bettercombat.client.animation;

import com.mojang.logging.LogUtils;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import net.bettercombat.logic.InputManager;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.PlayerInputState;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class CustomAnimationPlayer extends KeyframeAnimationPlayer {

    public static final Logger LOGGER = LogManager.getLogger(CustomAnimationPlayer.class);

    private final String name;


    public CustomAnimationPlayer(KeyframeAnimation emote, int t, boolean mutable) {
        super(emote, t, mutable);
        this.name = "";
    }

    public CustomAnimationPlayer(KeyframeAnimation emote, int t, String name) {
        super(emote, t, false);
        this.name = name;
    }

    public boolean isWindingDown(float tickDelta) {
        int windDownStart = getData().endTick + ((getData().stopTick - getData().endTick) / 4);
        return ((getTick() + tickDelta) > (windDownStart + 0.5F)); // + 0.5 for smoother transition
    }

    @Override
    public @NotNull FirstPersonMode getFirstPersonMode(float tickDelta) {
        boolean winding = isWindingDown(tickDelta);
        if (isFrozen()) {
            // #region agent log
            var superMode = super.getFirstPersonMode(tickDelta);
            try { var f = new java.io.FileWriter("debug-85875b.log", true); f.write("{\"loc\":\"getFPMode\",\"tickDelta\":" + tickDelta + ",\"isWinding\":" + winding + ",\"superMode\":\"" + superMode + "\",\"ts\":" + System.currentTimeMillis() + "}\n"); f.close(); } catch(Exception ignored) {}
            // #endregion
            if (winding) return FirstPersonMode.NONE;
            return superMode;
        }
        if (winding) {
            return FirstPersonMode.NONE;
        }
        return super.getFirstPersonMode(tickDelta);
    }

    public static float frozenPitch = 0.0f;
    public static boolean blockFrozen = false;

    private boolean isFrozen() {
        if (!name.contains("block") || getTick() < getData().endTick) {
            blockFrozen = false;
            return false;
        }
        var mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;
        var state = PlayerInputState.get(mc.player.getUuid());
        if (state != null && InputManager.rightClick(state.getMask())) {
            if (!blockFrozen) {
                frozenPitch = mc.player.getPitch();
                blockFrozen = true;
                // #region agent log
                try { var f = new java.io.FileWriter("debug-85875b.log", true); f.write("{\"loc\":\"isFrozen\",\"msg\":\"freeze onset\",\"frozenPitch\":" + frozenPitch + ",\"ts\":" + System.currentTimeMillis() + "}\n"); f.close(); } catch(Exception ignored) {}
                // #endregion
            }
            return true;
        }
        // #region agent log H3 — rightClick became false while animation is at endTick
        try { var f = new java.io.FileWriter("debug-85875b.log", true); f.write("{\"loc\":\"isFrozen\",\"msg\":\"unfreeze\",\"tick\":" + getTick() + ",\"mask\":" + (state != null ? state.getMask() : -1) + ",\"ts\":" + System.currentTimeMillis() + "}\n"); f.close(); } catch(Exception ignored) {}
        // #endregion
        blockFrozen = false;
        return false;
    }

    @Override
    public void tick() {
        if (isFrozen()) return;
        super.tick();
    }

    // Track previous headYaw for per-frame delta
    private static float prevHeadYawForLog = 0f;

    @Override
    public void setupAnim(float tickDelta) {
        if (isFrozen()) {
            var mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                float headYaw = mc.player.getHeadYaw();
                float bodyYawBefore = mc.player.bodyYaw;
                float headYawDelta = headYaw - prevHeadYawForLog;
                prevHeadYawForLog = headYaw;
                // #region agent log — only log when body is significantly off
                if (Math.abs(headYaw - bodyYawBefore) > 0.5f) { try { var f = new java.io.FileWriter("debug-85875b.log", true); f.write("{\"loc\":\"setupAnim\",\"inDelta\":" + tickDelta + ",\"bodyBefore\":" + bodyYawBefore + ",\"headYaw\":" + headYaw + ",\"diff\":" + Math.abs(headYaw - bodyYawBefore) + ",\"ts\":" + System.currentTimeMillis() + "}\n"); f.close(); } catch(Exception ignored) {} }
                // #endregion
                mc.player.bodyYaw = headYaw;
                mc.player.prevBodyYaw = headYaw;
            }
            super.setupAnim(0.0f);
            return;
        }
        super.setupAnim(tickDelta);
    }
}

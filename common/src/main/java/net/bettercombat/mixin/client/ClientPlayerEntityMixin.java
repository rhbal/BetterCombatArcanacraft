package net.bettercombat.mixin.client;

import net.bettercombat.BetterCombat;
import net.bettercombat.api.MinecraftClient_BetterCombat;
import net.bettercombat.logic.PlayerInputState;
import net.bettercombat.network.Packets;
import net.bettercombat.utils.MathHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    private static int lastMask = 0;
    private static long lastSent = 0L;
    private static final long SEND_INTERVAL_MS = 150;

    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/Input;tick(ZF)V", shift = At.Shift.AFTER))
    private void tickMovement_ModifyInput(CallbackInfo ci) {
        int mask = generateMask(MinecraftClient.getInstance());
        long now = System.currentTimeMillis();
        boolean changed = (mask != lastMask);
        boolean timeElapsed = (now - lastSent) >= SEND_INTERVAL_MS;

        if (changed || (timeElapsed && mask != 0)) {
            ClientPlayNetworking.send(
                    Packets.C2S_KeyInput.ID,
                    new Packets.C2S_KeyInput(mask, now).write()
            );
        }
        var config = BetterCombat.config;
        var multiplier = Math.min(Math.max(config.movement_speed_while_attacking, 0.0), 1.0);
//        System.out.println("Multiplier " + multiplier);
        if (multiplier == 1) {
            return;
        }
        var clientPlayer = (ClientPlayerEntity)((Object)this);
        if (clientPlayer.hasVehicle() && !config.movement_speed_effected_while_mounting) {
            return;
        }
        var client = (MinecraftClient_BetterCombat) MinecraftClient.getInstance();
        var swingProgress = client.getSwingProgress();
        if (swingProgress < 0.98) {
            if (config.movement_speed_applied_smoothly) {
                double p2 = 0;
                if (swingProgress <= 0.5) {
                    p2 = MathHelper.easeOutCubic(swingProgress * 2);
                } else {
                    p2 = MathHelper.easeOutCubic(1 - ((swingProgress - 0.5) * 2));
                }
                multiplier = (float) ( 1.0 - (1.0 - multiplier) * p2 );
//                var chart = "-".repeat((int)(100.0 * multiplier)) + "x";
//                System.out.println("Movement speed multiplier: " + String.format("%.4f", multiplier) + ">" + chart);
            }
            clientPlayer.input.movementForward *= multiplier;
            clientPlayer.input.movementSideways *= multiplier;
        }
    }

    private static int generateMask(MinecraftClient mc) {
        int mask = 0;
        if (mc.options.forwardKey.isPressed())    mask |= (1 << 0); // W
        if (mc.options.leftKey.isPressed())  mask |= (1 << 1); // A
        if (mc.options.backKey.isPressed())  mask |= (1 << 2); // S
        if (mc.options.rightKey.isPressed()) mask |= (1 << 3); // D
        return mask;
    }
}

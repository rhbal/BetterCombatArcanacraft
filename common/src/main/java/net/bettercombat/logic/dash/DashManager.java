package net.bettercombat.logic.dash;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DashManager {

    private static final Map<UUID, Integer> lastDashTick = new HashMap<>();
    private static final int DASH_COOLDOWN_TICKS = 40; // 2 секунды при 20 TPS

    public static void doDash(ServerPlayerEntity player, List<DashDirection> directions, int currentTick) {
        UUID id = player.getUuid();

        long last = lastDashTick.getOrDefault(id, -DASH_COOLDOWN_TICKS);
        long elapsed = currentTick - last;

        if (elapsed < DASH_COOLDOWN_TICKS) {
            return;
        }

        lastDashTick.put(id, currentTick);

        float yawRad = player.getYaw() * ((float) Math.PI / 180F);

        double lookX = -Math.sin(yawRad);
        double lookZ = Math.cos(yawRad);

        Vec3d forward  = new Vec3d(lookX, 0, lookZ).normalize();
        Vec3d backward = forward.negate();
        Vec3d right    = new Vec3d( lookZ, 0, -lookX).normalize();
        Vec3d left     = right.negate();

        Vec3d dashDir = Vec3d.ZERO;

        for (int i = 0; i < directions.size(); i++) {
            DashDirection dir = directions.get(i);
            switch (dir) {
                case RIGHT    -> dashDir = dashDir.add(right);
                case LEFT     -> dashDir = dashDir.add(left);
                case FORWARD  -> dashDir = dashDir.add(forward);
                case BACKWARD -> dashDir = dashDir.add(backward);
            }
        }

        if (dashDir.lengthSquared() < 1.0E-4) {
            return;
        }

        dashDir = dashDir.normalize();

        double speed = 0.7;

        double vx = dashDir.x * speed;
        double vz = dashDir.z * speed;
        double vy = player.getVelocity().y + 0.3;

        Vec3d newVel = new Vec3d(vx, vy , vz);

        player.setVelocity(newVel);
        player.velocityDirty = true;
        player.velocityModified = true;
    }
}

package net.bettercombat.logic.dash;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DashManager {

    private static final Map<UUID, Integer> lastDashTick = new HashMap<>();
    private static final int DASH_COOLDOWN_TICKS = 40; // 2 секунды при 20 TPS

    public static void doDash(ServerPlayerEntity player, DashDirection direction, int currentTick) {
        UUID id = player.getUuid();

        long last = lastDashTick.getOrDefault(id, -DASH_COOLDOWN_TICKS);
        long elapsed = currentTick - last;

        if (elapsed < DASH_COOLDOWN_TICKS) {
            // ещё на кулдауне, можем прислать сообщение/звук
            return;
        }

        lastDashTick.put(id, currentTick);

        float yawRad = player.getYaw() * ((float) Math.PI / 180F);

        double lookX = -Math.sin(yawRad);
        double lookZ =  Math.cos(yawRad);

        double strafeX, strafeZ;
        if (direction == DashDirection.RIGHT) {
            strafeX = lookZ;
            strafeZ = -lookX;
        } else {
            strafeX = -lookZ;
            strafeZ = lookX;
        }

        double len = Math.sqrt(strafeX * strafeX + strafeZ * strafeZ);
        if (len < 1.0E-4) return;
        strafeX /= len;
        strafeZ /= len;

        // для проверки – большая скорость
        double speed = 2.0;

        double vx = strafeX * speed;
        double vz = strafeZ * speed;
        double vy = player.getVelocity().y;

        Vec3d newVel = new Vec3d(vx, vy+0.1, vz);

        // игнорируем старый vy для наглядности
        player.setVelocity(newVel);
        player.velocityDirty = true;
        player.velocityModified = true;

        System.out.println("Dash vel = " + player.getVelocity());
    }
}

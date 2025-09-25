package net.bettercombat.logic;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerInputState {
    private volatile int mask;
    private volatile long lastUpdateMs;

    public int getMask() {
        return mask;
    }

    public void setMask(int mask) {
        this.mask = mask;
    }

    public long getLastUpdateMs() {
        return lastUpdateMs;
    }

    public void setLastUpdateMs(long lastUpdateMs) {
        this.lastUpdateMs = lastUpdateMs;
    }

    private static final Map<UUID, PlayerInputState> BY = new ConcurrentHashMap<>();
    public static PlayerInputState get(UUID playerId) { return BY.computeIfAbsent(playerId, k -> new PlayerInputState()); }
}

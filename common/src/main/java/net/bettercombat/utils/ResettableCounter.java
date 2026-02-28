package net.bettercombat.utils;

public class ResettableCounter<V> {
    private V value;
    private int remainingUses;

    public ResettableCounter(V initialValue, int usesBeforeReset) {
        this.value = initialValue;
        this.remainingUses = usesBeforeReset;
    }

    public ResettableCounter() {
        this.value = null;
        this.remainingUses = 0;
    }

    public V get() {
        if (remainingUses <= 0) {
            value = null;
            return value;
        }
        remainingUses--;
        if (remainingUses == 0) {
            V v = value;
            value = null;
            return v;
        }
        return value;
    }

    public void set(V newValue, int usesBeforeReset) {
        this.value = newValue;
        this.remainingUses = usesBeforeReset;
    }
}
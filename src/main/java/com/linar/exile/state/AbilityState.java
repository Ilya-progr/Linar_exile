package com.linar.exile.state;

public class AbilityState {
    private long activeUntilTick = 0;
    private long cooldownUntilTick = 0;

    public boolean isActive(long currentTick) {
        return currentTick <= activeUntilTick;
    }

    public boolean isOnCooldown(long currentTick) {
        return currentTick < cooldownUntilTick;
    }

    public void activate(long currentTick, long durationTicks, long cooldownTicks) {
        this.activeUntilTick = currentTick + durationTicks;
        this.cooldownUntilTick = currentTick + cooldownTicks;
    }
}

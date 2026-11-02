package com.wassuples.demonslayer.entity.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Rui's Cutting Thread Cage: "a wall of spider webs" that becomes "a narrow structure comprised of webs which
 * shrinks to become even thinner". A spider-web net unfurls in the air above the target; its point stays put
 * while only the rim falls to the ground around them, and the rim then constricts inward into a thin spire.
 *
 * <p>Timeline (ticks): 0-10 net opens above the target · 10-20 the rim falls to the ground (trap closes at 20) ·
 * 24-84 the rim constricts · 84 snap.
 */
public class CuttingThreadCageEntity extends ThreadTrapEntity {
    public static final int OPEN_TICKS = 10;
    public static final int FALL_TICKS = 10;
    /** Height above the ground at which the net opens. */
    public static final float OPEN_HEIGHT = 4.5f;

    public CuttingThreadCageEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override public float baseRadius() { return 3.5f; }
    @Override public int closeTick() { return OPEN_TICKS + FALL_TICKS; }
    @Override public int contractStartTick() { return closeTick() + 4; }
    @Override public int totalTicks() { return contractStartTick() + 60; }
    @Override public float finalRadiusFraction() { return 0.12f; }
    @Override public boolean spherical() { return false; }
    @Override protected float contactDamage() { return 3.0f; }
    @Override protected float contractDamageStart() { return 4.0f; }
    @Override protected float contractDamageEnd() { return 8.0f; }
    @Override protected float snapDamage() { return 6.0f; }
    @Override public boolean bloodThreads() { return false; }
}

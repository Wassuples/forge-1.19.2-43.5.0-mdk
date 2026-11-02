package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Rui's Murderous Eye Basket: "a net made out of threads similar to Cat's cradle ... manifests a spherical
 * cage from his threads around the target, that continuously shrinks until it cuts the target to pieces."
 *
 * <p>Timeline (ticks): 0-12 a circle of blood threads spreads on the ground under the target ·
 * 12-28 the circle rises and closes into a sphere around them (trap closes at 28) · 28-36 the sphere tightens
 * its weave · 36-56 it shrinks rapidly · 56 snap. Meanwhile the caster holds a cat's cradle of red threads
 * between their hands (see ClientFormAnimations / ThreadTrapRenderer).
 */
public class ThreadBasketEntity extends ThreadTrapEntity {
    public static final int RING_TICKS = 12;
    public static final int RISE_TICKS = 16;

    public ThreadBasketEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public ThreadBasketEntity(Level level) {
        this(ModEntityTypes.THREAD_BASKET.get(), level);
    }

    @Override public float baseRadius() { return 2.6f; }
    @Override public int closeTick() { return RING_TICKS + RISE_TICKS; }
    @Override public int contractStartTick() { return closeTick() + 8; }
    @Override public int totalTicks() { return contractStartTick() + 20; }
    @Override public float finalRadiusFraction() { return 0.08f; }
    @Override public boolean spherical() { return true; }
    @Override protected float contractEasing() { return 1.4f; } // shrinks fast, all the way
    @Override protected float contactDamage() { return 3.0f; }
    @Override protected float contractDamageStart() { return 5.0f; }
    @Override protected float contractDamageEnd() { return 11.0f; }
    @Override protected float snapDamage() { return 12.0f; }
    @Override public boolean bloodThreads() { return true; }
}

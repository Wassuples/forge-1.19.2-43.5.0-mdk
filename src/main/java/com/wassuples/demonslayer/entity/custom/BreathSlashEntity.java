package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * The visible stroke of a breathing form used by a story NPC: a short-lived arc of the style's element (Water's
 * flowing water, Flame's fire, Thunder's lightning, Wind's gusts, Stone's dust, Beast's twin jagged cuts, Flower's
 * petals) drawn in the shape of the form ({@link #ARC}, {@link #THRUST}, {@link #SPIN}, {@link #RISING}). It sits on its
 * owner and faces where they struck. Purely visual; never saved.
 */
public class BreathSlashEntity extends TimedVisualEntity {
    public static final int ARC = 0, THRUST = 1, SPIN = 2, RISING = 3;
    public static final int LIFE = 14;
    private static final EntityDataAccessor<Integer> STYLE = SynchedEntityData.defineId(BreathSlashEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SHAPE = SynchedEntityData.defineId(BreathSlashEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(BreathSlashEntity.class, EntityDataSerializers.FLOAT);

    public BreathSlashEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public BreathSlashEntity(Level level, LivingEntity owner, BreathingStyles style, int shape, float yaw) {
        this(ModEntityTypes.BREATH_SLASH.get(), level);
        this.setPos(owner.getX(), owner.getY(), owner.getZ());
        this.setOwnerId(owner.getId());
        this.entityData.set(STYLE, style.ordinal());
        this.entityData.set(SHAPE, shape);
        this.entityData.set(YAW, yaw);
    }

    public BreathingStyles style() {
        int i = this.entityData.get(STYLE);
        BreathingStyles[] all = BreathingStyles.values();
        return i >= 0 && i < all.length ? all[i] : BreathingStyles.NONE;
    }

    public int shape() {
        return this.entityData.get(SHAPE);
    }

    /** The direction of the strike (degrees, Minecraft yaw). */
    public float strikeYaw() {
        return this.entityData.get(YAW);
    }

    @Override
    protected int lifeTicks() {
        return LIFE;
    }

    @Override
    protected double cullRadius() {
        return 6.0;
    }

    @Override
    protected boolean followsOwner() {
        return true;
    }

    @Override
    protected void defineVisualData() {
        this.entityData.define(STYLE, 0);
        this.entityData.define(SHAPE, ARC);
        this.entityData.define(YAW, 0.0F);
    }
}

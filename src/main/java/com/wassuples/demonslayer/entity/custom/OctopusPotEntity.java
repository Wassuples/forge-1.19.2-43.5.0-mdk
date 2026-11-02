package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ally.SummonAllies;
import com.wassuples.demonslayer.gamerule.ModGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Octopus Vase Hell: Gyokko's vase with a giant octopus inside. Once a second its six tentacles each slam a spot
 * within 10 blocks: one per nearby enemy, the rest at random. A slam hurts everything within 2 blocks of its
 * spot, and the sweep smashes flimsy blocks (wood, glass, leaves, wool, plants...) in the tentacle's path,
 * never earth or stone (respects the formsGriefing gamerule).
 */
public class OctopusPotEntity extends Monster implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);
    private final int lifeTime = 400;
    private UUID ownerUUID;

    /** Damage pulse period (unchanged from before: one slam a second). */
    public static final int PULSE = 20;
    /** Strike spots are chosen this many ticks before each slam so the tentacles can rear up toward them. */
    public static final int AIM_LEAD = 6;
    public static final int TENTACLES = 6;
    public static final double REACH = 10.0;
    private static final double STRIKE_RADIUS = 2.0;
    private static final float DAMAGE = 12.0f;
    private static final int MAX_BLOCKS_PER_TENTACLE = 24;
    private static final Set<Material> FLIMSY = Set.of(
            Material.WOOD, Material.NETHER_WOOD, Material.GLASS, Material.LEAVES,
            Material.WOOL, Material.CLOTH_DECORATION, Material.PLANT, Material.REPLACEABLE_PLANT,
            Material.REPLACEABLE_FIREPROOF_PLANT, Material.BAMBOO, Material.BAMBOO_SAPLING, Material.VEGETABLE,
            Material.WEB, Material.DECORATION, Material.CACTUS, Material.CAKE);

    private static final EntityDataAccessor<CompoundTag> STRIKES = SynchedEntityData.defineId(OctopusPotEntity.class, EntityDataSerializers.COMPOUND_TAG);

    public OctopusPotEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 150.0D) // Very durable
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public void setOwner(LivingEntity owner) {
        if (owner != null) {
            this.ownerUUID = owner.getUUID();
        }
    }

    public UUID getOwner() {
        return this.ownerUUID;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STRIKES, new CompoundTag());
    }

    /**
     * Where each tentacle slams on the current pulse, relative to the pot's feet. Chosen {@link #AIM_LEAD} ticks
     * before the damage pulse so the client can wind the tentacles up toward them.
     */
    public List<Vec3> getStrikes() {
        List<Vec3> out = new ArrayList<>();
        for (Tag tag : this.entityData.get(STRIKES).getList("p", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) tag;
            out.add(new Vec3(t.getFloat("x"), t.getFloat("y"), t.getFloat("z")));
        }
        return out;
    }

    @Nullable
    private LivingEntity resolveOwner() {
        if (this.ownerUUID != null && this.level instanceof ServerLevel serverLevel) {
            Entity ownerEntity = serverLevel.getEntity(this.ownerUUID);
            if (ownerEntity instanceof LivingEntity living) return living;
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide) return;
        if (this.tickCount > lifeTime) {
            this.discard();
            return;
        }
        LivingEntity owner = resolveOwner();
        int phase = this.tickCount % PULSE;
        if (phase == PULSE - AIM_LEAD) {
            chooseStrikes(owner);
        } else if (phase == 0 && this.tickCount >= PULSE) {
            slam(owner);
        }
    }

    /**
     * Each tentacle picks a spot: one per nearby enemy first (where they stand now, so they can still dodge during
     * the wind-up), the rest at random places inside the reach.
     */
    private void chooseStrikes(@Nullable LivingEntity owner) {
        List<LivingEntity> enemies = this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(REACH),
                e -> e != this && e != owner && e.isAlive() && !SummonAllies.areAllies(this, e)
                        && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
                        && e.distanceToSqr(this) < REACH * REACH);
        java.util.Collections.shuffle(enemies, new java.util.Random(this.random.nextLong()));
        ListTag list = new ListTag();
        for (int i = 0; i < TENTACLES; i++) {
            Vec3 p;
            if (i < enemies.size()) {
                p = enemies.get(i).position().subtract(this.position());
            } else {
                double angle = Math.PI * 2.0 * (i + this.random.nextDouble() * 0.8) / TENTACLES;
                double dist = 3.0 + this.random.nextDouble() * (REACH - 3.5);
                p = new Vec3(Math.cos(angle) * dist, 0.0, Math.sin(angle) * dist);
                p = new Vec3(p.x, groundOffsetAt(p), p.z);
            }
            CompoundTag t = new CompoundTag();
            t.putFloat("x", (float) p.x);
            t.putFloat("y", (float) p.y);
            t.putFloat("z", (float) p.z);
            list.add(t);
        }
        CompoundTag root = new CompoundTag();
        root.put("p", list);
        root.putInt("pulse", this.tickCount); // forces a resync even if the spots repeat
        this.entityData.set(STRIKES, root);
    }

    /** Height of the walkable surface near a random strike spot, relative to the pot (searches 3 up / 4 down). */
    private double groundOffsetAt(Vec3 rel) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int baseY = this.blockPosition().getY();
        int x = Mth.floor(this.getX() + rel.x);
        int z = Mth.floor(this.getZ() + rel.z);
        for (int dy = 3; dy >= -4; dy--) {
            pos.set(x, baseY + dy - 1, z);
            BlockState below = this.level.getBlockState(pos);
            pos.set(x, baseY + dy, z);
            if (!below.getCollisionShape(this.level, pos.below()).isEmpty() && this.level.getBlockState(pos).getCollisionShape(this.level, pos).isEmpty()) {
                return baseY + dy - this.getY();
            }
        }
        return 0.0;
    }

    private void slam(@Nullable LivingEntity owner) {
        List<Vec3> strikes = getStrikes();
        if (strikes.isEmpty()) return;
        DamageSource damageSource = owner != null ? DamageSource.mobAttack(owner) : DamageSource.mobAttack(this);
        Set<LivingEntity> hit = new HashSet<>();
        boolean grief = this.level.getGameRules().getBoolean(ModGameRules.RULE_FORMS_GRIEFING);
        for (Vec3 rel : strikes) {
            Vec3 at = this.position().add(rel);
            AABB box = new AABB(at.x - STRIKE_RADIUS, at.y - 1.0, at.z - STRIKE_RADIUS, at.x + STRIKE_RADIUS, at.y + 3.0, at.z + STRIKE_RADIUS);
            for (LivingEntity target : this.level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (target == this || target == owner || hit.contains(target)) continue;
                if (SummonAllies.areAllies(this, target)) continue;
                if (target instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
                if (target.hurt(damageSource, DAMAGE)) {
                    hit.add(target);
                    Vec3 push = new Vec3(rel.x, 0, rel.z);
                    if (push.lengthSqr() > 1.0E-4) push = push.normalize();
                    target.push(push.x * 0.6, 0.35, push.z * 0.6);
                    target.hurtMarked = true;
                }
            }
            if (grief) smashWeakBlocks(rel, owner);
            if (this.level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.2, at.z, 14, 0.6, 0.1, 0.6, 0.2);
            }
        }
        this.level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, this.getSoundSource(), 1.0f, 1.5f);
        this.level.playSound(null, this.blockPosition(), SoundEvents.SLIME_BLOCK_FALL, this.getSoundSource(), 1.5f, 0.6f);
    }

    /**
     * The tentacle sweeps down from the vase to its strike spot, smashing anything flimsy in the way: wood, glass,
     * leaves, wool, plants and the like (a shed comes apart). It never digs: only blocks at or above the level of
     * the vase's base / the strike spot are touched, and earth, sand, stone, metal etc. are never on the list.
     */
    private void smashWeakBlocks(Vec3 rel, @Nullable LivingEntity owner) {
        Entity breaker = owner != null ? owner : this;
        int floorY = Mth.floor(Math.min(this.getY(), this.getY() + rel.y) + 0.01);
        Set<BlockPos> done = new HashSet<>();
        int broken = 0;
        double length = Math.sqrt(rel.x * rel.x + rel.z * rel.z);
        int steps = Math.max(2, (int) Math.ceil(length / 0.7));
        for (int i = 1; i <= steps && broken < MAX_BLOCKS_PER_TENTACLE; i++) {
            double t = (double) i / steps;
            double cx = this.getX() + rel.x * t;
            double cz = this.getZ() + rel.z * t;
            double groundY = this.getY() + rel.y * t;
            double band = t > 0.85 ? 1.3 : 0.8; // wider at the slam itself
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx * dx + dz * dz > band * band + 0.5) continue;
                    for (int dy = 0; dy <= 2; dy++) {
                        BlockPos pos = new BlockPos(Mth.floor(cx + dx * band), Mth.floor(groundY) + dy, Mth.floor(cz + dz * band));
                        if (pos.getY() < floorY || !done.add(pos)) continue;
                        BlockState state = this.level.getBlockState(pos);
                        if (!isFlimsy(state, pos)) continue;
                        if (this.level.destroyBlock(pos, true, breaker)) {
                            broken++;
                            if (broken >= MAX_BLOCKS_PER_TENTACLE) return;
                        }
                    }
                }
            }
        }
    }

    private boolean isFlimsy(BlockState state, BlockPos pos) {
        if (state.isAir() || this.level.getBlockEntity(pos) != null) return false;
        float hardness = state.getDestroySpeed(this.level, pos);
        if (hardness < 0.0f || hardness > 3.5f) return false;
        return FLIMSY.contains(state.getMaterial());
    }

    @Override
    public void travel(Vec3 pTravelVector) {}
    @Override
    public void knockback(double pStrength, double pX, double pZ) {}
    @Override
    public boolean isPushable() { return false; }
    @Override
    protected void doPush(Entity pEntity) {}
    @Override
    protected void registerGoals() {}
    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // --- Animation ---
    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        AnimationController controller = event.getController();
        if (this.tickCount < 15) {
            controller.setAnimation(new AnimationBuilder().addAnimation("emerge", false));
        } else {
            controller.setAnimation(new AnimationBuilder().addAnimation("flail", true));
        }

        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate));
    }
    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }
}

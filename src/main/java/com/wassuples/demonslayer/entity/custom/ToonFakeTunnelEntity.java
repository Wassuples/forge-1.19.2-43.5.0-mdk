package com.wassuples.demonslayer.entity.custom;

import com.wassuples.demonslayer.entity.ModEntityTypes;
import com.wassuples.demonslayer.util.ToonForceHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Fake Tunnel. The server never edits the world: it only knows the 2xN "tunnel" volume. Every client hides
 * those blocks locally ({@code ToonTunnelClient}) so the hole really shows what's behind the wall or through
 * the mountain. The caster is allowed through the (still solid, server-side) blocks; everyone else smacks
 * into the painted wall, rebounds onto their back and gets birds.
 */
public class ToonFakeTunnelEntity extends Entity {
    public static final int MAX_DEPTH = 32;
    private static final int MAX_LIFE_TICKS = 20 * 15;
    private static final int EXPIRE_GRACE_TICKS = 3;
    private static final int BONK_COOLDOWN_TICKS = 50;

    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(ToonFakeTunnelEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<BlockPos> ORIGIN = SynchedEntityData.defineId(ToonFakeTunnelEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> INTO = SynchedEntityData.defineId(ToonFakeTunnelEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LATERAL = SynchedEntityData.defineId(ToonFakeTunnelEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DEPTH = SynchedEntityData.defineId(ToonFakeTunnelEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> EXPIRING = SynchedEntityData.defineId(ToonFakeTunnelEntity.class, EntityDataSerializers.BOOLEAN);

    /** Live server-side tunnels, so owners/victims can be checked without big entity searches. */
    private static final Set<ToonFakeTunnelEntity> LIVE_SERVER_TUNNELS = Collections.newSetFromMap(new WeakHashMap<>());

    @Nullable
    private UUID ownerUUID;
    private int lifeTicks = MAX_LIFE_TICKS;
    private int expireTicks = -1;
    private int mobPassCooldown;
    private final Map<UUID, Long> bonkCooldowns = new HashMap<>();
    @Nullable
    private List<BlockPos> cachedCells;
    private int cachedKey;

    public ToonFakeTunnelEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public ToonFakeTunnelEntity(Level level, LivingEntity owner, BlockPos origin, Direction into, Direction lateral, int depth) {
        this(ModEntityTypes.TOON_FAKE_TUNNEL.get(), level);
        this.ownerUUID = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
        this.entityData.set(ORIGIN, origin);
        this.entityData.set(INTO, into.get3DDataValue());
        this.entityData.set(LATERAL, lateral.get3DDataValue());
        this.entityData.set(DEPTH, depth);
        // Sit on the entrance face, centred on the 2x2 opening.
        Vec3 face = Vec3.atLowerCornerOf(origin).add(0.5, 0.0, 0.5)
                .add(Vec3.atLowerCornerOf(lateral.getNormal()).scale(0.5))
                .subtract(Vec3.atLowerCornerOf(into.getNormal()).scale(0.5));
        this.setPos(face.x, face.y, face.z);
    }

    // ------------------------------------------------------------------ geometry

    public Direction getInto() {
        return Direction.from3DDataValue(this.entityData.get(INTO));
    }

    public Direction getLateral() {
        return Direction.from3DDataValue(this.entityData.get(LATERAL));
    }

    public int getDepth() {
        return this.entityData.get(DEPTH);
    }

    public int getOwnerEntityId() {
        return this.entityData.get(OWNER_ID);
    }

    public boolean isExpiring() {
        return this.entityData.get(EXPIRING);
    }

    /** Every block position the tunnel passes through (2 wide x 2 tall x depth). */
    public List<BlockPos> getCells() {
        BlockPos origin = this.entityData.get(ORIGIN);
        int depth = getDepth();
        int key = origin.hashCode() * 31 + depth * 7 + this.entityData.get(INTO) * 3 + this.entityData.get(LATERAL);
        if (cachedCells != null && cachedKey == key) return cachedCells;
        List<BlockPos> cells = new ArrayList<>();
        if (depth > 0) {
            Direction into = getInto();
            Direction lateral = getLateral();
            for (int d = 0; d < depth; d++) {
                BlockPos layer = origin.relative(into, d);
                for (int w = 0; w < 2; w++) {
                    for (int h = 0; h < 2; h++) {
                        cells.add(layer.relative(lateral, w).above(h));
                    }
                }
            }
        }
        cachedCells = Collections.unmodifiableList(cells);
        cachedKey = key;
        return cachedCells;
    }

    public static List<BlockPos> layerCells(BlockPos origin, Direction into, Direction lateral, int layer) {
        BlockPos base = origin.relative(into, layer);
        return List.of(base, base.relative(lateral), base.above(), base.relative(lateral).above());
    }

    public AABB getVolume() {
        List<BlockPos> cells = getCells();
        if (cells.isEmpty()) return new AABB(this.blockPosition());
        AABB box = new AABB(cells.get(0));
        for (BlockPos pos : cells) box = box.minmax(new AABB(pos));
        return box;
    }

    /** The 2x2 entrance layer pushed a little out of the wall, toward whoever is walking up to it. */
    private AABB getEntranceZone() {
        Vec3 out = Vec3.atLowerCornerOf(getInto().getOpposite().getNormal());
        AABB first = null;
        for (BlockPos pos : layerCells(this.entityData.get(ORIGIN), getInto(), getLateral(), 0)) {
            first = first == null ? new AABB(pos) : first.minmax(new AABB(pos));
        }
        return first.expandTowards(out.scale(0.4)).deflate(0.05, 0.0, 0.05);
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.tickCount++;

        if (this.level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.wassuples.demonslayer.client.toon.ToonTunnelClient.tick(this));
            return;
        }

        LIVE_SERVER_TUNNELS.add(this);
        ServerLevel serverLevel = (ServerLevel) this.level;

        if (this.expireTicks >= 0) {
            if (--this.expireTicks <= 0) {
                resendRealBlocks(serverLevel);
                this.discard();
            }
            return;
        }

        if (--this.lifeTicks <= 0) {
            beginExpiring(serverLevel);
            return;
        }

        LivingEntity owner = getOwner(serverLevel);
        if (owner != null && !(owner instanceof Player) && --this.mobPassCooldown <= 0) {
            passMobOwner(owner);
        }
        bonkIntruders(serverLevel, owner);
    }

    private void beginExpiring(ServerLevel level) {
        this.entityData.set(EXPIRING, true);
        this.expireTicks = EXPIRE_GRACE_TICKS;
        LivingEntity owner = getOwner(level);
        if (owner != null && owner.getBoundingBox().intersects(getVolume())) {
            ejectOwner(owner);
        }
        level.playSound(null, this.blockPosition(), SoundEvents.PAINTING_BREAK, SoundSource.PLAYERS, 0.8f, 1.6f);
    }

    /** Clients put their own copy back when they see EXPIRING; this re-sends the server's truth to be sure. */
    private void resendRealBlocks(ServerLevel level) {
        for (BlockPos pos : getCells()) {
            ClientboundBlockUpdatePacket packet = new ClientboundBlockUpdatePacket(level, pos);
            level.getChunkSource().broadcast(this, packet);
            if (getOwner(level) instanceof ServerPlayer player) {
                player.connection.send(new ClientboundBlockUpdatePacket(level, pos));
            }
        }
    }

    /** Tunnel closing with the caster still inside: pop them out of the nearer end instead of burying them. */
    private void ejectOwner(LivingEntity owner) {
        Direction into = getInto();
        Vec3 axis = Vec3.atLowerCornerOf(into.getNormal());
        Vec3 entrance = this.position().subtract(axis.scale(0.6));
        Vec3 exit = this.position().add(axis.scale(getDepth() + 0.6));
        double toEntrance = owner.position().distanceToSqr(entrance);
        double toExit = owner.position().distanceToSqr(exit);
        Vec3 target = toEntrance <= toExit ? entrance : exit;
        owner.teleportTo(target.x, this.getY(), target.z);
        owner.fallDistance = 0.0f;
    }

    private void passMobOwner(LivingEntity owner) {
        if (!owner.getBoundingBox().intersects(getEntranceZone())) return;
        Vec3 into = Vec3.atLowerCornerOf(getInto().getNormal());
        if (owner.getLookAngle().dot(into) < 0.3) return;
        Vec3 exit = this.position().add(into.scale(getDepth() + 0.6));
        owner.teleportTo(exit.x, this.getY(), exit.z);
        owner.fallDistance = 0.0f;
        this.mobPassCooldown = 20;
        this.level.playSound(null, owner.blockPosition(), SoundEvents.PAINTING_PLACE, SoundSource.PLAYERS, 0.9f, 1.7f);
    }

    private void bonkIntruders(ServerLevel level, @Nullable LivingEntity owner) {
        AABB zone = getEntranceZone();
        Vec3 into = Vec3.atLowerCornerOf(getInto().getNormal());
        long now = level.getGameTime();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, zone,
                e -> e.isAlive() && !e.isSpectator() && (this.ownerUUID == null || !this.ownerUUID.equals(e.getUUID())))) {
            Long until = bonkCooldowns.get(entity.getUUID());
            if (until != null && until > now) continue;

            boolean facing = entity.getLookAngle().dot(into) > 0.2;
            boolean moving = entity.getDeltaMovement().dot(into) > 0.01;
            if (!facing && !moving) continue;

            bonkCooldowns.put(entity.getUUID(), now + BONK_COOLDOWN_TICKS);
            ToonForceHelper.applyTunnelBonk(entity, owner, into.scale(-1.0));
            level.sendParticles(ParticleTypes.SQUID_INK, entity.getX(), entity.getEyeY(), entity.getZ(), 8, 0.2, 0.2, 0.2, 0.01);
        }
        if (this.tickCount % 100 == 0) bonkCooldowns.values().removeIf(t -> t <= now);
    }

    @Nullable
    private LivingEntity getOwner(ServerLevel level) {
        if (this.ownerUUID == null) return null;
        return level.getEntity(this.ownerUUID) instanceof LivingEntity living ? living : null;
    }

    private boolean isOwnedBy(Entity entity) {
        return this.ownerUUID != null && this.ownerUUID.equals(entity.getUUID());
    }

    // ------------------------------------------------------------------ owner passage (server)

    /**
     * Called at the END of the owner's player tick. Player#tick resets noPhysics every tick, so this re-enables
     * it right before the server processes the next movement packets, letting the caster's client walk
     * through blocks that only exist (as solid) on the server.
     */
    public static void tickOwnerPassage(ServerPlayer player) {
        if (LIVE_SERVER_TUNNELS.isEmpty()) return;
        for (ToonFakeTunnelEntity tunnel : LIVE_SERVER_TUNNELS) {
            if (tunnel.isRemoved() || tunnel.level != player.level || tunnel.isExpiring() || !tunnel.isOwnedBy(player)) continue;
            if (player.getBoundingBox().intersects(tunnel.getVolume().inflate(0.65, 0.0, 0.65))) {
                player.noPhysics = true;
                player.fallDistance = 0.0f;
                // The server sees solid blocks around the player and would force a crawl pose that others see.
                if (!player.isShiftKeyDown() && player.getPose() != Pose.STANDING) {
                    player.setPose(Pose.STANDING);
                }
                return;
            }
        }
    }

    /** One tunnel per caster: painting a new one closes (and un-hides) any they already have. */
    public static void collapseTunnelsOwnedBy(LivingEntity owner) {
        if (!(owner.level instanceof ServerLevel level)) return;
        for (ToonFakeTunnelEntity tunnel : new ArrayList<>(LIVE_SERVER_TUNNELS)) {
            if (!tunnel.isRemoved() && !tunnel.isExpiring() && tunnel.isOwnedBy(owner)) {
                ServerLevel tunnelLevel = (ServerLevel) tunnel.level;
                tunnel.beginExpiring(tunnelLevel);
            }
        }
    }

    /** True while an entity's head is inside a tunnel it owns (used to cancel suffocation damage). */
    public static boolean isInsideOwnTunnel(LivingEntity entity) {
        if (LIVE_SERVER_TUNNELS.isEmpty() || entity.level.isClientSide) return false;
        for (ToonFakeTunnelEntity tunnel : LIVE_SERVER_TUNNELS) {
            if (!tunnel.isRemoved() && tunnel.level == entity.level && tunnel.isOwnedBy(entity)
                    && tunnel.getVolume().inflate(0.3).intersects(entity.getBoundingBox())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void remove(RemovalReason reason) {
        LIVE_SERVER_TUNNELS.remove(this);
        super.remove(reason);
    }

    @Override
    public void onClientRemoval() {
        // Left tracking range, tunnel ended, or world change: put this client's blocks back.
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.wassuples.demonslayer.client.toon.ToonTunnelClient.onEntityRemoved(this));
        super.onClientRemoval();
    }

    // ------------------------------------------------------------------ boilerplate

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(ORIGIN, BlockPos.ZERO);
        this.entityData.define(INTO, Direction.NORTH.get3DDataValue());
        this.entityData.define(LATERAL, Direction.EAST.get3DDataValue());
        this.entityData.define(DEPTH, 0);
        this.entityData.define(EXPIRING, false);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return true; // must keep ticking/rendering logic regardless of how close the camera is
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // noSave entity type: tunnels never persist across reloads (nothing to restore server-side anyway).
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

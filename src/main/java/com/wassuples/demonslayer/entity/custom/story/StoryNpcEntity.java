package com.wassuples.demonslayer.entity.custom.story;

import com.wassuples.demonslayer.dialogue.DialogueManager;
import com.wassuples.demonslayer.entity.custom.HandDemonEntity;
import com.wassuples.demonslayer.item.ModItems;
import com.wassuples.demonslayer.nemesis.ai.NemesisDemonEntity;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.NpcSpeechS2CPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A named story character (see {@link StoryCharacter}): the Ubuyashiki twins who run the Final Selection, and the
 * candidates who take it with you (Tanjiro, Zenitsu, Inosuke, Kanao, Genya).
 *
 * <p>Behaviour is a single anchor: the NPC keeps within {@link #leash} blocks of {@link #anchor} (strolling around
 * it, walking back when drawn away, teleporting home when left far behind), unless it has a {@link #companion}, in
 * which case it keeps near that player instead. Fighters attack demons nearby. Right-clicking opens a dialogue
 * ({@link DialogueManager}); NPCs speak in the world through speech bubbles ({@link #say}), never in chat.
 *
 * <p>Story NPCs can't be killed: the twins take no damage at all, candidates are left at 1 health at worst, and
 * players can't hurt either.
 */
public class StoryNpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> CHARACTER = SynchedEntityData.defineId(StoryNpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(StoryNpcEntity.class, EntityDataSerializers.INT);
    /** Ticks after being hit (or seeing a demon close by) during which the NPC won't stop to chat. */
    private static final int COMBAT_GRACE = 60;
    /** Persistent-data UUID: the player a swordsmith has come to deliver a blade to. */
    public static final String SMITH_FOR = "demonslayer.smith_for";
    private int smithLeaving = -1;

    @Nullable
    private BlockPos anchor;
    private int leash = 6;
    @Nullable
    private UUID companion;
    private int keepWithin = 12;
    private boolean fightsHandDemon;
    /** Facing used while standing at the anchor (twins face the plaza). */
    private float restYaw;
    /** Breathing style whose forms it uses in a fight (NONE: plain sword work). */
    private com.wassuples.demonslayer.styles.BreathingStyles style = com.wassuples.demonslayer.styles.BreathingStyles.NONE;
    private int formCooldown = 100;
    /** Hiding (Zenitsu in the wisteria during the selection): won't fight at all. Set by the shrine's keeper. */
    private boolean hiding;
    /** Player currently in a dialogue with this NPC (server-side, not saved). */
    @Nullable
    private UUID talkingTo;
    private int talkTicks;

    public StoryNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CHARACTER, 0);
        this.entityData.define(VARIANT, 0);
    }

    /** Look of an anonymous {@link StoryCharacter#CANDIDATE} (0..{@link StoryCharacter#CANDIDATE_VARIANTS}-1). */
    public int variant() {
        return this.entityData.get(VARIANT);
    }

    /** Makes this NPC an anonymous candidate with its own name, look and breathing style. */
    public void setCandidate(String name, int variant, com.wassuples.demonslayer.styles.BreathingStyles style) {
        setCharacter(StoryCharacter.CANDIDATE);
        this.style = style;
        this.entityData.set(VARIANT, Math.floorMod(variant, StoryCharacter.CANDIDATE_VARIANTS));
        this.setCustomName(Component.literal(name));
    }

    /** The name to show: the character's, or an anonymous candidate's own. */
    public Component storyName() {
        return character() == StoryCharacter.CANDIDATE && this.getCustomName() != null ? this.getCustomName() : character().displayName();
    }

    public StoryCharacter character() {
        return StoryCharacter.byId(this.entityData.get(CHARACTER));
    }

    public void setCharacter(StoryCharacter character) {
        this.entityData.set(CHARACTER, character.ordinal());
        this.setCustomName(character.displayName());
        this.setCustomNameVisible(false);   // our own name plate is drawn by the renderer
        this.scale = character.scale();
        this.refreshDimensions();
        equipFor(character);
        // breathing at the Final Selection: Tanjiro is still a Water user, Zenitsu knows only Thunder's First Form,
        // Inosuke has his self-taught Beast breathing, Kanao Flower; Genya can't use breathing at all
        this.style = defaultStyle(character);
        // the candidates are a match for one starving demon, not for a crowd of them
        double damage = switch (character) {
            case INOSUKE, GENYA -> 3.5D;
            case ZENITSU -> 2.5D;
            case CANDIDATE -> 3.0D;
            default -> 3.0D;
        };
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
        if (character == StoryCharacter.CANDIDATE) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0D);
            this.getAttribute(Attributes.ARMOR).setBaseValue(2.0D);
            this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(64.0D);   // they climb the whole stairway to the shrine
            this.setHealth(40.0F);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (CHARACTER.equals(key)) {
            this.scale = character().scale();
            this.refreshDimensions();
        }
    }

    /** Mirrors the character's scale; read by {@link #getDimensions}, which can run before synced data exists. */
    private float scale;

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(scale <= 0.0F ? 1.0F : scale);
    }

    private void equipFor(StoryCharacter character) {
        if (character.blades() >= 1) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.NICHIRIN_BLADE.get()));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        } else {
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        if (character.blades() >= 2) {
            this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.NICHIRIN_BLADE.get()));
            this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        } else {
            this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FaceTalkerGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25D, true) {
            @Override
            public boolean canUse() {
                return character().fighter() && !hiding && super.canUse();
            }
        });
        this.goalSelector.addGoal(3, new KeepNearGoal(this));
        this.goalSelector.addGoal(5, new StrollNearAnchorGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !character().isTwin() && super.canUse();
            }
        });
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Player.class, StoryNpcEntity.class) {
            @Override
            public boolean canUse() {
                return character().fighter() && super.canUse();
            }
        });
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, this::isFoe) {
            @Override
            public boolean canUse() {
                return character().fighter() && !hiding && super.canUse();
            }
        });
    }

    private boolean isFoe(LivingEntity e) {
        if (e instanceof HandDemonEntity hand) return fightsHandDemon || hand.getTarget() == this;
        if (e instanceof NemesisDemonEntity) return true;
        return e instanceof Monster && !(e instanceof Creeper);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (!character().fighter() || hiding || target instanceof Player || target instanceof StoryNpcEntity) return false;
        if (target instanceof HandDemonEntity hand && !fightsHandDemon && hand.getTarget() != this && this.getLastHurtByMob() != hand) return false;
        return super.canAttack(target);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        equipFor(character());
        return result;
    }

    // ------------------------------------------------------------------ survival

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (!character().fighter() && !source.isBypassInvul()) return true;   // the twins, the swordsmith
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player && !source.isBypassInvul()) return false;
        if (source.getEntity() instanceof StoryNpcEntity && !source.isBypassInvul()) return false;   // allies' forms
        if (!character().fighter() && !source.isBypassInvul()) return false;
        if (character().named() && !source.isBypassInvul() && this.getHealth() - amount < 1.0F) {
            amount = Math.max(0.0F, this.getHealth() - 1.0F);   // the story needs them: badly hurt, never killed
        }
        return super.hurt(source, amount);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level.isClientSide) return;
        if (this.tickCount % 40 == 0 && this.getHealth() < this.getMaxHealth() && this.getTarget() == null) this.heal(2.0F);
        if (talkTicks > 0 && --talkTicks == 0) talkingTo = null;
        // a fight breaks off any conversation
        if (talkingTo != null && inCombat() && this.level.getPlayerByUUID(talkingTo) instanceof ServerPlayer sp) {
            DialogueManager.closeChatWith(sp, this);   // story-opened conversations (Tanjiro's offer) stay open
            talkingTo = null;
        }
        if (character() == StoryCharacter.SWORDSMITH) {
            tickSwordsmith();
            return;
        }
        // breathing forms: every so often, on a foe in reach
        if (formCooldown > 0) formCooldown--;
        LivingEntity foe = this.getTarget();
        if (formCooldown <= 0 && foe != null && foe.isAlive() && !hiding && style != com.wassuples.demonslayer.styles.BreathingStyles.NONE
                && this.distanceToSqr(foe) < 4.5 * 4.5 && this.level instanceof ServerLevel server) {
            java.util.List<com.wassuples.demonslayer.styles.BreathingForms> forms = forms();
            if (!forms.isEmpty()) {
                this.getLookControl().setLookAt(foe, 60.0F, 60.0F);
                forms.get(this.random.nextInt(forms.size())).performAction(this, server);
            }
            formCooldown = 160 + this.random.nextInt(120);
        }
        // Tanjiro (or anyone told to) goes for the Hand Demon when it is close
        if (fightsHandDemon && this.getTarget() == null && this.tickCount % 10 == 0) {
            java.util.List<HandDemonEntity> hands = this.level.getEntitiesOfClass(HandDemonEntity.class, this.getBoundingBox().inflate(40.0D), LivingEntity::isAlive);
            if (!hands.isEmpty()) this.setTarget(hands.get(0));
        }
    }

    /** The swordsmith walks up to his slayer, hands over the blade, then goes on his way and is gone. */
    private void tickSwordsmith() {
        if (smithLeaving >= 0) {
            if (++smithLeaving > 200) this.discard();
            return;
        }
        if (!this.getPersistentData().hasUUID(SMITH_FOR) || !(this.level.getPlayerByUUID(this.getPersistentData().getUUID(SMITH_FOR)) instanceof ServerPlayer owner)) {
            if (this.tickCount > 100) this.discard();   // his slayer isn't here
            return;
        }
        if (this.tickCount > 600) {   // never reached them: the blade arrives anyway (FinalSelectionMission), he leaves
            this.discard();
            return;
        }
        if (this.distanceToSqr(owner) < 3.0 * 3.0) {
            boolean handed = owner.getCapability(com.wassuples.demonslayer.missions.PlayerMissionsProvider.PLAYER_MISSIONS)
                    .map(d -> com.wassuples.demonslayer.missions.story.FinalSelectionMission.deliverBlade(owner, d)).orElse(false);
            this.getLookControl().setLookAt(owner, 30, 30);
            if (handed) say(Component.translatable("mission.demonslayer.final_selection.smith.deliver"), 160);
            smithLeaving = 0;
            this.setCompanion(null);
            net.minecraft.world.phys.Vec3 away = this.position().subtract(owner.position()).normalize().scale(24).add(this.position());
            this.setAnchor(new BlockPos(away), 1);
        }
    }

    /** The forms it knows: Zenitsu only the First Form; anonymous candidates their style's first three. */
    private java.util.List<com.wassuples.demonslayer.styles.BreathingForms> forms() {
        java.util.List<com.wassuples.demonslayer.styles.BreathingForms> out = new java.util.ArrayList<>();
        for (com.wassuples.demonslayer.styles.BreathingForms f : com.wassuples.demonslayer.styles.BreathingForms.values()) {
            if (f.getStyle() != style || f.isBuff()) continue;   // buffs only tick for players
            String n = f.name();
            if (character() == StoryCharacter.ZENITSU && !n.equals("THUNDER_1")) continue;
            if (character() == StoryCharacter.TANJIRO && n.equals("WATER_11")) continue;   // Dead Calm is Giyu's own
            if (character() == StoryCharacter.CANDIDATE && out.size() >= 3) break;
            out.add(f);
        }
        return out;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level.isClientSide) com.wassuples.demonslayer.forms.ActiveFormManager.forget(this);
        super.remove(reason);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide) {
            com.wassuples.demonslayer.forms.baseForms.ActiveForm active = com.wassuples.demonslayer.forms.ActiveFormManager.getActiveForm(this);
            if (active != null) active.tick();
        }
    }

    private static com.wassuples.demonslayer.styles.BreathingStyles defaultStyle(StoryCharacter character) {
        return switch (character) {
            case TANJIRO -> com.wassuples.demonslayer.styles.BreathingStyles.WATER;
            case ZENITSU -> com.wassuples.demonslayer.styles.BreathingStyles.THUNDER;
            case INOSUKE -> com.wassuples.demonslayer.styles.BreathingStyles.BEAST;
            case KANAO -> com.wassuples.demonslayer.styles.BreathingStyles.FLOWER;
            default -> com.wassuples.demonslayer.styles.BreathingStyles.NONE;
        };
    }

    public com.wassuples.demonslayer.styles.BreathingStyles style() {
        return style;
    }

    public void setHiding(boolean hiding) {
        this.hiding = hiding;
        if (hiding) this.setTarget(null);
    }

    public boolean isHiding() {
        return hiding;
    }

    /** Locked in a fight: its foe is close, or it was hit in the last few seconds. */
    public boolean inCombat() {
        LivingEntity t = this.getTarget();
        if (t != null && t.isAlive() && t.distanceToSqr(this) < 8 * 8) return true;
        return this.getLastHurtByMob() != null && this.tickCount - this.getLastHurtByMobTimestamp() < COMBAT_GRACE;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return !character().isTwin() && super.isPushable();
    }

    // ------------------------------------------------------------------ talking

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp && this.isAlive()) {
            if (character().fighter() && inCombat()) {
                // no time to talk mid-fight: a shout instead
                say(Component.translatable("npc.demonslayer.busy." + this.random.nextInt(4)), 50);
            } else {
                DialogueManager.open(sp, this);
            }
        }
        return InteractionResult.sidedSuccess(this.level.isClientSide);
    }

    /** Stops and faces {@code player} for a while (dialogue). */
    public void beginTalking(Player player, int ticks) {
        this.talkingTo = player.getUUID();
        this.talkTicks = ticks;
        this.getNavigation().stop();
    }

    public void endTalking(Player player) {
        if (player.getUUID().equals(talkingTo)) {
            talkingTo = null;
            talkTicks = 0;
        }
    }

    @Nullable
    Player talker() {
        return talkingTo == null ? null : this.level.getPlayerByUUID(talkingTo);
    }

    /** Speaks in the world: a speech bubble above the head for everyone nearby. */
    public void say(Component text, int ticks) {
        if (this.level.isClientSide) return;
        ModMessages.sendToTrackingAndSelf(new NpcSpeechS2CPacket(this.getId(), text, ticks), this);
    }

    /** Speech bubble sized to the text: roughly 1 s per 14 characters, at least 3 s. */
    public void say(Component text) {
        say(text, Math.max(60, Math.min(240, 30 + text.getString().length() * 3 / 2)));
    }

    // ------------------------------------------------------------------ story controls

    public void setAnchor(@Nullable BlockPos anchor, int leash) {
        this.anchor = anchor;
        this.leash = Math.max(0, leash);
    }

    @Nullable
    public BlockPos anchor() {
        return anchor;
    }

    public int leash() {
        return leash;
    }

    public void setRestYaw(float yaw) {
        this.restYaw = yaw;
    }

    public void setCompanion(@Nullable UUID companion) {
        this.companion = companion;
    }

    @Nullable
    public UUID companionId() {
        return companion;
    }

    @Nullable
    public Player getCompanion() {
        return companion == null ? null : this.level.getPlayerByUUID(companion);
    }

    /** Join the fight against the Hand Demon (accepted) or keep to other demons (declined). */
    public void setFightsHandDemon(boolean fights) {
        this.fightsHandDemon = fights;
        this.keepWithin = fights ? 8 : 26;
        if (!fights && this.getTarget() instanceof HandDemonEntity) this.setTarget(null);
    }

    public boolean fightsHandDemon() {
        return fightsHandDemon;
    }

    /** Appears near {@code player} (used when the story needs them close). */
    public boolean comeTo(Player player) {
        return teleportNear(player.position(), 4);
    }

    /** Teleports to a standable spot about {@code radius} blocks from {@code centre}. */
    public boolean teleportNear(Vec3 centre, int radius) {
        if (!(this.level instanceof ServerLevel server)) return false;
        for (int i = 0; i < 16; i++) {
            double ang = this.random.nextDouble() * Math.PI * 2;
            double r = radius * (0.4 + this.random.nextDouble() * 0.6);
            BlockPos p = new BlockPos(centre.x + Math.cos(ang) * r, centre.y, centre.z + Math.sin(ang) * r);
            for (int dy = 4; dy >= -4; dy--) {
                BlockPos q = p.offset(0, dy, 0);
                if (server.getBlockState(q.below()).isFaceSturdy(server, q.below(), net.minecraft.core.Direction.UP)
                        && server.getBlockState(q).getCollisionShape(server, q).isEmpty()
                        && server.getBlockState(q.above()).getCollisionShape(server, q.above()).isEmpty()
                        && server.getFluidState(q).isEmpty()) {
                    this.teleportTo(q.getX() + 0.5, q.getY(), q.getZ() + 0.5);
                    this.getNavigation().stop();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Character", character().name());
        tag.putInt("Variant", variant());
        tag.putString("Style", style.name());
        if (anchor != null) {
            tag.putInt("AnchorX", anchor.getX());
            tag.putInt("AnchorY", anchor.getY());
            tag.putInt("AnchorZ", anchor.getZ());
        }
        tag.putInt("Leash", leash);
        if (companion != null) tag.putUUID("Companion", companion);
        tag.putBoolean("FightsHandDemon", fightsHandDemon);
        tag.putInt("KeepWithin", keepWithin);
        tag.putFloat("RestYaw", restYaw);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        try {
            this.entityData.set(CHARACTER, StoryCharacter.valueOf(tag.getString("Character")).ordinal());
        } catch (IllegalArgumentException ignored) {
            this.entityData.set(CHARACTER, 0);
        }
        this.scale = character().scale();
        this.entityData.set(VARIANT, tag.getInt("Variant"));
        try {
            style = com.wassuples.demonslayer.styles.BreathingStyles.valueOf(tag.getString("Style"));
        } catch (IllegalArgumentException e) {
            style = defaultStyle(character());
        }
        anchor = tag.contains("AnchorX") ? new BlockPos(tag.getInt("AnchorX"), tag.getInt("AnchorY"), tag.getInt("AnchorZ")) : null;
        leash = tag.getInt("Leash");
        companion = tag.hasUUID("Companion") ? tag.getUUID("Companion") : null;
        fightsHandDemon = tag.getBoolean("FightsHandDemon");
        if (tag.contains("KeepWithin")) keepWithin = tag.getInt("KeepWithin");
        restYaw = tag.getFloat("RestYaw");
        this.refreshDimensions();
    }

    // ================================================================== goals

    /** Stand still and face the player you're talking with. */
    static class FaceTalkerGoal extends Goal {
        private final StoryNpcEntity npc;

        FaceTalkerGoal(StoryNpcEntity npc) {
            this.npc = npc;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player p = npc.talker();
            return p != null && npc.getTarget() == null && npc.distanceToSqr(p) < 100.0D;
        }

        @Override
        public void start() {
            npc.getNavigation().stop();
        }

        @Override
        public void tick() {
            Player p = npc.talker();
            if (p != null) npc.getLookControl().setLookAt(p, 30.0F, 30.0F);
        }
    }

    /** Keeps near the companion, or within the leash of the anchor; teleports when left very far behind. */
    static class KeepNearGoal extends Goal {
        private final StoryNpcEntity npc;
        private int repath;

        KeepNearGoal(StoryNpcEntity npc) {
            this.npc = npc;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Nullable
        private Vec3 goal() {
            Player p = npc.getCompanion();
            if (p != null && p.level == npc.level) return p.position();
            return npc.anchor == null ? null : Vec3.atBottomCenterOf(npc.anchor);
        }

        private double range() {
            return npc.getCompanion() != null ? npc.keepWithin : Math.max(1.5, npc.leash);
        }

        @Override
        public boolean canUse() {
            Vec3 g = goal();
            if (g == null || npc.getTarget() != null || npc.talker() != null) return false;
            double r = range();
            return npc.position().distanceToSqr(g) > r * r;
        }

        @Override
        public boolean canContinueToUse() {
            Vec3 g = goal();
            if (g == null || npc.getTarget() != null || npc.talker() != null) return false;
            double r = Math.max(1.0, range() * 0.5);
            return npc.position().distanceToSqr(g) > r * r;
        }

        @Override
        public void tick() {
            Vec3 g = goal();
            if (g == null) return;
            if (npc.position().distanceToSqr(g) > 64.0D * 64.0D) {
                if (npc.teleportNear(g, npc.getCompanion() != null ? 4 : Math.max(1, npc.leash))) return;
            }
            if (--repath <= 0) {
                repath = 10;
                npc.getNavigation().moveTo(g.x, g.y, g.z, 1.1D);
            }
        }

        @Override
        public void stop() {
            npc.getNavigation().stop();
            if (npc.companion == null && npc.leash <= 1 && npc.anchor != null) {
                npc.setYRot(npc.restYaw);
                npc.setYHeadRot(npc.restYaw);
                npc.setYBodyRot(npc.restYaw);
            }
        }
    }

    /** Idle strolling within the leash (not for NPCs leashed tight, like the twins at the shrine). */
    static class StrollNearAnchorGoal extends Goal {
        private final StoryNpcEntity npc;
        @Nullable
        private Vec3 target;

        StrollNearAnchorGoal(StoryNpcEntity npc) {
            this.npc = npc;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (npc.anchor == null || npc.leash < 3 || npc.getCompanion() != null || npc.talker() != null) return false;
            if (npc.getRandom().nextInt(90) != 0) return false;
            Vec3 pos = DefaultRandomPos.getPos(npc, Math.min(npc.leash, 8), 3);
            if (pos == null || pos.distanceToSqr(Vec3.atBottomCenterOf(npc.anchor)) > (double) npc.leash * npc.leash) return false;
            target = pos;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && !npc.getNavigation().isDone() && npc.talker() == null;
        }

        @Override
        public void start() {
            if (target != null) npc.getNavigation().moveTo(target.x, target.y, target.z, 0.8D);
        }
    }
}

package com.wassuples.demonslayer.forms.slayer.stone;

import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.forms.baseForms.TimedForm;
import com.wassuples.demonslayer.styles.IForm;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static com.wassuples.demonslayer.util.CombatHelper.*;

/**
 * Stone Breathing form: Arcs Of Justice.
 *
 * @see com.wassuples.demonslayer.styles.BreathingForms
 * @see com.wassuples.demonslayer.forms.baseForms.TimedForm
 */
public class ArcsOfJusticeForm extends TimedForm {
    // --- Constants ---
    private static final int AXE_IMPACT_TICK = 8;  // Axe hits quickly (0.4s)
    private static final int FLAIL_IMPACT_TICK = 14; // Flail hits later (0.7s)
    private static final double MAX_RANGE = 16.0;
    private static final float AXE_DAMAGE = 12.0f;
    private static final float FLAIL_DAMAGE = 20.0f;
    private static final double IMPACT_RADIUS = 2.0;

    private static final DustParticleOptions CHAIN_PARTICLE = new DustParticleOptions(new Vector3f(0.4f, 0.4f, 0.4f), 1.0f);

    @Nullable // The target can be null if we are aiming at the ground
    private final LivingEntity target;
    private Vec3 destinationPoint;
    private final Vec3 axeStartPos;
    private final Vec3 flailStartPos;
    private boolean axeHasHit = false;
    private boolean flailHasHit = false;

    public ArcsOfJusticeForm(LivingEntity user, IForm form) {
        super(user, form, FLAIL_IMPACT_TICK + 5);
        this.axeStartPos = user.getEyePosition().add(user.getLookAngle().cross(new Vec3(0, 1, 0)).normalize().scale(1.5));
        this.flailStartPos = user.getEyePosition().add(user.getLookAngle().cross(new Vec3(0, 1, 0)).normalize().scale(-1.5));

        this.target = getLookedAtEntity(user, MAX_RANGE);
        if (this.target != null) {
            this.destinationPoint = target.position();
        } else {
            BlockHitResult hitResult = (BlockHitResult) user.pick(MAX_RANGE, 0, false);
            if (hitResult.getType() == HitResult.Type.MISS) {
                this.destinationPoint = user.getEyePosition().add(user.getLookAngle().scale(MAX_RANGE));
            } else {
                this.destinationPoint = Vec3.atCenterOf(hitResult.getBlockPos());
            }
        }
    }

    @Override
    public void onStart() {
        performGenericAttack(user, SoundEvents.ANVIL_LAND, 1.2f, 0.7f);
    }

    @Override
    public void onTick() {
        super.onTick();

        // If we have a living target, update the destination point so the attack tracks them.
        if (this.target != null && this.target.isAlive()) {
            this.destinationPoint = this.target.position();
        }

        // Axe travels faster in a direct line
        double axeProgress = Math.min((double) ticksActive / AXE_IMPACT_TICK, 1.0);
        Vec3 currentAxePos = this.axeStartPos.lerp(this.destinationPoint, axeProgress);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState()), currentAxePos.x, currentAxePos.y, currentAxePos.z, 2, 0, 0, 0, 0);

        // Flail travels slower in a wider arc
        double flailProgress = Math.min((double) ticksActive / FLAIL_IMPACT_TICK, 1.0);
        Vec3 flailPathCenter = this.flailStartPos.lerp(this.destinationPoint, flailProgress);
        double arcHeight = Math.sin(flailProgress * Math.PI) * 3.0; // The arc goes 3 blocks high at its peak
        Vec3 currentFlailPos = flailPathCenter.add(0, arcHeight, 0);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()), currentFlailPos.x, currentFlailPos.y, currentFlailPos.z, 2, 0, 0, 0, 0);

        // Axe Impact
        if (!axeHasHit && ticksActive >= AXE_IMPACT_TICK) {
            axeHasHit = true;
            dealAOEDamage(user, this.destinationPoint, IMPACT_RADIUS, AXE_DAMAGE, 0.5f);
            level.playSound(null, destinationPoint.x, destinationPoint.y, destinationPoint.z, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 1.5f, 0.8f);
        }

        // Flail Impact
        if (!flailHasHit && ticksActive >= FLAIL_IMPACT_TICK) {
            flailHasHit = true;
            dealAOEDamage(user, this.destinationPoint, IMPACT_RADIUS * 1.5, FLAIL_DAMAGE, 1.0f);
            level.playSound(null, destinationPoint.x, destinationPoint.y, destinationPoint.z, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 1.5f, 1.0f);
        }
    }
}

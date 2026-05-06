package com.wassuples.demonslayer.mixin.gravity;

import com.wassuples.demonslayer.gravity.GravityDirection;
import com.wassuples.demonslayer.gravity.GravityUtil;
import com.wassuples.demonslayer.gravity.PlayerGravity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.core.Direction;

@Mixin(Entity.class)
public abstract class GravityMoveMixin {

    @Shadow public float maxUpStep;
    @Shadow public Level level;
    @Shadow public abstract Vec3 position();
    @Shadow public abstract void setPos(double x, double y, double z);
    @Shadow public abstract AABB getBoundingBox();

    @Unique private float            demonslayer$savedMaxUpStep   = -1f;
    @Unique private Vec3             demonslayer$preMovePos       = null;
    @Unique private Vec3             demonslayer$originalMovement = null;
    @Unique private GravityDirection demonslayer$moveGravity      = GravityDirection.DOWN;

    // ── HEAD: suppress vanilla's wrong-axis step ──────────────────────────────────
    @Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void demonslayer$preMoveCapture(MoverType type, Vec3 movement, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        demonslayer$moveGravity = self.getCapability(PlayerGravity.GRAVITY_CAPABILITY)
            .map(PlayerGravity::getGravity).orElse(GravityDirection.DOWN);

        if (demonslayer$moveGravity != GravityDirection.DOWN) {
            demonslayer$savedMaxUpStep   = this.maxUpStep;
            demonslayer$originalMovement = movement;
            demonslayer$preMovePos       = self.position();
            this.maxUpStep = 0f;   // vanilla reads this field inside move(); zero = no step
        }
    }

    // ── RETURN: redo step in the correct gravity-relative "up" direction ──────────
    @Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At("RETURN"))
    private void demonslayer$postMoveStep(MoverType type, Vec3 movement, CallbackInfo ci) {
        if (demonslayer$moveGravity == GravityDirection.DOWN
            || demonslayer$savedMaxUpStep <= 0f
            || demonslayer$originalMovement == null) {
            demonslayer$clear();
            return;
        }

        // Restore maxUpStep so nothing downstream sees 0.
        this.maxUpStep = demonslayer$savedMaxUpStep;
        Entity self    = (Entity)(Object)this;

        Vec3 postPos = self.position();   // where vanilla left the entity (step suppressed)
        Vec3 down    = GravityUtil.getWorldDown(demonslayer$moveGravity);
        Vec3 up      = GravityUtil.getWorldUp(demonslayer$moveGravity);

        // Intended movement decomposed into its lateral component (perpendicular to gravity).
        // BUG FIX: the original code used the full intendedMove in step 2, which for
        // non-DOWN gravity contains a gravity-axis component (e.g., an X component when
        // gravity is EAST) that shoves the entity into or away from the surface.
        Vec3   intendedLateral = GravityUtil.projectOntoPlane(demonslayer$originalMovement, down);
        double intendedLen     = intendedLateral.length();

        // Nothing to step over when there is no lateral intent.
        if (intendedLen < 1.0E-5) { demonslayer$clear(); return; }

        // How much lateral displacement did vanilla actually achieve?
        Vec3   actual        = postPos.subtract(demonslayer$preMovePos);
        Vec3   actualLateral = GravityUtil.projectOntoPlane(actual, down);
        double actualLen     = actualLateral.length();

        // Only attempt the step when we were meaningfully blocked (< 95 % of intent reached).
        if (actualLen >= intendedLen * 0.95) { demonslayer$clear(); return; }

        float stepHeight = demonslayer$savedMaxUpStep;
        Vec3  start      = demonslayer$preMovePos;

        // Rewind the entity to its position before move() ran.
        self.setPos(start.x, start.y, start.z);

        // 1. Step "up" along the gravity-relative up axis (always single-axis).
        Vec3 movedUp = demonslayer$collideAxis(self, up.scale(stepHeight));
        Vec3 pos1    = start.add(movedUp);
        self.setPos(pos1.x, pos1.y, pos1.z);

        // 2. Lateral movement only — strictly no gravity-axis component.
        //    Passing the full original movement here is the root bug this file had:
        //    it would push the entity along the gravity axis during the slide.
        Vec3 movedLat = demonslayer$tryCollide(self, intendedLateral);
        Vec3 pos2     = pos1.add(movedLat);
        self.setPos(pos2.x, pos2.y, pos2.z);

        // 3. Step back "down" to re-attach to the ledge surface (always single-axis).
        Vec3 movedDown = demonslayer$collideAxis(self, down.scale(stepHeight));
        Vec3 pos3      = pos2.add(movedDown);

        // Accept the stepped result only when it gained more lateral ground than vanilla did.
        Vec3 steppedLateral = GravityUtil.projectOntoPlane(pos3.subtract(start), down);
        if (steppedLateral.length() > actualLen + 1.0E-5) {
            self.setPos(pos3.x, pos3.y, pos3.z);
        } else {
            self.setPos(postPos.x, postPos.y, postPos.z);  // vanilla result was at least as good
        }

        demonslayer$clear();
    }

    @Unique
    private Vec3 demonslayer$collideAxis(Entity entity, Vec3 movement) {
        AABB   box = entity.getBoundingBox();
        double mx  = movement.x, my = movement.y, mz = movement.z;
        if      (my != 0) my = Shapes.collide(Direction.Axis.Y, box, this.level.getBlockCollisions(entity, box.expandTowards(0, my, 0)), my);
        else if (mx != 0) mx = Shapes.collide(Direction.Axis.X, box, this.level.getBlockCollisions(entity, box.expandTowards(mx, 0, 0)), mx);
        else if (mz != 0) mz = Shapes.collide(Direction.Axis.Z, box, this.level.getBlockCollisions(entity, box.expandTowards(0, 0, mz)), mz);
        return new Vec3(mx, my, mz);
    }

    @Unique
    private Vec3 demonslayer$tryCollide(Entity entity, Vec3 movement) {
        AABB   box = entity.getBoundingBox();
        double mx  = movement.x, my = movement.y, mz = movement.z;

        // Y first (vanilla convention).
        if (my != 0) {
            my  = Shapes.collide(Direction.Axis.Y, box, this.level.getBlockCollisions(entity, box.expandTowards(0, my, 0)), my);
            box = box.move(0, my, 0);  // advance AABB so X/Z tests start from moved position
        }

        // Larger horizontal magnitude first, matching vanilla's Entity.collideBoundingBox.
        boolean zFirst = Math.abs(mz) > Math.abs(mx);
        if (zFirst) {
            if (mz != 0) {
                mz  = Shapes.collide(Direction.Axis.Z, box, this.level.getBlockCollisions(entity, box.expandTowards(0, 0, mz)), mz);
                box = box.move(0, 0, mz);
            }
            if (mx != 0) {
                mx = Shapes.collide(Direction.Axis.X, box, this.level.getBlockCollisions(entity, box.expandTowards(mx, 0, 0)), mx);
            }
        } else {
            if (mx != 0) {
                mx  = Shapes.collide(Direction.Axis.X, box, this.level.getBlockCollisions(entity, box.expandTowards(mx, 0, 0)), mx);
                box = box.move(mx, 0, 0);
            }
            if (mz != 0) {
                mz = Shapes.collide(Direction.Axis.Z, box, this.level.getBlockCollisions(entity, box.expandTowards(0, 0, mz)), mz);
            }
        }

        return new Vec3(mx, my, mz);
    }

    @Unique
    private void demonslayer$clear() {
        demonslayer$savedMaxUpStep   = -1f;
        demonslayer$preMovePos       = null;
        demonslayer$originalMovement = null;
        demonslayer$moveGravity      = GravityDirection.DOWN;
    }

    @Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
        at = @At("RETURN"))
    private void demonslayer$fixOnGroundAfterMove(MoverType type, Vec3 movement, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        self.getCapability(PlayerGravity.GRAVITY_CAPABILITY).ifPresent(gravity -> {
            GravityDirection dir = gravity.getGravity();
            if (dir == GravityDirection.DOWN) return;
            // Vanilla's ground check only fires when movement.y < 0.
            // For wall gravity (X or Z axis), this never matches, so vanilla
            // always leaves onGround=false even when standing on a surface.
            if (GravityUtil.isStandingOnCustomSurface(self, dir)) {
                self.setOnGround(true);
            }
        });
    }
}


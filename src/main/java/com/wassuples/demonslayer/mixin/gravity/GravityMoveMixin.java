package com.wassuples.demonslayer.mixin.gravity;

import com.wassuples.demonslayer.gravity.GravityDirection;
import com.wassuples.demonslayer.gravity.GravityUtil;
import com.wassuples.demonslayer.gravity.PlayerGravity;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Translates player-local motion at the world-collision boundary. */
@Mixin(Entity.class)
public abstract class GravityMoveMixin {
    @Shadow
    private static Vec3 collideWithShapes(Vec3 movement, AABB box, List<VoxelShape> collisions) {
        throw new AssertionError();
    }

    @Unique private GravityDirection demonslayer$moveGravity = GravityDirection.DOWN;
    @Unique private boolean demonslayer$relativeMove;

    @ModifyVariable(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
        at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Vec3 demonslayer$movementToWorld(Vec3 movement, MoverType type) {
        Entity self = (Entity)(Object)this;
        demonslayer$moveGravity = demonslayer$getPlayerGravity(self);
        demonslayer$relativeMove = demonslayer$moveGravity != GravityDirection.DOWN
            && type == MoverType.SELF;
        return demonslayer$relativeMove
            ? GravityUtil.localToWorld(movement, demonslayer$moveGravity) : movement;
    }

    /* Before vanilla derives collision flags, put both requested and collided movement back
       in player coordinates. Vanilla stepping therefore remains gravity-relative. */
    @ModifyVariable(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V", ordinal = 0),
        argsOnly = true, ordinal = 0)
    private Vec3 demonslayer$requestBackToLocal(Vec3 movement) {
        return demonslayer$relativeMove ? GravityUtil.worldToLocal(movement, demonslayer$moveGravity) : movement;
    }

    @ModifyVariable(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V", ordinal = 0),
        ordinal = 1)
    private Vec3 demonslayer$collisionBackToLocal(Vec3 movement) {
        return demonslayer$relativeMove ? GravityUtil.worldToLocal(movement, demonslayer$moveGravity) : movement;
    }

    @Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
        at = @At("RETURN"))
    private void demonslayer$clearMoveFrame(MoverType type, Vec3 movement, CallbackInfo ci) {
        demonslayer$relativeMove = false;
        demonslayer$moveGravity = GravityDirection.DOWN;
    }

    @ModifyVariable(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
        at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/level/Level;getEntityCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"),
        ordinal = 0)
    private Vec3 demonslayer$collideInputToLocal(Vec3 movement) {
        GravityDirection gravity = demonslayer$getCollisionGravity((Entity)(Object)this);
        return gravity == GravityDirection.DOWN ? movement : GravityUtil.worldToLocal(movement, gravity);
    }

    @Inject(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
        at = @At("RETURN"), cancellable = true)
    private void demonslayer$collideResultToWorld(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        GravityDirection gravity = demonslayer$getCollisionGravity((Entity)(Object)this);
        if (gravity != GravityDirection.DOWN) cir.setReturnValue(GravityUtil.localToWorld(cir.getReturnValue(), gravity));
    }

    // Avoid ModifyArgs here: its generated Args classes fail to load under this ModLauncher runtime.
    @Redirect(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;expandTowards(DDD)Lnet/minecraft/world/phys/AABB;"))
    private AABB demonslayer$expandInWorld(AABB box, double x, double y, double z) {
        Vec3 world = GravityUtil.localToWorld(new Vec3(x, y, z),
            demonslayer$getCollisionGravity((Entity)(Object)this));
        return box.expandTowards(world.x, world.y, world.z);
    }

    @ModifyArg(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;move(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/AABB;"),
        index = 0)
    private Vec3 demonslayer$boxMoveInWorld(Vec3 movement) {
        return GravityUtil.localToWorld(movement, demonslayer$getCollisionGravity((Entity)(Object)this));
    }

    @ModifyVariable(method = "collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;",
        at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Vec3 demonslayer$boundingInputToWorld(Vec3 movement, Entity entity) {
        GravityDirection gravity = entity == null ? GravityDirection.DOWN : demonslayer$getCollisionGravity(entity);
        return GravityUtil.localToWorld(movement, gravity);
    }

    @Inject(method = "collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;",
        at = @At("RETURN"), cancellable = true)
    private static void demonslayer$boundingResultToLocal(Entity entity, Vec3 movement, AABB box,
            Level level, List<VoxelShape> collisions, CallbackInfoReturnable<Vec3> cir) {
        if (entity != null) {
            cir.setReturnValue(GravityUtil.worldToLocal(cir.getReturnValue(), demonslayer$getCollisionGravity(entity)));
        }
    }

    @Redirect(method = "collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collideWithShapes(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 demonslayer$collideWithShapesRelative(Vec3 worldMovement, AABB box,
            List<VoxelShape> collisions, Entity entity) {
        GravityDirection gravity = entity == null ? GravityDirection.DOWN : demonslayer$getCollisionGravity(entity);
        if (gravity == GravityDirection.DOWN) return collideWithShapes(worldMovement, box, collisions);

        Vec3 local = GravityUtil.worldToLocal(worldMovement, gravity);
        double x = local.x, y = local.y, z = local.z;
        Direction xWorld = demonslayer$worldDirection(new Vec3(1, 0, 0), gravity);
        Direction yWorld = demonslayer$worldDirection(new Vec3(0, 1, 0), gravity);
        Direction zWorld = demonslayer$worldDirection(new Vec3(0, 0, 1), gravity);
        y = demonslayer$collideAxis(y, yWorld, box, collisions);
        if (y != 0) box = box.move(GravityUtil.localToWorld(new Vec3(0, y, 0), gravity));
        boolean zFirst = Math.abs(x) < Math.abs(z);
        if (zFirst) {
            z = demonslayer$collideAxis(z, zWorld, box, collisions);
            if (z != 0) box = box.move(GravityUtil.localToWorld(new Vec3(0, 0, z), gravity));
        }
        x = demonslayer$collideAxis(x, xWorld, box, collisions);
        if (!zFirst && x != 0) box = box.move(GravityUtil.localToWorld(new Vec3(x, 0, 0), gravity));
        if (!zFirst) z = demonslayer$collideAxis(z, zWorld, box, collisions);
        return GravityUtil.localToWorld(new Vec3(x, y, z), gravity);
    }

    @Unique private static double demonslayer$collideAxis(double amount, Direction worldDirection,
            AABB box, List<VoxelShape> collisions) {
        int sign = worldDirection.getAxisDirection().getStep();
        return Shapes.collide(worldDirection.getAxis(), box, collisions, amount * sign) * sign;
    }

    @Unique private static Direction demonslayer$worldDirection(Vec3 localAxis, GravityDirection gravity) {
        Vec3 world = GravityUtil.localToWorld(localAxis, gravity);
        return Direction.getNearest((float)world.x, (float)world.y, (float)world.z);
    }

    @Unique private static GravityDirection demonslayer$getCollisionGravity(Entity entity) {
        GravityMoveMixin mixin = (GravityMoveMixin)(Object)entity;
        return mixin.demonslayer$relativeMove ? mixin.demonslayer$moveGravity : GravityDirection.DOWN;
    }

    @Unique private static GravityDirection demonslayer$getPlayerGravity(Entity entity) {
        if (!(entity instanceof Player) || entity.isPassenger() || entity.isSpectator()) return GravityDirection.DOWN;
        return entity.getCapability(PlayerGravity.GRAVITY_CAPABILITY)
            .map(PlayerGravity::getGravity).orElse(GravityDirection.DOWN);
    }
}

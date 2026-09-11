package com.wassuples.demonslayer.mixin.gravity;

import com.wassuples.demonslayer.gravity.GravityDirection;
import com.wassuples.demonslayer.gravity.GravityUtil;
import com.wassuples.demonslayer.gravity.PlayerGravity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps the public velocity contract world-space and scopes local physics to travel. */
@Mixin(Player.class)
public abstract class GravityPlayerTravelMixin {
    @Shadow
    protected abstract boolean isStayingOnGroundSurface();

    @Unique
    private GravityDirection demonslayer$travelGravity = GravityDirection.DOWN;
    @Unique
    private boolean demonslayer$localTravelFrame;

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void demonslayer$enterLocalFrame(Vec3 input, CallbackInfo ci) {
        Player self = (Player)(Object)this;
        demonslayer$travelGravity = self.isPassenger() || self.isSpectator()
            ? GravityDirection.DOWN
            : self.getCapability(PlayerGravity.GRAVITY_CAPABILITY)
                .map(PlayerGravity::getGravity)
                .orElse(GravityDirection.DOWN);
        demonslayer$localTravelFrame = demonslayer$travelGravity != GravityDirection.DOWN;
        if (demonslayer$localTravelFrame) {
            self.setDeltaMovement(GravityUtil.worldToLocal(
                self.getDeltaMovement(), demonslayer$travelGravity));
        }
    }

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("RETURN"))
    private void demonslayer$exitLocalFrame(Vec3 input, CallbackInfo ci) {
        if (demonslayer$localTravelFrame) {
            Player self = (Player)(Object)this;
            self.setDeltaMovement(GravityUtil.localToWorld(
                self.getDeltaMovement(), demonslayer$travelGravity));
        }
        demonslayer$localTravelFrame = false;
        demonslayer$travelGravity = GravityDirection.DOWN;
    }

    @Redirect(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 demonslayer$lookInLocalFrame(Player instance) {
        return GravityUtil.worldToLocal(instance.getLookAngle(), demonslayer$travelGravity);
    }

    @Inject(method = "maybeBackOffFromEdge(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/MoverType;)Lnet/minecraft/world/phys/Vec3;",
        at = @At("HEAD"), cancellable = true)
    private void demonslayer$backOffFromGravityEdge(Vec3 movement, MoverType type,
            CallbackInfoReturnable<Vec3> cir) {
        Player self = (Player)(Object)this;
        GravityDirection direction = self.getCapability(PlayerGravity.GRAVITY_CAPABILITY)
            .map(PlayerGravity::getGravity).orElse(GravityDirection.DOWN);
        if (direction != GravityDirection.DOWN
                && type == MoverType.SELF
                && !self.isPassenger()
                && !self.isSpectator()
                && !self.getAbilities().flying
                && this.isStayingOnGroundSurface()) {
            cir.setReturnValue(GravityUtil.maybeBackOffFromEdge(
                self, movement, direction, self.maxUpStep));
        }
    }
}

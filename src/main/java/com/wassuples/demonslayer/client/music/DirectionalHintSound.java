package com.wassuples.demonslayer.client.music;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Plays a sound that always seems to come from the direction of a far-off place (the Hand Demon's den), however far it
 * is: it is kept a few blocks from the listener, on the line toward that place.
 */
public class DirectionalHintSound extends AbstractTickableSoundInstance {
    private final Vec3 from;

    public DirectionalHintSound(SoundEvent event, BlockPos from, float volume) {
        super(event, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
        this.from = Vec3.atCenterOf(from);
        this.volume = volume;
        this.looping = false;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        this.relative = false;
        place();
    }

    private void place() {
        Player p = Minecraft.getInstance().player;
        if (p == null) return;
        Vec3 eye = p.getEyePosition();
        Vec3 dir = from.subtract(eye).multiply(1, 0, 1);
        dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
        Vec3 at = eye.add(dir.scale(6.0));
        this.x = at.x;
        this.y = at.y;
        this.z = at.z;
    }

    @Override
    public void tick() {
        if (Minecraft.getInstance().player == null) {
            this.stop();
            return;
        }
        place();
    }
}

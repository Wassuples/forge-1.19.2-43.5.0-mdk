package com.wassuples.demonslayer.client.music;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * One piece of a score (a loop or a one-shot), played without position. It can fade, and it can "tape-stop": slide down
 * in pitch while it fades, as if the reel were being dragged to a halt.
 */
public class MusicSegmentSound extends AbstractTickableSoundInstance {
    private float target;
    private float step;
    private float pitchStep;
    private boolean stopWhenSilent;

    public MusicSegmentSound(SoundEvent event, boolean loop, float volume) {
        super(event, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
        this.looping = loop;
        this.delay = 0;
        this.volume = volume;
        this.target = volume;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    /** Glide to {@code v} over {@code ticks}. */
    public void fadeTo(float v, int ticks) {
        this.target = v;
        this.step = Math.max(0.0005F, Math.abs(v - this.volume) / Math.max(1, ticks));
    }

    public void fadeOut(int ticks) {
        fadeTo(0.0F, ticks);
        this.stopWhenSilent = true;
    }

    /** Drags the music to a halt: down an octave while it fades out. */
    public void tapeStop(int ticks) {
        this.pitchStep = 0.5F / Math.max(1, ticks);
        fadeOut(ticks);
    }

    public void kill() {
        this.stop();
    }

    public boolean fading() {
        return stopWhenSilent;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (this.volume < target) this.volume = Math.min(target, this.volume + step);
        else if (this.volume > target) this.volume = Math.max(target, this.volume - step);
        if (pitchStep > 0) this.pitch = Math.max(0.5F, this.pitch - pitchStep);
        if (stopWhenSilent && this.volume <= 0.001F) this.stop();
    }
}

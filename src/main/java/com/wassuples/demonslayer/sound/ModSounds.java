
package com.wassuples.demonslayer.sound;
import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, DemonSlayer.MOD_ID);
    public static final RegistryObject<SoundEvent> TOON_BOING = registerSoundEvent("toon_boing");
    public static final RegistryObject<SoundEvent> TOON_SLIP = registerSoundEvent("toon_slip");
    public static final RegistryObject<SoundEvent> TOON_WHISTLE_FALL = registerSoundEvent("toon_whistle_fall");
    public static final RegistryObject<SoundEvent> TOON_CRASH = registerSoundEvent("toon_crash");
    public static final RegistryObject<SoundEvent> TOON_BONK = registerSoundEvent("toon_bonk");
    public static final RegistryObject<SoundEvent> TOON_SPLAT = registerSoundEvent("toon_splat");
    public static final RegistryObject<SoundEvent> TOON_EYE_POP = registerSoundEvent("toon_eye_pop");
    public static final RegistryObject<SoundEvent> TOON_RUN = registerSoundEvent("toon_run");
    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> new SoundEvent(new ResourceLocation(DemonSlayer.MOD_ID, name)));
    }
    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}


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
    // story / UI (synthesised for the mod, see sounds/story/)
    public static final RegistryObject<SoundEvent> DIALOGUE_BLIP = registerSoundEvent("dialogue_blip");
    public static final RegistryObject<SoundEvent> DIALOGUE_OPEN = registerSoundEvent("dialogue_open");
    public static final RegistryObject<SoundEvent> MISSION_BELL = registerSoundEvent("mission_bell");
    public static final RegistryObject<SoundEvent> NIGHT_DRUM = registerSoundEvent("night_drum");
    public static final RegistryObject<SoundEvent> MISSION_FAILED = registerSoundEvent("mission_failed");
    public static final RegistryObject<SoundEvent> RESOLVE = registerSoundEvent("resolve");
    public static final RegistryObject<SoundEvent> DEMON_GROWL = registerSoundEvent("demon_growl");
    public static final RegistryObject<SoundEvent> NOTICE = registerSoundEvent("notice");
    public static final RegistryObject<SoundEvent> DEMON_HISS = registerSoundEvent("demon_hiss");
    public static final RegistryObject<SoundEvent> DEMON_CRUMBLE = registerSoundEvent("demon_crumble");
    /** Total Concentration Breathing: the sharp intake before a breathing form. */
    public static final RegistryObject<SoundEvent> BREATH_INTAKE = registerSoundEvent("breath_intake");
    /** The Hand Demon's theme (music/hand_demon/): fight loops and transitions, the death reprise, the night 3-6 hints. */
    public static final java.util.Map<String, RegistryObject<SoundEvent>> HAND_DEMON_MUSIC = new java.util.HashMap<>();
    static {
        for (String n : new String[]{"intro", "crawl", "grudge", "thirteen", "thirteen_fourteen", "fear", "rewind", "collapse",
            "fill_up", "fill_fear", "reprise", "reach_start", "reach_hold", "ending_held", "ending_unheld", "night3", "night4", "night5", "night6"}) {
            HAND_DEMON_MUSIC.put(n, registerSoundEvent("music.hand_demon." + n));
        }
    }

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> new SoundEvent(new ResourceLocation(DemonSlayer.MOD_ID, name)));
    }
    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}

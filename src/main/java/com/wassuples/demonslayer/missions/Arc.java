package com.wassuples.demonslayer.missions;

import net.minecraft.network.chat.Component;

/**
 * The twelve story arcs (docs/MISSIONS_SYSTEM.md). Missions are numbered 0-24 across them.
 */
public enum Arc {
    I("final_selection"),
    II("kidnappers_bog"),
    III("asakusa"),
    IV("tsuzumi_mansion"),
    V("mount_natagumo"),
    VI("rehabilitation_training"),
    VII("mugen_train"),
    VIII("entertainment_district"),
    IX("swordsmith_village"),
    X("hashira_training"),
    XI("infinity_castle"),
    XII("sunrise_countdown");

    private final String key;

    Arc(String key) {
        this.key = key;
    }

    /** "Final Selection", "Kidnapper's Bog", ... */
    public Component title() {
        return Component.translatable("arc.demonslayer." + key);
    }

    /** "Arc I · Final Selection" */
    public Component label() {
        return Component.translatable("arc.demonslayer.label", name(), title());
    }
}

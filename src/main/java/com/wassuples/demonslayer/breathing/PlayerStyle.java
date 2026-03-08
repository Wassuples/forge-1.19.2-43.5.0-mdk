package com.wassuples.demonslayer.breathing;

import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Stores the player's currently selected Breathing Style and active form index.
 *
 * <p>This is a simple persisted state container (capability) used by UI/keybind logic. It is designed to be
 * resilient to enum reorderings or corrupted NBT by clamping/falling back to safe defaults.
 */
public class PlayerStyle {
    private int currentFormIndex = 0;
    private int styleIndex = -1;

    public final int MAX_FORM_INDEX = BreathingForms.values().length - 1;
    public final int MIN_FORM_INDEX = 0;

    /** @return index into {@link BreathingForms#values()} for the current form */
    public int getForm() {
        return currentFormIndex;
    }

    /**
     * @return a safe style index. If the stored index is invalid, defaults to WATER.
     */
    public int getStyleIndex() {
        if (styleIndex < 0 || styleIndex >= BreathingStyles.values().length) {
            return BreathingStyles.WATER.ordinal();
        }
        return styleIndex;
    }

    /** @return the current style enum constant, using the safe value from {@link #getStyleIndex()}. */
    public BreathingStyles getCurrentStyles() {
        return BreathingStyles.values()[getStyleIndex()];
    }

    /**
     * @return the currently selected form.
     *
     * <p>If the stored form index is invalid, falls back to the first form of the current style, or WATER_1
     * as an absolute fallback.
     */
    public BreathingForms getCurrentForms() {
        int safeFormIndex = getForm();
        if(safeFormIndex < 0 || safeFormIndex >= BreathingForms.values().length) {
            BreathingStyles currentStyle = getCurrentStyles();
            for (int i = 0; i < BreathingForms.values().length; i++) {
                if(BreathingForms.values()[i].getStyle() == currentStyle) {
                    return BreathingForms.values()[i];
                }
            }
            return BreathingForms.WATER_1; // Absolute fallback
        }
        return BreathingForms.values()[safeFormIndex];
    }

    /** @return a stream of all forms that belong to the currently selected style. */
    public Stream<BreathingForms> getFormsForCurrentStyle() {
        BreathingStyles current = getCurrentStyles();
        return Arrays.stream(BreathingForms.values()).filter(form -> form.getStyle() == current);
    }

    /**
     * Advances {@link #currentFormIndex} to the next form of the current style (wrapping around).
     */
    public void nextForm() {
        BreathingStyles currentStyle = getCurrentStyles();
        BreathingForms[] forms = BreathingForms.values();

        int startIndex = currentFormIndex + 1;
        for(int i = startIndex; i < forms.length; i++) {
            if (forms[i].getStyle() == currentStyle) {
                currentFormIndex = i;
                return;
            }
        }

        for (int i = 0; i < forms.length; i++) {
            if(forms[i].getStyle() == currentStyle) {
                currentFormIndex =  i;
                return;
            }
        }
    }

    /**
     * Moves {@link #currentFormIndex} to the previous form of the current style (wrapping around).
     */
    public void previousForm() {
        BreathingStyles currentStyle = getCurrentStyles();
        BreathingForms[] forms = BreathingForms.values();

        int startIndex = currentFormIndex - 1;
        for (int i = startIndex; i >= 0; i--) {
            if (forms[i].getStyle() == currentStyle) {
                currentFormIndex = i;
                return;
            }
        }

        for (int i = forms.length - 1; i >= 0; i--) {
            if (forms[i].getStyle() == currentStyle) {
                currentFormIndex = i;
                return;
            }
        }
    }

    /**
     * Sets the selected form index.
     *
     * @return true if the index was valid and applied
     */
    public boolean changeForm(int index) {
        if(index >=  MIN_FORM_INDEX && index <= MAX_FORM_INDEX) {
            currentFormIndex = index;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets the selected style index and also resets the selected form to the first form in that style.
     *
     * @return true if the style index was valid and applied
     */
    public boolean changeStyle(int index) {
        if (index >= 0 && index < BreathingStyles.values().length) {
            styleIndex = index;
            for (int i = 0; i < BreathingForms.values().length; i++) {
                if (BreathingForms.values()[i].getStyle() == getCurrentStyles()) {
                    currentFormIndex = i;
                    break;
                }
            }
            return true;
        }
        return false;
    }

    /** Copies state from another instance (used during cloning on respawn). */
    public void copyFrom(PlayerStyle source) {
        this.currentFormIndex = source.getForm();
        this.styleIndex = source.getStyleIndex();
    }

    /** Writes the style/form selection indices to NBT. */
    public void saveNBTData(CompoundTag nbt) {
        nbt.putInt("form", getForm());
        nbt.putInt("style", getStyleIndex());
    }

    /** Reads the style/form selection indices from NBT (with safe defaults). */
    public void loadNBTData(CompoundTag nbt) {
        this.styleIndex = nbt.contains("style") ? nbt.getInt("style") : BreathingStyles.WATER.ordinal();
        this.currentFormIndex = nbt.contains("form") ? nbt.getInt("form") : 0;
    }

    public void setStyleIndex(int index) {
        if(index >= 0 && index < BreathingStyles.values().length) {
            this.styleIndex = index;
        }
    }

    public void setFormIndex(int index) {
        if(index >= 0 && index <= MAX_FORM_INDEX) {
            this.currentFormIndex = index;
        }
    }
}

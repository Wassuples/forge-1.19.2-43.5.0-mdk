package com.wassuples.demonslayer.breathing;

import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;
import java.util.stream.Stream;

public class PlayerStyle {
    private int currentFormIndex = 0;
    private int styleIndex = -1;

    public final int MAX_FORM_INDEX = BreathingForms.values().length - 1;
    public final int MIN_FORM_INDEX = 0;

    public int getForm() {
        return currentFormIndex;
    }

    public int getStyleIndex() {
        if (styleIndex < 0 || styleIndex >= BreathingStyles.values().length) {
            return BreathingStyles.WATER.ordinal();
        }
        return styleIndex;
    }

    public BreathingStyles getCurrentStyles() {
        return BreathingStyles.values()[getStyleIndex()];
    }

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

    public Stream<BreathingForms> getFormsForCurrentStyle() {
        BreathingStyles current = getCurrentStyles();
        return Arrays.stream(BreathingForms.values()).filter(form -> form.getStyle() == current);
    }

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

    public boolean changeForm(int index) {
        if(index >=  MIN_FORM_INDEX && index <= MAX_FORM_INDEX) {
            currentFormIndex = index;
            return true;
        } else {
            return false;
        }
    }

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

    public void copyFrom(PlayerStyle source) {
        this.currentFormIndex = source.getForm();
        this.styleIndex = source.getStyleIndex();
    }

    public void saveNBTData(CompoundTag nbt) {
        nbt.putInt("form", getForm());
        nbt.putInt("style", getStyleIndex());
    }

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
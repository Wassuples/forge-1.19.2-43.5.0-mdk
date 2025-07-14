package com.wassuples.demonslayer.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBinding {
    public static final String KEY_GATEGORY_DEMONSLAYER = "key.category.demonslayer.category";
    public static final String KEY_CHANGE_FORM = "key.demonslayer.change_form";
    public static final String KEY_USE_FORM = "key.demonslayer.use_form";
    public static final String KEY_OPEN_MENU = "key.demonslayer.open_menu";

    public static final KeyMapping CHANGE_KEY = new KeyMapping(KEY_CHANGE_FORM, KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, KEY_GATEGORY_DEMONSLAYER);

    public static final KeyMapping USE_KEY = new KeyMapping(KEY_USE_FORM, KeyConflictContext.IN_GAME,
            InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_2, KEY_GATEGORY_DEMONSLAYER);

    public static final KeyMapping OPEN_MENU_KEY = new KeyMapping(KEY_OPEN_MENU, KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, KEY_GATEGORY_DEMONSLAYER);

    public static final KeyMapping BDA_TOGGLE_KEY = new KeyMapping("key.demonslayer.bda_toggle", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, KEY_GATEGORY_DEMONSLAYER);

}

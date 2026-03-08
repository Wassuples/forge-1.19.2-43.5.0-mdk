package com.wassuples.demonslayer.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Defines all custom keybindings for the Demon Slayer mod.
 *
 * <p>All keys are registered under the {@code "key.category.demonslayer.category"} category.
 * Default bindings:
 * <ul>
 *   <li><b>R</b> — cycle to next form ({@link #CHANGE_KEY})</li>
 *   <li><b>Right Click</b> — use current form ({@link #USE_KEY})</li>
 *   <li><b>G</b> — open breathing/demon menu ({@link #OPEN_MENU_KEY})</li>
 *   <li><b>V</b> — toggle BDA mode ({@link #BDA_TOGGLE_KEY})</li>
 * </ul>
 *
 * @see com.wassuples.demonslayer.events.ClientEvents
 */
public class KeyBinding {
    public static final String KEY_CATEGORY_DEMONSLAYER = "key.category.demonslayer.category";
    public static final String KEY_CHANGE_FORM = "key.demonslayer.change_form";
    public static final String KEY_USE_FORM = "key.demonslayer.use_form";
    public static final String KEY_OPEN_MENU = "key.demonslayer.open_menu";
    public static final String KEY_BDA_TOGGLE = "key.demonslayer.bda_toggle";

    public static final KeyMapping CHANGE_KEY = new KeyMapping(KEY_CHANGE_FORM, KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, KEY_CATEGORY_DEMONSLAYER);

    public static final KeyMapping USE_KEY = new KeyMapping(KEY_USE_FORM, KeyConflictContext.IN_GAME,
            InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_2, KEY_CATEGORY_DEMONSLAYER);

    public static final KeyMapping OPEN_MENU_KEY = new KeyMapping(KEY_OPEN_MENU, KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, KEY_CATEGORY_DEMONSLAYER);

    public static final KeyMapping BDA_TOGGLE_KEY = new KeyMapping(KEY_BDA_TOGGLE, KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, KEY_CATEGORY_DEMONSLAYER);
}

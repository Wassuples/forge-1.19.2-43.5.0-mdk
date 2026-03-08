package com.wassuples.demonslayer.worldgen.inventory;

import com.wassuples.demonslayer.DemonSlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, DemonSlayer.MOD_ID);

    public static final RegistryObject<MenuType<SunForgeMenu>> SUN_FORGE_MENU =
            registerMenuType("sun_forge_menu", SunForgeMenu::new);

    public static final RegistryObject<MenuType<SwordsmithTableMenu>> SWORDSMITH_TABLE_MENU =
            registerMenuType("swordsmith_table_menu", (id, inv, buf) -> new SwordsmithTableMenu(id, inv));


    private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IForgeMenuType.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}

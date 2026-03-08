package com.wassuples.demonslayer.block.entity;

import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Block entity: Mod Block Entities.
 */
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DemonSlayer.MOD_ID);

    public static final RegistryObject<BlockEntityType<SunForgeBlockEntity>> SUN_FORGE_BE =
            BLOCK_ENTITIES.register("sun_forge_be", () ->
                    BlockEntityType.Builder.of(SunForgeBlockEntity::new,
                            ModBlocks.SUN_FORGE.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}

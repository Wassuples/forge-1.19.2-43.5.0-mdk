package com.wassuples.demonslayer.worldgen.inventory;

import com.wassuples.demonslayer.block.ModBlocks;
import com.wassuples.demonslayer.recipe.ModRecipes;
import com.wassuples.demonslayer.recipe.SwordsmithingRecipe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class SwordsmithTableMenu extends CraftingMenu {

    private final ContainerLevelAccess access;

    // Client side constructor
    public SwordsmithTableMenu(int pContainerId, Inventory pPlayerInventory) {
        this(pContainerId, pPlayerInventory, ContainerLevelAccess.NULL);
    }

    // Server side constructor
    public SwordsmithTableMenu(int pContainerId, Inventory pPlayerInventory, ContainerLevelAccess pAccess) {
        super(pContainerId, pPlayerInventory, pAccess);
        this.access = pAccess;
    }

    @Override
    public MenuType<?> getType() {
        return ModMenuTypes.SWORDSMITH_TABLE_MENU.get();
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(this.access, pPlayer, ModBlocks.SWORDSMITHS_TABLE.get());
    }

    /**
     * CraftingMenu calls this static helper when the crafting grid changes.
     * We override it so ONLY demonslayer:swordsmithing recipes can produce an output.
     */
    protected static void slotChangedCraftingGrid(net.minecraft.world.inventory.AbstractContainerMenu menu,
                                                  Level level,
                                                  Player player,
                                                  CraftingContainer craftingContainer,
                                                  ResultContainer resultContainer) {
        if (level.isClientSide()) {
            return;
        }

        ItemStack result = ItemStack.EMPTY;
        Optional<SwordsmithingRecipe> match = level.getRecipeManager()
                .getRecipeFor(ModRecipes.SWORDSMITHING_TYPE.get(), craftingContainer, level);

        if (match.isPresent()) {
            result = match.get().assemble(craftingContainer);
            resultContainer.setRecipeUsed(match.get());
        } else {
            resultContainer.setRecipeUsed(null);
        }

        resultContainer.setItem(0, result);

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(
                    new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                            menu.containerId, menu.incrementStateId(), 0, result));
        }
    }
}

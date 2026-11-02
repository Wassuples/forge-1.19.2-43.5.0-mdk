package com.wassuples.demonslayer.worldgen.inventory;

import com.wassuples.demonslayer.block.ModBlocks;
import com.wassuples.demonslayer.recipe.ModRecipes;
import com.wassuples.demonslayer.recipe.SwordsmithingRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * The Swordsmith's Table: a 3x3 grid that only accepts {@code demonslayer:swordsmithing} recipes.
 *
 * <p>This used to extend {@code CraftingMenu} and "override" its static {@code slotChangedCraftingGrid}. Static methods
 * can't be overridden, so the table silently behaved like a vanilla crafting table and never matched a swordsmithing
 * recipe. It is now its own menu with its own result slot, so ingredients are consumed using the swordsmithing
 * recipe's remainders (the vanilla result slot looks up CRAFTING recipes, which would duplicate the grid here).
 *
 * <p>Slot layout (same positions as the vanilla crafting table): 0 = result, 1-9 = grid, 10-36 = inventory,
 * 37-45 = hotbar.
 */
public class SwordsmithTableMenu extends AbstractContainerMenu {
    public static final int RESULT_SLOT = 0;
    private static final int GRID_START = 1, GRID_END = 10, INV_START = 10, INV_END = 37, HOTBAR_END = 46;

    private final CraftingContainer craftSlots = new CraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;

    // Client side constructor
    public SwordsmithTableMenu(int pContainerId, Inventory pPlayerInventory) {
        this(pContainerId, pPlayerInventory, ContainerLevelAccess.NULL);
    }

    // Server side constructor
    public SwordsmithTableMenu(int pContainerId, Inventory pPlayerInventory, ContainerLevelAccess pAccess) {
        super(ModMenuTypes.SWORDSMITH_TABLE_MENU.get(), pContainerId);
        this.access = pAccess;
        this.player = pPlayerInventory.player;

        addSlot(new SwordsmithResultSlot(this.player, this.craftSlots, this.resultSlots, 0, 124, 35));
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 3; ++col) {
                addSlot(new Slot(this.craftSlots, col + row * 3, 30 + col * 18, 17 + row * 18));
            }
        }
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(new Slot(pPlayerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            addSlot(new Slot(pPlayerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void slotsChanged(Container container) {
        this.access.execute((level, pos) -> updateResult(this, level, this.player, this.craftSlots, this.resultSlots));
    }

    private static void updateResult(AbstractContainerMenu menu, Level level, Player player,
                                     CraftingContainer grid, ResultContainer result) {
        if (level.isClientSide()) return;
        ItemStack out = ItemStack.EMPTY;
        Optional<SwordsmithingRecipe> match = level.getRecipeManager().getRecipeFor(ModRecipes.SWORDSMITHING_TYPE.get(), grid, level);
        if (match.isPresent()) {
            out = match.get().assemble(grid);
            result.setRecipeUsed(match.get());
        } else {
            result.setRecipeUsed(null);
        }
        result.setItem(0, out);
        menu.setRemoteSlot(RESULT_SLOT, out);
        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), RESULT_SLOT, out));
        }
    }

    @Override
    public void removed(Player pPlayer) {
        super.removed(pPlayer);
        this.access.execute((level, pos) -> this.clearContainer(pPlayer, this.craftSlots));
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(this.access, pPlayer, ModBlocks.SWORDSMITHS_TABLE.get());
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        copy = stack.copy();
        if (index == RESULT_SLOT) {
            this.access.execute((level, pos) -> stack.getItem().onCraftedBy(stack, level, pPlayer));
            if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, copy);
        } else if (index >= INV_START && index < HOTBAR_END) {
            if (!this.moveItemStackTo(stack, GRID_START, GRID_END, false)) {
                if (index < INV_END) {
                    if (!this.moveItemStackTo(stack, INV_END, HOTBAR_END, false)) return ItemStack.EMPTY;
                } else if (!this.moveItemStackTo(stack, INV_START, INV_END, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(pPlayer, stack);
        if (index == RESULT_SLOT) pPlayer.drop(stack, false);
        return copy;
    }

    /** Result slot that consumes the grid using the swordsmithing recipe's remainders. */
    private static class SwordsmithResultSlot extends ResultSlot {
        private final CraftingContainer grid;

        SwordsmithResultSlot(Player player, CraftingContainer grid, Container result, int index, int x, int y) {
            super(player, grid, result, index, x, y);
            this.grid = grid;
        }

        @Override
        public void onTake(Player p, ItemStack taken) {
            this.checkTakeAchievements(taken);
            Level level = p.level;
            NonNullList<ItemStack> remaining = level.getRecipeManager()
                    .getRecipeFor(ModRecipes.SWORDSMITHING_TYPE.get(), this.grid, level)
                    .map(r -> r.getRemainingItems(this.grid))
                    .orElseGet(() -> NonNullList.withSize(this.grid.getContainerSize(), ItemStack.EMPTY));
            for (int i = 0; i < remaining.size(); ++i) {
                ItemStack inSlot = this.grid.getItem(i);
                ItemStack rest = remaining.get(i);
                if (!inSlot.isEmpty()) {
                    this.grid.removeItem(i, 1);
                    inSlot = this.grid.getItem(i);
                }
                if (!rest.isEmpty()) {
                    if (inSlot.isEmpty()) {
                        this.grid.setItem(i, rest);
                    } else if (ItemStack.isSame(inSlot, rest) && ItemStack.tagMatches(inSlot, rest)) {
                        rest.grow(inSlot.getCount());
                        this.grid.setItem(i, rest);
                    } else if (!p.getInventory().add(rest)) {
                        p.drop(rest, false);
                    }
                }
            }
        }
    }
}

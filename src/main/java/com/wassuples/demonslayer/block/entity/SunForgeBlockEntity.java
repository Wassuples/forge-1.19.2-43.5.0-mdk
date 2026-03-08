package com.wassuples.demonslayer.block.entity;

import com.wassuples.demonslayer.recipe.ModRecipes;
import com.wassuples.demonslayer.recipe.SunForgeRecipe;
import com.wassuples.demonslayer.worldgen.inventory.SunForgeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Block entity: Sun Forge Block Entity.
 */
public class SunForgeBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 100;

    public SunForgeBlockEntity(BlockPos pPos, BlockState pState) {
        super(ModBlockEntities.SUN_FORGE_BE.get(), pPos, pState);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> SunForgeBlockEntity.this.progress;
                    case 1 -> SunForgeBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> SunForgeBlockEntity.this.progress = pValue;
                    case 1 -> SunForgeBlockEntity.this.maxProgress = pValue;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Sun Forge");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new SunForgeMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        nbt.put("inventory", itemHandler.serializeNBT());
        nbt.putInt("sun_forge.progress", progress);
        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        itemHandler.deserializeNBT(nbt.getCompound("inventory"));
        progress = nbt.getInt("sun_forge.progress");
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        if (pLevel.isClientSide()) {
            return;
        }

        if (!pLevel.canSeeSky(pPos.above()) || !pLevel.isDay()) {
            if (progress > 0) {
                progress = Math.max(0, progress - 2);
                setChanged(pLevel, pPos, pState);
            }
            return;
        }

        Optional<SunForgeRecipe> recipe = getCurrentRecipe(pLevel);
        if (recipe.isPresent() && canCraft(recipe.get())) {
            // Keep maxProgress in sync with the active recipe (so UI/progress behaves correctly)
            int wantedMax = Math.max(1, recipe.get().getCookTime());
            if (this.maxProgress != wantedMax) {
                this.maxProgress = wantedMax;
            }

            progress++;
            setChanged(pLevel, pPos, pState);

            if (progress >= maxProgress) {
                craftItem(recipe.get());
                progress = 0;
            }
        } else {
            progress = 0;
            setChanged(pLevel, pPos, pState);
        }
    }

    private Optional<SunForgeRecipe> getCurrentRecipe(Level level) {
        SimpleContainer inventory = new SimpleContainer(2);
        inventory.setItem(0, itemHandler.getStackInSlot(0));
        inventory.setItem(1, itemHandler.getStackInSlot(1));

        return level.getRecipeManager().getRecipeFor(ModRecipes.SUN_FORGE_TYPE.get(), inventory, level);
    }

    private boolean canCraft(SunForgeRecipe recipe) {
        ItemStack resultSlot = itemHandler.getStackInSlot(2);
        ItemStack result = recipe.getResultItem();

        return canInsertAmountIntoOutput(resultSlot) && canInsertItemIntoOutput(resultSlot, result);
    }

    private void craftItem(SunForgeRecipe recipe) {
        ItemStack resultSlot = itemHandler.getStackInSlot(2);
        ItemStack result = recipe.getResultItem();

        itemHandler.extractItem(0, 1, false);
        itemHandler.extractItem(1, 1, false);

        if (resultSlot.isEmpty()) {
            itemHandler.setStackInSlot(2, result.copy());
        } else {
            ItemStack newStack = result.copy();
            newStack.setCount(resultSlot.getCount() + result.getCount());
            itemHandler.setStackInSlot(2, newStack);
        }
    }

    private boolean canInsertItemIntoOutput(ItemStack inventory, ItemStack stack) {
        return inventory.isEmpty() || inventory.getItem() == stack.getItem();
    }

    private boolean canInsertAmountIntoOutput(ItemStack inventory) {
        return inventory.getCount() + 1 <= inventory.getMaxStackSize();
    }
}

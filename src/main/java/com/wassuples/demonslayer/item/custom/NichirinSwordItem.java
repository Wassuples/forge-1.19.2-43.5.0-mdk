package com.wassuples.demonslayer.item.custom;

import com.wassuples.demonslayer.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/**
 * The Nichirin Blade — the primary weapon for Demon Slayer Corps members.
 *
 * <p>Each Nichirin sword stores a {@link SwordModel} via NBT ({@code CustomModelData}) to
 * determine its visual appearance. Breathing forms require the player to hold a Nichirin
 * sword (checked by {@link com.wassuples.demonslayer.util.CombatHelper#isPlayerHoldingNichirin}).
 *
 * @see SwordModel
 * @see com.wassuples.demonslayer.item.ModItems#NICHIRIN_BLADE
 */
public class NichirinSwordItem extends SwordItem {
    public NichirinSwordItem(Tier p_43269_, int p_43270_, float p_43271_, Properties p_43272_) {
        super(p_43269_, p_43270_, p_43271_, p_43272_);
    }

    public static ItemStack createSword(SwordModel model) {
        ItemStack stack = new ItemStack(ModItems.NICHIRIN_BLADE.get());
        CompoundTag nbt = stack.getOrCreateTag();
        nbt.putInt("CustomModelData", model.getCustomModelData());
        return stack;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // nothing.
        return true;
    }
}

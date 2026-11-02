package com.wassuples.demonslayer.client.mission;

import com.wassuples.demonslayer.client.gui.KnyUi;
import com.wassuples.demonslayer.entity.custom.HandDemonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

/**
 * The mod's own boss bar (instead of the vanilla one) for story bosses within sight: an ink strip with the boss's
 * name and epithet, a crimson health bar with a pale trail where recent damage was taken, and gold ends.
 */
public final class BossBarOverlay {
    private static final int W = 240;
    private static int bossId = -1;
    private static float trail = 1.0F;

    private BossBarOverlay() {
    }

    public static final IGuiOverlay HUD = (gui, pose, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;
        List<HandDemonEntity> near = mc.level.getEntitiesOfClass(HandDemonEntity.class, mc.player.getBoundingBox().inflate(48.0D), LivingEntity::isAlive);
        if (near.isEmpty()) {
            bossId = -1;
            return;
        }
        HandDemonEntity boss = near.get(0);
        float frac = Math.max(0, boss.getHealth() / boss.getMaxHealth());
        if (boss.getId() != bossId) {
            bossId = boss.getId();
            trail = frac;
        }
        trail = frac >= trail ? frac : trail - Math.max(0.0015F, (trail - frac) * 0.04F);

        Font font = mc.font;
        int x = (width - W) / 2, y = 12;
        Component name = Component.translatable("entity.demonslayer.hand_demon");
        Component epithet = Component.translatable("boss.demonslayer.hand_demon.epithet");
        GuiComponent.fill(pose, x - 8, y - 2, x + W + 8, y + 24, 0xC00C0608);
        GuiComponent.fill(pose, x - 8, y + 24, x + W + 8, y + 25, 0x90C9A04A);
        font.drawShadow(pose, name, x + (W - font.width(name)) / 2.0F, y, 0xFFE8D040);
        font.draw(pose, epithet, x + (W - font.width(epithet)) / 2.0F - 0.0F, y + 9, KnyUi.DIM);
        int by = y + 19;
        GuiComponent.fill(pose, x - 1, by - 1, x + W + 1, by + 5, KnyUi.GOLD_DARK);
        GuiComponent.fill(pose, x, by, x + W, by + 4, 0xFF1A0A0C);
        GuiComponent.fill(pose, x, by, x + (int) (W * trail), by + 4, 0xFFE8C8B0);
        GuiComponent.fill(pose, x, by, x + (int) (W * frac), by + 4, 0xFFB01818);
        GuiComponent.fill(pose, x, by, x + (int) (W * frac), by + 1, 0xFFE04040);
        KnyUi.diamond(pose, x - 4, by + 2);
        KnyUi.diamond(pose, x + W + 4, by + 2);
    };
}

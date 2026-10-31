package com.wassuples.demonslayer.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** First-person slot-machine anticipation for Pocket; world text remains only an accent. */
public final class ToonPocketOverlay {
    private static final ItemStack[] REELS = {
            new ItemStack(Items.IRON_AXE), new ItemStack(Items.TNT),
            new ItemStack(Items.COOKIE), new ItemStack(Items.COD)
    };
    private static int activeTicks;
    private static int result;

    private ToonPocketOverlay() {
    }

    public static void activate(int durationTicks, int rolledResult) {
        activeTicks = Math.max(activeTicks, durationTicks);
        result = Math.max(0, Math.min(REELS.length - 1, rolledResult));
    }

    public static void tick() {
        if (activeTicks > 0) activeTicks--;
    }

    public static final IGuiOverlay HUD = (gui, pose, partialTick, width, height) -> {
        if (activeTicks <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        int cx = width / 2;
        int top = height / 2 + 24;
        int wobble = (int) (Math.sin((activeTicks + partialTick) * 0.7) * 3.0);

        pose.pushPose();
        pose.translate(0, wobble, 0);
        GuiComponent.fill(pose, cx - 70, top - 16, cx + 70, top + 39, 0xEE17121F);
        GuiComponent.fill(pose, cx - 66, top - 12, cx + 66, top + 35, 0xFFFFC928);
        GuiComponent.fill(pose, cx - 61, top - 7, cx + 61, top + 30, 0xFFF8E8C1);
        GuiComponent.fill(pose, cx - 4, top - 7, cx + 4, top + 30, 0xFFDC3131);

        int spin = activeTicks > 10 ? (activeTicks / 2) : result;
        for (int reel = 0; reel < 3; reel++) {
            int item = activeTicks > 10 ? Math.floorMod(spin + reel, REELS.length) : result;
            int x = cx - 55 + reel * 46;
            mc.getItemRenderer().renderAndDecorateItem(REELS[item], x, top + 3);
        }

        String caption = activeTicks > 10 ? "POCKET ROULETTE!" : switch (result) {
            case 0 -> "MALLET!";
            case 1 -> "HOT POTATO!";
            case 2 -> "FORTUNE!";
            default -> "FISH?!";
        };
        pose.scale(1.35f, 1.35f, 1.0f);
        float scaledX = cx / 1.35f;
        float scaledY = (top - 29) / 1.35f;
        mc.font.drawShadow(pose, caption, scaledX - mc.font.width(caption) / 2.0f, scaledY, 0xFFFFFFFF);
        pose.popPose();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    };
}

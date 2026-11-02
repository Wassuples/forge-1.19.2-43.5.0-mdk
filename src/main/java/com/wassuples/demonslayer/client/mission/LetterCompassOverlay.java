package com.wassuples.demonslayer.client.mission;

import com.wassuples.demonslayer.client.gui.KnyUi;
import com.wassuples.demonslayer.item.ModItems;
import com.wassuples.demonslayer.item.custom.LetterOfIntroductionItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * While the letter of introduction is held: a brush-ink compass band across the top of the screen with the cardinal
 * points and a wisteria marker for the shrine on Mt. Fujikasane, plus the distance.
 */
public final class LetterCompassOverlay {
    private static final int HALF = 90;
    private static final String[] CARDINALS = {"S", "W", "N", "E"};

    private LetterCompassOverlay() {
    }

    public static final IGuiOverlay HUD = (gui, pose, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.level.dimension() != Level.OVERWORLD) return;
        ItemStack stack = mc.player.getMainHandItem();
        if (!stack.is(ModItems.LETTER_OF_INTRODUCTION.get())) stack = mc.player.getOffhandItem();
        if (!stack.is(ModItems.LETTER_OF_INTRODUCTION.get())) return;
        BlockPos target = LetterOfIntroductionItem.target(stack);
        Font font = mc.font;
        int cx = width / 2, y = 6;
        GuiComponent.fill(pose, cx - HALF - 6, y, cx + HALF + 6, y + 14, 0xB0120A0E);
        GuiComponent.fill(pose, cx - HALF - 6, y + 14, cx + HALF + 6, y + 15, 0x80C9A04A);
        float yaw = Mth.wrapDegrees(mc.player.getViewYRot(partialTick));
        // cardinal marks: yaw 0 = south, 90 = west, 180 = north, 270 = east
        for (int i = 0; i < 4; i++) {
            float rel = Mth.wrapDegrees(i * 90 - yaw);
            if (Math.abs(rel) > 90) continue;
            int x = cx + (int) (rel / 90.0F * HALF);
            font.draw(pose, CARDINALS[i], x - font.width(CARDINALS[i]) / 2.0F, y + 3, i == 2 ? KnyUi.CRIMSON : KnyUi.GOLD_LIGHT);
        }
        for (int d = 0; d < 360; d += 15) {
            if (d % 90 == 0) continue;
            float rel = Mth.wrapDegrees(d - yaw);
            if (Math.abs(rel) > 90) continue;
            int x = cx + (int) (rel / 90.0F * HALF);
            GuiComponent.fill(pose, x, y + 10, x + 1, y + 13, 0x80EDE3E3);
        }
        GuiComponent.fill(pose, cx, y - 1, cx + 1, y + 2, 0xFFEDE3E3);
        if (target == null) {
            Component unknown = Component.translatable("letter.demonslayer.compass.unknown");
            font.draw(pose, unknown, cx - font.width(unknown) / 2.0F, y + 18, KnyUi.DIM);
            return;
        }
        double dx = target.getX() + 0.5 - mc.player.getX(), dz = target.getZ() + 0.5 - mc.player.getZ();
        float bearing = (float) Math.toDegrees(Math.atan2(-dx, dz));   // same convention as yaw
        float rel = Mth.wrapDegrees(bearing - yaw);
        int mx = cx + (int) (Mth.clamp(rel, -90F, 90F) / 90.0F * HALF);
        int col = 0xFF000000 | 0xB9A2EC;
        if (Math.abs(rel) <= 90) {
            GuiComponent.fill(pose, mx - 2, y + 1, mx + 3, y + 13, col);
            GuiComponent.fill(pose, mx - 1, y + 13, mx + 2, y + 15, col);
        } else {
            String arrow = rel < 0 ? "<" : ">";
            font.draw(pose, arrow, mx - (rel < 0 ? 0 : font.width(arrow)), y + 3, col);
        }
        int dist = (int) Math.sqrt(dx * dx + dz * dz);
        Component label = Component.translatable("letter.demonslayer.compass", String.format("%,d", dist));
        font.drawShadow(pose, label, cx - font.width(label) / 2.0F, y + 18, col);
    };
}

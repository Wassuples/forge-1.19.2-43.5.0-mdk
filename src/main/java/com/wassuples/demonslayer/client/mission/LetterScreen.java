package com.wassuples.demonslayer.client.mission;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.item.custom.LetterOfIntroductionItem;
import com.wassuples.demonslayer.missions.story.FinalSelectionMission;
import com.wassuples.demonslayer.sound.ModSounds;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The letter of introduction, unfolded: a sheet of washi with a wisteria crest, the teacher's recommendation in ink,
 * and a postscript giving the way to Mt. Fujikasane from where the player stands.
 */
public class LetterScreen extends Screen {
    private static final ResourceLocation PAPER = new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/letter_paper.png");
    private static final int W = 220, H = 256, INK = 0xFF2A1E1A, SEAL = 0xFF7A2030;
    private final ItemStack stack;
    private List<FormattedCharSequence> body = List.of();

    public LetterScreen(ItemStack stack) {
        super(Component.translatable("item.demonslayer.letter_of_introduction"));
        this.stack = stack;
    }

    @Override
    protected void init() {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            lines.addAll(this.font.split(Component.translatable("letter.demonslayer.introduction." + i), W - 44));
            lines.add(FormattedCharSequence.EMPTY);
        }
        BlockPos target = LetterOfIntroductionItem.target(stack);
        if (target != null && this.minecraft != null && this.minecraft.player != null) {
            BlockPos from = this.minecraft.player.blockPosition();
            lines.addAll(this.font.split(Component.translatable("letter.demonslayer.introduction.way",
                    FinalSelectionMission.compass(from, target), FinalSelectionMission.distance(from, target)), W - 44));
        } else {
            lines.addAll(this.font.split(Component.translatable("letter.demonslayer.introduction.way_unknown"), W - 44));
        }
        body = lines;
        if (this.minecraft != null) this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.DIALOGUE_OPEN.get(), 0.8F, 0.5F));
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(pose);
        int x = (this.width - W) / 2, y = (this.height - H) / 2;
        RenderSystem.setShaderTexture(0, PAPER);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        blit(pose, x, y, 0, 0, W, H, 256, 256);
        Component title = Component.translatable("letter.demonslayer.introduction.title");
        this.font.draw(pose, title, x + (W - this.font.width(title)) / 2.0F, y + 18, SEAL);
        int ly = y + 36;
        for (FormattedCharSequence line : body) {
            this.font.draw(pose, line, x + 22, ly, INK);
            ly += 10;
        }
        Component sign = Component.translatable("letter.demonslayer.introduction.signature");
        this.font.draw(pose, sign, x + W - 22 - this.font.width(sign), y + H - 30, INK);
        super.render(pose, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

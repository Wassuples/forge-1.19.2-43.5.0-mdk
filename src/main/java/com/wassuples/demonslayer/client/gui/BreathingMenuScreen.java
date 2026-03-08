package com.wassuples.demonslayer.client.gui;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.demon.DemonPassive;
import com.wassuples.demonslayer.demon.DemonProgressionHelper;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.*;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
import com.wassuples.demonslayer.styles.BloodDemonArts;
import com.wassuples.demonslayer.styles.BreathingForms;
import com.wassuples.demonslayer.styles.BreathingStyles;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The main player progression GUI opened by the {@link com.wassuples.demonslayer.util.KeyBinding#OPEN_MENU_KEY}.
 *
 * <p>Displays breathing style/form selection, mastery levels, demon BDA management,
 * passive skill tree, rank information, and active missions. Supports both slayer
 * and demon-path layouts depending on the player's current path.
 */
public class BreathingMenuScreen extends Screen {
    private static final float GUI_SCALE = 1.5f;

    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/breathing_menu_background.png");
    private static final ResourceLocation WIDGETS_TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/breathing_menu_widgets.png");
    private static final ResourceLocation DEMON_WIDGETS_TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/demon_widgets.png");

    private final int windowWidth = (int) (256 * GUI_SCALE);
    private final int windowHeight = (int) (166 * GUI_SCALE);

    private final int playerModelXOffset = (int) (51 * GUI_SCALE);
    private final int playerModelYOffset = (int) (150 * GUI_SCALE);
    private final int playerModelScale = (int) (55 * GUI_SCALE);

    private final int infoPanelXOffset = (int) (95 * GUI_SCALE);
    private final int infoPanelYOffset = (int) (45 * GUI_SCALE);
    private final int infoPanelLineSpacing = (int) (15 * GUI_SCALE);

    private final int breathBarYOffset = (int) (95 * GUI_SCALE);
    private final int breathBarHeight = 12;

    private final int tabXStart = (int) (10 * GUI_SCALE);
    private final int tabYStart = (int) (5 * GUI_SCALE);
    private final int tabXSpacing = (int) (22 * GUI_SCALE);

    private final int textColor = 0xCCCCCC;
    private final int goldColor = 0xFFD700;
    private final int redColor = 0xFF5555;
    private final int grayColor = 0xAAAAAA;

    private final int MASTERY_MAX = 1000;
    private final int skillIconSize = 16;
    private final int skillMasteryBarWidth = 32;
    private final int skillMasteryBarHeight = 4;
    private final int subWindowXOffset = 8;
    private final int subWindowYOffset = 30;
    private final int subWindowWidth = (int) (240 * GUI_SCALE);
    private final int subWindowHeight = (int) (128 * GUI_SCALE);

    private final float skillTreeContentWidth = 700 * GUI_SCALE;
    private final float skillTreeContentHeight = 210 * GUI_SCALE;
    private final float skillTreeContentCenterX = 220f;
    private final float skillTreeContentCenterY = 105f;

    private double skillTreePanX = 0;
    private double skillTreePanY = 0;
    private double skillTreeZoom = 1.0;

    private List<BreathingForms> formsForSelectedStyle;
    private BreathingForms selectedForm = null;
    private double moveListScroll = 0.0;
    private final int moveListEntryHeight = 12;
    private int selectedFormIndex = -1;

    private enum Tab { PROFILE, SKILL_TREE, MOVE_LIST, DEMON_PROGRESSION }
    private Tab currentTab = Tab.PROFILE;

    private BreathingStyles selectedStyleForMoveList = null;

    private TabButton profileButton;
    private TabButton stylesButton;
    private TabButton demonProgressionButton;
    private Button switchStyleButton;
    private Button equipBdaButton;

    private enum DemonSubTab { RANK, BDA, PHYSIOLOGY }
    private DemonSubTab currentDemonTab = DemonSubTab.RANK;
    private Button rankUpButton;
    private Button rankSubTabButton, bdaSubTabButton, physiologySubTabButton;
    private BloodDemonArts selectedBdaForUpgrade;
    private double bdaUpgradeScroll;
    private Button upgradeBdaButton;
    private List<BloodDemonArts> demonArtsList = new ArrayList<>();
    private Button purchaseBdaButton;

    private Button parkourButton;

    private static final Map<DemonPassive, Vec2> PASSIVE_POSITIONS = new ImmutableMap.Builder<DemonPassive, Vec2>()
            .put(DemonPassive.ENHANCED_VIGOR, new Vec2(20, 20))
            .put(DemonPassive.DEMONIC_STRENGTH, new Vec2(20, 60))
            .put(DemonPassive.TOUGHENED_HIDE, new Vec2(90, 20))
            .put(DemonPassive.HYPER_REGENERATION, new Vec2(90, 60))
            .put(DemonPassive.BLOOD_ART_POTENCY, new Vec2(160, 20))
            .put(DemonPassive.STALKERS_GAIT, new Vec2(160, 60))
            .put(DemonPassive.OVERWHELMING_AURA, new Vec2(230, 40))
            .put(DemonPassive.SUNLIGHT_RESISTANCE, new Vec2(300, 40))
            .build();

    private static final Map<BreathingStyles, Vec2> STYLE_POSITIONS = new ImmutableMap.Builder<BreathingStyles, Vec2>()
            // TIER 0 (ROOT)
            .put(BreathingStyles.SUN, new Vec2(225, 10))

            // TIER 1 (PRIMARY BRANCHES - 90px spacing)
            .put(BreathingStyles.WATER,   new Vec2(0, 65))
            .put(BreathingStyles.MOON,    new Vec2(90, 65))
            .put(BreathingStyles.FLAME,   new Vec2(180, 65))
            .put(BreathingStyles.THUNDER, new Vec2(270, 65))
            .put(BreathingStyles.WIND,    new Vec2(360, 65))
            .put(BreathingStyles.STONE,   new Vec2(515, 65))

            // TIER 2 (SECONDARY BRANCHES - 70px spacing for siblings)
            .put(BreathingStyles.FLOWER,  new Vec2(-95, 120)) // from Water
            .put(BreathingStyles.SERPENT, new Vec2(0, 120))    // from Water
            .put(BreathingStyles.FROST,   new Vec2(95, 120))   // from Water
            .put(BreathingStyles.LOVE,    new Vec2(180, 120))  // from Flame
            .put(BreathingStyles.SOUND,   new Vec2(270, 120))  // from Thunder
            .put(BreathingStyles.MIST,    new Vec2(355, 120))  // from Wind
            .put(BreathingStyles.BEAST,   new Vec2(435, 120))  // from Wind
            .put(BreathingStyles.WOOD,    new Vec2(515, 120))  // from Stone

            // TIER 3 (TERTIARY BRANCHES)
            .put(BreathingStyles.INSECT,  new Vec2(-95, 175)) // from Flower
            .put(BreathingStyles.SAND,    new Vec2(355, 175))  // from Mist
            .build();

    private static final Map<BreathingStyles, BreathingStyles> STYLE_PARENTS = new ImmutableMap.Builder<BreathingStyles, BreathingStyles>()
            .put(BreathingStyles.WATER, BreathingStyles.SUN)
            .put(BreathingStyles.MOON, BreathingStyles.SUN)
            .put(BreathingStyles.FLAME, BreathingStyles.SUN)
            .put(BreathingStyles.THUNDER, BreathingStyles.SUN)
            .put(BreathingStyles.WIND, BreathingStyles.SUN)
            .put(BreathingStyles.STONE, BreathingStyles.SUN)
            .put(BreathingStyles.FLOWER, BreathingStyles.WATER)
            .put(BreathingStyles.SERPENT, BreathingStyles.WATER)
            .put(BreathingStyles.FROST, BreathingStyles.WATER)
            .put(BreathingStyles.LOVE, BreathingStyles.FLAME)
            .put(BreathingStyles.SOUND, BreathingStyles.THUNDER)
            .put(BreathingStyles.MIST, BreathingStyles.WIND)
            .put(BreathingStyles.BEAST, BreathingStyles.WIND)
            .put(BreathingStyles.WOOD, BreathingStyles.STONE)
            .put(BreathingStyles.INSECT, BreathingStyles.FLOWER)
            .put(BreathingStyles.SAND, BreathingStyles.MIST)
            .build();

    public BreathingMenuScreen() {
        super(Component.literal("Breathing Menu"));
    }

    @Override
    protected void init() {
        super.init();
        int leftPos = (this.width - this.windowWidth) / 2;
        int topPos = (this.height - this.windowHeight) / 2;

        this.profileButton = this.addRenderableWidget(new TabButton(leftPos + tabXStart, topPos + tabYStart, 0, (b) -> this.currentTab = Tab.PROFILE));
        this.stylesButton = this.addRenderableWidget(new TabButton(leftPos + tabXStart + tabXSpacing, topPos + tabYStart, 20, (b) -> this.currentTab = Tab.SKILL_TREE));
        this.demonProgressionButton = this.addRenderableWidget(new TabButton(leftPos + tabXStart + (tabXSpacing * 2), topPos + tabYStart, 60, (b) -> {
            this.currentTab = Tab.DEMON_PROGRESSION;
            this.prepareBdaUpgradeList();
        }));

        int subTabButtonY = topPos + subWindowYOffset + 5;
        this.rankSubTabButton = this.addRenderableWidget(new Button(leftPos + subWindowXOffset + 5, subTabButtonY, 50, 20, Component.literal("Rank"), (b) -> this.currentDemonTab = DemonSubTab.RANK));
        this.bdaSubTabButton = this.addRenderableWidget(new Button(leftPos + subWindowXOffset + 60, subTabButtonY, 50, 20, Component.literal("BDA"), (b) -> this.currentDemonTab = DemonSubTab.BDA));
        this.physiologySubTabButton = this.addRenderableWidget(new Button(leftPos + subWindowXOffset + 115, subTabButtonY, 70, 20, Component.literal("Physiology"), (b) -> this.currentDemonTab = DemonSubTab.PHYSIOLOGY));

        this.rankUpButton = this.addRenderableWidget(new Button(0, 0, 80, 20, Component.literal("ASCEND"), (b) -> ModMessages.sendToServer(new RankUpC2SPacket())));
        this.purchaseBdaButton = this.addRenderableWidget(new Button(0, 0, 80, 20, Component.literal("Purchase"), (b) -> {
            if (this.selectedBdaForUpgrade != null) ModMessages.sendToServer(new PurchaseBdaC2SPacket(this.selectedBdaForUpgrade));
        }));
        this.equipBdaButton = this.addRenderableWidget(new Button(0, 0, 80, 20, Component.literal("Equip"), (b) -> {
            if (this.selectedBdaForUpgrade != null) ModMessages.sendToServer(new EquipBdaC2SPacket(this.selectedBdaForUpgrade));
        }));
        this.upgradeBdaButton = this.addRenderableWidget(new Button(0, 0, 80, 20, Component.literal("UPGRADE"), (b) -> {
            if (this.selectedBdaForUpgrade != null) ModMessages.sendToServer(new UpgradeBdaC2SPacket(this.selectedBdaForUpgrade));
        }));

        this.switchStyleButton = this.addRenderableWidget(new Button(leftPos + windowWidth - 92, topPos + 5, 80, 20, Component.literal("Switch Style"), (b) -> {
            if (this.selectedStyleForMoveList != null) ModMessages.sendToServer(new SwitchStyleC2SPacket(this.selectedStyleForMoveList.ordinal()));
        }));
        this.prepareMoveList();
        this.prepareBdaUpgradeList();

        this.demonProgressionButton.visible = false;
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                this.demonProgressionButton.visible = demon.isDemon();
            });
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, BACKGROUND_TEXTURE);
        int x = (this.width - this.windowWidth) / 2;
        int y = (this.height - this.windowHeight) / 2;
        this.blit(poseStack, x, y, 0, 0, this.windowWidth, this.windowHeight);

        this.profileButton.isActive = (this.currentTab == Tab.PROFILE);
        this.stylesButton.isActive = (this.currentTab == Tab.SKILL_TREE);
        this.demonProgressionButton.isActive = (this.currentTab == Tab.DEMON_PROGRESSION);

        this.switchStyleButton.visible = false;
        this.equipBdaButton.visible = false;
        this.purchaseBdaButton.visible = false;
        this.rankUpButton.visible = false;
        this.upgradeBdaButton.visible = false;
        this.rankSubTabButton.visible = false;
        this.bdaSubTabButton.visible = false;
        this.physiologySubTabButton.visible = false;

        if (this.currentTab == Tab.MOVE_LIST) {
            this.switchStyleButton.visible = true;
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
                    this.switchStyleButton.active = style.getCurrentStyles() != this.selectedStyleForMoveList;
                });
            }
        } else if (this.currentTab == Tab.DEMON_PROGRESSION) {
            this.rankSubTabButton.visible = true;
            this.bdaSubTabButton.visible = true;
            this.physiologySubTabButton.visible = true;
            this.rankSubTabButton.active = (this.currentDemonTab != DemonSubTab.RANK);
            this.bdaSubTabButton.active = (this.currentDemonTab != DemonSubTab.BDA);
            this.physiologySubTabButton.active = (this.currentDemonTab != DemonSubTab.PHYSIOLOGY);
        }

        switch (this.currentTab) {
            case PROFILE -> renderProfileTab(poseStack, x, y, mouseX, mouseY);
            case SKILL_TREE -> renderSkillTreeTab(poseStack, x, y);
            case MOVE_LIST -> renderMoveListTab(poseStack, x, y, mouseX, mouseY);
            case DEMON_PROGRESSION -> renderDemonProgressionTab(poseStack, x, y, mouseX, mouseY);
        }

        super.render(poseStack, mouseX, mouseY, partialTick);

        switch (this.currentTab) {
            case PROFILE -> renderBreathTooltip(poseStack, x, y, mouseX, mouseY);
            case SKILL_TREE -> renderSkillTreeTooltip(poseStack, x, y, mouseX, mouseY);
        }
    }


    private void renderProfileTab(PoseStack poseStack, int leftPos, int topPos, int mouseX, int mouseY) {
        int entityRenderX = leftPos + playerModelXOffset;
        int entityRenderY = topPos + playerModelYOffset;

        InventoryScreen.renderEntityInInventory(
                entityRenderX,
                entityRenderY,
                playerModelScale,
                (float)entityRenderX - mouseX,
                (float)(entityRenderY - 50) - mouseY,
                Objects.requireNonNull(this.minecraft.player)
        );

        int infoX = leftPos + infoPanelXOffset;
        int infoY = topPos + infoPanelYOffset;

        if (this.minecraft.player == null) return;

        this.minecraft.player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression ->
                this.minecraft.player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style ->
                        this.minecraft.player.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {

                            this.font.draw(poseStack, "Name: " + this.minecraft.player.getGameProfile().getName(), infoX, infoY, textColor);

                            int rankY = infoY + infoPanelLineSpacing;
                            this.font.draw(poseStack, "Rank: ", infoX, rankY, textColor);
                            int rankTextXOffset = infoX + this.font.width("Rank: ");
                            this.minecraft.player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                                MutableComponent rankName;
                                if(demon.isDemon()) {
                                    rankName = demon.getRank().getDisplayName();
                                } else {
                                    rankName = progression.getRank().getDisplayName();
                                }
                                this.font.draw(poseStack, rankName.getString(), rankTextXOffset, rankY, rankName.getStyle().getColor().getValue());
                            });

                            BreathingStyles currentStyle = style.getCurrentStyles();
                            int styleY = infoY + (infoPanelLineSpacing * 2);
                            this.font.draw(poseStack, "Style: ", infoX, styleY, textColor);
                            int styleTextXOffset = infoX + this.font.width("Style: ");
                            this.font.draw(poseStack, currentStyle.getDisplayName(), styleTextXOffset, styleY, textColor);

                            RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
                            final int ICONS_PER_COLUMN = 12;
                            final int ICON_GRID_V_OFFSET = 64;
                            int styleIndex = Math.max(0, currentStyle.ordinal() - 1);
                            int iconU = (styleIndex / ICONS_PER_COLUMN) * 16;
                            int iconV = ICON_GRID_V_OFFSET + ((styleIndex % ICONS_PER_COLUMN) * 16);
                            this.blit(poseStack, styleTextXOffset + this.font.width(currentStyle.getDisplayName()) + 3, styleY - 4, iconU, iconV, 16, 16);

                            int barX = infoX;
                            int barY = topPos + breathBarYOffset;
                            int barWidth = 140;
                            int fillWidth = 0;
                            if (breath.getMaxBreath() > 0) {
                                fillWidth = (int)(((float)breath.getBreath() / breath.getMaxBreath()) * barWidth);
                            }

                            this.blit(poseStack, barX, barY, 0, 40, barWidth, breathBarHeight);
                            if (fillWidth > 0) {
                                this.blit(poseStack, barX, barY, 0, 52, fillWidth, breathBarHeight);
                            }
                        })
                )
        );
    }

    private void renderBreathTooltip(PoseStack poseStack, int leftPos, int topPos, int mouseX, int mouseY) {
        if (this.currentTab != Tab.PROFILE || this.minecraft.player == null) return;

        int barX = leftPos + infoPanelXOffset;
        int barY = topPos + breathBarYOffset;
        int barWidth = 140;

        if (mouseX >= barX && mouseX <= barX + barWidth && mouseY >= barY && mouseY <= barY + breathBarHeight) {
            this.minecraft.player.getCapability(PlayerBreathProvider.PLAYER_BREATH).ifPresent(breath -> {
                Component tooltipText = Component.literal("Breath: ").withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(breath.getBreath() + " / " + breath.getMaxBreath()).withStyle(ChatFormatting.WHITE));
                this.renderTooltip(poseStack, tooltipText, mouseX, mouseY);
            });
        }
    }

    private void renderSkillTreeTab(PoseStack poseStack, int leftPos, int topPos) {
        if (this.minecraft.player == null) return;

        int subWindowX = leftPos + subWindowXOffset;
        int subWindowY = topPos + subWindowYOffset;

        double guiScale = this.minecraft.getWindow().getGuiScale();
        RenderSystem.enableScissor(
                (int)(subWindowX * guiScale),
                (int)((this.height - (subWindowY + subWindowHeight)) * guiScale),
                (int)(subWindowWidth * guiScale),
                (int)(subWindowHeight * guiScale)
        );

        poseStack.pushPose();

        poseStack.translate(subWindowX + subWindowWidth / 2.0, subWindowY + subWindowHeight / 2.0, 0);
        poseStack.scale((float) this.skillTreeZoom, (float) this.skillTreeZoom, 1.0f);
        poseStack.translate(this.skillTreePanX, this.skillTreePanY, 0);
        poseStack.translate(-this.skillTreeContentCenterX, -this.skillTreeContentCenterY, 0);

        RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1.0f);
        for (Map.Entry<BreathingStyles, BreathingStyles> entry : STYLE_PARENTS.entrySet()) {
            BreathingStyles child = entry.getKey();
            BreathingStyles parent = entry.getValue();
            if (STYLE_POSITIONS.containsKey(child) && STYLE_POSITIONS.containsKey(parent)) {
                drawConnectingLine(poseStack,
                        STYLE_POSITIONS.get(parent).add(new Vec2(8, 8)),
                        STYLE_POSITIONS.get(child).add(new Vec2(8, 8))
                );
            }
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        this.minecraft.player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
            for (Map.Entry<BreathingStyles, Vec2> entry : STYLE_POSITIONS.entrySet()) {
                BreathingStyles style = entry.getKey();
                Vec2 pos = entry.getValue();
                int x = (int)pos.x;
                int y = (int)pos.y;
                boolean isUnlocked = progression.isStyleUnlocked(style);

                if (isUnlocked) {
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                } else {
                    RenderSystem.setShaderColor(0.2f, 0.2f, 0.2f, 1.0f);
                }
                RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);

                final int ICONS_PER_COLUMN = 12;
                final int ICON_GRID_V_OFFSET = 64;
                int styleIndex = Math.max(0, style.ordinal() - 1);
                int iconU = (styleIndex / ICONS_PER_COLUMN) * 16;
                int iconV = ICON_GRID_V_OFFSET + ((styleIndex % ICONS_PER_COLUMN) * 16);
                this.blit(poseStack, x, y, iconU, iconV, 16, 16);

                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

                int masteryXp = progression.getStyleMastery(style);
                float fillPercent = masteryXp > 0 ? (float)masteryXp / MASTERY_MAX : 0;
                int barX = x + (skillIconSize / 2) - (skillMasteryBarWidth / 2);
                int barY = y + skillIconSize + 2;
                fill(poseStack, barX, barY, barX + skillMasteryBarWidth, barY + skillMasteryBarHeight, 0xFF202020);
                if (fillPercent > 0) {
                    fill(poseStack, barX, barY, barX + (int)(skillMasteryBarWidth * fillPercent), barY + skillMasteryBarHeight, 0xFFD4AF37);
                }

                MutableComponent styleName = Component.literal(style.getDisplayName());
                if(!isUnlocked) styleName.withStyle(ChatFormatting.GRAY);
                poseStack.pushPose();
                poseStack.translate(x + (skillIconSize / 2f) - (this.font.width(styleName) / 2f), barY + skillMasteryBarHeight + 3, 100);
                this.font.draw(poseStack, styleName.getString(), 0, 0, textColor);
                poseStack.popPose();
            }
        });

        poseStack.popPose();
        RenderSystem.disableScissor();
    }

    private void renderSkillTreeTooltip(PoseStack poseStack, int leftPos, int topPos, int mouseX, int mouseY) {
        if (this.minecraft.player == null || this.currentTab != Tab.SKILL_TREE) return;

        int subWindowX = leftPos + subWindowXOffset;
        int subWindowY = topPos + subWindowYOffset;

        if(mouseX < subWindowX || mouseX > subWindowX + subWindowWidth || mouseY < subWindowY || mouseY > subWindowY + subWindowHeight){
            return;
        }

        double treeMouseX = (mouseX - (subWindowX + subWindowWidth / 2.0)) / skillTreeZoom - skillTreePanX + this.skillTreeContentCenterX;
        double treeMouseY = (mouseY - (subWindowY + subWindowHeight / 2.0)) / skillTreeZoom - skillTreePanY + this.skillTreeContentCenterY;

        this.minecraft.player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
            var reversedEntries = new java.util.ArrayList<>(STYLE_POSITIONS.entrySet());
            java.util.Collections.reverse(reversedEntries);

            for (Map.Entry<BreathingStyles, Vec2> entry : reversedEntries) {
                BreathingStyles style = entry.getKey();
                Vec2 pos = entry.getValue();

                if (treeMouseX >= pos.x && treeMouseX <= pos.x + skillIconSize &&
                        treeMouseY >= pos.y && treeMouseY <= pos.y + skillIconSize) {

                    List<Component> tooltipLines = new ArrayList<>();
                    if (progression.isStyleUnlocked(style)) {
                        tooltipLines.add(Component.literal(style.getDisplayName()).withStyle(ChatFormatting.GOLD));
                        tooltipLines.add(Component.literal("Mastery: " + progression.getStyleMastery(style) + " / " + MASTERY_MAX).withStyle(ChatFormatting.GRAY));
                        tooltipLines.add(Component.literal("Click to view moves.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                    } else {
                        tooltipLines.add(Component.literal(style.getDisplayName()).withStyle(ChatFormatting.RED));
                        tooltipLines.add(Component.literal("Locked").withStyle(ChatFormatting.GRAY));
                    }
                    this.renderComponentTooltip(poseStack, tooltipLines, mouseX, mouseY);
                    return;
                }
            }
        });
    }

    private void renderDemonProgressionTab(PoseStack poseStack, int leftPos, int topPos, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.player == null) return;
        int subWindowX = leftPos + subWindowXOffset;
        int subWindowY = topPos + subWindowYOffset;

        fill(poseStack, subWindowX, subWindowY + 28, subWindowX + subWindowWidth, subWindowY + subWindowHeight, 0x90000000);

        this.minecraft.player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            String bdpText = "Blood Demon Points: " + demon.getBloodPoints();
            this.font.draw(poseStack, bdpText, leftPos + subWindowXOffset + 195, topPos + subWindowYOffset + 12, goldColor);
        });

        switch(currentDemonTab) {
            case RANK -> renderRankUpView(poseStack, leftPos, topPos, mouseX, mouseY);
            case BDA -> renderBdaManagementView(poseStack, leftPos, topPos, mouseX, mouseY);
            case PHYSIOLOGY -> renderPhysiologyView(poseStack, leftPos, topPos, mouseX, mouseY);
        }
    }

    private void renderRankUpView(PoseStack poseStack, int leftPos, int topPos, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.player == null) return;

        int contentX = leftPos + subWindowXOffset + 10;
        int contentY = topPos + subWindowYOffset + 40;
        int rightColumnX = contentX + 160;

        this.minecraft.player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            // Left: Rank Up Info
            font.draw(poseStack, "Current Rank: ", contentX, contentY, textColor);
            font.draw(poseStack, demon.getRank().getDisplayName(), contentX + font.width("Current Rank: "), contentY, 0xFFFFFF);

            DemonProgressionHelper.getNextRank(demon.getRank()).ifPresentOrElse(nextRank -> {
                int cost = DemonProgressionHelper.RANK_UP_COSTS.getOrDefault(demon.getRank(), -1);
                font.draw(poseStack, "Next Rank: ", contentX, contentY + 12, textColor);
                font.draw(poseStack, nextRank.getDisplayName(), contentX + font.width("Next Rank: "), contentY + 12, 0xFFFFFF);
                Component costText = Component.literal("Cost: " + cost + " BDP").withStyle(demon.getBloodPoints() >= cost ? ChatFormatting.GREEN : ChatFormatting.RED);
                font.draw(poseStack, costText, contentX, contentY + 24, 0xFFFFFF);

                this.rankUpButton.visible = true;
                this.rankUpButton.active = demon.getBloodPoints() >= cost;
                this.rankUpButton.x = leftPos + (windowWidth / 2) - (rankUpButton.getWidth() / 2);
                this.rankUpButton.y = topPos + windowHeight - 40;
            }, () -> {
                font.draw(poseStack, "You have reached the final rank.", contentX, contentY + 24, goldColor);
                this.rankUpButton.visible = false;
            });

            // Right: Current Attributes
            font.draw(poseStack, Component.literal("Current Attributes").withStyle(ChatFormatting.GOLD), rightColumnX, contentY, 0xFFFFFF);
            int statY = contentY + 12;

            float bonusHealth = DemonProgressionHelper.getBonusHealth(demon);
            font.draw(poseStack, String.format("Bonus Health: +%.1f", bonusHealth), rightColumnX, statY, textColor);
            statY += 10;

            float bonusDamage = DemonProgressionHelper.getBonusDamage(demon);
            font.draw(poseStack, String.format("Bonus Damage: +%.0f%%", bonusDamage * 100), rightColumnX, statY, textColor);
            statY += 10;

            float bonusRegen = DemonProgressionHelper.getBonusRegen(demon);
            font.draw(poseStack, String.format("Regeneration: +%.1f/s", bonusRegen), rightColumnX, statY, textColor);
            statY += 10;

            float damageResist = DemonProgressionHelper.getDamageResistance(demon);
            font.draw(poseStack, String.format("Resistance: %.0f%%", damageResist * 100), rightColumnX, statY, textColor);
        });
    }

    private void renderBdaManagementView(PoseStack poseStack, int leftPos, int topPos, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.player == null) return;

        int contentTopY = topPos + subWindowYOffset + 32;
        int listWidth = 100;
        int listLeft = leftPos + subWindowXOffset + 5;
        int listHeight = subWindowHeight - 38;

        RenderSystem.enableScissor((int)(listLeft * minecraft.getWindow().getGuiScale()), (int)((this.height - (contentTopY + listHeight)) * minecraft.getWindow().getGuiScale()), (int)(listWidth * minecraft.getWindow().getGuiScale()), (int)(listHeight * minecraft.getWindow().getGuiScale()));
        for (int i = 0; i < demonArtsList.size(); i++) {
            BloodDemonArts bda = demonArtsList.get(i);
            int entryY = contentTopY + (i * moveListEntryHeight) - (int)bdaUpgradeScroll;
            if (entryY + moveListEntryHeight >= contentTopY && entryY <= contentTopY + listHeight) {
                if (bda == this.selectedBdaForUpgrade) {
                    fill(poseStack, listLeft - 2, entryY, listLeft + listWidth, entryY + moveListEntryHeight, 0x80FFFFFF);
                }
                this.font.draw(poseStack, bda.getDisplayName(), listLeft + 2, entryY + 2, textColor);
            }
        }
        RenderSystem.disableScissor();

        if (this.selectedBdaForUpgrade != null) {
            int detailX = listLeft + listWidth + 10;
            int yOffset = contentTopY + 5;
            font.draw(poseStack, this.selectedBdaForUpgrade.getDisplayName(), detailX, yOffset, goldColor);
            yOffset += 12;
            font.drawWordWrap(this.selectedBdaForUpgrade.getDescription().copy().withStyle(ChatFormatting.GRAY), detailX, yOffset, 130, textColor);
            yOffset += 45;

            final int finalYOffset = yOffset;
            this.minecraft.player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
                int buttonX = leftPos + (windowWidth / 2) - 40;
                int buttonY = topPos + windowHeight - 40;
                if (demon.isBdaUnlocked(this.selectedBdaForUpgrade)) {
                    this.upgradeBdaButton.visible = true;
                    this.equipBdaButton.visible = true;

                    this.equipBdaButton.x = buttonX - 42;
                    this.equipBdaButton.y = buttonY;
                    this.equipBdaButton.setWidth(80);
                    this.equipBdaButton.active = demon.getEquippedBda() != this.selectedBdaForUpgrade;

                    this.upgradeBdaButton.x = buttonX + 42;
                    this.upgradeBdaButton.y = buttonY;
                    this.upgradeBdaButton.setWidth(80);

                    int currentTier = demon.getBdaTier(this.selectedBdaForUpgrade);
                    int maxTier = DemonProgressionHelper.BDA_UPGRADE_COSTS.get(this.selectedBdaForUpgrade) != null ? DemonProgressionHelper.BDA_UPGRADE_COSTS.get(this.selectedBdaForUpgrade).size() : 0;

                    font.draw(poseStack, "Tier: " + currentTier + " / " + maxTier, detailX, finalYOffset, textColor);

                    int cost = DemonProgressionHelper.getModifiedBdaUpgradeCost(demon, this.selectedBdaForUpgrade);
                    if (cost != -1) {
                        font.draw(poseStack, "Next Cost: " + cost + " BDP", detailX, finalYOffset + 12, textColor);
                        this.upgradeBdaButton.active = demon.getBloodPoints() >= cost;
                    } else {
                        font.draw(poseStack, Component.literal("Max Tier Reached").withStyle(ChatFormatting.AQUA), detailX, finalYOffset + 12, 0xFFFFFF);
                        this.upgradeBdaButton.active = false;
                    }
                } else {
                    this.purchaseBdaButton.visible = true;
                    this.purchaseBdaButton.x = buttonX;
                    this.purchaseBdaButton.y = buttonY;

                    int cost = DemonProgressionHelper.getModifiedBdaPurchaseCost(demon, this.selectedBdaForUpgrade);
                    if (cost != -1) {
                        Component costText = Component.literal("Cost: " + cost + " BDP").withStyle(demon.getBloodPoints() >= cost ? ChatFormatting.GREEN : ChatFormatting.RED);
                        font.draw(poseStack, costText, detailX, finalYOffset, 0xFFFFFF);
                        this.purchaseBdaButton.active = demon.getBloodPoints() >= cost;
                    } else {
                        font.draw(poseStack, Component.literal("Not Purchasable").withStyle(ChatFormatting.GRAY), detailX, finalYOffset, 0xFFFFFF);
                        this.purchaseBdaButton.active = false;
                    }
                }
            });
        }
    }

    private void renderPhysiologyView(PoseStack poseStack, int leftPos, int topPos, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.player == null) return;
        int contentX = leftPos + subWindowXOffset + 10;
        int contentY = topPos + subWindowYOffset + 40;

        this.minecraft.player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            for(DemonPassive passive : DemonPassive.values()) {
                Vec2 pos = PASSIVE_POSITIONS.get(passive);
                if(pos == null) continue;

                int tier = demon.getPassiveTier(passive);
                int maxTier = passive.getMaxTier();
                boolean isActive = demon.isPassiveActive(passive);
                int iconX = contentX + (int)pos.x;
                int iconY = contentY + (int)pos.y;

                RenderSystem.setShaderTexture(0, DEMON_WIDGETS_TEXTURE);
                blit(poseStack, iconX, iconY, 0, 0, 16, 16);

                String tierText = tier + "/" + maxTier;
                font.draw(poseStack, tierText, iconX + 18, iconY + 4, textColor);

                if (tier > 0) {
                    Component status = isActive ? Component.literal("[ON]").withStyle(ChatFormatting.GREEN) : Component.literal("[OFF]").withStyle(ChatFormatting.RED);
                    font.draw(poseStack, status, iconX + font.width(tierText) + 22, iconY + 4, 0xFFFFFF);
                }
            }
        });

        for(DemonPassive passive : DemonPassive.values()) {
            Vec2 pos = PASSIVE_POSITIONS.get(passive);
            if(pos == null) continue;
            int iconX = contentX + (int)pos.x;
            int iconY = contentY + (int)pos.y;
            if(mouseX >= iconX && mouseX <= iconX + 16 && mouseY >= iconY && mouseY <= iconY + 16) {
                renderPassiveTooltip(poseStack, passive, mouseX, mouseY);
                break;
            }
        }
    }

    private void renderPassiveTooltip(PoseStack stack, DemonPassive passive, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.player == null) return;

        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(passive.getDisplayName()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(passive.getDescription()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(""));

        this.minecraft.player.getCapability(PlayerDemonProvider.PLAYER_DEMON).ifPresent(demon -> {
            int currentTier = demon.getPassiveTier(passive);
            tooltip.add(Component.literal("Current Tier: " + currentTier + "/" + passive.getMaxTier()).withStyle(ChatFormatting.WHITE));

            if(currentTier < passive.getMaxTier()) {
                int cost = DemonProgressionHelper.PASSIVE_SKILL_COSTS.get(passive).get(currentTier);
                tooltip.add(Component.literal("Next Tier Cost: " + cost + " BDP")
                        .withStyle(demon.getBloodPoints() >= cost ? ChatFormatting.GREEN : ChatFormatting.RED));
                tooltip.add(Component.literal("Left-click to Upgrade").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
            } else {
                tooltip.add(Component.literal("Max Tier Reached").withStyle(ChatFormatting.AQUA));
            }

            if (currentTier > 0) {
                tooltip.add(Component.literal("Right-click to Toggle").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        });

        renderComponentTooltip(stack, tooltip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (this.currentTab == Tab.SKILL_TREE) {
            if (this.minecraft.player != null) {
                int leftPos = (this.width - this.windowWidth) / 2;
                int topPos = (this.height - this.windowHeight) / 2;
                int subWindowX = leftPos + subWindowXOffset;
                int subWindowY = topPos + subWindowYOffset;

                if(pMouseX < subWindowX || pMouseX > subWindowX + subWindowWidth || pMouseY < subWindowY || pMouseY > subWindowY + subWindowHeight){
                    return super.mouseClicked(pMouseX, pMouseY, pButton);
                }

                double treeMouseX = (pMouseX - (subWindowX + subWindowWidth / 2.0)) / skillTreeZoom - skillTreePanX + this.skillTreeContentCenterX;
                double treeMouseY = (pMouseY - (subWindowY + subWindowHeight / 2.0)) / skillTreeZoom - skillTreePanY + this.skillTreeContentCenterY;

                this.minecraft.player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                    for (Map.Entry<BreathingStyles, Vec2> entry : STYLE_POSITIONS.entrySet()) {
                        BreathingStyles style = entry.getKey();
                        Vec2 pos = entry.getValue();

                        if (treeMouseX >= pos.x && treeMouseX <= pos.x + skillIconSize &&
                                treeMouseY >= pos.y && treeMouseY <= pos.y + skillIconSize) {

                            if (progression.isStyleUnlocked(style)) {
                                this.selectedStyleForMoveList = style;
                                this.prepareMoveList();
                                this.currentTab = Tab.MOVE_LIST;
                                Objects.requireNonNull(this.minecraft.getSoundManager()).play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                                return;
                            }
                        }
                    }
                });
            }
        }
        if (this.currentTab == Tab.MOVE_LIST && this.formsForSelectedStyle != null) {
            int leftPos = (this.width - this.windowWidth) / 2;
            int topPos = (this.height - this.windowHeight) / 2;
            int subWindowX = leftPos + 8;
            int listY = topPos + 30 + 28;
            int listWidth = 100;
            int listHeight = 128 - 32;

            if (pMouseX >= subWindowX && pMouseX < subWindowX + listWidth && pMouseY >= listY && pMouseY < listY + listHeight) {
                int clickedIndex = (int)((pMouseY - listY + moveListScroll) / moveListEntryHeight);

                if (clickedIndex >= 0 && clickedIndex < formsForSelectedStyle.size()) {
                    this.selectedFormIndex = clickedIndex;
                    this.selectedForm = formsForSelectedStyle.get(clickedIndex);
                    return true;
                }
            }
        }
        if (this.currentTab == Tab.DEMON_PROGRESSION) {
            if (this.currentDemonTab == DemonSubTab.BDA) {
                int contentTopY = (this.height - this.windowHeight) / 2 + subWindowYOffset + 32;
                int listLeft = (this.width - this.windowWidth) / 2 + subWindowXOffset + 5;
                int listWidth = 100;
                int listHeight = subWindowHeight - 38;
                if (pMouseX >= listLeft && pMouseX < listLeft + listWidth && pMouseY >= contentTopY && pMouseY < contentTopY + listHeight) {
                    int clickedIndex = (int) ((pMouseY - contentTopY + bdaUpgradeScroll) / moveListEntryHeight);
                    if (clickedIndex >= 0 && clickedIndex < this.demonArtsList.size()) {
                        this.selectedBdaForUpgrade = this.demonArtsList.get(clickedIndex);
                        return true;
                    }
                }
            }
            if (currentDemonTab == DemonSubTab.PHYSIOLOGY) {
                int leftPos = (this.width - this.windowWidth) / 2;
                int topPos = (this.height - this.windowHeight) / 2;
                int contentX = leftPos + subWindowXOffset + 10;
                int contentY = topPos + subWindowYOffset + 40;
                for (DemonPassive passive : DemonPassive.values()) {
                    Vec2 pos = PASSIVE_POSITIONS.get(passive);
                    if (pos == null) continue;
                    int iconX = contentX + (int) pos.x;
                    int iconY = contentY + (int) pos.y;
                    if (pMouseX >= iconX && pMouseX <= iconX + 16 && pMouseY >= iconY && pMouseY <= iconY + 16) {
                        if (pButton == 0) {
                            ModMessages.sendToServer(new PurchasePassiveC2SPacket(passive));
                            return true;
                        } else if (pButton == 1) {
                            ModMessages.sendToServer(new TogglePassiveC2SPacket(passive));
                            return true;
                        }
                    }
                }
            }
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    private void prepareMoveList() {
        if (selectedStyleForMoveList == null) return;

        this.formsForSelectedStyle = java.util.Arrays.stream(BreathingForms.values())
                .filter(form -> form.getStyle() == selectedStyleForMoveList)
                .collect(Collectors.toList());

        if (!this.formsForSelectedStyle.isEmpty()) {
            this.selectedFormIndex = 0;
            this.selectedForm = this.formsForSelectedStyle.get(0);
        } else {
            this.selectedFormIndex = -1;
            this.selectedForm = null;
        }
        this.moveListScroll = 0;
    }

    private void prepareBdaUpgradeList() {
        this.demonArtsList = Arrays.stream(BloodDemonArts.values())
                .filter(bda -> bda != BloodDemonArts.NONE)
                .collect(Collectors.toList());

        if (!this.demonArtsList.isEmpty() && this.selectedBdaForUpgrade == null) {
            this.selectedBdaForUpgrade = this.demonArtsList.get(0);
        }
        this.bdaUpgradeScroll = 0;
    }

    private void renderMoveListTab(PoseStack poseStack, int leftPos, int topPos, int mouseX, int mouseY) {
        if (selectedStyleForMoveList == null || this.minecraft == null || this.minecraft.player == null) return;

        int subWindowX = leftPos + 8;
        int subWindowY = topPos + 30;
        int subWindowWidth = (int) (240 * GUI_SCALE);
        int subWindowHeight = (int) (128 * GUI_SCALE);

        fill(poseStack, subWindowX, subWindowY, subWindowX + subWindowWidth, subWindowY + subWindowHeight, 0x90000000);

        // HEADER
        RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
        final int ICONS_PER_COLUMN = 12;
        final int ICON_GRID_V_OFFSET = 64;
        int styleIndex = Math.max(0, selectedStyleForMoveList.ordinal() - 1);
        int iconU = (styleIndex / ICONS_PER_COLUMN) * 16;
        int iconV = ICON_GRID_V_OFFSET + ((styleIndex % ICONS_PER_COLUMN) * 16);
        this.blit(poseStack, subWindowX + 4, subWindowY + 4, iconU, iconV, 16, 16);
        this.font.draw(poseStack, selectedStyleForMoveList.getDisplayName(), subWindowX + 24, subWindowY + 9, textColor);

        int listWidth = 100;
        int detailX = subWindowX + listWidth + 5;
        int listY = subWindowY + 28;
        int listHeight = subWindowHeight - 32;

        // LEFT: MOVE LIST
        RenderSystem.enableScissor((int)(subWindowX * minecraft.getWindow().getGuiScale()), (int)((this.height - (listY + listHeight)) * minecraft.getWindow().getGuiScale()), (int)(listWidth * minecraft.getWindow().getGuiScale()), (int)(listHeight * minecraft.getWindow().getGuiScale()));

        this.minecraft.player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
            for (int i = 0; i < formsForSelectedStyle.size(); i++) {
                BreathingForms form = formsForSelectedStyle.get(i);
                int entryY = listY + (i * moveListEntryHeight) - (int)moveListScroll;

                if (entryY + moveListEntryHeight >= listY && entryY <= listY + listHeight) {
                    boolean isSelected = (i == selectedFormIndex);
                    boolean isUnlocked = false;
                    Object style = form.getStyle();

                    if (style instanceof BreathingStyles breathingStyle) {
                        isUnlocked = progression.getStyleMastery(breathingStyle) >= form.getStyleMasteryToUnlock();
                    } else if (style instanceof BloodDemonArts bloodDemonArt) {
                        isUnlocked = progression.getDemonArtMastery(bloodDemonArt) >= form.getStyleMasteryToUnlock();
                    }

                    if (isSelected) {
                        fill(poseStack, subWindowX, entryY, subWindowX + listWidth, entryY + moveListEntryHeight, 0x80FFFFFF);
                    }

                    MutableComponent formName = Component.literal(form.getDisplayName());
                    if (!isUnlocked) {
                        formName.withStyle(ChatFormatting.GRAY, ChatFormatting.STRIKETHROUGH);
                    }

                    this.font.draw(poseStack, formName, subWindowX + 5, entryY + 2, isUnlocked ? textColor : grayColor);
                }
            }
        });
        RenderSystem.disableScissor();

        // RIGHT: DETAIL VIEW
        if (selectedForm != null) {
            this.minecraft.player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                int yOffset = subWindowY + 28;

                this.font.drawWordWrap(Component.literal(selectedForm.getDisplayName()), detailX, yOffset, subWindowWidth - listWidth - 10, goldColor);
                yOffset += this.font.wordWrapHeight(selectedForm.getDisplayName(), subWindowWidth - listWidth - 10) + 4;
                fill(poseStack, detailX, yOffset - 2, detailX + subWindowWidth - listWidth - 15, yOffset - 1, grayColor);
                this.font.drawWordWrap(Component.literal(selectedForm.getDescription()), detailX, yOffset, subWindowWidth - listWidth - 10, textColor);
                yOffset += this.font.wordWrapHeight(selectedForm.getDescription(), subWindowWidth - listWidth - 10) + 8;
                this.font.draw(poseStack, "Breath Cost: " + selectedForm.getBreathCost(), detailX, yOffset, textColor);
                yOffset += 12;
                this.font.draw(poseStack, "Form Mastery: " + progression.getFormMastery(selectedForm) + " / " + selectedForm.getMasteryGoal(), detailX, yOffset, textColor);
                yOffset += 12;

                Object selectedStyleObject = selectedForm.getStyle();
                boolean isUnlocked = false;

                if (selectedStyleObject instanceof BreathingStyles breathingStyle) {
                    isUnlocked = progression.getStyleMastery(breathingStyle) >= selectedForm.getStyleMasteryToUnlock();
                } else if (selectedStyleObject instanceof BloodDemonArts bloodDemonArt) {
                    isUnlocked = progression.getDemonArtMastery(bloodDemonArt) >= selectedForm.getStyleMasteryToUnlock();
                }

                if (isUnlocked) {
                    this.font.draw(poseStack, "Status: ", detailX, yOffset, textColor);
                    this.font.draw(poseStack, Component.literal("Unlocked").withStyle(ChatFormatting.GREEN).getVisualOrderText(), detailX + font.width("Status: "), yOffset, 0xFFFFFF);
                } else {
                    this.font.draw(poseStack, "Requires: ", detailX, yOffset, textColor);
                    String styleName = "Unknown";

                    if (selectedStyleObject instanceof BreathingStyles breathingStyle) {
                        styleName = breathingStyle.getDisplayName();
                    } else if (selectedStyleObject instanceof BloodDemonArts bloodDemonArt) {
                        styleName = bloodDemonArt.getDisplayName();
                    }

                    Component requirementText = Component.literal(selectedForm.getStyleMasteryToUnlock() + " " + styleName + " Mastery")
                            .withStyle(ChatFormatting.RED);
                    this.font.draw(poseStack, requirementText.getVisualOrderText(), detailX + font.width("Requires: "), yOffset, 0xFFFFFF);
                }
            });
        }
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (this.currentTab == Tab.MOVE_LIST && formsForSelectedStyle != null && !formsForSelectedStyle.isEmpty()) {
            if (pKeyCode == GLFW.GLFW_KEY_DOWN) {
                this.selectedFormIndex = Math.min(this.selectedFormIndex + 1, formsForSelectedStyle.size() - 1);
                this.selectedForm = formsForSelectedStyle.get(this.selectedFormIndex);
                this.ensureSelectionIsVisible();
                return true;
            } else if (pKeyCode == GLFW.GLFW_KEY_UP) {
                this.selectedFormIndex = Math.max(this.selectedFormIndex - 1, 0);
                this.selectedForm = formsForSelectedStyle.get(this.selectedFormIndex);
                this.ensureSelectionIsVisible();
                return true;
            }
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }


    private void ensureSelectionIsVisible() {
        int listHeight = 128 - 32;
        int itemTopY = this.selectedFormIndex * this.moveListEntryHeight;
        int itemBottomY = itemTopY + this.moveListEntryHeight;

        if (itemTopY < this.moveListScroll) {
            this.moveListScroll = itemTopY;
        } else if (itemBottomY > this.moveListScroll + listHeight) {
            this.moveListScroll = itemBottomY - listHeight;
        }
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if (this.currentTab == Tab.SKILL_TREE && pButton == 0) {
            this.skillTreePanX += pDragX / this.skillTreeZoom;
            this.skillTreePanY += pDragY / this.skillTreeZoom;

            float zoom = (float) this.skillTreeZoom;
            float overflowX = (skillTreeContentWidth * zoom) - subWindowWidth;
            float overflowY = (skillTreeContentHeight * zoom) - subWindowHeight;

            float panLimitX = Math.max(0, overflowX / (2f * zoom));
            float panLimitY = Math.max(0, overflowY / (2f * zoom));

            this.skillTreePanX = Mth.clamp(this.skillTreePanX, -panLimitX, panLimitX);
            this.skillTreePanY = Mth.clamp(this.skillTreePanY, -panLimitY, panLimitY);

            return true;
        }
        return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if (this.currentTab == Tab.SKILL_TREE) {
            this.skillTreeZoom += pDelta * 0.1;
            this.skillTreeZoom = Mth.clamp(this.skillTreeZoom, 0.35, 2.5);
            mouseDragged(0,0,0,0,0);
            return true;
        }
        if (this.currentTab == Tab.MOVE_LIST && this.formsForSelectedStyle != null) {
            int maxScroll = Math.max(0, (formsForSelectedStyle.size() * moveListEntryHeight) - (128 - 32));
            this.moveListScroll = Mth.clamp(this.moveListScroll - (pDelta * 10), 0, maxScroll);
            return true;
        }
        if (this.currentTab == Tab.DEMON_PROGRESSION && this.currentDemonTab == DemonSubTab.BDA) {
            int listHeight = subWindowHeight - 38;
            int maxScroll = Math.max(0, (this.demonArtsList.size() * moveListEntryHeight) - listHeight);
            this.bdaUpgradeScroll = Mth.clamp(this.bdaUpgradeScroll - (pDelta * 10), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }

    private void drawConnectingLine(PoseStack poseStack, Vec2 pos1, Vec2 pos2) {
        float x1 = pos1.x;
        float y1 = pos1.y;
        float x2 = pos2.x;
        float y2 = pos2.y;

        float distance = (float)Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        float angle = (float)Mth.atan2(y2 - y1, x2 - x1);

        poseStack.pushPose();
        poseStack.translate(x1, y1, 0);
        poseStack.mulPose(com.mojang.math.Quaternion.fromXYZ(new Vector3f(0, 0, angle)));
        fill(poseStack, 0, -1, (int)distance, 1, 0xFF808080);
        poseStack.popPose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    class TabButton extends Button {
        private final int u;
        public boolean isActive = false;

        public TabButton(int pX, int pY, int pU, OnPress pOnPress) {
            super(pX, pY, 20, 20, Component.empty(), pOnPress);
            this.u = pU;
        }

        @Override
        public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
            RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            int vOffset = 0;

            if (this.isActive || this.isHoveredOrFocused()) {
                vOffset = 20;
            }

            blit(pPoseStack, this.x, this.y, this.u, vOffset, this.width, this.height, 256, 256);
        }
    }
}

package com.wassuples.demonslayer.client.gui;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.breathing.PlayerBreathProvider;
import com.wassuples.demonslayer.breathing.PlayerStyleProvider;
import com.wassuples.demonslayer.demon.PlayerDemonProvider;
import com.wassuples.demonslayer.missions.*;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.AbandonMissionC2SPacket;
import com.wassuples.demonslayer.networking.packet.AcceptMissionC2SPacket;
import com.wassuples.demonslayer.networking.packet.SwitchStyleC2SPacket;
import com.wassuples.demonslayer.ranking.PlayerProgressionProvider;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class BreathingMenuScreen extends Screen {
    // --- GUI TEXTURES ---
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/breathing_menu_background.png");
    private static final ResourceLocation WIDGETS_TEXTURE = new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/breathing_menu_widgets.png");

    // --- WINDOW ---
    private final int windowWidth = 256;
    private final int windowHeight = 166;

    // --- PLAYER ---
    private final int playerModelXOffset = 51;
    private final int playerModelYOffset = 150;
    private final int playerModelScale = 55;

    // --- Info ---
    private final int infoPanelXOffset = 95;
    private final int infoPanelYOffset = 45;
    private final int infoPanelLineSpacing = 15;

    // --- Breath Bar ---
    private final int breathBarYOffset = 95;
    private final int breathBarHeight = 12;

    //  --- tabs ---
    private final int tabXStart = 10;
    private final int tabYStart = 5;
    private final int tabXSpacing = 22;

    // --- TEXT ---
    private final int textColor = 0xCCCCCC;
    private final int goldColor = 0xFFD700;
    private final int redColor = 0xFF5555;
    private final int grayColor = 0xAAAAAA;

    // --- SKILL TREE STUFF ---
    private final int MASTERY_MAX = 1000;
    private final int skillIconSize = 16;
    private final int skillMasteryBarWidth = 32;
    private final int skillMasteryBarHeight = 4;
    private final int subWindowXOffset = 8;
    private final int subWindowYOffset = 30;
    private final int subWindowWidth = 240;
    private final int subWindowHeight = 128;

    private final float skillTreeContentWidth = 700;
    private final float skillTreeContentHeight = 210;
    private final float skillTreeContentCenterX = 220f;
    private final float skillTreeContentCenterY = 105f;

    private double skillTreePanX = 0;
    private double skillTreePanY = 0;
    private double skillTreeZoom = 1.0;

    // --- MOVE LIST STUFF ---
    private List<BreathingForms> formsForSelectedStyle;
    private BreathingForms selectedForm = null;
    private double moveListScroll = 0.0;
    private final int moveListEntryHeight = 12;
    private int selectedFormIndex = -1;

    // --- MISSION LIST STUFF ---
    private List<Object> displayList = new ArrayList<>();
    private List<Mission> activeMissions = new ArrayList<>();
    private List<Mission> availableMissions = new ArrayList<>();
    private List<Mission> completedMissions = new ArrayList<>();
    private Mission selectedMission = null;
    private double missionListScroll = 0.0;
    private int selectedDisplayIndex = -1;
    private Button acceptMissionButton;
    private Button abandonMissionButton;

    private enum Tab { PROFILE, SKILL_TREE, MOVE_LIST, MISSIONS }
    private Tab currentTab = Tab.PROFILE;

    private BreathingStyles selectedStyleForMoveList = null;

    private TabButton profileButton;
    private TabButton stylesButton;
    private TabButton missionsButton;
    private Button switchStyleButton;

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

        this.profileButton = this.addRenderableWidget(new TabButton(leftPos + tabXStart, topPos + tabYStart, 0, (button) -> this.currentTab = Tab.PROFILE));
        this.stylesButton = this.addRenderableWidget(new TabButton(leftPos + tabXStart + tabXSpacing, topPos + tabYStart, 20, (button) -> this.currentTab = Tab.SKILL_TREE));
        this.missionsButton = this.addRenderableWidget(new TabButton(leftPos + tabXStart + (tabXSpacing * 2), topPos + tabYStart, 40, (button) -> {
            this.currentTab = Tab.MISSIONS;
            this.prepareMissionsList();
        }));
        this.switchStyleButton = this.addRenderableWidget(new Button(
                leftPos + windowWidth - 92, // Top-right corner of the window
                topPos + 34,
                80, 20,
                Component.literal("Switch Style"),
                (button) -> {
                    if (this.selectedStyleForMoveList != null) {
                        ModMessages.sendToServer(new SwitchStyleC2SPacket(this.selectedStyleForMoveList.ordinal()));
                    }
                }
        ));
        this.acceptMissionButton = this.addRenderableWidget(new Button(
                leftPos + windowWidth - 92,
                topPos + 5,
                80, 20,
                Component.literal("Accept"),
                (button) -> {
                    if(this.selectedMission != null) {
                        ModMessages.sendToServer(new AcceptMissionC2SPacket(this.selectedMission.id()));
                    }
                }
        ));

        this.abandonMissionButton = this.addRenderableWidget(new Button(
                leftPos + windowWidth - 92,
                topPos + 5,
                80, 20,
                Component.literal("Abandon"),
                (button) -> {
                    if (this.selectedMission != null) {
                        ModMessages.sendToServer(new AbandonMissionC2SPacket(this.selectedMission.id()));
                    }
                }
        ));

        this.prepareMoveList();
        this.prepareMissionsList();

        this.acceptMissionButton.visible = false;
        this.switchStyleButton.visible = false;
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
        this.missionsButton.isActive = (this.currentTab == Tab.MISSIONS);

        if(this.currentTab == Tab.MISSIONS) {
            boolean missionIsActive = this.selectedMission != null && this.activeMissions.contains(this.selectedMission);
            boolean missionIsAvailable = this.selectedMission != null && this.availableMissions.contains(this.selectedMission);

            this.acceptMissionButton.visible = missionIsAvailable;
            this.abandonMissionButton.visible = missionIsActive;
        } else {
            this.acceptMissionButton.visible = false;
            this.abandonMissionButton.visible = false;
        }

        this.switchStyleButton.visible = (this.currentTab == Tab.MOVE_LIST);
        if (this.switchStyleButton.visible && this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.getCapability(PlayerStyleProvider.PLAYER_STYLE).ifPresent(style -> {
                this.switchStyleButton.active = style.getCurrentStyles() != this.selectedStyleForMoveList;
            });
        }

        super.render(poseStack, mouseX, mouseY, partialTick);

        switch (this.currentTab) {
            case PROFILE -> renderProfileTab(poseStack, x, y, mouseX, mouseY);
            case SKILL_TREE -> renderSkillTreeTab(poseStack, x, y);
            case MOVE_LIST -> renderMoveListTab(poseStack, x, y, mouseX, mouseY);
            case MISSIONS -> renderMissionsTab(poseStack, x, y, mouseX, mouseY);
        }
        switch (this.currentTab) {
            case PROFILE -> renderBreathTooltip(poseStack, x, y, mouseX, mouseY);
            case SKILL_TREE -> renderSkillTreeTooltip(poseStack, x, y, mouseX, mouseY);
        }
    }

    private void renderMissionsTab(PoseStack poseStack, int leftPos, int topPos, int mouseX, int mouseY) {
        if(this.minecraft == null) return;
        int subWindowX = leftPos + subWindowXOffset;
        int subWindowY = topPos + subWindowYOffset;
        fill(poseStack, subWindowX, subWindowY, subWindowX + subWindowWidth, subWindowY + subWindowHeight, 0x90000000);

        int listWidth = 100;
        int detailX = subWindowX + listWidth + 5;
        int listY = subWindowY + 5;
        int listHeight = subWindowHeight - 10;

        // Left Panel
        RenderSystem.enableScissor((int)(subWindowX * minecraft.getWindow().getGuiScale()), (int)((this.height - (listY + listHeight)) * minecraft.getWindow().getGuiScale()), (int)(listWidth * minecraft.getWindow().getGuiScale()), (int)(listHeight * minecraft.getWindow().getGuiScale()));
        for (int i = 0; i < displayList.size(); i++) {
            int entryY = listY + (i * moveListEntryHeight) - (int)missionListScroll;
            if (entryY + moveListEntryHeight >= listY && entryY <= listY + listHeight) {
                Object entry = displayList.get(i);
                if (entry instanceof String header) {
                    // Draw a category header
                    this.font.draw(poseStack, Component.literal(header).withStyle(ChatFormatting.YELLOW), subWindowX + 5, entryY + 2, goldColor);
                } else if (entry instanceof Mission mission) {
                    // Draw a mission entry
                    if (i == selectedDisplayIndex) {
                        fill(poseStack, subWindowX, entryY, subWindowX + listWidth, entryY + moveListEntryHeight, 0x80FFFFFF);
                    }
                    ChatFormatting format = mission.type() == MissionType.RANK_UP ? ChatFormatting.GOLD : ChatFormatting.WHITE;
                    this.font.draw(poseStack, mission.title().copy().withStyle(format), subWindowX + 10, entryY + 2, textColor);
                }
            }
        }
        RenderSystem.disableScissor();

        // Right Panel
        if (selectedMission != null) {
            int yOffset = subWindowY + 5;
            this.font.drawWordWrap(selectedMission.title(), detailX, yOffset, subWindowWidth - listWidth - 10, goldColor);
            yOffset += this.font.wordWrapHeight(selectedMission.title().getString(), subWindowWidth - listWidth - 10) + 4;
            fill(poseStack, detailX, yOffset - 2, subWindowX + subWindowWidth - 5, yOffset - 1, grayColor);

            for(Component line : selectedMission.description()) {
                this.font.drawWordWrap(line, detailX, yOffset, subWindowWidth - listWidth - 10, textColor);
                yOffset += this.font.wordWrapHeight(line.getString(), subWindowWidth - listWidth - 10);
                yOffset += 2;
            }
            yOffset += 8;

            this.font.draw(poseStack, "Objectives:", detailX, yOffset, textColor);
            yOffset += 10;
            for (MissionObjective obj : selectedMission.objectives()) {
                this.font.draw(poseStack, "- " + obj.getDescription().getString(), detailX + 4, yOffset, textColor);
                yOffset += 10;
            }
            yOffset += 4;

            this.font.draw(poseStack, "Rewards:", detailX, yOffset, textColor);
            yOffset += 10;
            for (MissionReward reward : selectedMission.rewards()) {
                this.font.draw(poseStack, "- " + reward.getDescription().getString(), detailX + 4, yOffset, textColor);
                yOffset += 10;
            }
        }
    }

    public void prepareMissionsList() {
        if (this.minecraft == null || this.minecraft.player == null) return;

        // Get the real mission data from the client-side capability
        this.minecraft.player.getCapability(PlayerMissionsProvider.PLAYER_MISSIONS).ifPresent(playerMissions -> {
            this.activeMissions.clear();
            this.completedMissions.clear();
            this.availableMissions.clear();

            List<Mission> allMissions = MissionRegistry.getAllMissions();

            for(Mission mission : allMissions) {
                if (playerMissions.isActive(mission.id())) {
                    this.activeMissions.add(mission);
                } else if (playerMissions.hasCompleted(mission.id())) {
                    this.completedMissions.add(mission);
                } else {
                    // This check should ideally be done server-side, but for now we replicate it on the client
                    // for display purposes. The server will still have the final say.
                    this.minecraft.player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                        boolean canStart = false;
                        if (mission.type() == MissionType.RANK_UP) {
                            canStart = progression.getRank().ordinal() == mission.requirement().ordinal() - 1;
                        } else {
                            canStart = progression.getRank().ordinal() >= mission.requirement().ordinal();
                        }
                        if(canStart) {
                            this.availableMissions.add(mission);
                        }
                    });
                }
            }

            // Build the display list with categories
            this.displayList.clear();
            if (!activeMissions.isEmpty()) {
                this.displayList.add("--- ACTIVE ---");
                this.displayList.addAll(activeMissions);
            }
            if (!availableMissions.isEmpty()) {
                this.displayList.add("--- AVAILABLE ---");
                this.displayList.addAll(availableMissions);
            }
            if (!completedMissions.isEmpty()) {
                this.displayList.add("--- COMPLETED ---");
                this.displayList.addAll(completedMissions);
            }

            // Select the first actual mission in the list by default
            if (this.selectedDisplayIndex >= this.displayList.size() || this.selectedDisplayIndex == -1) {
                this.selectedDisplayIndex = -1;
                this.selectedMission = null;
                for (int i = 0; i < displayList.size(); i++) {
                    if (displayList.get(i) instanceof Mission) {
                        this.selectedDisplayIndex = i;
                        this.selectedMission = (Mission) displayList.get(i);
                        break;
                    }
                }
            }
            this.missionListScroll = 0;
        });
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
                            int iconU = (currentStyle.ordinal() / ICONS_PER_COLUMN) * 16;
                            int iconV = ICON_GRID_V_OFFSET + ((currentStyle.ordinal() % ICONS_PER_COLUMN) * 16);
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
                int iconU = (style.ordinal() / ICONS_PER_COLUMN) * 16;
                int iconV = ICON_GRID_V_OFFSET + ((style.ordinal() % ICONS_PER_COLUMN) * 16);
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

            // Check if the click was within the bounds of the list area
            if (pMouseX >= subWindowX && pMouseX < subWindowX + listWidth && pMouseY >= listY && pMouseY < listY + listHeight) {
                // Calculate which list item was clicked based on mouse Y and scroll position
                int clickedIndex = (int)((pMouseY - listY + moveListScroll) / moveListEntryHeight);

                // If the click was on a valid item, select it
                if (clickedIndex >= 0 && clickedIndex < formsForSelectedStyle.size()) {
                    this.selectedFormIndex = clickedIndex;
                    this.selectedForm = formsForSelectedStyle.get(clickedIndex);
                    return true; // We handled the click
                }
            }
        }
        if (this.currentTab == Tab.MISSIONS && this.availableMissions != null) {
            int leftPos = (this.width - this.windowWidth) / 2;
            int topPos = (this.height - this.windowHeight) / 2;
            int listX = leftPos + subWindowXOffset;
            int listY = topPos + subWindowYOffset + 5;
            int listWidth = 100;
            int listHeight = subWindowHeight - 10;

            if (pMouseX >= listX && pMouseX < listX + listWidth && pMouseY >= listY && pMouseY < listY + listHeight) {
                int clickedIndex = (int) ((pMouseY - listY + missionListScroll) / moveListEntryHeight);
                if (clickedIndex >= 0 && clickedIndex < displayList.size()) {
                    Object selectedObject = displayList.get(clickedIndex);
                    if (selectedObject instanceof Mission mission) { // Only select if it's a mission
                        this.selectedDisplayIndex = clickedIndex;
                        this.selectedMission = mission;
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    private void prepareMoveList() {
        if (selectedStyleForMoveList == null) return;

        // Filter all forms from the enum to get only the ones for the selected style
        this.formsForSelectedStyle = java.util.Arrays.stream(BreathingForms.values())
                .filter(form -> form.getStyle() == selectedStyleForMoveList)
                .collect(Collectors.toList());

        // Default to selecting the first form in the list
        if (!this.formsForSelectedStyle.isEmpty()) {
            this.selectedFormIndex = 0;
            this.selectedForm = this.formsForSelectedStyle.get(0);
        } else {
            this.selectedFormIndex = -1;
            this.selectedForm = null;
        }
        this.moveListScroll = 0; // Reset scroll on style change
    }

    private void renderMoveListTab(PoseStack poseStack, int leftPos, int topPos, int mouseX, int mouseY) {
        if (selectedStyleForMoveList == null || this.minecraft == null) return;

        int subWindowX = leftPos + 8;
        int subWindowY = topPos + 30;
        int subWindowWidth = 240;
        int subWindowHeight = 128;

        fill(poseStack, subWindowX, subWindowY, subWindowX + subWindowWidth, subWindowY + subWindowHeight, 0x90000000);

        // --- HEADER ---
        RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
        final int ICONS_PER_COLUMN = 12;
        final int ICON_GRID_V_OFFSET = 64;
        int iconU = (selectedStyleForMoveList.ordinal() / ICONS_PER_COLUMN) * 16;
        int iconV = ICON_GRID_V_OFFSET + ((selectedStyleForMoveList.ordinal() % ICONS_PER_COLUMN) * 16);
        this.blit(poseStack, subWindowX + 4, subWindowY + 4, iconU, iconV, 16, 16);
        this.font.draw(poseStack, selectedStyleForMoveList.getDisplayName(), subWindowX + 24, subWindowY + 9, textColor);

        // Define panel areas
        int listWidth = 100;
        int detailX = subWindowX + listWidth + 5;
        int listY = subWindowY + 28;
        int listHeight = subWindowHeight - 32;

        // --- LEFT PANEL: MOVE LIST ---
        // Use Scissor to make sure the list doesn't draw outside its box
        RenderSystem.enableScissor((int)(subWindowX * minecraft.getWindow().getGuiScale()), (int)((this.height - (listY + listHeight)) * minecraft.getWindow().getGuiScale()), (int)(listWidth * minecraft.getWindow().getGuiScale()), (int)(listHeight * minecraft.getWindow().getGuiScale()));

        this.minecraft.player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
            for (int i = 0; i < formsForSelectedStyle.size(); i++) {
                BreathingForms form = formsForSelectedStyle.get(i);
                int entryY = listY + (i * moveListEntryHeight) - (int)moveListScroll;

                if (entryY + moveListEntryHeight >= listY && entryY <= listY + listHeight) {
                    boolean isSelected = (i == selectedDisplayIndex);
                    boolean isUnlocked = progression.getStyleMastery(form.getStyle()) >= form.getStyleMasteryToUnlock();

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

        // --- RIGHT PANEL: DETAIL VIEW ---
        if (selectedForm != null) {
            this.minecraft.player.getCapability(PlayerProgressionProvider.PLAYER_PROGRESSION).ifPresent(progression -> {
                int yOffset = subWindowY + 28;

                // Full Name
                this.font.drawWordWrap(Component.literal(selectedForm.getDisplayName()), detailX, yOffset, subWindowWidth - listWidth - 10, goldColor);
                yOffset += this.font.wordWrapHeight(selectedForm.getDisplayName(), subWindowWidth - listWidth - 10) + 4;

                // Separator
                fill(poseStack, detailX, yOffset - 2, detailX + subWindowWidth - listWidth - 15, yOffset - 1, grayColor);

                // Description
                this.font.drawWordWrap(Component.literal(selectedForm.getDescription()), detailX, yOffset, subWindowWidth - listWidth - 10, textColor);
                yOffset += this.font.wordWrapHeight(selectedForm.getDescription(), subWindowWidth - listWidth - 10) + 8;

                // Breath Cost
                this.font.draw(poseStack, "Breath Cost: " + selectedForm.getBreathCost(), detailX, yOffset, textColor);
                yOffset += 12;

                // Mastery and Status
                this.font.draw(poseStack, "Form Mastery: " + progression.getFormMastery(selectedForm) + " / " + selectedForm.getMasteryGoal(), detailX, yOffset, textColor);
                yOffset += 12;

                boolean isUnlocked = progression.getStyleMastery(selectedForm.getStyle()) >= selectedForm.getStyleMasteryToUnlock();
                if (isUnlocked) {
                    this.font.draw(poseStack, "Status: ", detailX, yOffset, textColor);
                    this.font.draw(poseStack, Component.literal("Unlocked").withStyle(ChatFormatting.GREEN).getVisualOrderText(), detailX + font.width("Status: "), yOffset, 0xFFFFFF);
                } else {
                    this.font.draw(poseStack, "Requires: ", detailX, yOffset, textColor);
                    Component requirementText = Component.literal(selectedForm.getStyleMasteryToUnlock() + " " + selectedForm.getStyle().getDisplayName() + " Mastery")
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
                // Move selection down
                this.selectedFormIndex = Math.min(this.selectedFormIndex + 1, formsForSelectedStyle.size() - 1);
                this.selectedForm = formsForSelectedStyle.get(this.selectedFormIndex);
                this.ensureSelectionIsVisible();
                return true;
            } else if (pKeyCode == GLFW.GLFW_KEY_UP) {
                // Move selection up
                this.selectedFormIndex = Math.max(this.selectedFormIndex - 1, 0);
                this.selectedForm = formsForSelectedStyle.get(this.selectedFormIndex);
                this.ensureSelectionIsVisible();
                return true;
            }
        }
        if (this.currentTab == Tab.MISSIONS && displayList != null && !displayList.isEmpty()) {
            int direction = 0;
            if (pKeyCode == GLFW.GLFW_KEY_DOWN) {
                direction = 1;
            } else if (pKeyCode == GLFW.GLFW_KEY_UP) {
                direction = -1;
            }

            if (direction != 0) {
                int newIndex = selectedDisplayIndex;
                // Loop to find the next valid mission, skipping headers
                do {
                    newIndex += direction;
                } while (newIndex >= 0 && newIndex < displayList.size() && !(displayList.get(newIndex) instanceof Mission));

                if (newIndex >= 0 && newIndex < displayList.size()) {
                    selectedDisplayIndex = newIndex;
                    selectedMission = (Mission) displayList.get(newIndex);
                    this.ensureMissionIsVisible();
                }
                return true;
            }
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    private void ensureMissionIsVisible() {
        int listHeight = subWindowHeight - 10;
        int itemTopY = this.selectedDisplayIndex * moveListEntryHeight;
        int itemBottomY = itemTopY + moveListEntryHeight;

        if (itemTopY < this.missionListScroll) {
            this.missionListScroll = itemTopY;
        } else if (itemBottomY > this.missionListScroll + listHeight) {
            this.missionListScroll = itemBottomY - listHeight;
        }
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
            this.skillTreeZoom = Mth.clamp(this.skillTreeZoom, 0.35, 2.5); // Adjusted min zoom for wider tree
            mouseDragged(0,0,0,0,0);
            return true;
        }
        if (this.currentTab == Tab.MOVE_LIST && this.formsForSelectedStyle != null) {
            // Calculate the maximum scroll amount to prevent scrolling past the last item
            int maxScroll = Math.max(0, (formsForSelectedStyle.size() * moveListEntryHeight) - (128 - 32));
            // Update the scroll position, clamping it between 0 and the max
            this.moveListScroll = Mth.clamp(this.moveListScroll - (pDelta * 10), 0, maxScroll);
            return true;
        }
        if (this.currentTab == Tab.MISSIONS && this.availableMissions != null) {
            int maxScroll = Math.max(0, (availableMissions.size() * moveListEntryHeight) - (subWindowHeight - 10));
            this.missionListScroll = Mth.clamp(this.missionListScroll - (pDelta * 10), 0, maxScroll);
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

            // **FIXED**: Use the same texture for both hovered and active states as requested.
            if (this.isActive || this.isHoveredOrFocused()) {
                vOffset = 20; // Hover/Active texture is at v=20
            }

            blit(pPoseStack, this.x, this.y, this.u, vOffset, this.width, this.height);
        }
    }
}
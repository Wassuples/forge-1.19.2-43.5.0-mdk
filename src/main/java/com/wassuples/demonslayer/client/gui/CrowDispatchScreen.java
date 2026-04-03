package com.wassuples.demonslayer.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.missions.Mission;
import com.wassuples.demonslayer.missions.MissionObjective;
import com.wassuples.demonslayer.missions.MissionReward;
import com.wassuples.demonslayer.missions.MissionType;
import com.wassuples.demonslayer.nemesis.DemonDisplayData;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.AbandonMissionC2SPacket;
import com.wassuples.demonslayer.networking.packet.AcceptMissionC2SPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class CrowDispatchScreen extends Screen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/crow_dispatch.png");
    private static final int WIDTH = 276;
    private static final int HEIGHT = 226;

    private final List<DemonDisplayData> kizuki;
    private final List<DemonDisplayData> fodder;
    private final List<Mission> activeMissions;
    private final List<Mission> availableMissions;
    private final List<Mission> completedMissions;
    private final List<Object> missionDisplayList = new ArrayList<>();

    private Mission selectedMission;
    private int selectedMissionIndex = -1;
    private double missionListScroll = 0.0;

    private Button acceptButton;
    private Button abandonButton;
    private Tab currentTab = Tab.RANKINGS;

    private enum Tab { RANKINGS, MISSIONS }

    public CrowDispatchScreen(List<DemonDisplayData> kizuki, List<DemonDisplayData> fodder, List<Mission> active, List<Mission> available, List<Mission> completed) {
        super(Component.literal("Kasugai Dispatch"));
        this.kizuki = kizuki;
        this.fodder = fodder;
        this.activeMissions = active;
        this.availableMissions = available;
        this.completedMissions = completed;
    }

    @Override
    protected void init() {
        super.init();
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - HEIGHT) / 2;

        this.addRenderableWidget(new Button(left + 10, top + 10, 60, 20, Component.literal("Rankings"), b -> this.currentTab = Tab.RANKINGS));
        this.addRenderableWidget(new Button(left + 75, top + 10, 60, 20, Component.literal("Missions"), b -> this.currentTab = Tab.MISSIONS));

        this.acceptButton = this.addRenderableWidget(new Button(left + WIDTH - 130, top + HEIGHT - 30, 60, 20, Component.literal("Accept"), b -> {
            if (selectedMission != null) {
                ModMessages.sendToServer(new AcceptMissionC2SPacket(selectedMission.id()));
                this.onClose();
            }
        }));
        this.abandonButton = this.addRenderableWidget(new Button(left + WIDTH - 65, top + HEIGHT - 30, 60, 20, Component.literal("Abandon"), b -> {
            if (selectedMission != null) {
                ModMessages.sendToServer(new AbandonMissionC2SPacket(selectedMission.id()));
                this.onClose();
            }
        }));

        prepareMissionDisplayList();
    }

    private void prepareMissionDisplayList() {
        missionDisplayList.clear();
        if (!activeMissions.isEmpty()) {
            missionDisplayList.add("--- ACTIVE ---");
            missionDisplayList.addAll(activeMissions);
        }
        if (!availableMissions.isEmpty()) {
            missionDisplayList.add("--- AVAILABLE ---");
            missionDisplayList.addAll(availableMissions);
        }
        if (!completedMissions.isEmpty()) {
            missionDisplayList.add("--- COMPLETED ---");
            missionDisplayList.addAll(completedMissions);
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        RenderSystem.setShaderTexture(0, BACKGROUND);
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - HEIGHT) / 2;
        blit(poseStack, left, top, 0, 0, WIDTH, HEIGHT);

        drawCenteredString(poseStack, this.font, this.title, this.width / 2, top + 6, 0xFFFFFF);

        if (currentTab == Tab.RANKINGS) {
            renderRankingTab(poseStack, left + 10, top + 35);
            this.acceptButton.visible = false;
            this.abandonButton.visible = false;
        } else {
            renderMissionsTab(poseStack, left + 10, top + 35, mouseX, mouseY);
            this.acceptButton.visible = selectedMission != null && availableMissions.contains(selectedMission);
            this.abandonButton.visible = selectedMission != null && activeMissions.contains(selectedMission);
        }

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private void renderRankingTab(PoseStack poseStack, int x, int y) {
        int startY = y;
        int startX = x + 5;
        int columnWidth = (WIDTH - 30) / 2;

        // Kizuki Column
        this.font.draw(poseStack, Component.literal("The Twelve Kizuki").withStyle(ChatFormatting.GOLD), startX, startY, 0xFFFFFF);
        startY += 12;
        for (DemonDisplayData data : this.kizuki) {
            String line = String.format("%-14s | %s %s", data.rank(), data.name(), data.title());
            this.font.draw(poseStack, line, startX, startY, 0xFFFFFF);
            startY += 10;
        }

        // Fodder Column
        startY = y;
        startX = x + columnWidth + 15;
        this.font.draw(poseStack, Component.literal("Top Fodder").withStyle(ChatFormatting.GRAY), startX, startY, 0xCCCCCC);
        startY += 12;
        for (DemonDisplayData data : this.fodder) {
            String line = String.format("%s %s (BP: %d)", data.name(), data.title(), data.bloodPoints());
            this.font.draw(poseStack, line, startX, startY, 0xDDDDDD);
            startY += 10;
        }
    }

    private void renderMissionsTab(PoseStack poseStack, int x, int y, int mouseX, int mouseY) {
        int listWidth = 100;
        int listHeight = HEIGHT - 80;
        int detailX = x + listWidth + 5;

        // Mission List
        fill(poseStack, x, y, x + listWidth, y + listHeight, 0x50000000);
        if (this.minecraft != null) {
            RenderSystem.enableScissor((int)(x * minecraft.getWindow().getGuiScale()), (int)((this.height - (y + listHeight)) * minecraft.getWindow().getGuiScale()), (int)(listWidth * minecraft.getWindow().getGuiScale()), (int)(listHeight * minecraft.getWindow().getGuiScale()));
        }
        for (int i = 0; i < missionDisplayList.size(); i++) {
            int entryY = y + 5 + (i * 12) - (int)missionListScroll;
            if (entryY + 12 < y || entryY > y + listHeight) continue;

            Object entry = missionDisplayList.get(i);
            if (entry instanceof String header) {
                this.font.draw(poseStack, Component.literal(header).withStyle(ChatFormatting.YELLOW), x + 5, entryY, 0xFFD700);
            } else if (entry instanceof Mission mission) {
                if (i == selectedMissionIndex) {
                    fill(poseStack, x, entryY - 2, x + listWidth, entryY + 10, 0x80FFFFFF);
                }
                ChatFormatting format = mission.type() == MissionType.RANK_UP ? ChatFormatting.GOLD : ChatFormatting.WHITE;
                this.font.draw(poseStack, mission.title().copy().withStyle(format), x + 10, entryY, 0xFFFFFF);
            }
        }
        if (this.minecraft != null) {
            RenderSystem.disableScissor();
        }

        // Mission Details
        if (selectedMission != null) {
            int yOffset = y;
            this.font.drawWordWrap(selectedMission.title(), detailX, yOffset, WIDTH - listWidth - 30, 0xFFD700);
            yOffset += this.font.wordWrapHeight(selectedMission.title().getString(), WIDTH - listWidth - 30) + 4;
            fill(poseStack, detailX, yOffset - 2, x + WIDTH - 20, yOffset - 1, 0xAAAAAA);

            for(Component line : selectedMission.description()) {
                this.font.drawWordWrap(line, detailX, yOffset, WIDTH - listWidth - 30, 0xFFFFFF);
                yOffset += this.font.wordWrapHeight(line.getString(), WIDTH - listWidth - 30);
            }
            yOffset += 8;

            this.font.draw(poseStack, "Objectives:", detailX, yOffset, 0xFFFFFF);
            yOffset += 10;
            for (MissionObjective obj : selectedMission.objectives()) {
                this.font.draw(poseStack, "- " + obj.getDescription().getString(), detailX + 4, yOffset, 0xFFFFFF);
                yOffset += 10;
            }
            yOffset += 4;

            this.font.draw(poseStack, "Rewards:", detailX, yOffset, 0xFFFFFF);
            yOffset += 10;
            for (MissionReward reward : selectedMission.rewards()) {
                this.font.draw(poseStack, "- " + reward.getDescription().getString(), detailX + 4, yOffset, 0xFFFFFF);
                yOffset += 10;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.currentTab == Tab.MISSIONS) {
            int left = (this.width - WIDTH) / 2;
            int top = (this.height - HEIGHT) / 2;
            int listX = left + 10;
            int listY = top + 35;
            int listWidth = 100;
            int listHeight = HEIGHT - 80;

            if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight) {
                int index = (int) ((mouseY - listY - 5 + missionListScroll) / 12);
                if (index >= 0 && index < missionDisplayList.size()) {
                    Object entry = missionDisplayList.get(index);
                    if (entry instanceof Mission mission) {
                        this.selectedMissionIndex = index;
                        this.selectedMission = mission;
                    } else {
                        this.selectedMissionIndex = -1;
                        this.selectedMission = null;
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.currentTab == Tab.MISSIONS) {
            int maxScroll = Math.max(0, missionDisplayList.size() * 12 - (HEIGHT - 80));
            this.missionListScroll = Mth.clamp(this.missionListScroll - delta * 10, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}


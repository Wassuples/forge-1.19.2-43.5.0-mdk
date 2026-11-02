package com.wassuples.demonslayer.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.missions.Arc;
import com.wassuples.demonslayer.missions.MissionView;
import com.wassuples.demonslayer.nemesis.DemonDisplayData;
import com.wassuples.demonslayer.networking.ModMessages;
import com.wassuples.demonslayer.networking.packet.AbandonMissionC2SPacket;
import com.wassuples.demonslayer.networking.packet.AcceptMissionC2SPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * What the Kasugai crow carries: the demon rankings and the mission list. The window grows with the screen; the
 * mission list and the mission details each scroll on their own (mouse wheel over either, or drag their bars), and the
 * details pane shows the full summary, notes and every objective.
 */
public class CrowDispatchScreen extends Screen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(DemonSlayer.MOD_ID, "textures/gui/crow_dispatch.png");
    private static final int ROW = 12, BAR = 4;

    private int panelW = 276, panelH = 226, left, top;
    private int listX, listY, listW, listH, detailX, detailY, detailW, detailH;

    private final List<DemonDisplayData> kizuki;
    private final List<DemonDisplayData> fodder;
    private final List<MissionView> missions;
    /** Display rows: an {@link Arc} header or a {@link MissionView}. */
    private final List<Object> missionDisplayList = new ArrayList<>();

    private MissionView selectedMission;
    private int selectedMissionIndex = -1;
    private double missionListScroll = 0.0, detailScroll = 0.0;
    private int detailContentH;
    /** 1: dragging the list bar, 2: dragging the details bar. */
    private int dragging;

    private Button acceptButton;
    private Button abandonButton;
    private Tab currentTab = Tab.RANKINGS;

    private enum Tab { RANKINGS, MISSIONS }

    public CrowDispatchScreen(List<DemonDisplayData> kizuki, List<DemonDisplayData> fodder, List<MissionView> missions) {
        super(Component.literal("Kasugai Dispatch"));
        this.kizuki = kizuki;
        this.fodder = fodder;
        this.missions = missions;
    }

    @Override
    protected void init() {
        super.init();
        panelW = Mth.clamp(this.width - 24, 276, 440);
        panelH = Mth.clamp(this.height - 24, 200, 320);
        panelW = Math.min(panelW, this.width - 8);
        panelH = Math.min(panelH, this.height - 8);
        left = (this.width - panelW) / 2;
        top = (this.height - panelH) / 2;
        listX = left + 10;
        listY = top + 35;
        listW = Mth.clamp(panelW / 3, 112, 150);
        listH = panelH - 35 - 36;
        detailX = listX + listW + 8;
        detailY = listY;
        detailW = left + panelW - 10 - detailX;
        detailH = listH;

        this.addRenderableWidget(new KnyUi.KnyButton(left + 10, top + 10, 60, 20, Component.literal("Rankings"), b -> this.currentTab = Tab.RANKINGS));
        this.addRenderableWidget(new KnyUi.KnyButton(left + 75, top + 10, 60, 20, Component.literal("Missions"), b -> this.currentTab = Tab.MISSIONS));

        this.acceptButton = this.addRenderableWidget(new KnyUi.KnyButton(left + panelW - 130, top + panelH - 28, 60, 20, Component.translatable("gui.demonslayer.mission.begin"), b -> {
            if (selectedMission != null) {
                ModMessages.sendToServer(new AcceptMissionC2SPacket(selectedMission.id()));
                this.onClose();
            }
        }));
        this.abandonButton = this.addRenderableWidget(new KnyUi.KnyButton(left + panelW - 65, top + panelH - 28, 60, 20, Component.translatable("gui.demonslayer.mission.forfeit"), b -> {
            if (selectedMission != null) {
                ModMessages.sendToServer(new AbandonMissionC2SPacket(selectedMission.id()));
                this.onClose();
            }
        }));

        int keep = selectedMissionIndex;
        prepareMissionDisplayList();
        if (keep >= 0 && keep < missionDisplayList.size() && missionDisplayList.get(keep) instanceof MissionView) select(keep);
        missionListScroll = Mth.clamp(missionListScroll, 0, maxListScroll());
    }

    private void prepareMissionDisplayList() {
        missionDisplayList.clear();
        Arc arc = null;
        for (MissionView m : missions) {
            if (m.arc() != arc) {
                arc = m.arc();
                missionDisplayList.add(arc);
            }
            missionDisplayList.add(m);
        }
        // start on the active mission, else the first available one
        for (int i = 0; i < missionDisplayList.size(); i++) {
            if (missionDisplayList.get(i) instanceof MissionView m && m.status() == MissionView.Status.ACTIVE) {
                select(i);
                return;
            }
        }
        for (int i = 0; i < missionDisplayList.size(); i++) {
            if (missionDisplayList.get(i) instanceof MissionView m && m.status() == MissionView.Status.AVAILABLE) {
                select(i);
                return;
            }
        }
    }

    private void select(int index) {
        if (index != selectedMissionIndex) detailScroll = 0;
        this.selectedMissionIndex = index;
        this.selectedMission = (MissionView) missionDisplayList.get(index);
    }

    private static int statusColour(MissionView.Status status) {
        return switch (status) {
            case ACTIVE -> KnyUi.GOLD_LIGHT;
            case AVAILABLE -> KnyUi.TEXT;
            case COMPLETED -> KnyUi.GOOD;
            case LOCKED -> 0xFF6E6464;
        };
    }

    private int maxListScroll() {
        return Math.max(0, missionDisplayList.size() * ROW + 6 - listH);
    }

    private int maxDetailScroll() {
        return Math.max(0, detailContentH + 6 - detailH);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        KnyUi.panel(poseStack, left, top, panelW, panelH);

        drawCenteredString(poseStack, this.font, this.title, this.width / 2, top + 6, 0xFFFFFF);

        if (currentTab == Tab.RANKINGS) {
            renderRankingTab(poseStack, left + 10, top + 35);
            this.acceptButton.visible = false;
            this.abandonButton.visible = false;
        } else {
            renderMissionsTab(poseStack, mouseX, mouseY);
            this.acceptButton.visible = selectedMission != null && selectedMission.status() == MissionView.Status.AVAILABLE;
            this.abandonButton.visible = selectedMission != null && selectedMission.status() == MissionView.Status.ACTIVE;
        }

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private void renderRankingTab(PoseStack poseStack, int x, int y) {
        int startY = y;
        int startX = x + 5;
        int columnWidth = (panelW - 30) / 2;

        // Kizuki Column
        this.font.draw(poseStack, Component.literal("The Twelve Kizuki").withStyle(ChatFormatting.GOLD), startX, startY, 0xFFFFFF);
        startY += 12;
        for (DemonDisplayData data : this.kizuki) {
            String line = String.format("%-14s | %s %s", data.rank(), data.name(), data.title());
            this.font.draw(poseStack, this.font.plainSubstrByWidth(line, columnWidth - 4), startX, startY, 0xFFFFFF);
            startY += 10;
        }

        // Fodder Column
        startY = y;
        startX = x + columnWidth + 15;
        this.font.draw(poseStack, Component.literal("Top Fodder").withStyle(ChatFormatting.GRAY), startX, startY, 0xCCCCCC);
        startY += 12;
        for (DemonDisplayData data : this.fodder) {
            String line = String.format("%s %s (BP: %d)", data.name(), data.title(), data.bloodPoints());
            this.font.draw(poseStack, this.font.plainSubstrByWidth(line, columnWidth - 4), startX, startY, 0xDDDDDD);
            startY += 10;
        }
    }

    private void scissor(int x, int y, int w, int h) {
        if (this.minecraft == null) return;
        double scale = minecraft.getWindow().getGuiScale();
        RenderSystem.enableScissor((int) (x * scale), (int) ((this.height - (y + h)) * scale), Math.max(0, (int) (w * scale)), Math.max(0, (int) (h * scale)));
    }

    /** A thin scroll bar down the right edge of a pane (nothing when everything fits). */
    private void scrollBar(PoseStack pose, int x, int y, int h, double scroll, int max, int content) {
        if (max <= 0) return;
        fill(pose, x, y, x + BAR, y + h, 0x60000000);
        int thumb = Math.max(14, (int) (h * (h / (double) Math.max(h, content))));
        int ty = y + (int) ((h - thumb) * (scroll / max));
        fill(pose, x, ty, x + BAR, ty + thumb, KnyUi.GOLD_DARK);
        fill(pose, x, ty, x + BAR - 1, ty + thumb - 1, KnyUi.GOLD);
    }

    private void renderMissionsTab(PoseStack poseStack, int mouseX, int mouseY) {
        // ---- mission list
        KnyUi.inset(poseStack, listX, listY, listW, listH);
        int rowW = listW - BAR - 3;
        scissor(listX, listY + 1, listW, listH - 2);
        for (int i = 0; i < missionDisplayList.size(); i++) {
            int entryY = listY + 4 + (i * ROW) - (int) missionListScroll;
            if (entryY + ROW < listY || entryY > listY + listH) continue;
            Object entry = missionDisplayList.get(i);
            if (entry instanceof Arc arc) {
                this.font.draw(poseStack, this.font.plainSubstrByWidth("Arc " + arc.name(), rowW - 6), listX + 4, entryY, KnyUi.GOLD);
            } else if (entry instanceof MissionView m) {
                boolean hover = mouseX >= listX && mouseX < listX + rowW && mouseY >= Math.max(listY, entryY - 2)
                        && mouseY < Math.min(listY + listH, entryY + 10);
                if (i == selectedMissionIndex || hover) {
                    KnyUi.card(poseStack, listX + 1, entryY - 2, rowW - 1, ROW, i == selectedMissionIndex, hover);
                }
                String label = m.number() + "  " + m.title().getString();
                this.font.draw(poseStack, this.font.plainSubstrByWidth(label, rowW - 12), listX + 8, entryY, statusColour(m.status()));
            }
        }
        RenderSystem.disableScissor();
        missionListScroll = Mth.clamp(missionListScroll, 0, maxListScroll());
        scrollBar(poseStack, listX + listW - BAR - 1, listY + 1, listH - 2, missionListScroll, maxListScroll(), missionDisplayList.size() * ROW + 6);

        // ---- details
        KnyUi.inset(poseStack, detailX, detailY, detailW, detailH);
        if (selectedMission == null) return;
        MissionView m = selectedMission;
        int textW = detailW - 10 - BAR;
        int x = detailX + 5;
        scissor(detailX, detailY + 1, detailW, detailH - 2);
        int y0 = detailY + 5 - (int) detailScroll;
        int yy = y0;
        for (FormattedCharSequence line : this.font.split(m.title(), textW)) {
            this.font.drawShadow(poseStack, line, x, yy, KnyUi.GOLD_LIGHT);
            yy += 11;
        }
        this.font.draw(poseStack, m.arc().label(), x, yy, KnyUi.DIM);
        yy += 10;
        for (FormattedCharSequence line : this.font.split(m.location(), textW)) {
            this.font.draw(poseStack, line, x, yy, KnyUi.DIM);
            yy += 10;
        }
        yy += 3;
        fill(poseStack, x, yy - 2, x + textW, yy - 1, 0x60C9A04A);
        yy += 2;
        for (FormattedCharSequence line : this.font.split(m.summary(), textW)) {
            this.font.draw(poseStack, line, x, yy, KnyUi.TEXT);
            yy += 10;
        }
        yy += 4;
        for (FormattedCharSequence line : this.font.split(m.note(), textW)) {
            this.font.draw(poseStack, line, x, yy, statusColour(m.status()));
            yy += 10;
        }
        if (!m.briefing().isEmpty()) {
            yy += 5;
            this.font.draw(poseStack, Component.translatable("gui.demonslayer.mission.objectives"), x, yy, KnyUi.GOLD);
            yy += 11;
            for (Component obj : m.briefing()) {
                List<FormattedCharSequence> lines = this.font.split(obj, textW - 10);
                KnyUi.diamond(poseStack, x + 3, yy + 3);
                for (FormattedCharSequence line : lines) {
                    this.font.draw(poseStack, line, x + 10, yy, KnyUi.TEXT);
                    yy += 10;
                }
                yy += 2;
            }
        }
        RenderSystem.disableScissor();
        detailContentH = yy - y0;
        detailScroll = Mth.clamp(detailScroll, 0, maxDetailScroll());
        scrollBar(poseStack, detailX + detailW - BAR - 1, detailY + 1, detailH - 2, detailScroll, maxDetailScroll(), detailContentH + 6);
    }

    private boolean inList(double mx, double my) {
        return mx >= listX && mx < listX + listW && my >= listY && my < listY + listH;
    }

    private boolean inDetail(double mx, double my) {
        return mx >= detailX && mx < detailX + detailW && my >= detailY && my < detailY + detailH;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.currentTab == Tab.MISSIONS && button == 0) {
            if (inList(mouseX, mouseY)) {
                if (mouseX >= listX + listW - BAR - 3 && maxListScroll() > 0) {
                    dragging = 1;
                    dragTo(mouseY);
                    return true;
                }
                int index = (int) ((mouseY - listY - 2 + missionListScroll) / ROW);
                if (index >= 0 && index < missionDisplayList.size() && missionDisplayList.get(index) instanceof MissionView) {
                    select(index);
                }
                return true;
            }
            if (inDetail(mouseX, mouseY) && mouseX >= detailX + detailW - BAR - 3 && maxDetailScroll() > 0) {
                dragging = 2;
                dragTo(mouseY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void dragTo(double mouseY) {
        if (dragging == 1) {
            missionListScroll = Mth.clamp((mouseY - listY) / listH, 0, 1) * maxListScroll();
        } else if (dragging == 2) {
            detailScroll = Mth.clamp((mouseY - detailY) / detailH, 0, 1) * maxDetailScroll();
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging != 0) {
            dragTo(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = 0;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.currentTab == Tab.MISSIONS) {
            if (inDetail(mouseX, mouseY)) {
                this.detailScroll = Mth.clamp(this.detailScroll - delta * 12, 0, maxDetailScroll());
            } else {
                this.missionListScroll = Mth.clamp(this.missionListScroll - delta * 12, 0, maxListScroll());
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (this.currentTab == Tab.MISSIONS) {
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN) {
                detailScroll = Mth.clamp(detailScroll + detailH - 20, 0, maxDetailScroll());
                return true;
            }
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP) {
                detailScroll = Mth.clamp(detailScroll - detailH + 20, 0, maxDetailScroll());
                return true;
            }
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

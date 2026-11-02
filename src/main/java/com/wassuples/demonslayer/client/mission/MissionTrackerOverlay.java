package com.wassuples.demonslayer.client.mission;

import com.wassuples.demonslayer.client.gui.KnyUi;
import com.wassuples.demonslayer.missions.MissionTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.List;

/**
 * Compact objective tracker in the top-right corner while a story mission is active: arc, title, objectives with
 * check marks, and one live status line (time to dawn, direction to the destination, warnings).
 */
public final class MissionTrackerOverlay {
    private static final int W = 168;
    /** Bottom edge of the tracker panel last frame (0 when hidden); notices stack below it. */
    static int lastBottom;

    private MissionTrackerOverlay() {
    }

    public static final IGuiOverlay HUD = (gui, pose, partialTick, width, height) -> {
        MissionTracker t = ClientMissionHandler.tracker;
        Minecraft mc = Minecraft.getInstance();
        if (t == null || mc.player == null || mc.options.hideGui || mc.options.renderDebug) {
            lastBottom = 0;
            return;
        }
        Font font = mc.font;

        // lay out first so the panel fits its contents
        List<Row> rows = new ArrayList<>();
        for (MissionTracker.Line line : t.lines()) {
            List<FormattedCharSequence> wrapped = font.split(line.text(), W - 22);
            for (int i = 0; i < wrapped.size(); i++) rows.add(new Row(wrapped.get(i), line, i == 0));
        }
        List<FormattedCharSequence> footer = t.footer().getString().isEmpty() ? List.of() : font.split(t.footer(), W - 12);
        int h = 30 + rows.size() * 10 + (footer.isEmpty() ? 0 : 6 + footer.size() * 10) + 4;
        int x = width - W - 8, y = 8;

        lastBottom = y + h + 3;
        KnyUi.panel(pose, x, y, W, h);
        font.draw(pose, t.arc(), x + 6, y + 5, KnyUi.GOLD);
        font.drawShadow(pose, t.title(), x + 6, y + 16, KnyUi.TEXT);
        GuiComponent.fill(pose, x + 6, y + 27, x + W - 6, y + 28, KnyUi.accent(0xB0));

        int ry = y + 32;
        for (Row r : rows) {
            int colour = r.line.done() ? KnyUi.GOOD : (r.line.optional() ? KnyUi.DIM : KnyUi.TEXT);
            if (r.first) {
                int bx = x + 7, by = ry;
                if (r.line.optional() && !r.line.done()) {
                    GuiComponent.fill(pose, bx + 2, by + 3, bx + 5, by + 6, KnyUi.DIM);
                } else {
                    GuiComponent.fill(pose, bx, by + 1, bx + 7, by + 8, KnyUi.GOLD_DARK);
                    GuiComponent.fill(pose, bx + 1, by + 2, bx + 6, by + 7, 0xFF120A0C);
                    if (r.line.done()) GuiComponent.fill(pose, bx + 2, by + 3, bx + 5, by + 6, KnyUi.GOOD);
                }
            }
            font.draw(pose, r.text, x + 18, ry, colour);
            ry += 10;
        }
        if (!footer.isEmpty()) {
            ry += 3;
            GuiComponent.fill(pose, x + 6, ry - 2, x + W - 6, ry - 1, 0x40C9A04A);
            ry += 2;
            for (FormattedCharSequence f : footer) {
                font.draw(pose, f, x + 6, ry, t.urgent() ? KnyUi.BAD : KnyUi.GOLD_LIGHT);
                ry += 10;
            }
        }
    };

    private record Row(FormattedCharSequence text, MissionTracker.Line line, boolean first) {
    }
}

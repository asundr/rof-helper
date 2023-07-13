package com.roftracker;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

public class WarningOverlay extends OverlayPanel
{
    private final ROFTrackerPlugin plugin;

    @Inject
    public WarningOverlay(ROFTrackerPlugin plugin)
    {
        super(plugin);
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
        this.plugin = plugin;
        addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "ROF Overlay");
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        final String warningString = "WARNING: NOT WEARING RING OF FORGING";

        panelComponent.getChildren().add(TitleComponent.builder()
                .text(warningString)
                .color(Color.RED)
                .build());

        panelComponent.setPreferredSize(new Dimension(
                graphics.getFontMetrics().stringWidth(warningString) + 500,
                300));

        return super.render(graphics);
    }
}

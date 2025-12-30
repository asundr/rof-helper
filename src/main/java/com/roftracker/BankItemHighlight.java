package com.roftracker;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import lombok.Getter;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

public class BankItemHighlight extends WidgetItemOverlay
{
    private Rectangle bounds = null;

    private final ROFTrackerPlugin plugin;

    private final ROFTrackerConfig config;

    @Getter
    private final Cache<Integer, BufferedImage> heatmapImages = CacheBuilder.newBuilder()
            .maximumSize(160)
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build();

    @Inject
    BankItemHighlight(ROFTrackerPlugin plugin, ROFTrackerConfig config)
    {
        this.plugin = plugin;
        this.config = config;
        showOnBank();
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem itemWidget)
    {
        if (itemWidget.getWidget().getParentId() != InterfaceID.Bankmain.ITEMS)
        {
            return;
        }
        if (itemWidget.getId() == ItemID.IRON_ORE && plugin.isPlayerWearingROF() ||
            itemWidget.getId() == ItemID.RING_OF_FORGING && !plugin.isPlayerWearingROF())
        {
            if (bounds == null)
            {
                bounds = itemWidget.getCanvasBounds();
                bounds.y -= 3;
                bounds.height += 3;
                bounds.x -= 3;
                bounds.width += 3;
            }

            graphics.setColor(config.colorBankOutline());
            graphics.drawRect(bounds.x,  bounds.y, bounds.width, bounds.height);
            Color fill = config.colorBankOutline();
            graphics.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 20));
            graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

            //final BufferedImage image = itemManager.getImage(itemWidget.getId(), 1, false);
            //graphics.drawImage(image, bounds.x, bounds.y, null);
        }

    }

}

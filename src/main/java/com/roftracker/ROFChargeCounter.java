
package com.roftracker;

import lombok.Getter;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.Counter;
import net.runelite.client.util.QuantityFormatter;

import java.awt.image.BufferedImage;

class ROFChargeCounter extends Counter
{
	@Getter
	private final int itemID;

	ROFChargeCounter(Plugin plugin, int itemID, int count, BufferedImage image)
	{
		super(image, plugin, count);
		this.itemID = itemID;
	}

	@Override
	public String getText()
	{
		return QuantityFormatter.quantityToRSDecimalStack(getCount());
	}

	@Override
	public String getTooltip()
	{
		return "ROF Charges";
	}
}

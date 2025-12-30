package com.roftracker;

import com.google.inject.Provides;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;

@PluginDescriptor(
		name = "Ring of Forging Helper",
		description = "Shows remaining ROF charges and warns the player when ROF not equipped",
		tags = {"ring","forging","smithing", "smelting", "iron", "equipment"}
)
public class ROFTrackerPlugin extends Plugin
{
	private static final String ROF_CHARGE_KEY = "ringOfForging";
	private final static String GROUP_NAME_CHARGE = "itemCharge";
	private final static String MESSAGE_RETRIEVE_BAR = "You retrieve a bar of iron.";
	private final static String MESSAGE_RING_MELTED = "Your Ring of Forging has melted.";
	private final static String MESSAGE_FAIL_TO_REFINE = "The ore is too impure and you fail to refine it.";
	private final static String NOTIFY_WITHOUT_RING = "WARNING: Smelting without Ring of Forging.";
	private final static String NOTIFY_RING_MELTED = "Your Ring of Forging has melted!";

	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ROFTrackerConfig config;
	@Inject
	private InfoBoxManager infoBoxManager;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private ItemManager itemManager;
	@Inject
	private ConfigManager configManager;
	@Inject
	private Notifier notifier;

	private ROFChargeCounter counterBox = null;
	private BankItemHighlight bankItemOverlayROF = null;
	private WarningOverlay warningOverlay = null;
	private boolean playerWearingROF = false;

	@Provides
	ROFTrackerConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ROFTrackerConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		clientThread.invokeLater(() ->
		{
			final ItemContainer container = client.getItemContainer(InventoryID.WORN);
			if (container != null)
			{
				checkInventory(container.getItems());
			}
			if (!isPlayerWearingROF())
			{
				updateMissingROF();
			}
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		infoBoxManager.removeInfoBox(counterBox);
		overlayManager.remove(warningOverlay);
		overlayManager.remove(bankItemOverlayROF);
		counterBox = null;
		warningOverlay = null;
		bankItemOverlayROF = null;
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getItemContainer() != client.getItemContainer(InventoryID.WORN))
		{
			return;
		}
		checkInventory(event.getItemContainer().getItems());
		redrawBankOverlay();
	}

	@Subscribe(priority = -1e9f)
	public void onChatMessage(ChatMessage event)
	{
		if (event.getMessage().equals(MESSAGE_RETRIEVE_BAR))
		{
			// Smelting iron without wearing ROF
			if (counterBox == null)
			{
				if (config.cbNotifyOnSmeltWithoutRing())
				{
					notifier.notify(NOTIFY_WITHOUT_RING, TrayIcon.MessageType.ERROR);
				}
			}
			// Player is wearing ROF, update the charge count
			else
			{
				updateInfoBox();
				final int chargeCount = getRingCharge();
				if (counterBox != null)
				{
					counterBox.setCount(chargeCount);
				}
			}
		}
		else if (event.getMessage().equals(MESSAGE_RING_MELTED))
		{
			if (config.cbMeltNotify())
			{
				notifier.notify(NOTIFY_RING_MELTED, TrayIcon.MessageType.ERROR);
			}
			updateMissingROF();
		}
		// Smelting iron without wearing ROF
		else if (event.getMessage().equals(MESSAGE_FAIL_TO_REFINE))
		{
			if (config.cbNotifyOnSmeltWithoutRing())
			{
				notifier.notify(NOTIFY_WITHOUT_RING, TrayIcon.MessageType.ERROR);
			}
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded widgetLoaded)
	{
		if (isBankVisible())
		{
			removeBankOverlay();
			addBankIconOverlay();
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed widgetClosed)
	{
		if (!isBankVisible())
		{
			removeBankOverlay();
		}
	}
	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (isBankVisible())
		{
			redrawBankOverlay();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (config.cbWarningBox() && !playerWearingROF)
		{
			addWarningOverlay();
		}
		else if (!config.cbWarningBox() || playerWearingROF)
		{
			removeWarningOverlay();
		}
	}

	private void checkInventory(final Item[] items)
	{
		if (items.length >= EquipmentInventorySlot.RING.getSlotIdx())
		{
			final Item ring = items[EquipmentInventorySlot.RING.getSlotIdx()];

			if (ring != null && ring.getId() == ItemID.RING_OF_FORGING)
			{
				final ItemComposition ringComp = itemManager.getItemComposition(ring.getId());
				updateInfobox(ring, ringComp);
				removeWarningOverlay();
				playerWearingROF = true;
			}
			else
			{
				updateMissingROF();
				playerWearingROF = false;
			}
		}
		else {
			updateMissingROF();
			playerWearingROF = false;
		}
	}

	private void updateInfobox(final Item item, final ItemComposition comp)
	{
		final int chargeCount = getRingCharge();
		if (counterBox != null && counterBox.getItemID() == item.getId())
		{
			counterBox.setCount(chargeCount);
			return;
		}
		updateInfoBox();
	}

	private void updateInfoBox()
	{
		if (counterBox == null)
		{
			createInfobox();
		}
		redrawBankOverlay();
	}

	private void removeInfobox()
	{
		infoBoxManager.removeInfoBox(counterBox);
		counterBox = null;
	}

	private void createInfobox()
	{
		removeInfobox();
		final int chargeCount = getRingCharge();
		final BufferedImage image = itemManager.getImage(ItemID.RING_OF_FORGING, 1, false);
		counterBox = new ROFChargeCounter(this, ItemID.RING_OF_FORGING, chargeCount, image);
		infoBoxManager.addInfoBox(counterBox);
	}

	private void addBankIconOverlay()
	{
		redrawBankOverlay();
	}

	private void removeBankOverlay()
	{
		overlayManager.remove(bankItemOverlayROF);
	}

	private void redrawBankOverlay()
	{
		overlayManager.remove(bankItemOverlayROF);
		if (config.cbBankOutline())
		{
			bankItemOverlayROF = new BankItemHighlight(this, config);
			overlayManager.add(bankItemOverlayROF);
		}
	}

	private void addWarningOverlay()
	{
		overlayManager.remove(warningOverlay);
		warningOverlay = new WarningOverlay(this);
		warningOverlay.setPreferredColor(Color.RED);
		warningOverlay.setBounds(new Rectangle(100,100));
		overlayManager.add(warningOverlay);
	}

	private void removeWarningOverlay()
	{
		overlayManager.remove(warningOverlay);
		warningOverlay = null;
	}

	private void updateMissingROF()
	{
		final int chargeCount = getRingCharge();
		removeInfobox();
		if (config.cbWarningBox() && warningOverlay == null)
		{
			addWarningOverlay();
		}
	}

	private int getRingCharge()
	{
		return getItemCharges(ROF_CHARGE_KEY);
	}

	// From Item Charges plugin
	private int getItemCharges(String key)
	{
		Integer i = configManager.getRSProfileConfiguration(GROUP_NAME_CHARGE, key, Integer.class);
		return i == null ? -1 : i;
	}

	private boolean isBankVisible()
	{
		final Widget bank = client.getWidget(InterfaceID.Bankmain.ITEMS);
		return bank != null && !bank.isHidden();
	}

	public boolean isPlayerWearingROF()
	{
		return playerWearingROF;
	}

}

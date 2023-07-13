package com.roftracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import java.awt.*;

@ConfigGroup("roftracker")
public interface ROFTrackerConfig extends Config
{

    @ConfigItem(keyName = "meltNotify", name = "ROF Melt Notification" , description = "Notifies the player when the ring of forging melts", position = 1)
    default boolean cbMeltNotify()
    {
        return false;
    }

    @ConfigItem(keyName = "smeltNotify", name = "Smelting Notification", description = "Notifies the player every time they smelt iron without wearing the Ring of Forging", position = 2)
    default boolean cbNotifyOnSmeltWithoutRing() { return true; }

    @ConfigItem(keyName = "warningBox", name = "Display warning box", description = "Displays a big red warning box when not wearing the Ring of Forging", position = 3)
    default boolean cbWarningBox()
    {
        return true;
    }

    @ConfigItem(keyName = "bankIcons", name = "Bank outlines", description = "Displays an outline around the the bank item to use", position = 4)
    default boolean cbBankOutline()
    {
        return true;
    }

    @ConfigItem(keyName = "bankIconColor", name = "Bank outline color", description = "Sets the color of the bank outlines", position = 5)
    default Color colorBankOutline() { return Color.YELLOW; }
}

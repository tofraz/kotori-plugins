package com.theplug.kotori.oaioswapper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup(oAIOSwapperConfig.GROUP)
public interface oAIOSwapperConfig extends Config
{
	String GROUP = "oaioswapper";

	@ConfigItem(
			keyName = "actionsPerTick",
			name = "Actions Per Tick",
			description = "Number of actions to perform per game tick",
			position = 1
	)
	default int actionsPerTick() {
		return 5;
	}

	@ConfigItem(
			keyName = "hotkey",
			name = "Switch Gear Hotkey",
			description = "Hotkey to execute the selected gear switch profile",
			position = 0
	)
	default Keybind hotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
			keyName = "switchDelay",
			name = "Switch Delay (ms)",
			description = "Delay between switching items (in milliseconds)",
			position = 1
	)
	default int switchDelay()
	{
		return 25;
	}

	@ConfigItem(
			keyName = "profiles",
			name = "Gear Profiles",
			description = "Stored gear profiles",
			hidden = true
	)
	default String profiles()
	{
		return "";
	}

	@ConfigItem(
			keyName = "selectedProfile",
			name = "Selected Profile",
			description = "Currently selected gear profile",
			hidden = true
	)
	default String selectedProfile()
	{
		return "";
	}
}
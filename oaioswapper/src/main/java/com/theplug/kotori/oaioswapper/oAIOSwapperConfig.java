package com.theplug.kotori.oaioswapper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

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
			keyName = "switchDelay",
			name = "Switch Delay (ms)",
			description = "Delay between switching items (in milliseconds)",
			position = 1
	)
	default int switchDelay()
	{
		return 25;
	}
}
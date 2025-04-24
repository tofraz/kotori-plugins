package com.theplug.kotori.oaioswapper;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.input.KeyManager;
import net.runelite.client.util.HotkeyListener;
import com.theplug.kotori.kotoriutils.ReflectionLibrary;
import com.theplug.kotori.kotoriutils.methods.MiscUtilities;
import com.theplug.kotori.kotoriutils.KotoriUtils;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Collectors;

@PluginDependency(KotoriUtils.class)
@Singleton
@Slf4j
@PluginDescriptor(
		name = "<html><font color=#6b8af6>Kotori</font> oAIO Swapper</html>",
		description = "Automatically switches gear based on configured profiles",
		tags = {"gear", "switcher", "equipment", "inventory", "oaio"},
		enabledByDefault = false
)
public class oAIOSwapperPlugin extends Plugin
{
	private static final String WEAR_ACTION = "Wear";

	@Inject
	private Client client;

	@Inject
	private oAIOSwapperConfig config;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ClientToolbar clientToolbar;

	private oAIOSwapperPanel panel;
	private NavigationButton navButton;
	private final Map<String, List<Integer>> profiles = new HashMap<>();
	private boolean isRecording = false;
	private String currentProfile = "";

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.hotkey()) {
		@Override
		public void hotkeyPressed() {
			executeSwitch();
		}
	};

	@Provides
	oAIOSwapperConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(oAIOSwapperConfig.class);
	}

	@Override
	protected void startUp() {
		panel = injector.getInstance(oAIOSwapperPanel.class);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/gear_icon.png");

		navButton = NavigationButton.builder()
				.tooltip("oAIO Swapper")
				.icon(icon)
				.priority(5)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);
		keyManager.registerKeyListener(hotkeyListener);
		loadProfiles();
	}

	@Override
	protected void shutDown() {
		clientToolbar.removeNavigation(navButton);
		keyManager.unregisterKeyListener(hotkeyListener);
		profiles.clear();
		panel = null;
		navButton = null;
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (client.getGameState() != GameState.LOGGED_IN) {
			return;
		}

		if (isRecording) {
			recordCurrentGear();
		}
	}

	private void recordCurrentGear() {
		if (currentProfile.isEmpty()) {
			return;
		}

		List<Integer> items = new ArrayList<>();

		final ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment != null) {
			for (Item item : equipment.getItems()) {
				if (item.getId() != -1) {
					items.add(item.getId());
				}
			}
		}

		profiles.put(currentProfile, items);
		saveProfiles();
		MiscUtilities.sendGameMessage("Profile '" + currentProfile + "' recorded with " + items.size() + " items.");
	}

	private void executeSwitch() {
		if (client.getGameState() != GameState.LOGGED_IN || currentProfile.isEmpty()) {
			return;
		}

		List<Integer> items = profiles.get(currentProfile);
		if (items == null || items.isEmpty()) {
			return;
		}

		Widget inventory = client.getWidget(WidgetInfo.INVENTORY);
		if (inventory == null || inventory.isHidden()) {
			return;
		}

		for (Integer itemId : items) {
			Widget[] inventoryItems = inventory.getDynamicChildren();
			for (Widget item : inventoryItems) {
				if (item.getItemId() == itemId) {
					final int itemIndex = item.getIndex();
					final int widgetId = item.getId();

					ReflectionLibrary.invokeMenuAction(
							MenuAction.CC_OP.getId(),  // identifier
							itemIndex,                 // param0
							MenuAction.CC_OP.getId(),  // opcode
							widgetId,                 // param1
							2                        // option (2 is the option ID for "Wear")
					);
				}
			}
		}

		MiscUtilities.sendGameMessage("Switched to profile: " + currentProfile);
	}


	private void loadProfiles()
	{
		String profileData = configManager.getConfiguration(oAIOSwapperConfig.GROUP, "profiles");
		if (profileData != null && !profileData.isEmpty())
		{
			String[] profileEntries = profileData.split(";");
			for (String entry : profileEntries)
			{
				String[] parts = entry.split(":");
				if (parts.length == 2)
				{
					String name = parts[0];
					List<Integer> items = Arrays.stream(parts[1].split(","))
							.map(Integer::parseInt)
							.collect(Collectors.toList());
					profiles.put(name, items);
				}
			}
			MiscUtilities.sendGameMessage("Loaded " + profiles.size() + " gear profiles");
		}
	}

	private void saveProfiles()
	{
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, List<Integer>> entry : profiles.entrySet())
		{
			sb.append(entry.getKey())
					.append(":")
					.append(entry.getValue().stream()
							.map(String::valueOf)
							.collect(Collectors.joining(",")))
					.append(";");
		}
		configManager.setConfiguration(oAIOSwapperConfig.GROUP, "profiles", sb.toString());
	}

	public void startRecording(String profileName)
	{
		currentProfile = profileName;
		isRecording = true;
		MiscUtilities.sendGameMessage("Started recording profile: " + profileName);
	}

	public void stopRecording()
	{
		isRecording = false;
		MiscUtilities.sendGameMessage("Stopped recording profile: " + currentProfile);
		currentProfile = "";
	}

	public Map<String, List<Integer>> getProfiles()
	{
		return profiles;
	}

	public void deleteProfile(String name)
	{
		profiles.remove(name);
		saveProfiles();
		MiscUtilities.sendGameMessage("Deleted profile: " + name);
	}

	public void setCurrentProfile(String name)
	{
		currentProfile = name;
	}
}

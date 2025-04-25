package com.theplug.kotori.oaioswapper;

import com.google.inject.Provides;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.theplug.kotori.kotoriutils.KotoriUtils;
import com.theplug.kotori.kotoriutils.methods.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Type;
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
public class oAIOSwapperPlugin extends Plugin {
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

	@Inject
	private ClientThread clientThread;

	private oAIOSwapperPanel panel;
	private NavigationButton navButton;
	private final Map<String, Profile> profiles = new HashMap<>();
	private final Map<String, HotkeyListener> hotkeyListeners = new HashMap<>();
	private boolean isRecording = false;
	private String currentProfile = "";
	private final Gson GSON = new GsonBuilder().create();

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
		loadProfiles();
		registerAllHotkeyListeners();
	}

	@Override
	protected void shutDown() {
		clientToolbar.removeNavigation(navButton);
		unregisterAllHotkeyListeners();
		saveProfiles();
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
			isRecording = false;
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

		Profile profile = profiles.get(currentProfile);
		if (profile == null) {
			profile = new Profile(currentProfile, items, Keybind.NOT_SET);
		} else {
			profile.setItems(items);
		}
		profiles.put(currentProfile, profile);

		saveProfiles();
		MiscUtilities.sendGameMessage("Profile '" + currentProfile + "' recorded with " + items.size() + " items.");

		if (panel != null) {
			SwingUtilities.invokeLater(() -> {
				String itemIds = items.stream()
						.map(String::valueOf)
						.collect(Collectors.joining(", "));
				panel.updateItemIds(itemIds);
			});
		}
	}

	private void executeSwitch(String profileName) {
		clientThread.invoke(() -> {
			if (client.getGameState() != GameState.LOGGED_IN) {
				return;
			}

			Profile profile = profiles.get(profileName);
			if (profile == null || profile.getItems() == null || profile.getItems().isEmpty()) {
				return;
			}

			int[] itemsArray = profile.getItems().stream().mapToInt(i -> i).toArray();
			boolean finishedEquipping = InventoryInteractions.equipItems(itemsArray, config.actionsPerTick());

			if (finishedEquipping) {
				MiscUtilities.sendGameMessage("Switched to profile: " + profileName);
			}
		});
	}

	private void registerAllHotkeyListeners() {
		for (Profile profile : profiles.values()) {
			registerHotkeyListener(profile);
		}
	}

	private void unregisterAllHotkeyListeners() {
		for (HotkeyListener listener : hotkeyListeners.values()) {
			keyManager.unregisterKeyListener(listener);
		}
		hotkeyListeners.clear();
	}

	private void registerHotkeyListener(Profile profile) {
		if (hotkeyListeners.containsKey(profile.getName())) {
			keyManager.unregisterKeyListener(hotkeyListeners.get(profile.getName()));
		}

		if (profile.getHotkey() != null) {
			HotkeyListener hotkeyListener = new HotkeyListener(() -> profile.getHotkey()) {
				@Override
				public void hotkeyPressed() {
					executeSwitch(profile.getName());
				}
			};

			hotkeyListeners.put(profile.getName(), hotkeyListener);
			keyManager.registerKeyListener(hotkeyListener);
		}
	}

	private void loadProfiles() {
		String json = configManager.getConfiguration(oAIOSwapperConfig.GROUP, "profilesv2");
		if (json != null && !json.isEmpty()) {
			try {
				Type type = new TypeToken<Map<String, Profile>>(){}.getType();
				Map<String, Profile> loadedProfiles = GSON.fromJson(json, type);
				profiles.putAll(loadedProfiles);
				registerAllHotkeyListeners();

				MiscUtilities.sendGameMessage("Loaded " + profiles.size() + " gear profiles");
			} catch (Exception e) {
				log.error("Error loading profiles", e);
			}
		}
	}

	public void saveProfiles() {
		try {
			String json = GSON.toJson(profiles);
			configManager.setConfiguration(oAIOSwapperConfig.GROUP, "profilesv2", json);
		} catch (Exception e) {
			log.error("Error saving profiles", e);
		}
	}

	public void startRecording(String profileName) {
		currentProfile = profileName;
		isRecording = true;
		MiscUtilities.sendGameMessage("Recording profile: " + profileName);
	}

	public Map<String, Profile> getProfiles() {
		return profiles;
	}

	public void deleteProfile(String name) {
		Profile profile = profiles.remove(name);
		if (profile != null && hotkeyListeners.containsKey(name)) {
			keyManager.unregisterKeyListener(hotkeyListeners.get(name));
			hotkeyListeners.remove(name);
		}

		if (currentProfile.equals(name)) {
			currentProfile = "";
		}
		saveProfiles();
		MiscUtilities.sendGameMessage("Deleted profile: " + name);
	}

	public void setCurrentProfile(String name) {
		currentProfile = name;
	}

	public void updateProfile(String name, List<Integer> items, Keybind hotkey) {
		Profile profile = new Profile(name, items, hotkey);
		profiles.put(name, profile);
		registerHotkeyListener(profile);
		saveProfiles();
	}
}
package com.theplug.kotori.oaioswapper;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.config.Keybind;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.Map;

@Slf4j
public class oAIOSwapperPanel extends PluginPanel {
    private final oAIOSwapperPlugin plugin;
    private final oAIOSwapperConfig config;

    private final JComboBox<String> profileDropdown;
    private final JTextArea itemIdsInput;
    private final JButton hotkeyButton;
    private final JButton createProfileButton;
    private final JButton saveButton;
    private final JButton deleteButton;
    private final JButton saveEquipmentButton;

    private Keybind currentHotkey = Keybind.NOT_SET;

    @Inject
    public oAIOSwapperPanel(oAIOSwapperPlugin plugin, oAIOSwapperConfig config) {
        this.plugin = plugin;
        this.config = config;

        setBorder(new EmptyBorder(10, 10, 10, 10));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Profile Dropdown
        JPanel profilePanel = new JPanel(new BorderLayout());
        profilePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        profileDropdown = new JComboBox<>();
        profileDropdown.setFocusable(false);
        profileDropdown.setMaximumSize(new Dimension(PANEL_WIDTH - 20, 25));

        JLabel profileLabel = new JLabel("Profile:");
        profileLabel.setForeground(Color.WHITE);
        profilePanel.add(profileLabel, BorderLayout.NORTH);
        profilePanel.add(profileDropdown, BorderLayout.CENTER);
        add(profilePanel);
        add(Box.createRigidArea(new Dimension(0, 10)));

        // Item IDs TextArea
        JPanel textAreaPanel = new JPanel(new BorderLayout());
        textAreaPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        itemIdsInput = new JTextArea(5, 20);
        itemIdsInput.setLineWrap(true);
        itemIdsInput.setWrapStyleWord(true);
        itemIdsInput.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
        itemIdsInput.setForeground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(itemIdsInput);
        scrollPane.setMaximumSize(new Dimension(PANEL_WIDTH - 20, 80));

        JLabel itemLabel = new JLabel("Item IDs (comma-separated):");
        itemLabel.setForeground(Color.WHITE);
        textAreaPanel.add(itemLabel, BorderLayout.NORTH);
        textAreaPanel.add(scrollPane, BorderLayout.CENTER);
        add(textAreaPanel);
        add(Box.createRigidArea(new Dimension(0, 10)));

        // Hotkey Button
        JPanel hotkeyPanel = new JPanel(new BorderLayout());
        hotkeyPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        hotkeyButton = new JButton("Set Hotkey: None");
        hotkeyButton.setMaximumSize(new Dimension(PANEL_WIDTH - 20, 25));
        hotkeyButton.addActionListener(e -> startHotkeyInput());

        JLabel hotkeyLabel = new JLabel("Profile Hotkey:");
        hotkeyLabel.setForeground(Color.WHITE);
        hotkeyPanel.add(hotkeyLabel, BorderLayout.NORTH);
        hotkeyPanel.add(hotkeyButton, BorderLayout.CENTER);
        add(hotkeyPanel);
        add(Box.createRigidArea(new Dimension(0, 10)));

        // Buttons Panel
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        buttonsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        createProfileButton = new JButton("Create New");
        saveButton = new JButton("Save");
        deleteButton = new JButton("Delete");
        saveEquipmentButton = new JButton("Save equipment as swap");

        createProfileButton.addActionListener(e -> createNewProfile());
        saveButton.addActionListener(e -> saveCurrentProfile());
        deleteButton.addActionListener(e -> deleteCurrentProfile());
        saveEquipmentButton.addActionListener(e -> saveCurrentEquipment());

        buttonsPanel.add(createProfileButton);
        buttonsPanel.add(saveButton);
        buttonsPanel.add(deleteButton);
        buttonsPanel.add(saveEquipmentButton);

        add(buttonsPanel);

        // Profile dropdown listener
        profileDropdown.addActionListener(e -> {
            String selected = (String) profileDropdown.getSelectedItem();
            if (selected != null) {
                plugin.setCurrentProfile(selected);
                loadProfileData(selected);
            }
        });

        // Load initial profiles
        loadProfiles();
    }

    private void startHotkeyInput() {
        hotkeyButton.setText("Press any key...");
        hotkeyButton.setForeground(Color.WHITE);
    }

    private void loadProfiles() {
        Map<String, Profile> profiles = plugin.getProfiles();
        profileDropdown.removeAllItems();
        for (String profile : profiles.keySet()) {
            profileDropdown.addItem(profile);
        }

        String selectedProfile = (String) profileDropdown.getSelectedItem();
        if (selectedProfile != null && !selectedProfile.isEmpty()) {
            loadProfileData(selectedProfile);
        }
    }

    private void loadProfileData(String profileName) {
        Map<String, Profile> profiles = plugin.getProfiles();
        Profile profile = profiles.get(profileName);
        if (profile != null) {
            String itemIds = profile.getItems().stream()
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(", "));
            itemIdsInput.setText(itemIds);
            currentHotkey = profile.getHotkey();
            hotkeyButton.setText("Set Hotkey: " + (currentHotkey != null ? currentHotkey.toString() : "None"));
        } else {
            itemIdsInput.setText("");
            currentHotkey = Keybind.NOT_SET;
            hotkeyButton.setText("Set Hotkey: None");
        }
    }

    private void createNewProfile() {
        String name = JOptionPane.showInputDialog(this, "Enter profile name:");
        if (name != null && !name.isEmpty()) {
            profileDropdown.addItem(name);
            profileDropdown.setSelectedItem(name);
            plugin.setCurrentProfile(name);
            currentHotkey = Keybind.NOT_SET;
            hotkeyButton.setText("Set Hotkey: None");
            itemIdsInput.setText("");
        }
    }

    private void saveCurrentProfile() {
        String currentProfile = (String) profileDropdown.getSelectedItem();
        if (currentProfile != null && !currentProfile.isEmpty()) {
            List<Integer> items = java.util.Arrays.stream(itemIdsInput.getText().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .collect(java.util.stream.Collectors.toList());

            plugin.updateProfile(currentProfile, items, currentHotkey);
            loadProfiles();
        }
    }

    private void deleteCurrentProfile() {
        String currentProfile = (String) profileDropdown.getSelectedItem();
        if (currentProfile != null) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete profile: " + currentProfile + "?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                plugin.deleteProfile(currentProfile);
                loadProfiles();
            }
        }
    }

    private void saveCurrentEquipment() {
        String currentProfile = (String) profileDropdown.getSelectedItem();
        if (currentProfile != null && !currentProfile.isEmpty()) {
            plugin.startRecording(currentProfile);
        }
    }

    public void updateItemIds(String itemIds) {
        SwingUtilities.invokeLater(() -> {
            itemIdsInput.setText(itemIds);
        });
    }

    public void setHotkey(Keybind hotkey) {
        this.currentHotkey = hotkey;
        hotkeyButton.setText("Set Hotkey: " + (hotkey != null ? hotkey.toString() : "None"));
        saveCurrentProfile();
    }
}
package com.theplug.kotori.oaioswapper;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

@Slf4j
public class oAIOSwapperPanel extends PluginPanel
{
    private final oAIOSwapperPlugin plugin;
    private final oAIOSwapperConfig config;
    private final ItemManager itemManager;

    private final JComboBox<String> profileSelector;
    private final JPanel profilePanel;
    private final JPanel itemsContainer;

    @Inject
    public oAIOSwapperPanel(oAIOSwapperPlugin plugin, oAIOSwapperConfig config, ItemManager itemManager)
    {
        super(false);
        this.plugin = plugin;
        this.config = config;
        this.itemManager = itemManager;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        profilePanel = new JPanel(new GridBagLayout());
        profilePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        profilePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;

        profileSelector = new JComboBox<>();
        profileSelector.setFocusable(false);
        profileSelector.addActionListener(e -> updateItemsPanel());
        profilePanel.add(profileSelector, c);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 5, 0));
        buttonPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JButton newProfile = new JButton("New");
        newProfile.setFocusable(false);
        newProfile.addActionListener(e -> createNewProfile());

        JButton recordButton = new JButton("Record");
        recordButton.setFocusable(false);
        recordButton.addActionListener(e -> {
            String profileName = (String) profileSelector.getSelectedItem();
            if (profileName != null)
            {
                if (recordButton.getText().equals("Record"))
                {
                    plugin.startRecording(profileName);
                    recordButton.setText("Stop");
                }
                else
                {
                    plugin.stopRecording();
                    recordButton.setText("Record");
                    updateItemsPanel();
                }
            }
        });

        JButton deleteProfile = new JButton("Delete");
        deleteProfile.setFocusable(false);
        deleteProfile.addActionListener(e -> {
            String profileName = (String) profileSelector.getSelectedItem();
            if (profileName != null)
            {
                plugin.deleteProfile(profileName);
                updateProfileSelector();
            }
        });

        buttonPanel.add(newProfile);
        buttonPanel.add(recordButton);
        buttonPanel.add(deleteProfile);

        c.gridy++;
        profilePanel.add(buttonPanel, c);

        itemsContainer = new JPanel();
        itemsContainer.setLayout(new DynamicGridLayout(0, 1, 0, 3));
        itemsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JScrollPane scrollPane = new JScrollPane(itemsContainer);
        scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(profilePanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        updateProfileSelector();
    }

    private void createNewProfile()
    {
        String name = JOptionPane.showInputDialog(this,
                "Enter profile name:",
                "New Profile",
                JOptionPane.PLAIN_MESSAGE);

        if (name != null && !name.isEmpty())
        {
            plugin.getProfiles().putIfAbsent(name, List.of());
            updateProfileSelector();
            profileSelector.setSelectedItem(name);
        }
    }

    private void updateProfileSelector()
    {
        profileSelector.removeAllItems();
        for (String profile : plugin.getProfiles().keySet())
        {
            profileSelector.addItem(profile);
        }
    }

    private void updateItemsPanel()
    {
        itemsContainer.removeAll();

        String selectedProfile = (String) profileSelector.getSelectedItem();
        if (selectedProfile != null)
        {
            List<Integer> items = plugin.getProfiles().get(selectedProfile);
            if (items != null)
            {
                for (Integer itemId : items)
                {
                    JPanel itemPanel = new JPanel();
                    itemPanel.setLayout(new BorderLayout());
                    itemPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                    itemPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

                    BufferedImage icon = itemManager.getImage(itemId);
                    JLabel iconLabel = new JLabel(new ImageIcon(icon));

                    JLabel nameLabel = new JLabel(itemManager.getItemComposition(itemId).getName());
                    nameLabel.setForeground(Color.WHITE);

                    itemPanel.add(iconLabel, BorderLayout.WEST);
                    itemPanel.add(nameLabel, BorderLayout.CENTER);

                    itemsContainer.add(itemPanel);
                }
            }
        }

        itemsContainer.revalidate();
        itemsContainer.repaint();
    }
}
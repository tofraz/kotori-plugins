package com.theplug.kotori.fortiscolosseumcontroller; // Adjust package

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("fortiscolosseumwave5controller")
public interface FortisColosseumWave5ControllerConfig extends Config {

	@ConfigItem(
			keyName = "enablePlugin",
			name = "Enable Plugin",
			description = "Master toggle for the plugin.",
			position = 0
	)
	default boolean enablePlugin() {
		return true;
	}

	@ConfigItem(
			keyName = "wave5CompletionMessagePrefix",
			name = "Wave 5 Msg Prefix",
			description = "The start of the game message for wave 5 completion. Case sensitive.",
			position = 1
	)
	default String wave5CompletionMessagePrefix() {
		return "Wave 5 completed! Wave duration: ";
	}

	@ConfigItem(
			keyName = "delayAfterHotkey",
			name = "Delay After Hotkey (Fallback)",
			description = "Fallback delay if not waiting for deactivation message, or after message before next action.",
			position = 2
	)
	@Units(Units.MILLISECONDS)
	@Range(min = 500, max = 5000)
	default int delayAfterHotkey() { return 1000; }

	@ConfigItem(
			keyName = "delayBetweenActions",
			name = "Delay Between Actions",
			description = "General delay between automated clicks/actions.",
			position = 3
	)
	@Units(Units.MILLISECONDS)
	@Range(min = 300, max = 2000)
	default int delayBetweenActions() { return 750; }

	// Hidden dev info
	@ConfigItem(keyName = "currentUserLogin", name = "", description = "", hidden = true)
	default String currentUserLogin() { return "tofraz"; }
	@ConfigItem(keyName = "currentDateTimeUTC", name = "", description = "", hidden = true)
	default String currentDateTimeUTC() { return "2025-05-10 08:14:11"; } // Updated


	@ConfigSection(
			name = "Other Plugin Control",
			description = "Settings for controlling the PAutoColosseum plugin.",
			position = 50,
			closedByDefault = false
	)
	String otherPluginControlSection = "otherPluginControlSection";

	@ConfigItem(
			keyName = "otherPluginDeactivationMessage",
			name = "PAutoColosseum Deactivation Message",
			description = "Exact chat message from PAutoColosseum when it deactivates.",
			section = otherPluginControlSection,
			position = 1
	)
	default String otherPluginDeactivationMessage() { return "PAutoColosseum: Disabled PAutoColosseum"; }

	@ConfigItem(
			keyName = "timeoutForDeactivationMessage",
			name = "Deactivation Msg Timeout",
			description = "Max time (ms) to wait for PAutoColosseum's deactivation message before proceeding anyway.",
			section = otherPluginControlSection,
			position = 2
	)
	@Units(Units.MILLISECONDS)
	@Range(min = 1000, max = 10000)
	default int timeoutForDeactivationMessage() { return 4000; }


	@ConfigSection(
			name = "Interaction IDs",
			description = "IDs and Opcodes for various interactions. Generally do not change unless game updates.",
			position = 100,
			closedByDefault = true
	)
	String interactionIdsSection = "interactionIdsSection";


	// --- Claim Button ---
	@ConfigItem(keyName = "claimButtonWidgetGroupId", name = "Claim GroupID", description = "Widget Group ID for 'Claim' button", section = interactionIdsSection, position = 10)
	default int claimButtonWidgetGroupId() { return 862; }
	@ConfigItem(keyName = "claimButtonWidgetChildId", name = "Claim ChildID", description = "Widget Child ID for 'Claim' button", section = interactionIdsSection, position = 11)
	default int claimButtonWidgetChildId() { return 10; }
	@ConfigItem(keyName = "claimButtonOpcode", name = "Claim Opcode", description = "Opcode for 'Claim' button", section = interactionIdsSection, position = 12)
	default int claimButtonOpcode() { return 57; }
	@ConfigItem(keyName = "claimButtonIdentifier", name = "Claim Identifier", description = "Identifier for 'Claim' button", section = interactionIdsSection, position = 13)
	default int claimButtonIdentifier() { return 1; }
	@ConfigItem(keyName = "claimButtonParam0", name = "Claim Param0", description = "Param0 for 'Claim' button", section = interactionIdsSection, position = 14)
	default int claimButtonParam0() { return -1; }

	// --- Confirm Claim Button ---
	@ConfigItem(keyName = "confirmClaimWidgetGroupId", name = "Confirm Claim GroupID", description = "GroupID for 'Confirm Claim'", section = interactionIdsSection, position = 20)
	default int confirmClaimWidgetGroupId() { return 862; }
	@ConfigItem(keyName = "confirmClaimWidgetChildId", name = "Confirm Claim ChildID", description = "ChildID for 'Confirm Claim'", section = interactionIdsSection, position = 21)
	default int confirmClaimWidgetChildId() { return 22; }
	@ConfigItem(keyName = "confirmClaimOpcode", name = "Confirm Claim Opcode", description = "Opcode for 'Confirm Claim'", section = interactionIdsSection, position = 22)
	default int confirmClaimOpcode() { return 57; }
	@ConfigItem(keyName = "confirmClaimIdentifier", name = "Confirm Claim Identifier", description = "Identifier for 'Confirm Claim'", section = interactionIdsSection, position = 23)
	default int confirmClaimIdentifier() { return 1; }
	@ConfigItem(keyName = "confirmClaimParam0", name = "Confirm Claim Param0", description = "Param0 for 'Confirm Claim'", section = interactionIdsSection, position = 24)
	default int confirmClaimParam0() { return -1; }

	// --- Rewards Chest Object ---
	@ConfigItem(keyName = "rewardsChestObjectId", name = "Rewards Chest Obj ID", description = "Object ID for 'Rewards Chest'", section = interactionIdsSection, position = 30)
	default int rewardsChestObjectId() { return 50741; }
	@ConfigItem(keyName = "rewardsChestSearchOption", name = "Rewards Chest Option", description = "Menu option for searching chest", section = interactionIdsSection, position = 31)
	default String rewardsChestSearchOption() { return "Search"; }
	@ConfigItem(keyName = "rewardsChestSearchOpcode", name = "Rewards Chest Opcode", description = "Opcode for searching chest", section = interactionIdsSection, position = 32)
	default int rewardsChestSearchOpcode() { return 3; }

	// --- Bank-All Button ---
	@ConfigItem(keyName = "bankAllButtonWidgetGroupId", name = "Bank-All GroupID", description = "GroupID for 'Bank-all'", section = interactionIdsSection, position = 40)
	default int bankAllButtonWidgetGroupId() { return 861; }
	@ConfigItem(keyName = "bankAllButtonWidgetChildId", name = "Bank-All ChildID", description = "ChildID for 'Bank-all'", section = interactionIdsSection, position = 41)
	default int bankAllButtonWidgetChildId() { return 15; }
	@ConfigItem(keyName = "bankAllButtonOpcode", name = "Bank-All Opcode", description = "Opcode for 'Bank-all'", section = interactionIdsSection, position = 42)
	default int bankAllButtonOpcode() { return 1007; }
	@ConfigItem(keyName = "bankAllButtonIdentifier", name = "Bank-All Identifier", description = "Identifier for 'Bank-all'", section = interactionIdsSection, position = 43)
	default int bankAllButtonIdentifier() { return 6; }
	@ConfigItem(keyName = "bankAllButtonParam0", name = "Bank-All Param0", description = "Param0 for 'Bank-all'", section = interactionIdsSection, position = 44)
	default int bankAllButtonParam0() { return -1; }

	// --- Close Rewards Widget Button ---
	@ConfigItem(keyName = "closeRewardsWidgetGroupId", name = "Close Rewards GroupID", description = "GroupID for 'Close' rewards widget", section = interactionIdsSection, position = 50)
	default int closeRewardsWidgetGroupId() { return 861; }
	@ConfigItem(keyName = "closeRewardsWidgetChildId", name = "Close Rewards ChildID", description = "ChildID for 'Close' rewards widget", section = interactionIdsSection, position = 51)
	default int closeRewardsWidgetChildId() { return 2; }
	@ConfigItem(keyName = "closeRewardsWidgetOpcode", name = "Close Rewards Opcode", description = "Opcode for 'Close' rewards widget", section = interactionIdsSection, position = 52)
	default int closeRewardsWidgetOpcode() { return 57; }
	@ConfigItem(keyName = "closeRewardsWidgetIdentifier", name = "Close Rewards Identifier", description = "Identifier for 'Close' rewards widget", section = interactionIdsSection, position = 53)
	default int closeRewardsWidgetIdentifier() { return 1; }
	@ConfigItem(keyName = "closeRewardsWidgetParam0", name = "Close Rewards Param0", description = "Param0 for 'Close' rewards widget", section = interactionIdsSection, position = 54)
	default int closeRewardsWidgetParam0() { return 11; }

	// --- Minimus NPC ---
	@ConfigItem(keyName = "minimusNpcId", name = "Minimus NPC ID", description = "NPC ID for 'Minimus'", section = interactionIdsSection, position = 60)
	default int minimusNpcId() { return 44978; }
	@ConfigItem(keyName = "minimusLeaveOption", name = "Minimus Option", description = "Menu option for Minimus", section = interactionIdsSection, position = 61)
	default String minimusLeaveOption() { return "Leave"; }
	@ConfigItem(keyName = "minimusLeaveOpcode", name = "Minimus Opcode", description = "Opcode for Minimus interaction", section = interactionIdsSection, position = 62)
	default int minimusLeaveOpcode() { return 11; }

	// --- Leave Dialogue (Continue/Yes) ---
	@ConfigItem(keyName = "leaveDialogueWidgetGroupId", name = "Leave Dialogue GroupID", description = "GroupID for 'Continue/Yes' in leave dialogue", section = interactionIdsSection, position = 70)
	default int leaveDialogueWidgetGroupId() { return 219; }
	@ConfigItem(keyName = "leaveDialogueWidgetChildId", name = "Leave Dialogue ChildID", description = "ChildID for 'Continue/Yes' in leave dialogue", section = interactionIdsSection, position = 71)
	default int leaveDialogueWidgetChildId() { return 1; }
	@ConfigItem(keyName = "leaveDialogueOpcode", name = "Leave Dialogue Opcode", description = "Opcode for 'Continue/Yes'", section = interactionIdsSection, position = 72)
	default int leaveDialogueOpcode() { return 30; }
	@ConfigItem(keyName = "leaveDialogueIdentifier", name = "Leave Dialogue Identifier", description = "Identifier for 'Continue/Yes'", section = interactionIdsSection, position = 73)
	default int leaveDialogueIdentifier() { return 0; }
	@ConfigItem(keyName = "leaveDialogueParam0", name = "Leave Dialogue Param0", description = "Param0 for 'Continue/Yes'", section = interactionIdsSection, position = 74)
	default int leaveDialogueParam0() { return 1; }
}
package com.theplug.kotori.fortiscolosseumcontroller; // Adjust package as needed

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned; // For event-based tracking
import net.runelite.api.events.GameObjectSpawned;  // For event-based tracking
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
// import net.runelite.client.ui.overlay.OverlayManager; // Not currently used
import net.runelite.client.util.Text;

// Your KotoriUtils static utility classes
import com.theplug.kotori.kotoriutils.methods.MiscUtilities;
import com.theplug.kotori.kotoriutils.methods.NPCInteractions;
import com.theplug.kotori.kotoriutils.ReflectionLibrary;
// Main KotoriUtils class, potentially for initializing ReflectionLibrary hooks
import com.theplug.kotori.kotoriutils.KotoriUtils;


import java.time.Instant;

@Slf4j
@PluginDescriptor(
		name = "Fortis Colosseum Wave 5 Controller",
		description = "Automates interactions after Fortis Colosseum Wave 5 completion.",
		tags = {"colosseum", "fortis", "automation", "kotori", "controller"}
)
public class FortisColosseumWave5ControllerPlugin extends Plugin {

	@Inject
	private Client client;

	@Inject
	private FortisColosseumWave5ControllerConfig config;

	// Injected if KotoriUtils is responsible for initializing ReflectionLibrary hooks
	@Inject
	private KotoriUtils kotoriUtils;

	private GameObject rewardsChestObject; // Field to store the Rewards Chest instance

	private enum State {
		IDLE,
		WAITING_FOR_WAVE_COMPLETION,
		WAITING_FOR_OTHER_PLUGIN_DEACTIVATION,
		CLICKING_CLAIM_BUTTON,
		CONFIRMING_CLAIM,
		SEARCHING_REWARDS_CHEST,
		CLICKING_BANK_ALL,
		CLOSING_REWARDS_WIDGET,
		LEAVING_MINIMUS,
		CLICKING_LEAVE_DIALOGUE,
		COMPLETED_SEQUENCE
	}

	private State currentState = State.IDLE;
	private long lastActionTime = 0;
	private long stateEntryTime = 0;
	private boolean hotkeyPressed = false;
	private String uniqueTriggerId = "";


	@Override
	protected void startUp() throws Exception {
		log.info("Fortis Colosseum Wave 5 Controller started!");
		currentState = State.IDLE;
		hotkeyPressed = false;
		rewardsChestObject = null; // Initialize to null
	}

	@Override
	protected void shutDown() throws Exception {
		log.info("Fortis Colosseum Wave 5 Controller stopped!");
		currentState = State.IDLE;
		hotkeyPressed = false;
		rewardsChestObject = null; // Clear on shutdown
	}

	@Provides
	FortisColosseumWave5ControllerConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(FortisColosseumWave5ControllerConfig.class);
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event) {
		GameObject spawnedObject = event.getGameObject();
		if (spawnedObject.getId() == config.rewardsChestObjectId()) {
			rewardsChestObject = spawnedObject;
			// Optional: log or send game message for debugging
			// MiscUtilities.sendGameMessage("Controller: Rewards Chest detected (spawned).");
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event) {
		GameObject despawnedObject = event.getGameObject();
		if (despawnedObject.getId() == config.rewardsChestObjectId()) {
			// Check if it's the same instance we are tracking
			if (rewardsChestObject == despawnedObject) {
				rewardsChestObject = null;
				// Optional: log or send game message for debugging
				// MiscUtilities.sendGameMessage("Controller: Rewards Chest despawned.");
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event) {
		// Listen only to GAMEMESSAGE for triggers and PUBLICCHAT for manual command
		if (event.getType() != ChatMessageType.GAMEMESSAGE &&
				event.getType() != ChatMessageType.PUBLICCHAT) {
			return;
		}

		String message = Text.removeTags(event.getMessage()).toLowerCase();

		// Manual start command
		if (event.getType() == ChatMessageType.PUBLICCHAT && message.equals("!fcw5start")) {
			if (currentState == State.IDLE || currentState == State.COMPLETED_SEQUENCE) {
				MiscUtilities.sendGameMessage("Controller: Started by command. Waiting for Wave 5 completion.");
				uniqueTriggerId = "!fcw5start_" + Instant.now().toEpochMilli();
				hotkeyPressed = true;
				enterState(State.WAITING_FOR_WAVE_COMPLETION);
			} else {
				MiscUtilities.sendGameMessage("Controller: Already active.");
			}
			return;
		}

		// Wave completion trigger
		if (event.getType() == ChatMessageType.GAMEMESSAGE &&
				hotkeyPressed && currentState == State.WAITING_FOR_WAVE_COMPLETION &&
				Text.removeTags(event.getMessage()).startsWith(config.wave5CompletionMessagePrefix())) {
			MiscUtilities.sendGameMessage("Controller: Wave 5 Complete message received!");

			if (config.otherPluginDeactivationMessage() != null && !config.otherPluginDeactivationMessage().isEmpty()) {
				enterState(State.WAITING_FOR_OTHER_PLUGIN_DEACTIVATION);
			} else {
				enterState(State.CLICKING_CLAIM_BUTTON);
			}
			return;
		}

		// Other plugin deactivation trigger
		if (event.getType() == ChatMessageType.GAMEMESSAGE &&
				hotkeyPressed && currentState == State.WAITING_FOR_OTHER_PLUGIN_DEACTIVATION &&
				Text.removeTags(event.getMessage()).equals(config.otherPluginDeactivationMessage())) {
			MiscUtilities.sendGameMessage("Controller: Other plugin deactivation message received.");
			enterState(State.CLICKING_CLAIM_BUTTON);
		}
	}


	@Subscribe
	public void onGameTick(GameTick event) {
		if (!config.enablePlugin() || !hotkeyPressed || client.getGameState() != GameState.LOGGED_IN) {
			return;
		}

		long currentTime = Instant.now().toEpochMilli();
		if (currentTime - lastActionTime < config.delayBetweenActions()) {
			return;
		}

		// Timeout for waiting for other plugin deactivation
		if (currentState == State.WAITING_FOR_OTHER_PLUGIN_DEACTIVATION &&
				(currentTime - stateEntryTime > config.timeoutForDeactivationMessage())) {
			MiscUtilities.sendGameMessage("Controller: Timeout waiting for other plugin. Proceeding.");
			enterState(State.CLICKING_CLAIM_BUTTON);
		}

		// Main state machine logic
		switch (currentState) {
			case CLICKING_CLAIM_BUTTON:     performClaimButton(); break;
			case CONFIRMING_CLAIM:          performConfirmClaim(); break;
			case SEARCHING_REWARDS_CHEST:   performSearchRewardsChest(); break;
			case CLICKING_BANK_ALL:         performBankAll(); break;
			case CLOSING_REWARDS_WIDGET:    performCloseRewardsWidget(); break;
			case LEAVING_MINIMUS:           performLeaveMinimus(); break;
			case CLICKING_LEAVE_DIALOGUE:   performLeaveDialogue(); break;
			case COMPLETED_SEQUENCE:
				MiscUtilities.sendGameMessage("Controller: Sequence completed for trigger: " + uniqueTriggerId);
				hotkeyPressed = false;
				uniqueTriggerId = "";
				rewardsChestObject = null; // Clear reference after sequence is done
				enterState(State.IDLE);
				break;
			default:
				// Do nothing in IDLE, WAITING_FOR_WAVE_COMPLETION states here,
				// as they are primarily transitioned by events or initial hotkey.
				break;
		}
	}

	private void enterState(State newState) {
		MiscUtilities.sendGameMessage("Controller: Entering state " + newState.name());
		currentState = newState;
		stateEntryTime = Instant.now().toEpochMilli();
		// Update lastActionTime for states that perform an action, to respect delayBetweenActions
		if (newState != State.WAITING_FOR_WAVE_COMPLETION &&
				newState != State.WAITING_FOR_OTHER_PLUGIN_DEACTIVATION &&
				newState != State.IDLE &&
				newState != State.COMPLETED_SEQUENCE) { // COMPLETED_SEQUENCE is a terminal state before IDLE
			updateLastActionTime();
		}
	}

	private void updateLastActionTime() {
		lastActionTime = Instant.now().toEpochMilli();
	}

	private Widget findWidget(int groupId, int childId) {
		return client.getWidget(groupId, childId);
	}

	// findGameObject() is removed; using event-based rewardsChestObject field

	private NPC findNpcById(int npcId) {
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null) return null;
		WorldPoint playerLocation = localPlayer.getWorldLocation();
		if (playerLocation == null) return null;

		// Using static method from your NPCInteractions utility
		for (NPC npc : NPCInteractions.getNpcs()) {
			if (npc.getId() == npcId && npc.getWorldLocation().distanceTo(playerLocation) < 15) {
				return npc;
			}
		}
		return null;
	}

	private String formatTarget(String name) {
		// Ensure "null" string isn't used as target name
		return name != null && !name.equalsIgnoreCase("null") ? name : "Object";
	}

	private void clickWidget(Widget widget, int opcode, int identifier, int param0, String actionNameForLog, State nextState) {
		if (widget != null && !widget.isHidden()) {
			// Using static method from your ReflectionLibrary utility
			ReflectionLibrary.invokeMenuAction(
					param0, widget.getId(), opcode, identifier, widget.getItemId(),
					-1, // worldViewId, typically -1 for pure widget ops
					actionNameForLog, // Option string
					formatTarget(widget.getName()), // Target string
					-1, -1 // x, y typically -1 for widgets
			);
			MiscUtilities.sendGameMessage("Controller Action: " + actionNameForLog + ". Next: " + nextState.name());
			updateLastActionTime();
			enterState(nextState);
		} else {
			MiscUtilities.sendGameMessage("Controller Warning: Widget for '" + actionNameForLog + "' not found/hidden.");
			// Consider if you want to retry or halt if a critical widget is missing
		}
	}

	private void performClaimButton() {
		Widget claimButton = findWidget(config.claimButtonWidgetGroupId(), config.claimButtonWidgetChildId());
		clickWidget(claimButton, config.claimButtonOpcode(), config.claimButtonIdentifier(), config.claimButtonParam0(),
				"Claim Rewards", State.CONFIRMING_CLAIM);
	}

	private void performConfirmClaim() {
		Widget confirmButton = findWidget(config.confirmClaimWidgetGroupId(), config.confirmClaimWidgetChildId());
		clickWidget(confirmButton, config.confirmClaimOpcode(), config.confirmClaimIdentifier(), config.confirmClaimParam0(),
				"Confirm Claim", State.SEARCHING_REWARDS_CHEST);
	}

	private void performSearchRewardsChest() {
		if (rewardsChestObject != null) {
			String targetName = "Object"; // Default
			// If ObjectComposition is available and client.getObjectDefinition() works in your API version:
			// net.runelite.api.ObjectComposition comp = client.getObjectDefinition(rewardsChestObject.getId());
			// if (comp != null && comp.getName() != null && !comp.getName().equalsIgnoreCase("null")) {
			//    targetName = comp.getName();
			// }

			// Using static method from your ReflectionLibrary utility
			ReflectionLibrary.invokeMenuAction(
					rewardsChestObject.getSceneMinLocation().getX(), rewardsChestObject.getSceneMinLocation().getY(),
					config.rewardsChestSearchOpcode(), rewardsChestObject.getId(),
					-1, // itemId
					client.getTopLevelWorldView().getId(), // worldViewId
					config.rewardsChestSearchOption(), // option string
					formatTarget(targetName), // target string
					(int) rewardsChestObject.getConvexHull().getBounds2D().getCenterX(),
					(int) rewardsChestObject.getConvexHull().getBounds2D().getCenterY()
			);
			MiscUtilities.sendGameMessage("Controller Action: Search Rewards Chest. Next: " + State.CLICKING_BANK_ALL.name());
			updateLastActionTime();
			enterState(State.CLICKING_BANK_ALL);
		} else {
			MiscUtilities.sendGameMessage("Controller Warning: Rewards Chest object not available (was it spawned and detected?). Retrying or halting might be needed.");
			// Decide on behavior: retry, wait, or halt. For now, it will simply not proceed with this step.
			// To retry, you could stay in this state or go to a waiting state.
		}
	}

	private void performBankAll() {
		Widget bankAllButton = findWidget(config.bankAllButtonWidgetGroupId(), config.bankAllButtonWidgetChildId());
		clickWidget(bankAllButton, config.bankAllButtonOpcode(), config.bankAllButtonIdentifier(), config.bankAllButtonParam0(),
				"Bank All", State.CLOSING_REWARDS_WIDGET);
	}

	private void performCloseRewardsWidget() {
		Widget closeButton = findWidget(config.closeRewardsWidgetGroupId(), config.closeRewardsWidgetChildId());
		clickWidget(closeButton, config.closeRewardsWidgetOpcode(), config.closeRewardsWidgetIdentifier(), config.closeRewardsWidgetParam0(),
				"Close Rewards", State.LEAVING_MINIMUS);
	}

	private void interactWithNpc(String actionNameForLog, int npcId, String option, int opcode, State nextState) {
		NPC targetNpc = findNpcById(npcId);
		if (targetNpc != null) {
			// Using static method from your ReflectionLibrary utility
			ReflectionLibrary.invokeMenuAction(
					0, // param0, or targetNpc.getSceneMinLocation().getX() if walking
					0, // param1, or targetNpc.getSceneMinLocation().getY() if walking
					opcode,
					targetNpc.getIndex(), // identifier (NPC index)
					-1, // itemId
					client.getTopLevelWorldView().getId(), // worldViewId
					option, // option string
					formatTarget(targetNpc.getName()), // target string
					(int) targetNpc.getConvexHull().getBounds2D().getCenterX(),
					(int) targetNpc.getConvexHull().getBounds2D().getCenterY()
			);
			MiscUtilities.sendGameMessage("Controller Action: " + actionNameForLog + ". Next: " + nextState.name());
			updateLastActionTime();
			enterState(nextState);
		} else {
			MiscUtilities.sendGameMessage("Controller Warning: NPC " + npcId + " for '" + actionNameForLog + "' not found.");
		}
	}

	private void performLeaveMinimus() {
		interactWithNpc("Leave Minimus", config.minimusNpcId(),
				config.minimusLeaveOption(), config.minimusLeaveOpcode(),
				State.CLICKING_LEAVE_DIALOGUE);
	}

	private void performLeaveDialogue() {
		Widget leaveDialogueButton = findWidget(config.leaveDialogueWidgetGroupId(), config.leaveDialogueWidgetChildId());
		clickWidget(leaveDialogueButton, config.leaveDialogueOpcode(), config.leaveDialogueIdentifier(), config.leaveDialogueParam0(),
				"Confirm Leave (Dialogue)", State.COMPLETED_SEQUENCE);
	}
}
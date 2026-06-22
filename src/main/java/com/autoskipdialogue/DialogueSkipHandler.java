package com.autoskipdialogue;

import com.autoskipdialogue.config.AutoSkipDialogueConfig;
import com.autoskipdialogue.config.AutoSkipDialogueConfigManager;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.core.mod.TickSchedulerManager;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.SystemMessageEvent;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.IterationDecision;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Input;
import net.neoforged.bus.api.SubscribeEvent;

public final class DialogueSkipHandler implements DialogueStateTracker.DialogueTransitionCallbacks {
    private static final int SHIFT_RELEASE_DELAY_MS = 200;
    private static final int MIN_DELAY_FOR_SHIFT_PULSE = 100;
    private static final FontDescription.Resource COORDINATES_FONT = new FontDescription.Resource(
            Identifier.withDefaultNamespace("hud/gameplay/default/top_right"));

    private final AutoSkipDialogueConfig config;
    private final DialogueSkipKeybindResolver skipKeybindResolver = new DialogueSkipKeybindResolver();
    private final DialogueStateTracker dialogueStateTracker = new DialogueStateTracker();

    private long progressAtMs = 0L;
    private long releaseSkipAtMs = 0L;
    private boolean syntheticSkipDown = false;
    private boolean skipAttempted = false;
    private KeyMapping resolvedSkipKey;
    private TickSchedulerManager.ScheduledTask scheduledSkipTask = null;

    public DialogueSkipHandler(AutoSkipDialogueConfig config) {
        this.config = config;
    }

    @SubscribeEvent
    public void onGameInfoReceived(SystemMessageEvent.GameInfoReceivedEvent event) {
        StyledText actionBarText = stripCoordinates(event.getOriginalStyledText());
        dialogueStateTracker.update(ActionBarDialogueParser.parse(actionBarText), this);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (!isActive()) {
            clearProgressState();
            return;
        }

        if (!Models.WorldState.onWorld()) {
            clearProgressState();
            return;
        }

        long now = System.currentTimeMillis();

        if (progressAtMs > 0L && now >= progressAtMs) {
            progressAtMs = 0L;

            if (isDirectHoldMode()) {
                if (!holdSkipForDirectSkip()) {
                    scheduleProgress(getSkipDelayMs());
                }
            } else {
                boolean skipPulseSent = sendSkipPulse();
                if (skipPulseSent) {
                    releaseSkipAtMs = now + SHIFT_RELEASE_DELAY_MS;
                } else {
                    scheduleProgress(getSkipDelayMs());
                }
            }
        }

        if (releaseSkipAtMs > 0L && now >= releaseSkipAtMs) {
            releaseSkipAtMs = 0L;
            restoreSkipState();
        }
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        skipKeybindResolver.invalidateCache();
        clearProgressState();
    }

    @Override
    public void onStarted(NpcDialogueSnapshot snapshot) {
        if (!isActive() || !canAutoSkip(snapshot)) {
            clearProgressState();
            return;
        }

        if (isDirectHoldActive()) return;

        clearProgressState();

        if (config.skipDuringAnimation) {
            scheduleProgress(getSkipDelayMs());
        }
    }

    @Override
    public void onUpdated(NpcDialogueSnapshot snapshot) {
        if (!isActive() || !canAutoSkip(snapshot)) {
            clearProgressState();
            return;
        }

        scheduleRetryIfSkipDidNotProgress(snapshot.dialogueText());
    }

    @Override
    public void onFinished(NpcDialogueSnapshot snapshot) {
        if (!isActive() || !canAutoSkip(snapshot)) {
            clearProgressState();
            return;
        }

        skipDialogue(snapshot.dialogueText());
    }

    @Override
    public void onEnded(NpcDialogueSnapshot snapshot) {
        if (!isActive()) {
            clearProgressState();
            return;
        }

        if (isDirectHoldActive() && !snapshot.hasChoices()) {
            Managers.TickScheduler.scheduleNextTick(() -> {
                if (isDirectHoldActive() && !dialogueStateTracker.isDialoguePresent()) {
                    clearProgressState();
                }
            });
            return;
        }

        clearProgressState();
    }

    private static StyledText stripCoordinates(StyledText packetText) {
        if (packetText.isEmpty()) {
            return packetText;
        }

        return packetText.iterate((part, changes) -> {
            if (part.getPartStyle().getFont().equals(COORDINATES_FONT)) {
                changes.remove(part);
            }

            return IterationDecision.CONTINUE;
        });
    }

    private boolean isActive() {
        AutoSkipDialogueConfig activeConfig = AutoSkipDialogueConfigManager.get();
        return activeConfig != null && activeConfig.enabled;
    }

    private boolean canAutoSkip(NpcDialogueSnapshot snapshot) {
        return snapshot.requiresShift() && !snapshot.hasChoices();
    }

    private void skipDialogue(String dialogueText) {
        cancelScheduledSkipRetry();
        if (isDirectHoldActive()) return;

        scheduleProgress(getSkipDelayMs());
    }

    private int getSkipDelayMs() {
        return Math.max(50, config.skipDelayMs);
    }

    private boolean isDirectHoldMode() {
        return config.skipDuringAnimation && getSkipDelayMs() <= MIN_DELAY_FOR_SHIFT_PULSE;
    }

    private boolean sendSkipPulse() {
        resolvedSkipKey = skipKeybindResolver.getSkipKeyMapping();
        LocalPlayer player = McUtils.player();
        if (player == null) return false;

        if (usesShiftInput(resolvedSkipKey)) {
            if (isPlayerPressingSkip(player)) return false;

            if (isServerSkipDown(player) && !syntheticSkipDown) {
                sendShift(player, false);
                return false;
            }

            sendShift(player, true);
            syntheticSkipDown = true;
            return true;
        }

        if (isPlayerPressingSkip(player)) return false;

        KeyMapping.click(resolvedSkipKey.key);
        return true;
    }

    private boolean holdSkipForDirectSkip() {
        resolvedSkipKey = skipKeybindResolver.getSkipKeyMapping();
        LocalPlayer player = McUtils.player();
        if (player == null) return false;

        if (usesShiftInput(resolvedSkipKey)) {
            if (syntheticSkipDown) return true;
            if (isPlayerPressingSkip(player)) return true;

            if (isServerSkipDown(player) && !syntheticSkipDown) {
                sendShift(player, false);
                return false;
            }

            if (!isPlayerPressingSkip(player) && !isServerSkipDown(player)) {
                sendShift(player, true);
                syntheticSkipDown = true;
            }

            return true;
        }

        if (isPlayerPressingSkip(player)) return true;

        KeyMapping.set(resolvedSkipKey.key, true);
        syntheticSkipDown = true;
        return true;
    }

    private void restoreSkipState() {
        LocalPlayer player = McUtils.player();
        if (player == null) {
            syntheticSkipDown = false;
            return;
        }

        if (!syntheticSkipDown) return;

        if (resolvedSkipKey != null && usesShiftInput(resolvedSkipKey)) {
            if (!isPlayerPressingSkip(player)) {
                sendShift(player, false);
            }
        } else if (resolvedSkipKey != null && !isPlayerPressingSkip(player)) {
            KeyMapping.set(resolvedSkipKey.key, false);
        }

        syntheticSkipDown = false;
    }

    private boolean isPlayerPressingSkip(LocalPlayer player) {
        if (resolvedSkipKey == null) {
            resolvedSkipKey = skipKeybindResolver.getSkipKeyMapping();
        }

        if (usesShiftInput(resolvedSkipKey)) {
            return player.isShiftKeyDown() || McUtils.options().keyShift.isDown();
        }

        return resolvedSkipKey.isDown();
    }

    private boolean isServerSkipDown(LocalPlayer player) {
        if (resolvedSkipKey == null || !usesShiftInput(resolvedSkipKey)) {
            return false;
        }

        return getLastSentInput(player).shift();
    }

    private static boolean usesShiftInput(KeyMapping mapping) {
        return mapping == McUtils.options().keyShift;
    }

    private static void sendShift(LocalPlayer player, boolean shift) {
        Input input = getLastSentInput(player);

        McUtils.sendPacket(new ServerboundPlayerInputPacket(new Input(
                input.forward(), input.backward(), input.left(), input.right(), input.jump(), shift, input.sprint())));
    }

    private static Input getLastSentInput(LocalPlayer player) {
        Input input = player.getLastSentInput();
        if (input == null) {
            input = Input.EMPTY;
        }

        return input;
    }

    private void clearProgressState() {
        progressAtMs = 0L;
        skipAttempted = false;
        releaseSkipAtMs = 0L;

        cancelScheduledSkipRetry();
        restoreSkipState();
    }

    private void scheduleProgress(long delayMs) {
        if (progressAtMs > 0L || releaseSkipAtMs > 0L || isDirectHoldActive()) return;

        skipAttempted = true;
        progressAtMs = System.currentTimeMillis() + delayMs;
    }

    private void scheduleRetryIfSkipDidNotProgress(String dialogueText) {
        if (!skipAttempted
                || releaseSkipAtMs > 0L
                || progressAtMs > 0L
                || scheduledSkipTask != null
                || isDirectHoldActive()) {
            return;
        }

        LocalPlayer player = McUtils.player();
        if (player == null || isPlayerPressingSkip(player) || isServerSkipDown(player)) return;

        scheduledSkipTask = Managers.TickScheduler.scheduleLater(
                () -> {
                    scheduledSkipTask = null;

                    if (!isActive() || !Models.WorldState.onWorld() || !dialogueStateTracker.isDialoguePresent()) {
                        return;
                    }

                    skipDialogue(dialogueText);
                },
                getPingDelayTicks());
    }

    private int getPingDelayTicks() {
        return Math.max(2, Services.Ping.getPing() / 50 + 2);
    }

    private boolean isDirectHoldActive() {
        return isDirectHoldMode() && syntheticSkipDown;
    }

    private void cancelScheduledSkipRetry() {
        if (scheduledSkipTask == null) return;

        Managers.TickScheduler.cancel(scheduledSkipTask);
        scheduledSkipTask = null;
    }
}

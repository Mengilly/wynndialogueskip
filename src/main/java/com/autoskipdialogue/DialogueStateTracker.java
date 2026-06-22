package com.autoskipdialogue;

import java.util.Optional;
import java.util.function.Consumer;

final class DialogueStateTracker {
    private NpcDialogueSnapshot currentDialogue;

    void update(Optional<NpcDialogueSnapshot> snapshot, DialogueTransitionCallbacks callbacks) {
        if (snapshot.isEmpty()) {
            endDialogue(callbacks);
            return;
        }

        updateDialogue(snapshot.get(), callbacks);
    }

    boolean isDialoguePresent() {
        return currentDialogue != null;
    }

    private void updateDialogue(NpcDialogueSnapshot snapshot, DialogueTransitionCallbacks callbacks) {
        if (isNewDialogue(snapshot)) {
            startDialogue(snapshot, callbacks);
            return;
        }

        boolean wasDoneRendering = currentDialogue.dialogueText().equals(snapshot.dialogueText());
        currentDialogue = snapshot;

        if (wasDoneRendering) {
            callbacks.onFinished(currentDialogue);
            return;
        }

        callbacks.onUpdated(currentDialogue);
    }

    private void startDialogue(NpcDialogueSnapshot snapshot, DialogueTransitionCallbacks callbacks) {
        if (currentDialogue != null) {
            endDialogue(callbacks);
        }

        currentDialogue = snapshot;
        callbacks.onStarted(currentDialogue);
    }

    private void endDialogue(DialogueTransitionCallbacks callbacks) {
        if (currentDialogue == null) {
            return;
        }

        NpcDialogueSnapshot endedDialogue = currentDialogue;
        currentDialogue = null;
        callbacks.onEnded(endedDialogue);
    }

    private boolean isNewDialogue(NpcDialogueSnapshot snapshot) {
        if (currentDialogue == null) {
            return true;
        }

        String dialogueText = snapshot.dialogueText();
        String currentDialogueText = currentDialogue.dialogueText();
        return !(dialogueText.equals(currentDialogueText) || dialogueText.startsWith(currentDialogueText));
    }

    interface DialogueTransitionCallbacks {
        void onStarted(NpcDialogueSnapshot snapshot);

        void onUpdated(NpcDialogueSnapshot snapshot);

        void onFinished(NpcDialogueSnapshot snapshot);

        void onEnded(NpcDialogueSnapshot snapshot);
    }
}

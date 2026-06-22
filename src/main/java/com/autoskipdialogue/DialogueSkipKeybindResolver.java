package com.autoskipdialogue;

import com.wynntils.utils.mc.McUtils;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.language.I18n;

public final class DialogueSkipKeybindResolver {
    private static final String UNBOUND_KEY = "key.keyboard.unknown";
    private static final String MOD_TOGGLE_KEY = "key.wynndialogueskip.toggle";
    private static final String[] KEYBIND_HINTS = {
        "skip dialogue",
        "advance dialogue",
        "dialogue skip",
        "continue dialogue",
        "skip_dialogue",
        "advance_dialogue",
        "dialogue_skip",
        "dialogue_advance",
    };

    private KeyMapping cachedMapping;
    private long lastResolveMs;

    public KeyMapping getSkipKeyMapping() {
        long now = System.currentTimeMillis();
        if (cachedMapping != null && now - lastResolveMs < 5000) {
            return cachedMapping;
        }

        cachedMapping = resolveSkipKeyMapping();
        lastResolveMs = now;
        return cachedMapping;
    }

    public void invalidateCache() {
        cachedMapping = null;
        lastResolveMs = 0;
    }

    private static KeyMapping resolveSkipKeyMapping() {
        KeyMapping fallback = McUtils.options().keyShift;

        for (KeyMapping mapping : McUtils.options().keyMappings) {
            if (mapping == fallback) continue;
            if (isModKeybind(mapping)) continue;
            if (isUnbound(mapping)) continue;

            if (matchesDialogueSkip(mapping)) {
                return mapping;
            }
        }

        return fallback;
    }

    private static boolean isModKeybind(KeyMapping mapping) {
        return mapping.getName().equals(MOD_TOGGLE_KEY);
    }

    private static boolean matchesDialogueSkip(KeyMapping mapping) {
        String translatedName = mapping.getTranslatedKeyMessage().getString().toLowerCase(Locale.ROOT);
        return containsDialogueHint(translatedName);
    }

    private static boolean containsDialogueHint(String value) {
        for (String hint : KEYBIND_HINTS) {
            String compactHint = hint.replace(" ", "");
            if (value.contains(hint) || value.contains(compactHint)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isUnbound(KeyMapping mapping) {
        return mapping.getTranslatedKeyMessage().getString().equals(I18n.get(UNBOUND_KEY));
    }
}

package com.autoskipdialogue;

import com.autoskipdialogue.config.AutoSkipDialogueConfig;
import com.wynntils.core.WynntilsMod;

final class WynntilsIntegration {
    private WynntilsIntegration() {}

    static void init(AutoSkipDialogueConfig config) {
        DialogueSkipHandler handler = new DialogueSkipHandler(config);
        WynntilsMod.registerEventListener(handler);
    }
}

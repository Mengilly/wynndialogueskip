package com.autoskipdialogue.config;

import com.autoskipdialogue.AutoSkipDialogueMod;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

public final class AutoSkipDialogueConfigManager {
    private static AutoSkipDialogueConfig config;

    private AutoSkipDialogueConfigManager() {}

    public static void init() {
        if (config != null) {
            return;
        }

        AutoConfig.register(
                AutoSkipDialogueConfig.class,
                GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(AutoSkipDialogueConfig.class).getConfig();
        AutoSkipDialogueMod.LOGGER.info("Loaded Auto Skip NPC Dialogue config.");
    }

    public static AutoSkipDialogueConfig get() {
        return config;
    }

    public static void save() {
        AutoConfig.getConfigHolder(AutoSkipDialogueConfig.class).save();
    }
}

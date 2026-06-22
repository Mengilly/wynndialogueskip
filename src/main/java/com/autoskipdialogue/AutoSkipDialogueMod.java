package com.autoskipdialogue;

import com.autoskipdialogue.config.AutoSkipDialogueConfig;
import com.autoskipdialogue.config.AutoSkipDialogueConfigManager;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AutoSkipDialogueMod implements ClientModInitializer {
    public static final String MOD_ID = "autoskipdialogue";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @Override
    public void onInitializeClient() {
        ModToggleKeybind.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (this.initialized.get()) {
                return;
            }

            if (!FabricLoader.getInstance().isModLoaded(WynntilsCompatibility.getWynntilsModId())) {
                return;
            }

            if (!WynntilsCompatibility.isSupported()) {
                return;
            }

            AutoSkipDialogueConfigManager.init();

            try {
                Class<?> integrationClass = Class.forName(
                        "com.autoskipdialogue.WynntilsIntegration",
                        false,
                        AutoSkipDialogueMod.class.getClassLoader());
                Method initMethod = integrationClass.getDeclaredMethod("init", AutoSkipDialogueConfig.class);
                initMethod.invoke(null, AutoSkipDialogueConfigManager.get());
            } catch (ReflectiveOperationException exception) {
                LOGGER.error("Failed to initialize Auto Skip NPC Dialogue with Wynntils.", exception);
                return;
            }

            this.initialized.set(true);
            LOGGER.info("Auto Skip NPC Dialogue initialized.");
        });
    }
}

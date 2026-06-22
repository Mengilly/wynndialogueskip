package com.autoskipdialogue;

import com.autoskipdialogue.config.AutoSkipDialogueConfig;
import com.autoskipdialogue.config.AutoSkipDialogueConfigManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ModToggleKeybind {
    private static final long TOAST_DISPLAY_MS = 3000L;
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("wynndialogueskip", "general"));

    private static KeyMapping toggleKey;

    private ModToggleKeybind() {}

    public static void register() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.wynndialogueskip.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                toggleMod(client);
            }
        });
    }

    private static void toggleMod(Minecraft client) {
        AutoSkipDialogueConfigManager.init();
        AutoSkipDialogueConfig config = AutoSkipDialogueConfigManager.get();
        if (config == null) {
            return;
        }

        config.enabled = !config.enabled;
        AutoSkipDialogueConfigManager.save();

        Component title = Component.translatable("text.autoconfig.autoskipdialogue.title");
        Component message = Component.translatable(
                        config.enabled ? "autoskipdialogue.toggle.enabled" : "autoskipdialogue.toggle.disabled")
                .withStyle(config.enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
        client.getToastManager()
                .addToast(new SystemToast(new SystemToast.SystemToastId(TOAST_DISPLAY_MS), title, message));
    }
}

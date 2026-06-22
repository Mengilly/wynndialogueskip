package com.autoskipdialogue;

public final class WynntilsCompatibility {
    private static final String WYNNTILS_MOD_ID = "wynntils";
    private static final String[] REQUIRED_CLASSES = {
        "com.wynntils.core.WynntilsMod",
        "com.wynntils.core.text.StyledText",
        "com.wynntils.core.components.Models",
        "com.wynntils.core.components.Managers",
        "com.wynntils.core.components.Services",
        "com.wynntils.mc.event.TickEvent",
        "com.wynntils.mc.event.SystemMessageEvent",
        "com.wynntils.utils.mc.McUtils",
    };

    private WynntilsCompatibility() {}

    public static String getWynntilsModId() {
        return WYNNTILS_MOD_ID;
    }

    public static boolean isSupported() {
        for (String className : REQUIRED_CLASSES) {
            if (!isClassPresent(className)) {
                return false;
            }
        }

        return true;
    }

    public static String getMissingRequirementMessage() {
        for (String className : REQUIRED_CLASSES) {
            if (!isClassPresent(className)) {
                return "Auto Skip NPC Dialogue requires Wynntils "
                        + "(missing " + className + "). Install or update Wynntils and try again.";
            }
        }

        return "Auto Skip NPC Dialogue requires Wynntils to be installed.";
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, WynntilsCompatibility.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}

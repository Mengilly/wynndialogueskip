package com.autoskipdialogue.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "autoskipdialogue")
public class AutoSkipDialogueConfig implements ConfigData {
    @Comment("Automatically press your dialogue skip key when NPC dialogue can be advanced.")
    public boolean enabled = true;

    @Comment("How long to wait before skipping dialogue. Lower values skip faster.")
    @ConfigEntry.Gui.TransitiveObject
    @ConfigEntry.BoundedDiscrete(min = 50, max = 2000)
    public int skipDelayMs = 400;

    @Comment("Skip dialogue while text is still animating instead of waiting for it to finish.")
    public boolean skipDuringAnimation = false;
}

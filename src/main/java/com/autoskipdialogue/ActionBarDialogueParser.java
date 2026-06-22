package com.autoskipdialogue;

import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.core.text.type.StyleType;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

public final class ActionBarDialogueParser {
    private static final String DIALOGUE_FONT_PREFIX = "hud/dialogue/";
    private static final String BODY_FONT_MARKER = "/body_";
    private static final String CHOICE_FONT_MARKER = "/choice";
    private static final String CONTROL_FONT = "hud/dialogue/text/control";
    private static final String FADE_FONT_PREFIX = "hud/dialogue/effect/fade";
    private static final char POSITION_MARKER = '\uDAFF';

    private static final String WYNNTILS_MATCHER_CLASS =
            "com.wynntils.models.dialogue.actionbar.matchers.DialogueSegmentMatcher";

    private ActionBarDialogueParser() {}

    public static Optional<NpcDialogueSnapshot> parse(StyledText actionBar) {
        if (actionBar == null || actionBar.isEmpty()) {
            return Optional.empty();
        }

        Optional<NpcDialogueSnapshot> fromWynntils = parseWithWynntilsMatcher(actionBar);
        if (fromWynntils.isPresent()) {
            return fromWynntils;
        }

        return parseLocally(actionBar);
    }

    private static Optional<NpcDialogueSnapshot> parseWithWynntilsMatcher(StyledText actionBar) {
        try {
            Class<?> matcherClass = Class.forName(WYNNTILS_MATCHER_CLASS);
            Object matcher = matcherClass.getConstructor().newInstance();
            Method parseMethod = matcherClass.getMethod("parse", StyledText.class);
            Object segment = parseMethod.invoke(matcher, actionBar);
            if (segment == null) {
                return Optional.empty();
            }

            Class<?> segmentClass = segment.getClass();
            String dialogueText =
                    (String) segmentClass.getMethod("getDialogueText").invoke(segment);
            boolean requiresShift =
                    (boolean) segmentClass.getMethod("requiresShift").invoke(segment);
            boolean hasChoices = (boolean) segmentClass.getMethod("hasChoices").invoke(segment);
            return Optional.of(new NpcDialogueSnapshot(dialogueText, requiresShift, hasChoices));
        } catch (ClassNotFoundException ignored) {
            return Optional.empty();
        } catch (ReflectiveOperationException exception) {
            AutoSkipDialogueMod.LOGGER.warn(
                    "Failed to parse dialogue with Wynntils matcher; falling back to local parser.",
                    exception);
            return Optional.empty();
        }
    }

    private static Optional<NpcDialogueSnapshot> parseLocally(StyledText actionBar) {
        List<StyledTextPart> dialogueParts = new LinkedList<>();
        StringBuilder dialogueTextBuilder = new StringBuilder();
        boolean requiresShift = false;
        boolean hasChoices = false;

        for (StyledTextPart part : actionBar) {
            FontDescription font = part.getPartStyle().getFont();
            if (!(font instanceof FontDescription.Resource(Identifier id))) {
                continue;
            }

            String path = id.getPath();
            if (!path.startsWith(DIALOGUE_FONT_PREFIX) || path.startsWith(FADE_FONT_PREFIX)) {
                continue;
            }

            dialogueParts.add(part);

            if (path.equals(CONTROL_FONT)) {
                requiresShift = true;
            }
            if (path.contains(CHOICE_FONT_MARKER)) {
                hasChoices = true;
            }
            if (path.contains(BODY_FONT_MARKER)) {
                dialogueTextBuilder.append(part.getString(null, StyleType.NONE));
            }
        }

        if (dialogueParts.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new NpcDialogueSnapshot(
                cleanDialogueText(dialogueTextBuilder.toString()), requiresShift, hasChoices));
    }

    private static String cleanDialogueText(String text) {
        StringBuilder output = new StringBuilder();
        boolean skipNext = false;
        char lastChar = 0;

        for (char character : text.trim().toCharArray()) {
            if (skipNext) {
                skipNext = false;
                continue;
            }
            if (character == POSITION_MARKER) {
                if (lastChar != ' ') {
                    output.append(' ');
                    lastChar = ' ';
                }
                skipNext = true;
                continue;
            }
            if (character == ' ' && lastChar == ' ') {
                continue;
            }
            lastChar = character;
            output.append(character);
        }

        return output.toString().trim();
    }
}

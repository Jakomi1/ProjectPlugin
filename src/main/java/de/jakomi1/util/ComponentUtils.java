package de.jakomi1.util;

import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ComponentUtils {

    private static final Key ALT_FONT = Key.key("minecraft", "alt");

    private static final Pattern TAG_PATTERN = Pattern.compile(
            "(?i)<(/)?\\s*([a-z0-9_#]+)(?:\\s*(?:=|:)\\s*([^>]+?))?\\s*(/?)>"
    );

    private static final Map<String, TextColor> COLOR_NAME_MAP = createColorNameMap();

    private static final PlainTextComponentSerializer PLAIN =
            PlainTextComponentSerializer.plainText();

    private ComponentUtils() {
    }

    public static Component createBasicGUIComponent(String text) {
        return createGradientComponent(text, "#E6A800", "#E6C35A");
    }

    public static Component createComponent(String text) {
        return createComponent(text, NamedTextColor.GRAY, false);
    }

    public static Component createComponent(String text, NamedTextColor color) {
        return createComponent(text, color, false);
    }

    public static Component createComponent(String text, TextColor color) {
        return createComponent(text, color, false);
    }

    public static Component createComponent(String text, NamedTextColor color, boolean bold) {
        return createComponent(text, (TextColor) color, bold);
    }

    public static Component createComponent(String text, TextColor color, boolean bold) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        Component component = Component.text(text);

        if (color != null) {
            component = component.color(color);
        }

        return component
                .decoration(TextDecoration.BOLD, bold)
                .decoration(TextDecoration.ITALIC, false);
    }

    public static Component createGradientComponent(String text, String startHex, String endHex) {
        return createGradientComponent(text, startHex, endHex, false);
    }

    public static Component createGradientComponent(
            String text,
            String startHex,
            String endHex,
            boolean bold
    ) {
        return createGradientComponent(text, List.of(startHex, endHex), bold);
    }

    public static Component createGradientComponent(String text, String[] hexColors, boolean bold) {
        if (hexColors == null) {
            return createGradientComponent(text, List.of(), bold);
        }

        return createGradientComponent(text, Arrays.asList(hexColors), bold);
    }

    public static Component createGradientComponent(String text, List<String> hexColors) {
        return createGradientComponent(text, hexColors, false);
    }

    public static Component createGradientComponent(
            String text,
            List<String> hexColors,
            boolean bold
    ) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        List<TextColor> colors = new ArrayList<>();

        if (hexColors != null) {
            for (String hex : hexColors) {
                TextColor color = parseColor(hex);

                if (color != null) {
                    colors.add(color);
                }
            }
        }

        if (colors.isEmpty()) {
            return createComponent(text, NamedTextColor.GRAY, bold);
        }

        return createGradientStyledComponent(text, colors, bold, null);
    }

    private static Component createGradientStyledComponent(
            String text,
            List<TextColor> colors,
            boolean bold,
            Key font
    ) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        if (colors == null || colors.isEmpty()) {
            return createComponent(text, NamedTextColor.GRAY, bold);
        }

        int length = text.codePointCount(0, text.length());

        if (length == 0) {
            return Component.empty();
        }

        Component result = Component.empty();

        for (int i = 0; i < length; i++) {
            double ratio = length <= 1
                    ? 0.0
                    : (double) i / (length - 1);

            TextColor color = interpolateColors(colors, ratio);

            int codePoint = text.codePointAt(text.offsetByCodePoints(0, i));

            Component character = styledChar(
                    new String(Character.toChars(codePoint)),
                    color,
                    bold,
                    font
            );

            result = result.append(character);
        }

        return result;
    }

    private static Component styledChar(
            String text,
            TextColor color,
            boolean bold,
            Key font
    ) {
        Component component = Component.text(text);

        if (color != null) {
            component = component.color(color);
        }

        component = component
                .decoration(TextDecoration.BOLD, bold)
                .decoration(TextDecoration.ITALIC, false);

        if (font != null) {
            component = component.style(
                    component.style().font(font)
            );
        }

        return component;
    }

    private static TextColor interpolateColors(
            List<TextColor> colors,
            double ratio
    ) {
        if (colors == null || colors.isEmpty()) {
            return NamedTextColor.GRAY;
        }

        if (colors.size() == 1) {
            return colors.get(0);
        }

        double scaled = ratio * (colors.size() - 1);

        int segment = (int) Math.floor(scaled);

        if (segment < 0) {
            segment = 0;
        }

        if (segment > colors.size() - 2) {
            segment = colors.size() - 2;
        }

        double localRatio = scaled - segment;

        TextColor start = colors.get(segment);
        TextColor end = colors.get(segment + 1);

        if (start == null) {
            return end != null ? end : NamedTextColor.GRAY;
        }

        if (end == null) {
            return start;
        }

        int red = clamp((int) Math.round(
                start.red() + (end.red() - start.red()) * localRatio
        ));

        int green = clamp((int) Math.round(
                start.green() + (end.green() - start.green()) * localRatio
        ));

        int blue = clamp((int) Math.round(
                start.blue() + (end.blue() - start.blue()) * localRatio
        ));

        return TextColor.color(red, green, blue);
    }

    public static TextColor parseColor(String input) {
        String hex = normalizeHex(input);

        if (hex == null) {
            return null;
        }

        try {
            return TextColor.fromHexString(hex);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String normalizeHex(String input) {
        if (input == null) {
            return null;
        }

        String hex = input.trim();

        if (hex.isEmpty()) {
            return null;
        }

        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }

        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }

        if (hex.length() == 3) {
            hex = ""
                    + hex.charAt(0) + hex.charAt(0)
                    + hex.charAt(1) + hex.charAt(1)
                    + hex.charAt(2) + hex.charAt(2);
        }

        if (hex.length() != 6) {
            return null;
        }

        if (!hex.matches("[0-9a-fA-F]{6}")) {
            return null;
        }

        return "#" + hex;
    }

    public static Component createMinecraftSubtleGradient(
            String text,
            TextColor baseColor
    ) {
        return createMinecraftSubtleGradient(
                text,
                baseColor,
                0.75,
                1.25,
                true
        );
    }

    public static Component createMinecraftSubtleGradient(
            String text,
            TextColor baseColor,
            boolean bold
    ) {
        return createMinecraftSubtleGradient(
                text,
                baseColor,
                0.75,
                1.25,
                bold
        );
    }

    public static Component createMinecraftSubtleGradient(
            String text,
            TextColor baseColor,
            double darkFactor,
            double lightFactor
    ) {
        return createMinecraftSubtleGradient(
                text,
                baseColor,
                darkFactor,
                lightFactor,
                true
        );
    }

    public static Component createMinecraftSubtleGradient(
            String text,
            TextColor baseColor,
            double darkFactor,
            double lightFactor,
            boolean bold
    ) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        if (baseColor == null) {
            baseColor = NamedTextColor.GRAY;
        }

        int red = baseColor.red();
        int green = baseColor.green();
        int blue = baseColor.blue();

        TextColor dark = TextColor.color(
                clamp((int) (red * darkFactor)),
                clamp((int) (green * darkFactor)),
                clamp((int) (blue * darkFactor))
        );

        TextColor light = TextColor.color(
                clamp((int) (red * lightFactor)),
                clamp((int) (green * lightFactor)),
                clamp((int) (blue * lightFactor))
        );

        return createGradientStyledComponent(
                text,
                List.of(dark, light),
                bold,
                null
        );
    }

    public static Component makeAltFont(Component component) {
        if (component == null) {
            return Component.empty();
        }

        return component.style(
                Style.style().font(ALT_FONT)
        );
    }

    public static Component createItemComponent(Material material) {
        if (material == null || !material.isItem()) {
            return Component.empty();
        }

        String name = material.name().toLowerCase(Locale.ROOT);

        JsonObject json = new JsonObject();
        json.addProperty("object", "atlas");
        json.addProperty("atlas", "minecraft:items");
        json.addProperty("sprite", "item/" + name);

        return GsonComponentSerializer.gson().deserialize(
                json.toString()
        );
    }

    public static Component createBlockComponent(Material material) {
        if (material == null || !material.isBlock()) {
            return Component.empty();
        }

        String name = material.name().toLowerCase(Locale.ROOT);

        JsonObject json = new JsonObject();
        json.addProperty("object", "atlas");
        json.addProperty("atlas", "minecraft:blocks");
        json.addProperty("sprite", "block/" + name);

        return GsonComponentSerializer.gson().deserialize(
                json.toString()
        );
    }

    public static String plainText(Component component) {
        if (component == null) {
            return "";
        }

        String result = PLAIN.serialize(component);

        if (result.isEmpty()) {
            return result;
        }

        if (result.charAt(0) == '[') {
            result = result.substring(1);
        }

        int lastChar = result.length() - 1;

        if (lastChar >= 0 && result.charAt(lastChar) == ']') {
            result = result.substring(0, lastChar);
        }

        return result;
    }

    public static List<Component> createMarkupLines(String input) {
        return createMarkupLines(input, "#ffffff");
    }

    public static List<Component> createMarkupLines(
            String input,
            NamedTextColor defaultColor
    ) {
        if (input == null) {
            return List.of(Component.empty());
        }

        if (defaultColor == null) {
            defaultColor = NamedTextColor.GRAY;
        }

        return parseMarkupLinesWithAbility(
                input,
                defaultColor,
                defaultColor
        );
    }

    public static List<Component> createMarkupLines(
            String input,
            String hex
    ) {
        if (input == null) {
            return List.of(Component.empty());
        }

        TextColor color = parseColor(hex);

        if (color == null) {
            color = NamedTextColor.WHITE;
        }

        return parseMarkupLinesWithAbility(
                input,
                NamedTextColor.GRAY,
                color
        );
    }

    public static List<Component> createMarkupLines(
            String input,
            Component displayName
    ) {
        return createMarkupLines(
                input,
                getMiddleGradientHex(displayName)
        );
    }

    public static Component createMarkupLine(String input) {
        return createMarkupLine(input, NamedTextColor.WHITE);
    }

    public static Component createMarkupLine(
            String input,
            NamedTextColor defaultColor
    ) {
        if (input == null) {
            return Component.empty();
        }

        List<Component> lines = createMarkupLines(
                input,
                defaultColor
        );

        return appendLines(lines);
    }

    public static Component createMarkupLine(
            String input,
            String hex
    ) {
        if (input == null) {
            return Component.empty();
        }

        return appendLines(
                createMarkupLines(input, hex)
        );
    }

    public static Component createMarkupLine(
            String input,
            Component displayName
    ) {
        if (input == null) {
            return Component.empty();
        }

        return appendLines(
                createMarkupLines(input, displayName)
        );
    }

    private static Component appendLines(List<Component> lines) {
        Component result = Component.empty();

        for (int i = 0; i < lines.size(); i++) {
            result = result.append(lines.get(i));

            if (i < lines.size() - 1) {
                result = result.appendNewline();
            }
        }

        return result;
    }

    private static List<Component> parseMarkupLinesWithAbility(
            String input,
            TextColor defaultTextColor,
            TextColor colorTagDefault
    ) {
        if (input == null) {
            return List.of(Component.empty());
        }

        final String token = "<ability>";

        if (!input.contains(token)) {
            return parseMarkupLinesNoAbility(
                    input,
                    defaultTextColor,
                    colorTagDefault
            );
        }

        List<Component> result = new ArrayList<>();
        String remaining = input;

        while (true) {
            int index = remaining.indexOf(token);

            if (index == -1) {
                result.addAll(
                        parseMarkupLinesNoAbility(
                                remaining,
                                defaultTextColor,
                                colorTagDefault
                        )
                );

                break;
            }

            String before = remaining.substring(0, index);

            if (!before.isEmpty()) {
                result.addAll(
                        parseMarkupLinesNoAbility(
                                before,
                                defaultTextColor,
                                colorTagDefault
                        )
                );
            }

            result.addAll(
                    parseMarkupLinesNoAbility(
                            getSingleAbilityPrefix(),
                            defaultTextColor,
                            colorTagDefault
                    )
            );

            remaining = remaining.substring(
                    index + token.length()
            );
        }

        return result;
    }

    private static List<Component> parseMarkupLinesNoAbility(
            String input,
            TextColor defaultTextColor,
            TextColor colorTagDefault
    ) {
        if (input == null) {
            return List.of(Component.empty());
        }

        TextColor defaultColor = defaultTextColor == null
                ? NamedTextColor.GRAY
                : defaultTextColor;

        List<Component> resultLines = new ArrayList<>();
        Deque<TagState> stack = new ArrayDeque<>();

        Component currentLine = Component.empty();

        Matcher matcher = TAG_PATTERN.matcher(input);
        int lastIndex = 0;

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            if (start > lastIndex) {
                String segment = input.substring(lastIndex, start);

                if (!segment.isEmpty()) {
                    currentLine = currentLine.append(
                            buildStyledComponent(
                                    segment,
                                    stack,
                                    defaultColor,
                                    colorTagDefault
                            )
                    );
                }
            }

            String tagName = matcher.group(2)
                    .toLowerCase(Locale.ROOT);

            String attribute = matcher.group(3);
            String rawTag = input.substring(start, end);

            boolean closing = rawTag.startsWith("</");
            boolean selfClosing = rawTag.endsWith("/>");

            if ("br".equalsIgnoreCase(tagName)) {
                resultLines.add(currentLine);
                currentLine = Component.empty();
            } else if (!closing) {
                stack.push(
                        new TagState(
                                tagName,
                                attribute
                        )
                );

                if (selfClosing) {
                    stack.pop();
                }
            } else {
                int index = findTagIndex(stack, tagName);

                if (index >= 0) {
                    Deque<TagState> temporary = new ArrayDeque<>();

                    for (int i = 0; i < index; i++) {
                        temporary.push(stack.pop());
                    }

                    if (!stack.isEmpty()) {
                        stack.pop();
                    }

                    while (!temporary.isEmpty()) {
                        stack.push(temporary.pop());
                    }
                }
            }

            lastIndex = end;
        }

        if (lastIndex < input.length()) {
            String tail = input.substring(lastIndex);

            if (!tail.isEmpty()) {
                currentLine = currentLine.append(
                        buildStyledComponent(
                                tail,
                                stack,
                                defaultColor,
                                colorTagDefault
                        )
                );
            }
        }

        resultLines.add(currentLine);

        return resultLines;
    }

    private static int findTagIndex(
            Deque<TagState> stack,
            String tagName
    ) {
        int index = 0;

        for (TagState tag : stack) {
            if (tag.name.equalsIgnoreCase(tagName)) {
                return index;
            }

            index++;
        }

        return -1;
    }

    private static Component buildStyledComponent(
            String text,
            Deque<TagState> stack,
            TextColor defaultColor,
            TextColor colorTagDefault
    ) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        TextColor effectiveColor = null;
        List<TextColor> gradient = null;

        boolean bold = false;
        Key font = null;

        for (TagState tag : stack) {
            if (!bold && tag.isBoldTag()) {
                bold = true;
            }

            if (font == null && tag.isAltTag()) {
                font = ALT_FONT;
            }

            if (gradient == null && tag.isGradientTag()) {
                gradient = tag.gradientColors;
            }

            if (effectiveColor == null && gradient == null) {
                TextColor color = tag.resolveColor(colorTagDefault);

                if (color != null) {
                    effectiveColor = color;
                }
            }
        }

        if (gradient != null && !gradient.isEmpty()) {
            return createGradientStyledComponent(
                    text,
                    gradient,
                    bold,
                    font
            );
        }

        TextColor color = effectiveColor != null
                ? effectiveColor
                : defaultColor;

        return styledComponent(
                text,
                color,
                bold,
                font
        );
    }

    private static Component styledComponent(
            String text,
            TextColor color,
            boolean bold,
            Key font
    ) {
        Component component = Component.text(text);

        if (color != null) {
            component = component.color(color);
        }

        component = component
                .decoration(TextDecoration.BOLD, bold)
                .decoration(TextDecoration.ITALIC, false);

        if (font != null) {
            component = component.style(
                    component.style().font(font)
            );
        }

        return component;
    }

    private static List<TextColor> parseGradientColors(String raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }

        List<TextColor> colors = new ArrayList<>(4);
        int start = 0;

        for (int i = 0; i <= raw.length(); i++) {
            boolean end = i == raw.length();
            char character = end ? ':' : raw.charAt(i);

            if (character == ':'
                    || character == ';'
                    || character == ',') {

                if (i > start) {
                    TextColor color = parseColor(
                            raw.substring(start, i)
                    );

                    if (color != null) {
                        colors.add(color);
                    }
                }

                start = i + 1;
            }
        }

        if (colors.isEmpty()) {
            TextColor single = parseColor(raw);

            if (single != null) {
                colors.add(single);
            }
        }

        return Collections.unmodifiableList(colors);
    }

    public static Map.Entry<String, String> extractGradientEndpoints(
            Component component
    ) {
        if (component == null) {
            return new AbstractMap.SimpleEntry<>(
                    "#ffffff",
                    "#ffffff"
            );
        }

        List<TextColor> found = new ArrayList<>();
        Deque<Component> stack = new ArrayDeque<>();

        stack.push(component);

        while (!stack.isEmpty()) {
            Component current = stack.pop();

            TextColor color = current.style().color();

            if (color != null) {
                found.add(color);
            }

            List<Component> children = current.children();

            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
        }

        if (found.isEmpty()) {
            return new AbstractMap.SimpleEntry<>(
                    "#ffffff",
                    "#ffffff"
            );
        }

        return new AbstractMap.SimpleEntry<>(
                toHex(found.get(0)),
                toHex(found.get(found.size() - 1))
        );
    }

    public static String getMiddleGradientHex(Component component) {
        Map.Entry<String, String> endpoints =
                extractGradientEndpoints(component);

        int[] start = parseHexToRgb(endpoints.getKey());
        int[] end = parseHexToRgb(endpoints.getValue());

        int red = (start[0] + end[0]) / 2;
        int green = (start[1] + end[1]) / 2;
        int blue = (start[2] + end[2]) / 2;

        return String.format(
                "#%02x%02x%02x",
                red,
                green,
                blue
        );
    }

    private static int[] parseHexToRgb(String hex) {
        String normalized = normalizeHex(hex);

        if (normalized == null) {
            return new int[]{255, 255, 255};
        }

        try {
            return new int[]{
                    Integer.parseInt(normalized.substring(1, 3), 16),
                    Integer.parseInt(normalized.substring(3, 5), 16),
                    Integer.parseInt(normalized.substring(5, 7), 16)
            };
        } catch (Exception ignored) {
            return new int[]{255, 255, 255};
        }
    }

    private static String toHex(TextColor color) {
        if (color == null) {
            return "#ffffff";
        }

        return String.format(
                "#%02x%02x%02x",
                clamp(color.red()),
                clamp(color.green()),
                clamp(color.blue())
        );
    }

    public static String gradientComponentToMarkupString(
            Component component
    ) {
        if (component == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        traverseComponent(component, builder);

        return builder.toString();
    }

    private static void traverseComponent(
            Component component,
            StringBuilder builder
    ) {
        if (component == null) {
            return;
        }

        TextColor color = component.style().color();

        if (color == null) {
            color = NamedTextColor.WHITE;
        }

        String hex = toHex(color);

        String text;

        if (component instanceof TextComponent textComponent) {
            text = textComponent.content();
        } else {
            text = PLAIN.serialize(component);
        }

        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            String character = new String(
                    Character.toChars(codePoint)
            );

            builder
                    .append("<")
                    .append(hex)
                    .append(">")
                    .append(character)
                    .append("</")
                    .append(hex)
                    .append(">");

            i += Character.charCount(codePoint);
        }

        for (Component child : component.children()) {
            traverseComponent(child, builder);
        }
    }

    public static String getDisplayNameAsMarkup(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return "";
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return "";
        }

        Component displayName = meta.displayName();

        if (displayName == null) {
            return "";
        }

        return gradientComponentToMarkupString(displayName);
    }

    public static String getAbilityPrefix() {
        return getSingleAbilityPrefix();
    }

    public static String getSingleAbilityPrefix() {
        return "<bold>>> Spezielle Fähigkeit <<</bold>";
    }

    public static String getAbilitiesPrefix() {
        return "<bold>>> Spezielle Fähigkeiten <<</bold>";
    }

    public static String getUpgradePrefix() {
        return "<bold>>> Verbesserungskosten <<</bold>";
    }

    public static String getEnchantmentsPrefix() {
        return "<bold>>> Verzauberungen <<</bold>";
    }

    public static String getSingleEnchantmentPrefix() {
        return "<bold>>> Verzauberung <<</bold>";
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static Map<String, TextColor> createColorNameMap() {
        Map<String, TextColor> map = new LinkedHashMap<>();

        map.put("white", NamedTextColor.WHITE);
        map.put("black", NamedTextColor.BLACK);

        map.put("gray", NamedTextColor.GRAY);
        map.put("grey", NamedTextColor.GRAY);

        map.put("dark_gray", NamedTextColor.DARK_GRAY);
        map.put("darkgray", NamedTextColor.DARK_GRAY);
        map.put("darkgrey", NamedTextColor.DARK_GRAY);

        map.put("red", NamedTextColor.RED);
        map.put("dark_red", NamedTextColor.DARK_RED);
        map.put("darkred", NamedTextColor.DARK_RED);

        map.put("green", NamedTextColor.GREEN);
        map.put("dark_green", NamedTextColor.DARK_GREEN);
        map.put("darkgreen", NamedTextColor.DARK_GREEN);

        map.put("blue", NamedTextColor.BLUE);
        map.put("dark_blue", NamedTextColor.DARK_BLUE);
        map.put("darkblue", NamedTextColor.DARK_BLUE);

        map.put("aqua", NamedTextColor.AQUA);
        map.put("dark_aqua", NamedTextColor.DARK_AQUA);
        map.put("darkaqua", NamedTextColor.DARK_AQUA);

        map.put("yellow", NamedTextColor.YELLOW);
        map.put("gold", NamedTextColor.GOLD);

        map.put("purple", NamedTextColor.LIGHT_PURPLE);
        map.put("light_purple", NamedTextColor.LIGHT_PURPLE);
        map.put("pink", NamedTextColor.LIGHT_PURPLE);

        return Collections.unmodifiableMap(map);
    }

    private static final class TagState {

        private final String name;
        private final String attribute;
        private final List<TextColor> gradientColors;

        private TagState(String name, String attribute) {
            this.name = name.toLowerCase(Locale.ROOT);
            this.attribute = attribute == null
                    ? null
                    : stripQuotes(attribute.trim());

            this.gradientColors = "gradient".equalsIgnoreCase(this.name)
                    ? parseGradientColors(this.attribute)
                    : null;
        }

        private boolean isBoldTag() {
            return "bold".equalsIgnoreCase(name);
        }

        private boolean isAltTag() {
            return "alt".equalsIgnoreCase(name);
        }

        private boolean isGradientTag() {
            return "gradient".equalsIgnoreCase(name);
        }

        private TextColor resolveColor(TextColor colorTagDefault) {
            TextColor named = COLOR_NAME_MAP.get(name);

            if (named != null) {
                return named;
            }

            if ("color".equalsIgnoreCase(name)) {
                if (attribute != null && !attribute.isEmpty()) {
                    TextColor parsed = parseColor(attribute);

                    if (parsed != null) {
                        return parsed;
                    }
                }

                return colorTagDefault;
            }

            if (name.startsWith("#")) {
                return parseColor(name);
            }

            return null;
        }

        private static String stripQuotes(String value) {
            if (value == null || value.length() < 2) {
                return value;
            }

            if ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'"))) {

                return value.substring(
                        1,
                        value.length() - 1
                );
            }

            return value;
        }
    }
}
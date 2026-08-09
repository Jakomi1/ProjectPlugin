package de.jakomi1.permission;

import de.jakomi1.util.ComponentUtils;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class Role {

    public enum Source {
        BUILTIN,
        LOCAL,
        SUPABASE
    }

    private final String name;
    private final String display;
    private final String gradient1;
    private final String gradient2;
    private final int priority;
    private final String parentName;
    private final Set<String> permissions;
    private final Source source;

    Role(String name, String display, String gradient1, String gradient2,
         int priority, String parentName, Set<String> permissions, Source source) {
        this.name = name;
        this.display = display;
        this.gradient1 = gradient1;
        this.gradient2 = gradient2;
        this.priority = priority;
        this.parentName = parentName;
        this.permissions = Set.copyOf(permissions);
        this.source = source;
    }

    public String name() {
        return name;
    }

    public String display() {
        return display;
    }

    public String gradient1() {
        return gradient1;
    }

    public String gradient2() {
        return gradient2;
    }

    public int priority() {
        return priority;
    }

    public Source source() {
        return source;
    }

    public Role parent() {
        return parentName == null ? null : RoleRegistry.getDefault().role(parentName);
    }

    public String permission(String prefix) {
        return prefix + "." + name.toLowerCase(Locale.ROOT);
    }

    public Set<String> permissions() {
        return permissions;
    }

    public Component coloredDisplay() {
        return ComponentUtils.createGradientComponent(display, gradient1, gradient2);
    }

    public Set<String> collectPermissions(String prefix) {
        Set<String> result = new LinkedHashSet<>();
        Role current = this;
        while (current != null) {
            result.add(current.permission(prefix));
            result.addAll(current.permissions);
            current = current.parent();
        }
        return result;
    }

    public boolean inherits(Role role) {
        if (role == null) return false;

        Role current = this;
        while (current != null) {
            if (current.name.equalsIgnoreCase(role.name)) return true;
            current = current.parent();
        }
        return false;
    }

    public boolean is(String name) {
        return name != null && this.name.equalsIgnoreCase(name);
    }

    public boolean isMember() {
        return is("MEMBER");
    }

    public boolean isOwner() {
        return is("OWNER");
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Role other && other.name.equalsIgnoreCase(name);
    }

    @Override
    public int hashCode() {
        return name.toLowerCase(Locale.ROOT).hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    public static Role role(String name) {
        return RoleRegistry.getDefault().role(name);
    }

    public static final Role MEMBER = new Role("MEMBER", "Mitglied", "#219752", "#2ecc71", 0, null, Set.of(), Source.BUILTIN);
    public static final Role CONTENT_CREATOR = new Role("CONTENT_CREATOR", "Creator", "#aa2a86", "#f27ba4", 1, "MEMBER", Set.of(), Source.BUILTIN);
    public static final Role BOOSTER = new Role("BOOSTER", "Booster", "#965f7f", "#ffaadc", 2, "MEMBER", Set.of(), Source.BUILTIN);
    public static final Role VIP = new Role("VIP", "VIP", "#beab70", "#fbe7ab", 3, "MEMBER", Set.of(), Source.BUILTIN);
    public static final Role DEVELOPER = new Role("DEVELOPER", "Developer", "#4cadd0", "#b2f9ff", 4, "MEMBER", Set.of(), Source.BUILTIN);
    public static final Role BUILDER = new Role("BUILDER", "Builder", "#6c45b4", "#5d96ff", 5, "MEMBER", Set.of(), Source.BUILTIN);
    public static final Role SUPPORTER = new Role("SUPPORTER", "Supporter", "#2a57e9", "#5B7FEB", 6, "MEMBER", Set.of(), Source.BUILTIN);
    public static final Role MODERATOR = new Role("MODERATOR", "Moderator", "#c25a00", "#ecb83e", 7, "SUPPORTER", Set.of(), Source.BUILTIN);
    public static final Role ADMIN = new Role("ADMIN", "Admin", "#700707", "#ff0000", 8, "MODERATOR", Set.of(), Source.BUILTIN);
    public static final Role OWNER = new Role("OWNER", "Owner", "#c305ff", "#2bd9fd", 9, "ADMIN", Set.of(), Source.BUILTIN);

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static final class Builder {

        private final String name;
        private String display;
        private String gradient1 = "#ffffff";
        private String gradient2 = "#ffffff";
        private int priority = 0;
        private String parent;
        private final Set<String> permissions = new LinkedHashSet<>();
        private Source source = Source.LOCAL;

        private Builder(String name) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Rollenname darf nicht leer sein.");
            }
            this.name = name.toUpperCase(Locale.ROOT);
        }

        public Builder display(String display) {
            this.display = display == null ? name : display;
            return this;
        }

        public Builder gradient1(String hex) {
            this.gradient1 = hex;
            return this;
        }

        public Builder gradient2(String hex) {
            this.gradient2 = hex;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder parent(String parent) {
            this.parent = parent;
            return this;
        }

        public Builder permissions(String... permissions) {
            if (permissions != null) {
                Collections.addAll(this.permissions, permissions);
            }
            return this;
        }

        public Builder source(Source source) {
            this.source = source == null ? Source.LOCAL : source;
            return this;
        }

        public Role build() {
            if (display == null) {
                display = name;
            }
            return new Role(name, display, gradient1, gradient2, priority, parent, permissions, source);
        }
    }
}

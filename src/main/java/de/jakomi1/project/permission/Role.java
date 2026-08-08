package de.jakomi1.project.permission;

import de.jakomi1.util.ComponentUtils;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Eine dynamische Rolle.
 *
 * Rollen sind nicht mehr fest als Enum definiert, sondern werden über die
 * {@link RoleRegistry} verwaltet:
 * <ul>
 *   <li><b>BUILTIN</b> – die Standard-Rollen (Mitglied, Supporter, Owner, ...).</li>
 *   <li><b>LOCAL</b> – projekt-spezifische Rollen, per API registriert
 *       (z.B. {@link RoleManager#registerRole}). Werden nie nach Supabase gepusht
 *       und nie durch Supabase-Updates ueberschrieben.</li>
 *   <li><b>SUPABASE</b> – aus der Supabase-Tabelle {@code roles} gezogen
 *       (nur GET, read-only).</li>
 * </ul>
 */
public final class Role {

    /** Herkunft einer Rollen-Definition. */
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

    /** Die Rolle, von der diese Rolle erbt (Permissions), oder {@code null}. */
    public Role parent() {
        return parentName == null ? null : RoleRegistry.getDefault().role(parentName);
    }

    /** Bukkit-Permission dieser Rolle, z.B. {@code cracked.owner}. */
    public String permission(String prefix) {
        return prefix + "." + name.toLowerCase(Locale.ROOT);
    }

    /** In der Rollen-Definition fest hinterlegte Permissions. */
    public Set<String> permissions() {
        return permissions;
    }

    public Component coloredDisplay() {
        return ComponentUtils.createGradientComponent(display, gradient1, gradient2);
    }

    /**
     * Alle Permissions dieser Rolle inkl. der geerbten Rollen
     * (eigene Role-Permission + feste Permissions + Eltern).
     */
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

    /** Rolle aus der Standard-Registry nach Namen aufloesen. */
    public static Role role(String name) {
        return RoleRegistry.getDefault().role(name);
    }

    // Standard-Rollen (aufgeloest aus der Standard-Registry).
    public static final Role MEMBER = RoleRegistry.getDefault().role("MEMBER");
    public static final Role CONTENT_CREATOR = RoleRegistry.getDefault().role("CONTENT_CREATOR");
    public static final Role BOOSTER = RoleRegistry.getDefault().role("BOOSTER");
    public static final Role VIP = RoleRegistry.getDefault().role("VIP");
    public static final Role DEVELOPER = RoleRegistry.getDefault().role("DEVELOPER");
    public static final Role BUILDER = RoleRegistry.getDefault().role("BUILDER");
    public static final Role SUPPORTER = RoleRegistry.getDefault().role("SUPPORTER");
    public static final Role MODERATOR = RoleRegistry.getDefault().role("MODERATOR");
    public static final Role ADMIN = RoleRegistry.getDefault().role("ADMIN");
    public static final Role OWNER = RoleRegistry.getDefault().role("OWNER");

    public static Builder builder(String name) {
        return new Builder(name);
    }

    /** Builder fuer projekt-spezifische Rollen. */
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

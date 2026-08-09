package de.jakomi1.project.state;

import de.jakomi1.project.permission.Role;
import de.jakomi1.project.permission.RoleManager;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class StateRule {

    public enum Type {
        ALL,
        NONE,
        ROLES
    }

    private final Type type;
    private final Set<String> roles;

    private StateRule(Type type, Set<String> roles) {
        this.type = type;
        this.roles = roles;
    }

    public static StateRule all() {
        return new StateRule(Type.ALL, Set.of());
    }

    public static StateRule none() {
        return new StateRule(Type.NONE, Set.of());
    }

    public static StateRule roles(String... roleNames) {
        Set<String> names = new LinkedHashSet<>();
        if (roleNames != null) {
            for (String name : roleNames) {
                if (name != null && !name.isBlank()) {
                    names.add(name.toUpperCase(Locale.ROOT));
                }
            }
        }
        return names.isEmpty() ? none() : new StateRule(Type.ROLES, names);
    }

    public static StateRule roles(Role... roles) {
        Set<String> names = new LinkedHashSet<>();
        if (roles != null) {
            for (Role role : roles) {
                if (role != null) {
                    names.add(role.name());
                }
            }
        }
        return names.isEmpty() ? none() : new StateRule(Type.ROLES, names);
    }

    public Type type() {
        return type;
    }

    public Set<String> roles() {
        return roles;
    }

    public boolean allows(RoleManager permissions, UUID uuid) {
        return switch (type) {
            case ALL -> true;
            case NONE -> false;
            case ROLES -> hasRole(permissions, uuid);
        };
    }

    private boolean hasRole(RoleManager permissions, UUID uuid) {
        if (permissions == null || uuid == null) return false;

        Role role = permissions.roleOf(uuid);
        for (String name : roles) {
            if (role != null && role.inherits(Role.role(name))) return true;
        }
        return false;
    }
}

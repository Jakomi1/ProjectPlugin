package de.jakomi1.project.nms;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ReflectionUtils {

    private ReflectionUtils() {
    }

    @Nullable
    public static Class<?> findClass(String... names) {
        for (String name : names) {
            if (name == null) continue;

            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {
            }
        }

        return null;
    }

    @Nullable
    public static Method getMethod(Class<?> clazz, String name, Class<?>... params) {
        if (clazz == null) return null;

        try {
            Method method = clazz.getDeclaredMethod(name, params);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException ignored) {
        }

        return null;
    }

    @Nullable
    public static Field getField(Class<?> clazz, String name) {
        if (clazz == null) return null;

        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ignored) {
        }

        return null;
    }

    @Nullable
    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... params) {
        if (clazz == null) return null;

        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor(params);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException ignored) {
        }

        return null;
    }

    @Nullable
    public static Object construct(@Nullable Constructor<?> constructor, Object... args) {
        if (constructor == null) return null;

        try {
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException ignored) {
        }

        return null;
    }

    @Nullable
    public static Object invoke(@Nullable Method method, @Nullable Object target, Object... args) {
        if (method == null) return null;

        try {
            return method.invoke(target, args);
        } catch (ReflectiveOperationException ignored) {
        }

        return null;
    }

    @Nullable
    public static Object invokeStatic(@Nullable Method method, Object... args) {
        return invoke(method, null, args);
    }

    @Nullable
    public static Object get(@Nullable Field field, @Nullable Object target) {
        if (field == null) return null;

        try {
            return field.get(target);
        } catch (IllegalAccessException ignored) {
        }

        return null;
    }

    @Nullable
    public static Object enumConstant(@Nullable Class<?> enumClass, String name) {
        if (enumClass == null || !enumClass.isEnum() || name == null) return null;

        for (Object constant : enumClass.getEnumConstants()) {
            if (((Enum<?>) constant).name().equals(name)) {
                return constant;
            }
        }

        return null;
    }
}

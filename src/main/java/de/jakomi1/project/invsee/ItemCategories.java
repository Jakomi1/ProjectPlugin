/*
 * ItemCategories – Rüstungs-Erkennung als Ersatz für com.destroystokyo.paper.MaterialTags
 * (aus dem Port des "Invsee"-Plugins, at.noahb.invsee, GPL-3.0).
 * Siehe LICENSE.md in diesem Repository.
 */
package de.jakomi1.project.invsee;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class ItemCategories {

    private ItemCategories() {
    }

    public static boolean isArmor(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return false;

        String name = itemStack.getType().name();
        return name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS")
                || name.equals("ELYTRA");
    }

    public static boolean isHelmet(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return false;

        String name = itemStack.getType().name();
        return name.endsWith("_HELMET") || name.equals("TURTLE_HELMET");
    }

    public static boolean isChestplate(ItemStack itemStack) {
        return isType(itemStack, "_CHESTPLATE");
    }

    public static boolean isLeggings(ItemStack itemStack) {
        return isType(itemStack, "_LEGGINGS");
    }

    public static boolean isBoots(ItemStack itemStack) {
        return isType(itemStack, "_BOOTS");
    }

    private static boolean isType(ItemStack itemStack, String suffix) {
        if (itemStack == null || itemStack.isEmpty()) return false;
        return itemStack.getType().name().endsWith(suffix);
    }

    public static boolean isType(Material material, String suffix) {
        return material != null && material.name().endsWith(suffix);
    }
}

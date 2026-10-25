package com.slopgobbler.vaultfinder.automation;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves standard Minecraft item maximum stack limits (e.g. 64, 16 for ender pearls/snowballs, 1 for tools/armor/potions).
 * Decoupled from Minecraft game registry for fast, headless, and deterministic inventory automation calculations.
 */
public final class StackSizeResolver {

    private static final Map<String, Integer> CUSTOM_OVERRIDES = new ConcurrentHashMap<>();

    private StackSizeResolver() {
    }

    /**
     * Registers a custom max stack size override for a given item identifier.
     */
    public static void registerOverride(String itemId, int maxStackSize) {
        if (itemId != null && !itemId.isBlank()) {
            CUSTOM_OVERRIDES.put(normalizeId(itemId), Math.max(1, maxStackSize));
        }
    }

    /**
     * Clears all registered custom stack size overrides.
     */
    public static void clearOverrides() {
        CUSTOM_OVERRIDES.clear();
    }

    /**
     * Resolves the maximum stack size for the given item identifier.
     *
     * @param itemId Namespaced item ID (e.g. "minecraft:ender_pearl", "minecraft:diamond_sword")
     * @return Maximum stack size (1, 16, or 64)
     */
    public static int getMaxStackSize(String itemId) {
        if (itemId == null || itemId.isBlank() || itemId.equals("minecraft:air") || itemId.equals("air")) {
            return 64;
        }

        String id = normalizeId(itemId);
        Integer custom = CUSTOM_OVERRIDES.get(id);
        if (custom != null) {
            return custom;
        }

        if (isUnstackable(id)) {
            return 1;
        }

        if (isStackOf16(id)) {
            return 16;
        }

        return 64;
    }

    private static String normalizeId(String itemId) {
        String trimmed = itemId.trim().toLowerCase(Locale.ROOT);
        return trimmed.startsWith("minecraft:") ? trimmed.substring("minecraft:".length()) : trimmed;
    }

    private static boolean isUnstackable(String id) {
        // Weapons and Tools
        if (id.endsWith("_sword") || id.equals("sword") ||
            id.endsWith("_pickaxe") || id.equals("pickaxe") ||
            id.endsWith("_axe") || id.equals("axe") ||
            id.endsWith("_shovel") || id.equals("shovel") ||
            id.endsWith("_hoe") || id.equals("hoe")) {
            return true;
        }

        // Armor and Wearables
        if (id.endsWith("_helmet") || id.equals("helmet") ||
            id.endsWith("_chestplate") || id.equals("chestplate") ||
            id.endsWith("_leggings") || id.equals("leggings") ||
            id.endsWith("_boots") || id.equals("boots") ||
            id.equals("turtle_helmet") || id.equals("elytra")) {
            return true;
        }

        // Combat gear & utilities
        if (id.equals("shield") || id.equals("trident") || id.equals("bow") ||
            id.equals("crossbow") || id.equals("mace") || id.equals("spyglass") ||
            id.equals("shears") || id.equals("flint_and_steel") || id.equals("fishing_rod") ||
            id.equals("carrot_on_a_stick") || id.equals("warped_fungus_on_a_stick") ||
            id.equals("brush") || id.equals("totem_of_undying") || id.equals("saddle")) {
            return true;
        }

        // Filled buckets (water, lava, milk, powder snow, fish buckets)
        if (id.endsWith("_bucket") && !id.equals("bucket")) {
            return true;
        }

        // Potions and Stews
        if (id.equals("potion") || id.equals("splash_potion") || id.equals("lingering_potion") ||
            id.equals("mushroom_stew") || id.equals("rabbit_stew") || id.equals("beetroot_soup") ||
            id.equals("suspicious_stew") || id.equals("honey_bottle_poisoned")) {
            return true;
        }

        // Shulker boxes & storage containers
        if (id.endsWith("shulker_box") || id.equals("bundle")) {
            return true;
        }

        // Books & Discs & Beds
        if (id.equals("enchanted_book") || id.startsWith("music_disc_") ||
            id.endsWith("_bed") || id.equals("bed") || id.equals("goat_horn")) {
            return true;
        }

        // Horse armor
        if (id.endsWith("_horse_armor")) {
            return true;
        }

        return false;
    }

    private static boolean isStackOf16(String id) {
        if (id.equals("ender_pearl") || id.equals("snowball") || id.equals("egg") ||
            id.equals("honey_bottle") || id.equals("bucket") || id.equals("armor_stand")) {
            return true;
        }

        if (id.endsWith("_sign") || id.endsWith("_hanging_sign") || id.equals("sign") || id.equals("hanging_sign")) {
            return true;
        }

        if (id.endsWith("_banner") || id.equals("banner")) {
            return true;
        }

        if (id.endsWith("_boat") || id.endsWith("_raft") || id.equals("boat") || id.equals("raft") ||
            id.endsWith("_chest_boat") || id.endsWith("_chest_raft")) {
            return true;
        }

        return false;
    }
}

package com.slopgobbler.vaultfinder.model;

import java.util.Locale;

/**
 * Enumeration representing supported container types in VaultFinder Pro.
 */
public enum ContainerType {
    CHEST("chest", "Chest", 27),
    DOUBLE_CHEST("double_chest", "Double Chest", 54),
    SHULKER_BOX("shulker_box", "Shulker Box", 27),
    ENDER_CHEST("ender_chest", "Ender Chest", 27),
    BARREL("barrel", "Barrel", 27),
    HOPPER("hopper", "Hopper", 5),
    DISPENSER("dispenser", "Dispenser", 9),
    DROPPER("dropper", "Dropper", 9),
    CRAFTER("crafter", "Crafter", 9);

    private final String id;
    private final String displayName;
    private final int defaultCapacity;

    ContainerType(String id, String displayName, int defaultCapacity) {
        this.id = id;
        this.displayName = displayName;
        this.defaultCapacity = defaultCapacity;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDefaultCapacity() {
        return defaultCapacity;
    }

    public int getSlotCount() {
        return defaultCapacity;
    }

    public boolean isChest() {
        return this == CHEST || this == DOUBLE_CHEST;
    }

    public boolean isShulker() {
        return this == SHULKER_BOX;
    }

    public boolean isEnderChest() {
        return this == ENDER_CHEST;
    }

    /**
     * Resolves a ContainerType by its identifier or enum name (case-insensitive).
     *
     * @param identifier ID or enum constant name
     * @return Matching ContainerType, or null if unknown
     */
    public static ContainerType fromId(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        String normalized = identifier.trim().toLowerCase(Locale.ROOT);
        for (ContainerType type : values()) {
            if (type.id.equalsIgnoreCase(normalized) || type.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Identifies container type from a block identifier string (e.g. "minecraft:trapped_chest", "minecraft:cyan_shulker_box").
     *
     * @param blockId Block identifier string
     * @return Resolved ContainerType, or null if not a recognized container block
     */
    public static ContainerType fromBlockId(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return null;
        }
        String lower = blockId.toLowerCase(Locale.ROOT);
        if (lower.contains("shulker_box")) {
            return SHULKER_BOX;
        }
        if (lower.contains("ender_chest")) {
            return ENDER_CHEST;
        }
        if (lower.contains("barrel")) {
            return BARREL;
        }
        if (lower.contains("hopper")) {
            return HOPPER;
        }
        if (lower.contains("dispenser")) {
            return DISPENSER;
        }
        if (lower.contains("dropper")) {
            return DROPPER;
        }
        if (lower.contains("crafter")) {
            return CRAFTER;
        }
        if (lower.contains("double_chest")) {
            return DOUBLE_CHEST;
        }
        if (lower.contains("chest")) {
            return CHEST;
        }
        return null;
    }

    @Override
    public String toString() {
        return id;
    }
}

package com.slopgobbler.vaultfinder.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents an individual item stack recorded in a container slot.
 */
public class ItemEntry {
    private final String itemId;
    private final int count;
    private final String customName;
    private final Map<String, Integer> enchantments;
    private final int durability;
    private final int maxDurability;
    private final int slotIndex;
    private final Set<String> tags;

    public ItemEntry(String itemId,
                     int count,
                     String customName,
                     Map<String, Integer> enchantments,
                     int durability,
                     int maxDurability,
                     int slotIndex,
                     Set<String> tags) {
        this.itemId = itemId != null ? itemId.trim().toLowerCase(Locale.ROOT) : "";
        this.count = Math.max(0, count);
        this.customName = (customName != null && !customName.isBlank()) ? customName.trim() : null;
        this.enchantments = enchantments != null ? Collections.unmodifiableMap(new HashMap<>(enchantments)) : Collections.emptyMap();
        this.durability = Math.max(0, durability);
        this.maxDurability = Math.max(0, maxDurability);
        this.slotIndex = slotIndex;
        this.tags = tags != null ? Collections.unmodifiableSet(new HashSet<>(tags)) : Collections.emptySet();
    }

    public ItemEntry(String itemId,
                     int count,
                     String customName,
                     List<String> enchantments,
                     int durabilityPercent,
                     int slotIndex) {
        this(itemId, count, customName, toEnchantmentMap(enchantments), durabilityPercent, 100, slotIndex, Collections.emptySet());
    }

    private static Map<String, Integer> toEnchantmentMap(List<String> list) {
        if (list == null || list.isEmpty()) return Collections.emptyMap();
        Map<String, Integer> map = new HashMap<>();
        for (String e : list) {
            if (e != null && !e.isBlank()) {
                map.put(e.trim(), 1);
            }
        }
        return map;
    }

    public ItemEntry(String itemId,
                     int count,
                     String customName,
                     Map<String, Integer> enchantments,
                     int durability,
                     int slotIndex) {
        this(itemId, count, customName, enchantments, durability, 0, slotIndex, Collections.emptySet());
    }

    public ItemEntry(String itemId, int count, int slotIndex) {
        this(itemId, count, null, Collections.emptyMap(), 0, 0, slotIndex, Collections.emptySet());
    }

    public String getItemId() {
        return itemId;
    }

    public int getCount() {
        return count;
    }

    public String getCustomName() {
        return customName;
    }

    public Map<String, Integer> getEnchantments() {
        return enchantments;
    }

    public int getDurability() {
        return durability;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public Set<String> getTags() {
        return tags;
    }

    /**
     * Extracts the mod namespace (e.g. "minecraft" from "minecraft:iron_sword").
     */
    public String getModId() {
        int idx = itemId.indexOf(':');
        return idx != -1 ? itemId.substring(0, idx) : "minecraft";
    }

    /**
     * Extracts the item name path (e.g. "iron_sword" from "minecraft:iron_sword").
     */
    public String getItemName() {
        int idx = itemId.indexOf(':');
        return idx != -1 ? itemId.substring(idx + 1) : itemId;
    }

    /**
     * Returns custom display name if set, otherwise formats the item ID into human-readable words.
     */
    public String getReadableName() {
        if (customName != null && !customName.isBlank()) {
            return customName;
        }
        String path = getItemName();
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    public boolean hasEnchantments() {
        return !enchantments.isEmpty();
    }

    public boolean hasEnchantment(String nameOrId) {
        if (nameOrId == null || nameOrId.isBlank() || enchantments.isEmpty()) {
            return false;
        }
        String query = nameOrId.trim().toLowerCase(Locale.ROOT);
        for (String key : enchantments.keySet()) {
            String lowerKey = key.toLowerCase(Locale.ROOT);
            if (lowerKey.equals(query) || lowerKey.endsWith(":" + query)) {
                return true;
            }
        }
        return false;
    }

    public int getEnchantmentLevel(String nameOrId) {
        if (nameOrId == null || nameOrId.isBlank() || enchantments.isEmpty()) {
            return 0;
        }
        String query = nameOrId.trim().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
            String lowerKey = entry.getKey().toLowerCase(Locale.ROOT);
            if (lowerKey.equals(query) || lowerKey.endsWith(":" + query)) {
                return entry.getValue() != null ? entry.getValue() : 0;
            }
        }
        return 0;
    }

    public boolean isDamaged() {
        return maxDurability > 0 && durability < maxDurability;
    }

    public double getDurabilityRatio() {
        if (maxDurability <= 0) {
            return 1.0D;
        }
        return Math.clamp((double) durability / (double) maxDurability, 0.0D, 1.0D);
    }

    public int getDurabilityPercent() {
        return (int) Math.round(getDurabilityRatio() * 100.0D);
    }

    public boolean matches(com.slopgobbler.vaultfinder.query.ItemQuery query) {
        return query != null && query.test(this);
    }

    public boolean matches(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String q = query.trim();
        if (q.startsWith("#")) {
            return matchesTag(q);
        }
        if (q.startsWith("@")) {
            return matchesMod(q);
        }
        return matchesKeyword(q);
    }

    public boolean matchesKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String kw = keyword.trim().toLowerCase(Locale.ROOT);
        if (itemId.contains(kw)) {
            return true;
        }
        if (customName != null && customName.toLowerCase(Locale.ROOT).contains(kw)) {
            return true;
        }
        if (getReadableName().toLowerCase(Locale.ROOT).contains(kw)) {
            return true;
        }
        for (String ench : enchantments.keySet()) {
            if (ench.toLowerCase(Locale.ROOT).contains(kw)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return true;
        }
        String targetTag = tag.startsWith("#") ? tag.substring(1) : tag;
        targetTag = targetTag.trim().toLowerCase(Locale.ROOT);
        for (String t : tags) {
            String lowerT = t.startsWith("#") ? t.substring(1).toLowerCase(Locale.ROOT) : t.toLowerCase(Locale.ROOT);
            if (lowerT.equals(targetTag) || lowerT.endsWith(":" + targetTag)) {
                return true;
            }
        }
        if (hasEnchantment(targetTag)) {
            return true;
        }
        return false;
    }

    public boolean matchesMod(String modId) {
        if (modId == null || modId.isBlank()) {
            return true;
        }
        String targetMod = modId.startsWith("@") ? modId.substring(1) : modId;
        return getModId().equalsIgnoreCase(targetMod.trim());
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("itemId", itemId);
        obj.addProperty("count", count);
        obj.addProperty("slotIndex", slotIndex);

        if (customName != null) {
            obj.addProperty("customName", customName);
        }
        if (durability > 0) {
            obj.addProperty("durability", durability);
        }
        if (maxDurability > 0) {
            obj.addProperty("maxDurability", maxDurability);
        }

        if (!enchantments.isEmpty()) {
            JsonObject enchObj = new JsonObject();
            for (Map.Entry<String, Integer> e : enchantments.entrySet()) {
                enchObj.addProperty(e.getKey(), e.getValue());
            }
            obj.add("enchantments", enchObj);
        }

        if (!tags.isEmpty()) {
            JsonArray tagArray = new JsonArray();
            for (String tag : tags) {
                tagArray.add(new JsonPrimitive(tag));
            }
            obj.add("tags", tagArray);
        }

        return obj;
    }

    public static ItemEntry fromJson(JsonObject json) {
        if (json == null) {
            return null;
        }
        String itemId = json.has("itemId") ? json.get("itemId").getAsString() : "";
        int count = json.has("count") ? json.get("count").getAsInt() : 1;
        int slotIndex = json.has("slotIndex") ? json.get("slotIndex").getAsInt() : 0;
        String customName = json.has("customName") && !json.get("customName").isJsonNull()
                ? json.get("customName").getAsString() : null;
        int durability = json.has("durability") ? json.get("durability").getAsInt() : 0;
        int maxDurability = json.has("maxDurability") ? json.get("maxDurability").getAsInt() : 0;

        Map<String, Integer> enchantments = new HashMap<>();
        if (json.has("enchantments") && json.get("enchantments").isJsonObject()) {
            JsonObject enchObj = json.getAsJsonObject("enchantments");
            for (String key : enchObj.keySet()) {
                enchantments.put(key, enchObj.get(key).getAsInt());
            }
        }

        Set<String> tags = new HashSet<>();
        if (json.has("tags") && json.get("tags").isJsonArray()) {
            JsonArray tagArray = json.getAsJsonArray("tags");
            for (JsonElement el : tagArray) {
                tags.add(el.getAsString());
            }
        }

        return new ItemEntry(itemId, count, customName, enchantments, durability, maxDurability, slotIndex, tags);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemEntry that)) return false;
        return count == that.count &&
                durability == that.durability &&
                maxDurability == that.maxDurability &&
                slotIndex == that.slotIndex &&
                Objects.equals(itemId, that.itemId) &&
                Objects.equals(customName, that.customName) &&
                Objects.equals(enchantments, that.enchantments) &&
                Objects.equals(tags, that.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, count, customName, enchantments, durability, maxDurability, slotIndex, tags);
    }

    @Override
    public String toString() {
        return "ItemEntry{" +
                "itemId='" + itemId + '\'' +
                ", count=" + count +
                ", customName='" + customName + '\'' +
                ", slot=" + slotIndex +
                (durability > 0 ? ", dur=" + durability + "/" + maxDurability : "") +
                (!enchantments.isEmpty() ? ", ench=" + enchantments : "") +
                '}';
    }
}

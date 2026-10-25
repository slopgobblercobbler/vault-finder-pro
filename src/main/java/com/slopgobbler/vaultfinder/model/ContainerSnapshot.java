package com.slopgobbler.vaultfinder.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.slopgobbler.vaultfinder.query.ItemQuery;
import com.slopgobbler.vaultfinder.query.QueryParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of a container's contents and metadata at a specific world coordinate.
 */
public class ContainerSnapshot {
    private final BlockPos blockPos;
    private final String dimension;
    private final ContainerType type;
    private final List<ItemEntry> items;
    private final long lastVisitedTimestamp;
    private final String customName;

    public ContainerSnapshot(BlockPos blockPos,
                             String dimension,
                             ContainerType type,
                             List<ItemEntry> items,
                             long lastVisitedTimestamp,
                             String customName) {
        this.blockPos = blockPos != null ? blockPos.toImmutable() : BlockPos.ORIGIN;
        this.dimension = dimension != null ? dimension : "minecraft:overworld";
        this.type = type != null ? type : ContainerType.CHEST;
        this.items = items != null ? Collections.unmodifiableList(new ArrayList<>(items)) : Collections.emptyList();
        this.lastVisitedTimestamp = lastVisitedTimestamp > 0 ? lastVisitedTimestamp : System.currentTimeMillis();
        this.customName = (customName != null && !customName.isBlank()) ? customName.trim() : null;
    }

    public ContainerSnapshot(BlockPos blockPos,
                             String dimension,
                             ContainerType type,
                             List<ItemEntry> items,
                             long lastVisitedTimestamp) {
        this(blockPos, dimension, type, items, lastVisitedTimestamp, null);
    }

    public ContainerSnapshot(int x,
                             int y,
                             int z,
                             String dimension,
                             ContainerType type,
                             List<ItemEntry> items,
                             long lastVisitedTimestamp) {
        this(new BlockPos(x, y, z), dimension, type, items, lastVisitedTimestamp, null);
    }

    public ContainerSnapshot(String dimension,
                             int x,
                             int y,
                             int z,
                             ContainerType type,
                             List<ItemEntry> items,
                             long lastVisitedTimestamp) {
        this(new BlockPos(x, y, z), dimension, type, items, lastVisitedTimestamp, null);
    }

    public String getId() {
        return dimension + ":" + getX() + "," + getY() + "," + getZ();
    }

    public double distanceSquared(int targetX, int targetY, int targetZ) {
        return distanceSq(targetX, targetY, targetZ);
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public int getX() {
        return blockPos.getX();
    }

    public int getY() {
        return blockPos.getY();
    }

    public int getZ() {
        return blockPos.getZ();
    }

    public String getDimension() {
        return dimension;
    }

    public ContainerType getType() {
        return type;
    }

    public List<ItemEntry> getItems() {
        return items;
    }

    public long getLastVisitedTimestamp() {
        return lastVisitedTimestamp;
    }

    public String getCustomName() {
        return customName;
    }

    public boolean hasCustomName() {
        return customName != null && !customName.isBlank();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int getFilledSlotCount() {
        return items.size();
    }

    public int getTotalItemCount() {
        int total = 0;
        for (ItemEntry item : items) {
            total += item.getCount();
        }
        return total;
    }

    /**
     * Determines whether this container contains any item satisfying the given query.
     */
    public boolean matches(ItemQuery query) {
        if (query == null || query.isEmpty()) {
            return !items.isEmpty();
        }
        for (ItemEntry item : items) {
            if (query.test(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper to match against a raw query string.
     */
    public boolean matches(String rawQuery) {
        return matches(QueryParser.parse(rawQuery));
    }

    /**
     * Finds and returns all items in this container matching the given query.
     */
    public List<ItemEntry> findMatchingItems(ItemQuery query) {
        if (query == null || query.isEmpty()) {
            return new ArrayList<>(items);
        }
        List<ItemEntry> matches = new ArrayList<>();
        for (ItemEntry item : items) {
            if (query.test(item)) {
                matches.add(item);
            }
        }
        return matches;
    }

    /**
     * Finds and returns all items matching a raw query string.
     */
    public List<ItemEntry> findMatchingItems(String rawQuery) {
        return findMatchingItems(QueryParser.parse(rawQuery));
    }

    /**
     * Counts the total items (summing stack counts) matching the given query.
     */
    public int countMatchingItems(ItemQuery query) {
        int total = 0;
        for (ItemEntry item : items) {
            if (query == null || query.test(item)) {
                total += item.getCount();
            }
        }
        return total;
    }

    public int countMatchingItems(String rawQuery) {
        return countMatchingItems(QueryParser.parse(rawQuery));
    }

    /**
     * Calculates squared distance from this snapshot to a given BlockPos.
     */
    public double distanceSq(BlockPos other) {
        if (other == null) return Double.MAX_VALUE;
        double dx = getX() - other.getX();
        double dy = getY() - other.getY();
        double dz = getZ() - other.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Calculates Euclidean distance from this snapshot to a given BlockPos.
     */
    public double distanceTo(BlockPos other) {
        return Math.sqrt(distanceSq(other));
    }

    public double distanceSq(double px, double py, double pz) {
        double dx = getX() - px;
        double dy = getY() - py;
        double dz = getZ() - pz;
        return dx * dx + dy * dy + dz * dz;
    }

    public double distanceTo(double px, double py, double pz) {
        return Math.sqrt(distanceSq(px, py, pz));
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        JsonObject posObj = new JsonObject();
        posObj.addProperty("x", getX());
        posObj.addProperty("y", getY());
        posObj.addProperty("z", getZ());
        obj.add("pos", posObj);

        obj.addProperty("dimension", dimension);
        obj.addProperty("type", type.getId());
        obj.addProperty("lastVisitedTimestamp", lastVisitedTimestamp);

        if (customName != null) {
            obj.addProperty("customName", customName);
        }

        JsonArray itemArray = new JsonArray();
        for (ItemEntry item : items) {
            itemArray.add(item.toJson());
        }
        obj.add("items", itemArray);

        return obj;
    }

    public static ContainerSnapshot fromJson(JsonObject json) {
        if (json == null) {
            return null;
        }

        BlockPos pos = BlockPos.ORIGIN;
        if (json.has("pos") && json.get("pos").isJsonObject()) {
            JsonObject p = json.getAsJsonObject("pos");
            int x = p.has("x") ? p.get("x").getAsInt() : 0;
            int y = p.has("y") ? p.get("y").getAsInt() : 0;
            int z = p.has("z") ? p.get("z").getAsInt() : 0;
            pos = new BlockPos(x, y, z);
        }

        String dimension = json.has("dimension") ? json.get("dimension").getAsString() : "minecraft:overworld";
        ContainerType type = ContainerType.CHEST;
        if (json.has("type")) {
            ContainerType parsed = ContainerType.fromId(json.get("type").getAsString());
            if (parsed != null) {
                type = parsed;
            }
        }

        long timestamp = json.has("lastVisitedTimestamp") ? json.get("lastVisitedTimestamp").getAsLong() : System.currentTimeMillis();
        String customName = json.has("customName") && !json.get("customName").isJsonNull()
                ? json.get("customName").getAsString() : null;

        List<ItemEntry> items = new ArrayList<>();
        if (json.has("items") && json.get("items").isJsonArray()) {
            JsonArray arr = json.getAsJsonArray("items");
            for (JsonElement el : arr) {
                if (el.isJsonObject()) {
                    ItemEntry entry = ItemEntry.fromJson(el.getAsJsonObject());
                    if (entry != null) {
                        items.add(entry);
                    }
                }
            }
        }

        return new ContainerSnapshot(pos, dimension, type, items, timestamp, customName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContainerSnapshot that)) return false;
        return Objects.equals(blockPos, that.blockPos) &&
                Objects.equals(dimension, that.dimension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockPos, dimension);
    }

    @Override
    public String toString() {
        return "ContainerSnapshot{" +
                "pos=" + blockPos.toShortString() +
                ", dim='" + dimension + '\'' +
                ", type=" + type +
                ", items=" + items.size() +
                ", customName='" + customName + '\'' +
                '}';
    }
}

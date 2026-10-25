package com.slopgobbler.vaultfinder.tracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.slopgobbler.vaultfinder.model.ContainerSnapshot;
import com.slopgobbler.vaultfinder.model.ItemEntry;
import com.slopgobbler.vaultfinder.query.ItemQuery;
import com.slopgobbler.vaultfinder.query.QueryParser;
import com.slopgobbler.vaultfinder.model.BlockPos;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe in-memory cache and persistence engine for container snapshots.
 * Provides multi-dimensional indexing, spatial radius queries, and structured item search.
 */
public class StorageCache {

    /**
     * Search result entry containing the matched container snapshot and item entries.
     */
    public record SearchResult(
            ContainerSnapshot snapshot,
            List<ItemEntry> matchingItems,
            int totalMatchingCount,
            double distance
    ) {
        public BlockPos getBlockPos() {
            return snapshot.getBlockPos();
        }

        public String getDimension() {
            return snapshot.getDimension();
        }
    }

    /**
     * Listener interface for storage cache lifecycle and mutation events.
     */
    @FunctionalInterface
    public interface StorageChangeListener {
        void onStorageChanged();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Map: dimension -> (BlockPos -> ContainerSnapshot)
    private final Map<String, Map<BlockPos, ContainerSnapshot>> dimensionMap = new ConcurrentHashMap<>();
    private final List<StorageChangeListener> listeners = new CopyOnWriteArrayList<>();

    public StorageCache() {
    }

    /**
     * Stores or updates a container snapshot in the cache.
     *
     * @param snapshot Container snapshot to cache
     */
    public void put(ContainerSnapshot snapshot) {
        if (snapshot == null) return;
        Map<BlockPos, ContainerSnapshot> dimSnapshots = dimensionMap.computeIfAbsent(
                snapshot.getDimension(),
                k -> new ConcurrentHashMap<>()
        );
        dimSnapshots.put(snapshot.getBlockPos(), snapshot);
        notifyListeners();
    }

    /**
     * Retrieves a container snapshot for a dimension and integer coordinates.
     */
    public Optional<ContainerSnapshot> get(String dimension, int x, int y, int z) {
        return get(dimension, new BlockPos(x, y, z));
    }

    /**
     * Retrieves a container snapshot for a dimension and position.
     */
    public Optional<ContainerSnapshot> get(String dimension, BlockPos pos) {
        if (dimension == null || pos == null) return Optional.empty();
        Map<BlockPos, ContainerSnapshot> dimSnapshots = dimensionMap.get(dimension);
        if (dimSnapshots == null) return Optional.empty();
        return Optional.ofNullable(dimSnapshots.get(pos));
    }

    /**
     * Retrieves a container snapshot by its string id (e.g. dimension:x,y,z).
     */
    public ContainerSnapshot get(String id) {
        if (id == null) return null;
        for (Map<BlockPos, ContainerSnapshot> map : dimensionMap.values()) {
            for (ContainerSnapshot snapshot : map.values()) {
                if (id.equals(snapshot.getId())) {
                    return snapshot;
                }
            }
        }
        return null;
    }

    /**
     * Convenience search method matching position coordinates and radius.
     */
    public List<ContainerSnapshot> search(String query, String dimension, int playerX, int playerY, int playerZ, double maxRadius) {
        List<SearchResult> results = searchNearby(dimension, new BlockPos(playerX, playerY, playerZ), maxRadius, query);
        List<ContainerSnapshot> list = new ArrayList<>();
        for (SearchResult r : results) {
            list.add(r.snapshot());
        }
        return list;
    }

    /**
     * Removes a container snapshot from the cache.
     */
    public ContainerSnapshot remove(String dimension, BlockPos pos) {
        if (dimension == null || pos == null) return null;
        Map<BlockPos, ContainerSnapshot> dimSnapshots = dimensionMap.get(dimension);
        if (dimSnapshots == null) return null;
        ContainerSnapshot removed = dimSnapshots.remove(pos);
        if (removed != null) {
            notifyListeners();
        }
        return removed;
    }

    /**
     * Checks if a snapshot exists at the given dimension and position.
     */
    public boolean contains(String dimension, BlockPos pos) {
        if (dimension == null || pos == null) return false;
        Map<BlockPos, ContainerSnapshot> dimSnapshots = dimensionMap.get(dimension);
        return dimSnapshots != null && dimSnapshots.containsKey(pos);
    }

    /**
     * Checks if a snapshot exists at the given dimension and coordinates.
     */
    public boolean contains(String dimension, int x, int y, int z) {
        return contains(dimension, new BlockPos(x, y, z));
    }

    /**
     * Clears all cached containers across all dimensions.
     */
    public void clear() {
        dimensionMap.clear();
        notifyListeners();
    }

    /**
     * Clears all cached containers for a specific dimension.
     */
    public void clearDimension(String dimension) {
        if (dimension == null) return;
        if (dimensionMap.remove(dimension) != null) {
            notifyListeners();
        }
    }

    /**
     * Returns total count of cached containers across all dimensions.
     */
    public int size() {
        int count = 0;
        for (Map<BlockPos, ContainerSnapshot> map : dimensionMap.values()) {
            count += map.size();
        }
        return count;
    }

    /**
     * Returns count of cached containers in a specific dimension.
     */
    public int size(String dimension) {
        if (dimension == null) return 0;
        Map<BlockPos, ContainerSnapshot> map = dimensionMap.get(dimension);
        return map != null ? map.size() : 0;
    }

    /**
     * Returns a set of all known dimension identifiers.
     */
    public Set<String> getDimensions() {
        return Collections.unmodifiableSet(dimensionMap.keySet());
    }

    /**
     * Returns all cached snapshots in a specific dimension.
     */
    public Collection<ContainerSnapshot> getSnapshots(String dimension) {
        if (dimension == null) return Collections.emptyList();
        Map<BlockPos, ContainerSnapshot> map = dimensionMap.get(dimension);
        return map != null ? Collections.unmodifiableCollection(map.values()) : Collections.emptyList();
    }

    /**
     * Returns all cached snapshots across all dimensions.
     */
    public Collection<ContainerSnapshot> getAllSnapshots() {
        List<ContainerSnapshot> all = new ArrayList<>();
        for (Map<BlockPos, ContainerSnapshot> map : dimensionMap.values()) {
            all.addAll(map.values());
        }
        return Collections.unmodifiableList(all);
    }

    // =========================================================================
    // Search Operations
    // =========================================================================

    /**
     * Searches containers in a specific dimension using an {@link ItemQuery}.
     *
     * @param dimension Dimension identifier
     * @param query     Structured search query
     * @return List of matching results
     */
    public List<SearchResult> search(String dimension, ItemQuery query) {
        if (dimension == null) return Collections.emptyList();
        Map<BlockPos, ContainerSnapshot> map = dimensionMap.get(dimension);
        if (map == null || map.isEmpty()) return Collections.emptyList();

        List<SearchResult> results = new ArrayList<>();
        for (ContainerSnapshot snapshot : map.values()) {
            List<ItemEntry> matchedItems = snapshot.findMatchingItems(query);
            if (!matchedItems.isEmpty()) {
                int totalCount = 0;
                for (ItemEntry item : matchedItems) {
                    totalCount += item.getCount();
                }
                results.add(new SearchResult(snapshot, matchedItems, totalCount, 0.0D));
            }
        }
        return results;
    }

    /**
     * Searches containers in a specific dimension using a raw query string.
     */
    public List<SearchResult> search(String dimension, String queryStr) {
        return search(dimension, QueryParser.parse(queryStr));
    }

    /**
     * Searches across all dimensions using an {@link ItemQuery}.
     */
    public List<SearchResult> searchAllDimensions(ItemQuery query) {
        List<SearchResult> results = new ArrayList<>();
        for (String dim : dimensionMap.keySet()) {
            results.addAll(search(dim, query));
        }
        return results;
    }

    /**
     * Searches across all dimensions using a raw query string.
     */
    public List<SearchResult> searchAllDimensions(String queryStr) {
        return searchAllDimensions(QueryParser.parse(queryStr));
    }

    /**
     * Searches nearby containers within a maximum Euclidean radius of an origin, sorted by proximity.
     *
     * @param dimension Dimension identifier
     * @param origin    Player or center coordinates
     * @param maxRadius Maximum search radius in blocks
     * @param query     Structured query
     * @return Matching results sorted by ascending distance
     */
    public List<SearchResult> searchNearby(String dimension, BlockPos origin, double maxRadius, ItemQuery query) {
        if (dimension == null || origin == null || maxRadius <= 0.0D) {
            return Collections.emptyList();
        }
        Map<BlockPos, ContainerSnapshot> map = dimensionMap.get(dimension);
        if (map == null || map.isEmpty()) return Collections.emptyList();

        double maxRadiusSq = maxRadius * maxRadius;
        List<SearchResult> results = new ArrayList<>();

        for (ContainerSnapshot snapshot : map.values()) {
            double distSq = snapshot.distanceSq(origin);
            if (distSq <= maxRadiusSq) {
                List<ItemEntry> matched = snapshot.findMatchingItems(query);
                if (!matched.isEmpty()) {
                    int totalCount = 0;
                    for (ItemEntry item : matched) {
                        totalCount += item.getCount();
                    }
                    results.add(new SearchResult(snapshot, matched, totalCount, Math.sqrt(distSq)));
                }
            }
        }

        results.sort(Comparator.comparingDouble(SearchResult::distance));
        return results;
    }

    /**
     * Searches nearby containers using a raw query string.
     */
    public List<SearchResult> searchNearby(String dimension, BlockPos origin, double maxRadius, String queryStr) {
        return searchNearby(dimension, origin, maxRadius, QueryParser.parse(queryStr));
    }

    /**
     * Finds the nearest container matching the query.
     */
    public Optional<SearchResult> findNearestMatching(String dimension, BlockPos origin, ItemQuery query) {
        List<SearchResult> results = searchNearby(dimension, origin, Double.MAX_VALUE, query);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Returns all containers within a given radius regardless of items.
     */
    public List<ContainerSnapshot> getSnapshotsWithinRadius(String dimension, BlockPos center, double radius) {
        if (dimension == null || center == null || radius <= 0.0D) {
            return Collections.emptyList();
        }
        Map<BlockPos, ContainerSnapshot> map = dimensionMap.get(dimension);
        if (map == null || map.isEmpty()) return Collections.emptyList();

        double radiusSq = radius * radius;
        List<ContainerSnapshot> results = new ArrayList<>();
        for (ContainerSnapshot snapshot : map.values()) {
            if (snapshot.distanceSq(center) <= radiusSq) {
                results.add(snapshot);
            }
        }
        return results;
    }

    /**
     * Returns all containers inside an axis-aligned bounding box.
     */
    public List<ContainerSnapshot> getSnapshotsInBox(String dimension, BlockPos min, BlockPos max) {
        if (dimension == null || min == null || max == null) {
            return Collections.emptyList();
        }
        Map<BlockPos, ContainerSnapshot> map = dimensionMap.get(dimension);
        if (map == null || map.isEmpty()) return Collections.emptyList();

        int minX = Math.min(min.getX(), max.getX());
        int minY = Math.min(min.getY(), max.getY());
        int minZ = Math.min(min.getZ(), max.getZ());
        int maxX = Math.max(min.getX(), max.getX());
        int maxY = Math.max(min.getY(), max.getY());
        int maxZ = Math.max(min.getZ(), max.getZ());

        List<ContainerSnapshot> results = new ArrayList<>();
        for (ContainerSnapshot snapshot : map.values()) {
            int x = snapshot.getX();
            int y = snapshot.getY();
            int z = snapshot.getZ();
            if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ) {
                results.add(snapshot);
            }
        }
        return results;
    }

    /**
     * Computes the total quantity of a specific item across all containers in a dimension.
     */
    public int getTotalItemCount(String dimension, String itemId) {
        if (dimension == null || itemId == null) return 0;
        Map<BlockPos, ContainerSnapshot> map = dimensionMap.get(dimension);
        if (map == null) return 0;

        String target = itemId.trim().toLowerCase();
        int total = 0;
        for (ContainerSnapshot snapshot : map.values()) {
            for (ItemEntry item : snapshot.getItems()) {
                if (item.getItemId().equalsIgnoreCase(target)) {
                    total += item.getCount();
                }
            }
        }
        return total;
    }

    // =========================================================================
    // Serialization & Persistence
    // =========================================================================

    /**
     * Serializes entire cache to a JSON string.
     */
    public String toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);

        JsonObject dimsObj = new JsonObject();
        for (Map.Entry<String, Map<BlockPos, ContainerSnapshot>> dimEntry : dimensionMap.entrySet()) {
            JsonArray arr = new JsonArray();
            for (ContainerSnapshot snapshot : dimEntry.getValue().values()) {
                arr.add(snapshot.toJson());
            }
            dimsObj.add(dimEntry.getKey(), arr);
        }
        root.add("dimensions", dimsObj);

        return GSON.toJson(root);
    }

    /**
     * Creates a new StorageCache populated from a JSON string.
     */
    public static StorageCache fromJson(String jsonString) {
        StorageCache cache = new StorageCache();
        cache.loadFromJson(jsonString);
        return cache;
    }

    /**
     * Deserializes and loads containers from a JSON string into this cache.
     */
    public void loadFromJson(String jsonString) {
        if (jsonString == null || jsonString.isBlank()) {
            return;
        }

        JsonObject root = JsonParser.parseString(jsonString).getAsJsonObject();
        if (root.has("dimensions") && root.get("dimensions").isJsonObject()) {
            JsonObject dims = root.getAsJsonObject("dimensions");
            for (String dimKey : dims.keySet()) {
                JsonElement el = dims.get(dimKey);
                if (el.isJsonArray()) {
                    JsonArray arr = el.getAsJsonArray();
                    Map<BlockPos, ContainerSnapshot> map = dimensionMap.computeIfAbsent(
                            dimKey,
                            k -> new ConcurrentHashMap<>()
                    );
                    for (JsonElement itemEl : arr) {
                        if (itemEl.isJsonObject()) {
                            ContainerSnapshot snapshot = ContainerSnapshot.fromJson(itemEl.getAsJsonObject());
                            if (snapshot != null) {
                                map.put(snapshot.getBlockPos(), snapshot);
                            }
                        }
                    }
                }
            }
        }
        notifyListeners();
    }

    /**
     * Saves cache atomically to a file on disk.
     */
    public void saveToFile(Path path) throws IOException {
        if (path == null) return;
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        Path tempPath = path.resolveSibling(path.getFileName().toString() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(tempPath)) {
            writer.write(toJson());
        }

        Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Loads cache from a file on disk.
     */
    public void loadFromFile(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            loadFromJson(sb.toString());
        }
    }

    // =========================================================================
    // Listeners
    // =========================================================================

    public void addListener(StorageChangeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(StorageChangeListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void notifyListeners() {
        for (StorageChangeListener listener : listeners) {
            try {
                listener.onStorageChanged();
            } catch (Throwable ignored) {
            }
        }
    }
}

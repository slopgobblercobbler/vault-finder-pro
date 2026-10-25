package com.slopgobbler.vaultfinder.tracker;

import com.slopgobbler.vaultfinder.model.BlockPos;
import com.slopgobbler.vaultfinder.model.ContainerSnapshot;
import com.slopgobbler.vaultfinder.model.ContainerType;
import com.slopgobbler.vaultfinder.model.ItemEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StorageCache Test Suite")
public class StorageCacheTest {

    private StorageCache cache;

    @BeforeEach
    void setUp() {
        cache = new StorageCache();
    }

    @Nested
    @DisplayName("Snapshot Lifecycle: Add, Retrieve, Update, and Remove")
    class LifecycleTests {

        @Test
        @DisplayName("Add snapshot and retrieve by formatted ID")
        void testAddAndRetrieveById() {
            ContainerSnapshot snapshot = new ContainerSnapshot(
                    "minecraft:overworld", 100, 64, 200, ContainerType.CHEST,
                    List.of(new ItemEntry("minecraft:diamond", 32, 0)),
                    System.currentTimeMillis()
            );

            cache.put(snapshot);

            assertEquals(1, cache.size(), "Cache size should be 1");
            ContainerSnapshot retrieved = cache.get("minecraft:overworld:100,64,200");
            assertNotNull(retrieved, "Retrieved snapshot should not be null");
            assertEquals("minecraft:overworld", retrieved.getDimension());
            assertEquals(100, retrieved.getX());
            assertEquals(64, retrieved.getY());
            assertEquals(200, retrieved.getZ());
            assertEquals(ContainerType.CHEST, retrieved.getType());
            assertEquals(1, retrieved.getItems().size());
            assertEquals("minecraft:diamond", retrieved.getItems().get(0).getItemId());
            assertEquals(32, retrieved.getItems().get(0).getCount());
        }

        @Test
        @DisplayName("Retrieve snapshot by dimension and integer coordinates")
        void testRetrieveByDimensionAndCoordinates() {
            ContainerSnapshot snapshot = new ContainerSnapshot(
                    "minecraft:overworld", 50, 70, -30, ContainerType.BARREL,
                    List.of(new ItemEntry("minecraft:iron_ingot", 64, 0)),
                    System.currentTimeMillis()
            );
            cache.put(snapshot);

            Optional<ContainerSnapshot> found = cache.get("minecraft:overworld", 50, 70, -30);
            assertTrue(found.isPresent(), "Snapshot should be found by dimension and coords");
            assertEquals(ContainerType.BARREL, found.get().getType());

            Optional<ContainerSnapshot> notFound = cache.get("minecraft:overworld", 99, 99, 99);
            assertTrue(notFound.isEmpty(), "Non-existent coordinates should return empty Optional");
        }

        @Test
        @DisplayName("Retrieve snapshot by BlockPos")
        void testRetrieveByBlockPos() {
            BlockPos pos = new BlockPos(-15, 60, 45);
            ContainerSnapshot snapshot = new ContainerSnapshot(
                    pos, "minecraft:the_nether", ContainerType.SHULKER_BOX,
                    List.of(new ItemEntry("minecraft:blaze_rod", 16, 0)),
                    System.currentTimeMillis()
            );
            cache.put(snapshot);

            Optional<ContainerSnapshot> found = cache.get("minecraft:the_nether", pos);
            assertTrue(found.isPresent());
            assertEquals("minecraft:blaze_rod", found.get().getItems().get(0).getItemId());
        }

        @Test
        @DisplayName("Updating container snapshot replaces items without increasing size")
        void testUpdateExistingSnapshot() {
            ContainerSnapshot initial = new ContainerSnapshot(
                    "minecraft:overworld", 0, 64, 0, ContainerType.CHEST,
                    List.of(new ItemEntry("minecraft:cobblestone", 10, 0)),
                    System.currentTimeMillis()
            );
            cache.put(initial);
            assertEquals(1, cache.size());
            assertEquals(10, cache.get("minecraft:overworld:0,64,0").getItems().get(0).getCount());

            // Player deposits more cobblestone -> update snapshot
            ContainerSnapshot updated = new ContainerSnapshot(
                    "minecraft:overworld", 0, 64, 0, ContainerType.CHEST,
                    List.of(new ItemEntry("minecraft:cobblestone", 64, 0)),
                    System.currentTimeMillis() + 1000
            );
            cache.put(updated);

            assertEquals(1, cache.size(), "Cache size must remain 1 after updating existing position");
            ContainerSnapshot current = cache.get("minecraft:overworld:0,64,0");
            assertEquals(64, current.getItems().get(0).getCount(), "Updated count should be reflected");
        }

        @Test
        @DisplayName("Contains checks for dimension and position")
        void testContainsCheck() {
            cache.put(new ContainerSnapshot("minecraft:overworld", 10, 20, 30, ContainerType.CHEST, List.of(), 0));

            assertTrue(cache.contains("minecraft:overworld", 10, 20, 30));
            assertTrue(cache.contains("minecraft:overworld", new BlockPos(10, 20, 30)));
            assertFalse(cache.contains("minecraft:overworld", 10, 20, 31));
            assertFalse(cache.contains("minecraft:the_nether", 10, 20, 30));
        }

        @Test
        @DisplayName("Remove container snapshot decreases size")
        void testRemoveSnapshot() {
            ContainerSnapshot snapshot = new ContainerSnapshot(
                    "minecraft:overworld", 10, 64, 10, ContainerType.HOPPER,
                    List.of(), System.currentTimeMillis()
            );
            cache.put(snapshot);
            assertEquals(1, cache.size());

            ContainerSnapshot removed = cache.remove("minecraft:overworld", new BlockPos(10, 64, 10));
            assertNotNull(removed);
            assertEquals(0, cache.size());
            assertNull(cache.get("minecraft:overworld:10,64,10"));
        }

        @Test
        @DisplayName("Clear removes all snapshots across all dimensions")
        void testClearCache() {
            cache.put(new ContainerSnapshot("minecraft:overworld", 0, 64, 0, ContainerType.CHEST, List.of(), 0));
            cache.put(new ContainerSnapshot("minecraft:the_nether", 0, 64, 0, ContainerType.CHEST, List.of(), 0));
            assertEquals(2, cache.size());

            cache.clear();
            assertEquals(0, cache.size());
            assertTrue(cache.getDimensions().isEmpty());
        }

        @Test
        @DisplayName("Mutation listeners trigger on cache changes")
        void testStorageChangeListeners() {
            AtomicInteger eventCount = new AtomicInteger(0);
            StorageCache.StorageChangeListener listener = eventCount::incrementAndGet;

            cache.addListener(listener);

            cache.put(new ContainerSnapshot("minecraft:overworld", 0, 64, 0, ContainerType.CHEST, List.of(), 0));
            assertEquals(1, eventCount.get(), "Listener should fire on put");

            cache.remove("minecraft:overworld", new BlockPos(0, 64, 0));
            assertEquals(2, eventCount.get(), "Listener should fire on remove");

            cache.clear();
            assertEquals(3, eventCount.get(), "Listener should fire on clear");

            cache.removeListener(listener);
            cache.put(new ContainerSnapshot("minecraft:overworld", 5, 5, 5, ContainerType.CHEST, List.of(), 0));
            assertEquals(3, eventCount.get(), "Listener should not fire after being removed");
        }
    }

    @Nested
    @DisplayName("Multi-Dimension Separation Tests")
    class MultiDimensionTests {

        @Test
        @DisplayName("Containers at identical coordinates in different dimensions are separate")
        void testSameCoordinatesAcrossDimensions() {
            ContainerSnapshot overworld = new ContainerSnapshot(
                    "minecraft:overworld", 100, 64, 100, ContainerType.CHEST,
                    List.of(new ItemEntry("minecraft:oak_log", 64, 0)),
                    System.currentTimeMillis()
            );
            ContainerSnapshot nether = new ContainerSnapshot(
                    "minecraft:the_nether", 100, 64, 100, ContainerType.CHEST,
                    List.of(new ItemEntry("minecraft:crimson_stem", 64, 0)),
                    System.currentTimeMillis()
            );
            ContainerSnapshot end = new ContainerSnapshot(
                    "minecraft:the_end", 100, 64, 100, ContainerType.CHEST,
                    List.of(new ItemEntry("minecraft:purpur_block", 64, 0)),
                    System.currentTimeMillis()
            );

            cache.put(overworld);
            cache.put(nether);
            cache.put(end);

            assertEquals(3, cache.size(), "Should hold 3 distinct containers across 3 dimensions");
            assertEquals(1, cache.size("minecraft:overworld"));
            assertEquals(1, cache.size("minecraft:the_nether"));
            assertEquals(1, cache.size("minecraft:the_end"));

            // Verify items are isolated
            assertEquals("minecraft:oak_log",
                    cache.get("minecraft:overworld", 100, 64, 100).get().getItems().get(0).getItemId());
            assertEquals("minecraft:crimson_stem",
                    cache.get("minecraft:the_nether", 100, 64, 100).get().getItems().get(0).getItemId());
            assertEquals("minecraft:purpur_block",
                    cache.get("minecraft:the_end", 100, 64, 100).get().getItems().get(0).getItemId());
        }

        @Test
        @DisplayName("Search query with dimension filter isolates dimension results")
        void testDimensionQueryIsolation() {
            cache.put(new ContainerSnapshot("minecraft:overworld", 10, 64, 10, ContainerType.CHEST,
                    List.of(new ItemEntry("minecraft:gold_ingot", 16, 0)), 0));
            cache.put(new ContainerSnapshot("minecraft:the_nether", 10, 64, 10, ContainerType.CHEST,
                    List.of(new ItemEntry("minecraft:gold_ingot", 64, 0)), 0));

            List<ContainerSnapshot> overworldResults = cache.search("gold", "minecraft:overworld", 0, 64, 0, 1000.0);
            assertEquals(1, overworldResults.size());
            assertEquals("minecraft:overworld", overworldResults.get(0).getDimension());
            assertEquals(16, overworldResults.get(0).getItems().get(0).getCount());

            List<ContainerSnapshot> netherResults = cache.search("gold", "minecraft:the_nether", 0, 64, 0, 1000.0);
            assertEquals(1, netherResults.size());
            assertEquals("minecraft:the_nether", netherResults.get(0).getDimension());
            assertEquals(64, netherResults.get(0).getItems().get(0).getCount());
        }

        @Test
        @DisplayName("clearDimension only clears specified dimension")
        void testClearSpecificDimension() {
            cache.put(new ContainerSnapshot("minecraft:overworld", 0, 64, 0, ContainerType.CHEST, List.of(), 0));
            cache.put(new ContainerSnapshot("minecraft:the_nether", 0, 64, 0, ContainerType.CHEST, List.of(), 0));
            cache.put(new ContainerSnapshot("minecraft:the_end", 0, 64, 0, ContainerType.CHEST, List.of(), 0));

            cache.clearDimension("minecraft:the_nether");

            assertEquals(2, cache.size());
            assertEquals(0, cache.size("minecraft:the_nether"));
            assertEquals(1, cache.size("minecraft:overworld"));
            assertEquals(1, cache.size("minecraft:the_end"));
        }
    }

    @Nested
    @DisplayName("Spatial Radius Query Tests")
    class SpatialRadiusTests {

        @Test
        @DisplayName("Radius search finds containers within distance and orders by proximity")
        void testRadiusFilteringAndOrdering() {
            // Player at (0, 64, 0)
            ContainerSnapshot closeBox = new ContainerSnapshot(
                    "minecraft:overworld", 10, 64, 0, ContainerType.CHEST,
                    List.of(new ItemEntry("minecraft:emerald", 16, 0)), 0
            ); // distance = 10
            ContainerSnapshot mediumBox = new ContainerSnapshot(
                    "minecraft:overworld", 40, 64, 0, ContainerType.CHEST,
                    List.of(new ItemEntry("minecraft:emerald", 32, 0)), 0
            ); // distance = 40
            ContainerSnapshot farBox = new ContainerSnapshot(
                    "minecraft:overworld", 200, 64, 0, ContainerType.CHEST,
                    List.of(new ItemEntry("minecraft:emerald", 64, 0)), 0
            ); // distance = 200

            cache.put(farBox);
            cache.put(closeBox);
            cache.put(mediumBox);

            // Radius = 50 -> Should include close (10) and medium (40), exclude far (200)
            List<ContainerSnapshot> results = cache.search("emerald", "minecraft:overworld", 0, 64, 0, 50.0);
            assertEquals(2, results.size(), "Should return exactly 2 containers within 50 blocks");
            assertEquals(closeBox.getId(), results.get(0).getId(), "Closest container must be first");
            assertEquals(mediumBox.getId(), results.get(1).getId(), "Second closest container must be second");

            // Radius = 15 -> Should include only close (10)
            List<ContainerSnapshot> closeResults = cache.search("emerald", "minecraft:overworld", 0, 64, 0, 15.0);
            assertEquals(1, closeResults.size());
            assertEquals(closeBox.getId(), closeResults.get(0).getId());

            // Radius = 5 -> None
            List<ContainerSnapshot> noneResults = cache.search("emerald", "minecraft:overworld", 0, 64, 0, 5.0);
            assertTrue(noneResults.isEmpty());
        }

        @Test
        @DisplayName("Search with maxRadius <= 0 returns all containers regardless of distance")
        void testUnlimitedRadius() {
            cache.put(new ContainerSnapshot("minecraft:overworld", 10, 64, 10, ContainerType.CHEST,
                    List.of(new ItemEntry("minecraft:iron_ingot", 1, 0)), 0));
            cache.put(new ContainerSnapshot("minecraft:overworld", 1000, 64, 1000, ContainerType.CHEST,
                    List.of(new ItemEntry("minecraft:iron_ingot", 1, 0)), 0));

            List<ContainerSnapshot> results = cache.search("iron", "minecraft:overworld", 0, 64, 0, 0.0);
            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("3D Euclidean distance calculation includes Y coordinate")
        void test3DEuclideanDistance() {
            // Player at (0, 0, 0), container at (0, 100, 0) -> vertical distance 100
            ContainerSnapshot highBox = new ContainerSnapshot(
                    "minecraft:overworld", 0, 100, 0, ContainerType.CHEST,
                    List.of(new ItemEntry("minecraft:feather", 1, 0)), 0
            );
            cache.put(highBox);

            List<ContainerSnapshot> within50 = cache.search("feather", "minecraft:overworld", 0, 0, 0, 50.0);
            assertTrue(within50.isEmpty(), "Vertical distance 100 should exceed radius 50");

            List<ContainerSnapshot> within150 = cache.search("feather", "minecraft:overworld", 0, 0, 0, 150.0);
            assertEquals(1, within150.size(), "Vertical distance 100 should be within radius 150");
        }
    }

    @Nested
    @DisplayName("Search Query Matching Against Cached Containers")
    class SearchQueryTests {

        @Test
        @DisplayName("Search matches item keyword across multiple containers")
        void testSearchByKeyword() {
            ContainerSnapshot diamondChest = new ContainerSnapshot(
                    "minecraft:overworld", 0, 64, 0, ContainerType.CHEST,
                    List.of(new ItemEntry("minecraft:diamond", 64, 0)), 0
            );
            ContainerSnapshot stoneChest = new ContainerSnapshot(
                    "minecraft:overworld", 1, 64, 0, ContainerType.CHEST,
                    List.of(new ItemEntry("minecraft:stone", 64, 0)), 0
            );
            cache.put(diamondChest);
            cache.put(stoneChest);

            List<ContainerSnapshot> diaResults = cache.search("diamond", "minecraft:overworld", 0, 64, 0, 100.0);
            assertEquals(1, diaResults.size());
            assertEquals(diamondChest.getId(), diaResults.get(0).getId());

            List<ContainerSnapshot> stoneResults = cache.search("stone", "minecraft:overworld", 0, 64, 0, 100.0);
            assertEquals(1, stoneResults.size());
            assertEquals(stoneChest.getId(), stoneResults.get(0).getId());
        }

        @Test
        @DisplayName("Search with mod namespace filter (@mod)")
        void testSearchByModFilter() {
            ContainerSnapshot createChest = new ContainerSnapshot(
                    "minecraft:overworld", 10, 64, 10, ContainerType.BARREL,
                    List.of(new ItemEntry("create:cogwheel", 32, 0)), 0
            );
            ContainerSnapshot mcChest = new ContainerSnapshot(
                    "minecraft:overworld", 20, 64, 20, ContainerType.CHEST,
                    List.of(new ItemEntry("minecraft:redstone", 64, 0)), 0
            );
            cache.put(createChest);
            cache.put(mcChest);

            List<ContainerSnapshot> createResults = cache.search("@create", "minecraft:overworld", 0, 64, 0, 100.0);
            assertEquals(1, createResults.size());
            assertEquals(createChest.getId(), createResults.get(0).getId());
        }

        @Test
        @DisplayName("Search with count comparison filter (>64)")
        void testSearchByCountFilter() {
            ContainerSnapshot largeStack = new ContainerSnapshot(
                    "minecraft:overworld", 5, 64, 5, ContainerType.DOUBLE_CHEST,
                    List.of(new ItemEntry("minecraft:dirt", 128, 0)), 0
            );
            ContainerSnapshot regularStack = new ContainerSnapshot(
                    "minecraft:overworld", 15, 64, 15, ContainerType.CHEST,
                    List.of(new ItemEntry("minecraft:dirt", 64, 0)), 0
            );
            cache.put(largeStack);
            cache.put(regularStack);

            List<ContainerSnapshot> over64Results = cache.search(">64", "minecraft:overworld", 0, 64, 0, 100.0);
            assertEquals(1, over64Results.size());
            assertEquals(largeStack.getId(), over64Results.get(0).getId());
        }

        @Test
        @DisplayName("Search with enchantment tag filter (#mending)")
        void testSearchByEnchantmentFilter() {
            ContainerSnapshot enchantedBox = new ContainerSnapshot(
                    "minecraft:overworld", 0, 64, 0, ContainerType.SHULKER_BOX,
                    List.of(new ItemEntry("minecraft:diamond_pickaxe", 1, "Miner", List.of("minecraft:mending"), 100, 0)), 0
            );
            ContainerSnapshot plainBox = new ContainerSnapshot(
                    "minecraft:overworld", 5, 64, 0, ContainerType.CHEST,
                    List.of(new ItemEntry("minecraft:iron_pickaxe", 1, null, Collections.emptyList(), 100, 0)), 0
            );
            cache.put(enchantedBox);
            cache.put(plainBox);

            List<ContainerSnapshot> mendingResults = cache.search("#mending", "minecraft:overworld", 0, 64, 0, 100.0);
            assertEquals(1, mendingResults.size());
            assertEquals(enchantedBox.getId(), mendingResults.get(0).getId());
        }

        @Test
        @DisplayName("Empty search query matches all containers with items")
        void testEmptyQueryMatchesAll() {
            cache.put(new ContainerSnapshot("minecraft:overworld", 1, 64, 1, ContainerType.CHEST,
                    List.of(new ItemEntry("minecraft:apple", 5, 0)), 0));
            cache.put(new ContainerSnapshot("minecraft:overworld", 2, 64, 2, ContainerType.BARREL,
                    List.of(new ItemEntry("minecraft:bread", 10, 0)), 0));

            List<ContainerSnapshot> results = cache.search("", "minecraft:overworld", 0, 64, 0, 50.0);
            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("Aggregate item count helper calculation")
        void testTotalItemCountAcrossContainers() {
            cache.put(new ContainerSnapshot("minecraft:overworld", 0, 64, 0, ContainerType.CHEST,
                    List.of(new ItemEntry("minecraft:iron_ingot", 64, 0), new ItemEntry("minecraft:iron_ingot", 36, 1)), 0));
            cache.put(new ContainerSnapshot("minecraft:overworld", 10, 64, 0, ContainerType.BARREL,
                    List.of(new ItemEntry("minecraft:iron_ingot", 50, 0)), 0));

            int totalIron = cache.getTotalItemCount("minecraft:overworld", "minecraft:iron_ingot");
            assertEquals(150, totalIron, "64 + 36 + 50 = 150 iron ingots");

            int totalGold = cache.getTotalItemCount("minecraft:overworld", "minecraft:gold_ingot");
            assertEquals(0, totalGold);
        }
    }

    @Nested
    @DisplayName("JSON Serialization & Deserialization Round-Trip")
    class JsonRoundTripTests {

        @Test
        @DisplayName("Full round-trip JSON serialization and deserialization")
        void testJsonSerializationRoundTrip() {
            // Build rich multi-dimensional test data
            ContainerSnapshot chest = new ContainerSnapshot(
                    new BlockPos(100, 64, -200),
                    "minecraft:overworld",
                    ContainerType.DOUBLE_CHEST,
                    List.of(
                            new ItemEntry("minecraft:diamond", 64, 0),
                            new ItemEntry("minecraft:netherite_sword", 1, "Excalibur",
                                    Map.of("minecraft:sharpness", 5, "minecraft:unbreaking", 3),
                                    1500, 2031, 1, Collections.emptySet())
                    ),
                    1700000000000L,
                    "Treasury Chest"
            );

            ContainerSnapshot netherBox = new ContainerSnapshot(
                    new BlockPos(-50, 120, 30),
                    "minecraft:the_nether",
                    ContainerType.SHULKER_BOX,
                    List.of(
                            new ItemEntry("minecraft:ancient_debris", 12, 0),
                            new ItemEntry("minecraft:golden_helmet", 1, null,
                                    Collections.emptyMap(), 50, 100, 1, Collections.emptySet())
                    ),
                    1700000005000L,
                    "Nether Cache"
            );

            ContainerSnapshot endBarrel = new ContainerSnapshot(
                    new BlockPos(0, 70, 0),
                    "minecraft:the_end",
                    ContainerType.BARREL,
                    List.of(
                            new ItemEntry("minecraft:elytra", 1, null,
                                    Map.of("minecraft:mending", 1),
                                    432, 432, 0, Collections.emptySet())
                    ),
                    1700000010000L,
                    null
            );

            cache.put(chest);
            cache.put(netherBox);
            cache.put(endBarrel);

            assertEquals(3, cache.size());

            // 1. Serialize to JSON
            String json = cache.toJson();
            assertNotNull(json, "Serialized JSON should not be null");
            assertFalse(json.isBlank(), "Serialized JSON should not be empty");
            assertTrue(json.contains("minecraft:overworld"), "JSON should contain overworld dimension");
            assertTrue(json.contains("minecraft:the_nether"), "JSON should contain nether dimension");
            assertTrue(json.contains("minecraft:the_end"), "JSON should contain end dimension");
            assertTrue(json.contains("Excalibur"), "JSON should contain custom name");
            assertTrue(json.contains("ancient_debris"), "JSON should contain item IDs");

            // 2. Deserialize from JSON into a new cache
            StorageCache restoredCache = StorageCache.fromJson(json);

            // 3. Verify total structure and counts
            assertNotNull(restoredCache);
            assertEquals(cache.size(), restoredCache.size(), "Restored cache should match original size");
            assertEquals(3, restoredCache.getDimensions().size());

            // 4. Verify specific snapshots in restored cache
            Optional<ContainerSnapshot> restoredChestOpt = restoredCache.get("minecraft:overworld", 100, 64, -200);
            assertTrue(restoredChestOpt.isPresent());
            ContainerSnapshot restoredChest = restoredChestOpt.get();
            assertEquals("Treasury Chest", restoredChest.getCustomName());
            assertEquals(ContainerType.DOUBLE_CHEST, restoredChest.getType());
            assertEquals(2, restoredChest.getItems().size());

            ItemEntry diamondItem = restoredChest.getItems().get(0);
            assertEquals("minecraft:diamond", diamondItem.getItemId());
            assertEquals(64, diamondItem.getCount());

            ItemEntry swordItem = restoredChest.getItems().get(1);
            assertEquals("minecraft:netherite_sword", swordItem.getItemId());
            assertEquals("Excalibur", swordItem.getCustomName());
            assertEquals(5, swordItem.getEnchantmentLevel("sharpness"));
            assertEquals(3, swordItem.getEnchantmentLevel("unbreaking"));

            // Verify nether container
            Optional<ContainerSnapshot> restoredNetherOpt = restoredCache.get("minecraft:the_nether", -50, 120, 30);
            assertTrue(restoredNetherOpt.isPresent());
            assertEquals(2, restoredNetherOpt.get().getItems().size());
            assertEquals("minecraft:ancient_debris", restoredNetherOpt.get().getItems().get(0).getItemId());
            assertEquals(12, restoredNetherOpt.get().getItems().get(0).getCount());

            // Verify end container
            Optional<ContainerSnapshot> restoredEndOpt = restoredCache.get("minecraft:the_end", 0, 70, 0);
            assertTrue(restoredEndOpt.isPresent());
            assertEquals("minecraft:elytra", restoredEndOpt.get().getItems().get(0).getItemId());
        }

        @Test
        @DisplayName("Atomic file save and load round-trip")
        void testFileSaveAndLoadRoundTrip(@TempDir Path tempDir) throws IOException {
            Path saveFile = tempDir.resolve("storage_cache.json");

            cache.put(new ContainerSnapshot(
                    "minecraft:overworld", 42, 65, 42, ContainerType.CHEST,
                    List.of(new ItemEntry("minecraft:totem_of_undying", 2, 0)),
                    System.currentTimeMillis()
            ));

            // Save to file
            cache.saveToFile(saveFile);
            assertTrue(java.nio.file.Files.exists(saveFile), "Saved JSON file should exist on disk");

            // Load into fresh cache
            StorageCache freshCache = new StorageCache();
            freshCache.loadFromFile(saveFile);

            assertEquals(1, freshCache.size());
            Optional<ContainerSnapshot> loaded = freshCache.get("minecraft:overworld", 42, 65, 42);
            assertTrue(loaded.isPresent());
            assertEquals("minecraft:totem_of_undying", loaded.get().getItems().get(0).getItemId());
            assertEquals(2, loaded.get().getItems().get(0).getCount());
        }

        @Test
        @DisplayName("Deserializing null or blank JSON string produces empty cache safely")
        void testEmptyJsonHandling() {
            StorageCache nullCache = StorageCache.fromJson(null);
            assertNotNull(nullCache);
            assertEquals(0, nullCache.size());

            StorageCache emptyCache = StorageCache.fromJson("");
            assertNotNull(emptyCache);
            assertEquals(0, emptyCache.size());

            StorageCache whitespaceCache = StorageCache.fromJson("   \n\t  ");
            assertNotNull(whitespaceCache);
            assertEquals(0, whitespaceCache.size());
        }
    }
}

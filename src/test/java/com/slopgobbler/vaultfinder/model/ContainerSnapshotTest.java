package com.slopgobbler.vaultfinder.model;

import com.google.gson.JsonObject;
import com.slopgobbler.vaultfinder.query.ItemQuery;
import com.slopgobbler.vaultfinder.query.QueryParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContainerSnapshotTest {

    @Test
    public void testSnapshotCreationAndQueries() {
        BlockPos pos = new BlockPos(100, 64, -200);
        ItemEntry diamonds = new ItemEntry("minecraft:diamond", 64, 0);
        ItemEntry pickaxe = new ItemEntry("minecraft:diamond_pickaxe", 1, "Miner", Map.of("minecraft:fortune", 3), 1000, 1561, 1, Set.of("tools"));
        ContainerSnapshot snapshot = new ContainerSnapshot(pos, "minecraft:overworld", ContainerType.CHEST, List.of(diamonds, pickaxe), 123456789L, "My Stash");

        Assertions.assertEquals(pos, snapshot.getBlockPos());
        Assertions.assertEquals(100, snapshot.getX());
        Assertions.assertEquals(64, snapshot.getY());
        Assertions.assertEquals(-200, snapshot.getZ());
        Assertions.assertEquals("minecraft:overworld", snapshot.getDimension());
        Assertions.assertEquals(ContainerType.CHEST, snapshot.getType());
        Assertions.assertEquals("My Stash", snapshot.getCustomName());
        Assertions.assertEquals(2, snapshot.getFilledSlotCount());
        Assertions.assertEquals(65, snapshot.getTotalItemCount());

        Assertions.assertTrue(snapshot.matches("diamond"));
        Assertions.assertTrue(snapshot.matches("fortune"));
        Assertions.assertTrue(snapshot.matches(">50"));
        Assertions.assertFalse(snapshot.matches("netherite"));

        List<ItemEntry> found = snapshot.findMatchingItems("Miner");
        Assertions.assertEquals(1, found.size());
        Assertions.assertEquals("Miner", found.get(0).getCustomName());

        Assertions.assertEquals(64, snapshot.countMatchingItems(">50"));
        Assertions.assertEquals(65, snapshot.countMatchingItems("diamond"));
    }

    @Test
    public void testDistanceCalculation() {
        ContainerSnapshot snapshot = new ContainerSnapshot(new BlockPos(0, 0, 0), "minecraft:overworld", ContainerType.BARREL, List.of(), 1000L);
        BlockPos target = new BlockPos(3, 4, 0);

        Assertions.assertEquals(25.0D, snapshot.distanceSq(target), 0.001);
        Assertions.assertEquals(5.0D, snapshot.distanceTo(target), 0.001);
    }

    @Test
    public void testJsonRoundtrip() {
        BlockPos pos = new BlockPos(10, 20, 30);
        ItemEntry sword = new ItemEntry("minecraft:iron_sword", 1, 0);
        ContainerSnapshot snapshot = new ContainerSnapshot(pos, "minecraft:the_nether", ContainerType.BARREL, List.of(sword), 55555L, "Nether Barrel");

        JsonObject json = snapshot.toJson();
        ContainerSnapshot deserialized = ContainerSnapshot.fromJson(json);

        Assertions.assertNotNull(deserialized);
        Assertions.assertEquals(snapshot.getBlockPos(), deserialized.getBlockPos());
        Assertions.assertEquals(snapshot.getDimension(), deserialized.getDimension());
        Assertions.assertEquals(snapshot.getType(), deserialized.getType());
        Assertions.assertEquals(snapshot.getLastVisitedTimestamp(), deserialized.getLastVisitedTimestamp());
        Assertions.assertEquals(snapshot.getCustomName(), deserialized.getCustomName());
        Assertions.assertEquals(snapshot.getItems().size(), deserialized.getItems().size());
        Assertions.assertEquals(snapshot.getItems().get(0), deserialized.getItems().get(0));
    }
}

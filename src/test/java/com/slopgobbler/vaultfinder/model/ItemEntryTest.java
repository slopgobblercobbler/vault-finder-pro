package com.slopgobbler.vaultfinder.model;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

public class ItemEntryTest {

    @Test
    public void testBasicProperties() {
        ItemEntry item = new ItemEntry("minecraft:diamond_sword", 1, 0);
        Assertions.assertEquals("minecraft:diamond_sword", item.getItemId());
        Assertions.assertEquals("minecraft", item.getModId());
        Assertions.assertEquals("diamond_sword", item.getItemName());
        Assertions.assertEquals("Diamond Sword", item.getReadableName());
        Assertions.assertEquals(1, item.getCount());
        Assertions.assertEquals(0, item.getSlotIndex());
        Assertions.assertNull(item.getCustomName());
        Assertions.assertFalse(item.hasEnchantments());
        Assertions.assertFalse(item.isDamaged());
    }

    @Test
    public void testCustomNameAndEnchantments() {
        Map<String, Integer> ench = Map.of("minecraft:sharpness", 5, "minecraft:unbreaking", 3);
        ItemEntry item = new ItemEntry("minecraft:diamond_sword", 1, "Excalibur", ench, 1200, 1561, 4, Set.of("swords", "weapons"));

        Assertions.assertEquals("Excalibur", item.getReadableName());
        Assertions.assertEquals("Excalibur", item.getCustomName());
        Assertions.assertTrue(item.hasEnchantments());
        Assertions.assertTrue(item.hasEnchantment("sharpness"));
        Assertions.assertTrue(item.hasEnchantment("minecraft:sharpness"));
        Assertions.assertEquals(5, item.getEnchantmentLevel("sharpness"));
        Assertions.assertEquals(3, item.getEnchantmentLevel("unbreaking"));
        Assertions.assertEquals(0, item.getEnchantmentLevel("fire_aspect"));
        Assertions.assertTrue(item.isDamaged());
        Assertions.assertTrue(item.getDurabilityPercent() > 70 && item.getDurabilityPercent() < 80);
        Assertions.assertTrue(item.matchesTag("swords"));
        Assertions.assertTrue(item.matchesTag("#weapons"));
        Assertions.assertTrue(item.matchesMod("minecraft"));
        Assertions.assertTrue(item.matchesMod("@minecraft"));
    }

    @Test
    public void testJsonRoundtrip() {
        Map<String, Integer> ench = Map.of("minecraft:fortune", 3);
        ItemEntry item = new ItemEntry("minecraft:diamond_pickaxe", 1, "Miner", ench, 1000, 1561, 2, Set.of("c:pickaxes"));

        JsonObject json = item.toJson();
        ItemEntry deserialized = ItemEntry.fromJson(json);

        Assertions.assertNotNull(deserialized);
        Assertions.assertEquals(item.getItemId(), deserialized.getItemId());
        Assertions.assertEquals(item.getCount(), deserialized.getCount());
        Assertions.assertEquals(item.getCustomName(), deserialized.getCustomName());
        Assertions.assertEquals(item.getDurability(), deserialized.getDurability());
        Assertions.assertEquals(item.getMaxDurability(), deserialized.getMaxDurability());
        Assertions.assertEquals(item.getSlotIndex(), deserialized.getSlotIndex());
        Assertions.assertEquals(item.getEnchantments(), deserialized.getEnchantments());
        Assertions.assertEquals(item.getTags(), deserialized.getTags());
        Assertions.assertEquals(item, deserialized);
    }
}

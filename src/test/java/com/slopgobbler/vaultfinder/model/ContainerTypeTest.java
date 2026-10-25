package com.slopgobbler.vaultfinder.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ContainerTypeTest {

    @Test
    public void testEnumValues() {
        Assertions.assertEquals(9, ContainerType.values().length);
        Assertions.assertEquals(27, ContainerType.CHEST.getDefaultCapacity());
        Assertions.assertEquals(54, ContainerType.DOUBLE_CHEST.getDefaultCapacity());
        Assertions.assertEquals(27, ContainerType.SHULKER_BOX.getDefaultCapacity());
        Assertions.assertEquals(27, ContainerType.ENDER_CHEST.getDefaultCapacity());
        Assertions.assertEquals(27, ContainerType.BARREL.getDefaultCapacity());
        Assertions.assertEquals(5, ContainerType.HOPPER.getDefaultCapacity());
        Assertions.assertEquals(9, ContainerType.DISPENSER.getDefaultCapacity());
        Assertions.assertEquals(9, ContainerType.DROPPER.getDefaultCapacity());
        Assertions.assertEquals(9, ContainerType.CRAFTER.getDefaultCapacity());
    }

    @Test
    public void testTypePredicates() {
        Assertions.assertTrue(ContainerType.CHEST.isChest());
        Assertions.assertTrue(ContainerType.DOUBLE_CHEST.isChest());
        Assertions.assertFalse(ContainerType.BARREL.isChest());

        Assertions.assertTrue(ContainerType.SHULKER_BOX.isShulker());
        Assertions.assertFalse(ContainerType.CHEST.isShulker());

        Assertions.assertTrue(ContainerType.ENDER_CHEST.isEnderChest());
        Assertions.assertFalse(ContainerType.BARREL.isEnderChest());
    }

    @Test
    public void testFromId() {
        Assertions.assertEquals(ContainerType.CHEST, ContainerType.fromId("chest"));
        Assertions.assertEquals(ContainerType.DOUBLE_CHEST, ContainerType.fromId("DOUBLE_CHEST"));
        Assertions.assertEquals(ContainerType.SHULKER_BOX, ContainerType.fromId("shulker_box"));
        Assertions.assertEquals(ContainerType.CRAFTER, ContainerType.fromId("crafter"));
        Assertions.assertNull(ContainerType.fromId("unknown_container"));
        Assertions.assertNull(ContainerType.fromId(null));
        Assertions.assertNull(ContainerType.fromId("   "));
    }

    @Test
    public void testFromBlockId() {
        Assertions.assertEquals(ContainerType.CHEST, ContainerType.fromBlockId("minecraft:chest"));
        Assertions.assertEquals(ContainerType.CHEST, ContainerType.fromBlockId("minecraft:trapped_chest"));
        Assertions.assertEquals(ContainerType.SHULKER_BOX, ContainerType.fromBlockId("minecraft:magenta_shulker_box"));
        Assertions.assertEquals(ContainerType.BARREL, ContainerType.fromBlockId("minecraft:barrel"));
        Assertions.assertEquals(ContainerType.HOPPER, ContainerType.fromBlockId("minecraft:hopper"));
        Assertions.assertEquals(ContainerType.CRAFTER, ContainerType.fromBlockId("minecraft:crafter"));
        Assertions.assertEquals(ContainerType.DOUBLE_CHEST, ContainerType.fromBlockId("vaultfinder:double_chest"));
        Assertions.assertNull(ContainerType.fromBlockId("minecraft:stone"));
        Assertions.assertNull(ContainerType.fromBlockId(null));
    }
}

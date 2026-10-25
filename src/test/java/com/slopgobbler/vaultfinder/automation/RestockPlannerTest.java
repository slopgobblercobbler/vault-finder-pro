package com.slopgobbler.vaultfinder.automation;

import com.slopgobbler.vaultfinder.model.ContainerSnapshot;
import com.slopgobbler.vaultfinder.model.ContainerType;
import com.slopgobbler.vaultfinder.model.ItemEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RestockPlannerTest {

    @BeforeEach
    public void setUp() {
        StackSizeResolver.clearOverrides();
    }

    @Test
    public void testRestockDepletedHotbarStack() {
        List<ItemEntry> hotbar = List.of(
                new ItemEntry("minecraft:firework_rocket", 14, 0)
        );

        List<ItemEntry> chest = List.of(
                new ItemEntry("minecraft:firework_rocket", 64, 5)
        );

        List<RestockPlanner.RestockAction> actions = RestockPlanner.planRestock(hotbar, chest);

        assertEquals(1, actions.size());
        RestockPlanner.RestockAction action = actions.get(0);
        assertEquals(50, action.getAmountNeeded()); // 64 - 14 = 50
        assertEquals(50, action.getCount());
        assertEquals(5, action.getContainerSlot());
        assertEquals(0, action.getPlayerSlot());
        assertEquals("minecraft:firework_rocket", action.getItemId());
    }

    @Test
    public void testNoRestockWhenFull() {
        List<ItemEntry> hotbar = List.of(
                new ItemEntry("minecraft:cobblestone", 64, 0)
        );

        List<ItemEntry> chest = List.of(
                new ItemEntry("minecraft:cobblestone", 64, 1)
        );

        List<RestockPlanner.RestockAction> actions = RestockPlanner.planRestock(hotbar, chest);
        assertTrue(actions.isEmpty());
    }

    @Test
    public void testRestockDepletedCobblestoneStack() {
        // As specified in requirement: player has 12 cobblestone, container has 64 -> transfer 52 to top up stack
        List<ItemEntry> hotbar = List.of(
                new ItemEntry("minecraft:cobblestone", 12, 0)
        );

        List<ItemEntry> chest = List.of(
                new ItemEntry("minecraft:cobblestone", 64, 3)
        );

        List<RestockPlanner.RestockAction> actions = RestockPlanner.planRestock(hotbar, chest);

        assertEquals(1, actions.size());
        assertEquals(52, actions.get(0).getAmountNeeded());
        assertEquals(3, actions.get(0).getContainerSlot());
        assertEquals(0, actions.get(0).getPlayerSlot());
    }

    @Test
    public void testRestockRespectsMaxStackSize16ForEnderPearls() {
        List<ItemEntry> hotbar = List.of(
                new ItemEntry("minecraft:ender_pearl", 6, 1)
        );

        List<ItemEntry> chest = List.of(
                new ItemEntry("minecraft:ender_pearl", 16, 2)
        );

        List<RestockPlanner.RestockAction> actions = RestockPlanner.planRestock(hotbar, chest);

        assertEquals(1, actions.size());
        // Max stack is 16: needs 16 - 6 = 10 (not 64 - 6 = 58)
        assertEquals(10, actions.get(0).getAmountNeeded());
        assertEquals(2, actions.get(0).getContainerSlot());
        assertEquals(1, actions.get(0).getPlayerSlot());
    }

    @Test
    public void testRestockMultipleContainerSources() {
        List<ItemEntry> hotbar = List.of(
                new ItemEntry("minecraft:torch", 10, 0) // needs 54
        );

        List<ItemEntry> chest = List.of(
                new ItemEntry("minecraft:torch", 20, 1),
                new ItemEntry("minecraft:torch", 64, 4)
        );

        List<RestockPlanner.RestockAction> actions = RestockPlanner.planRestock(hotbar, chest);

        assertEquals(2, actions.size());
        // First take 20 from container slot 1
        assertEquals(1, actions.get(0).getContainerSlot());
        assertEquals(20, actions.get(0).getAmountNeeded());

        // Then take remaining 34 from container slot 4
        assertEquals(4, actions.get(1).getContainerSlot());
        assertEquals(34, actions.get(1).getAmountNeeded());
    }

    @Test
    public void testRestockMultipleDepletedPlayerSlotsWithoutOverdraw() {
        List<ItemEntry> hotbar = List.of(
                new ItemEntry("minecraft:arrow", 24, 0), // needs 40
                new ItemEntry("minecraft:arrow", 24, 1)  // needs 40
        );

        // Container only has 50 arrows in slot 3
        List<ItemEntry> chest = List.of(
                new ItemEntry("minecraft:arrow", 50, 3)
        );

        List<RestockPlanner.RestockAction> actions = RestockPlanner.planRestock(hotbar, chest);

        assertEquals(2, actions.size());
        // Slot 0 gets 40
        assertEquals(0, actions.get(0).getPlayerSlot());
        assertEquals(40, actions.get(0).getAmountNeeded());
        assertEquals(3, actions.get(0).getContainerSlot());

        // Slot 1 gets remaining 10
        assertEquals(1, actions.get(1).getPlayerSlot());
        assertEquals(10, actions.get(1).getAmountNeeded());
        assertEquals(3, actions.get(1).getContainerSlot());
    }

    @Test
    public void testRestockToolsNeverRestocked() {
        List<ItemEntry> hotbar = List.of(
                new ItemEntry("minecraft:netherite_pickaxe", 1, 0)
        );

        List<ItemEntry> chest = List.of(
                new ItemEntry("minecraft:netherite_pickaxe", 1, 2)
        );

        List<RestockPlanner.RestockAction> actions = RestockPlanner.planRestock(hotbar, chest);
        assertTrue(actions.isEmpty(), "Unstackable tools should not be restocked");
    }

    @Test
    public void testRestockFromTargetLayout() {
        List<ItemEntry> current = List.of(
                new ItemEntry("minecraft:golden_apple", 2, 8)
        );

        List<ItemEntry> target = List.of(
                new ItemEntry("minecraft:golden_apple", 16, 8) // target explicitly 16
        );

        List<ItemEntry> chest = List.of(
                new ItemEntry("minecraft:golden_apple", 64, 0)
        );

        List<RestockPlanner.RestockAction> actions = RestockPlanner.planRestockFromTarget(current, target, chest);

        assertEquals(1, actions.size());
        assertEquals(14, actions.get(0).getAmountNeeded()); // 16 - 2 = 14
        assertEquals(0, actions.get(0).getContainerSlot());
        assertEquals(8, actions.get(0).getPlayerSlot());
    }

    @Test
    public void testRestockWithContainerSnapshot() {
        ContainerSnapshot snapshot = new ContainerSnapshot("minecraft:overworld", 10, 64, 10, ContainerType.BARREL, List.of(
                new ItemEntry("minecraft:bread", 64, 0)
        ), System.currentTimeMillis());

        List<ItemEntry> hotbar = List.of(
                new ItemEntry("minecraft:bread", 20, 2)
        );

        List<RestockPlanner.RestockAction> actions = RestockPlanner.planRestock(hotbar, snapshot);
        assertEquals(1, actions.size());
        assertEquals(44, actions.get(0).getAmountNeeded());
        assertEquals(0, actions.get(0).getContainerSlot());
        assertEquals(2, actions.get(0).getPlayerSlot());
    }
}

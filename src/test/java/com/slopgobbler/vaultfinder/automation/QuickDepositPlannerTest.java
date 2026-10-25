package com.slopgobbler.vaultfinder.automation;

import com.slopgobbler.vaultfinder.model.ContainerSnapshot;
import com.slopgobbler.vaultfinder.model.ContainerType;
import com.slopgobbler.vaultfinder.model.ItemEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class QuickDepositPlannerTest {

    @BeforeEach
    public void setUp() {
        StackSizeResolver.clearOverrides();
    }

    @Test
    public void testDepositOnlyMatchingItems() {
        List<ItemEntry> playerInv = List.of(
                new ItemEntry("minecraft:iron_ingot", 64, 9),
                new ItemEntry("minecraft:dirt", 64, 10)
        );

        List<ItemEntry> chestInv = List.of(
                new ItemEntry("minecraft:iron_ingot", 32, 0)
        );

        List<QuickDepositPlanner.MoveAction> actions = QuickDepositPlanner.planDeposit(playerInv, chestInv, 27, false);

        assertEquals(1, actions.size());
        QuickDepositPlanner.MoveAction action = actions.get(0);
        assertEquals("minecraft:iron_ingot", action.getItemId());
        assertEquals(9, action.getSourceSlot());
        assertEquals(0, action.getTargetSlot());
        assertEquals(32, action.getCount());
    }

    @Test
    public void testDepositAllowEmptySlots() {
        List<ItemEntry> playerInv = List.of(
                new ItemEntry("minecraft:diamond", 10, 9)
        );

        List<ItemEntry> chestInv = List.of();

        List<QuickDepositPlanner.MoveAction> actions = QuickDepositPlanner.planDeposit(playerInv, chestInv, 27, true);

        assertEquals(1, actions.size());
        QuickDepositPlanner.MoveAction action = actions.get(0);
        assertEquals("minecraft:diamond", action.getItemId());
        assertEquals(9, action.getSourceSlot());
        assertEquals(0, action.getTargetSlot());
        assertEquals(10, action.getCount());
    }

    @Test
    public void testDepositRespectsMaxStackSize64Splitting() {
        List<ItemEntry> playerInv = List.of(
                new ItemEntry("minecraft:cobblestone", 64, 9)
        );

        List<ItemEntry> chestInv = List.of(
                new ItemEntry("minecraft:cobblestone", 40, 0)
        );

        // Disallowing empty slots: only fills slot 0 up to 64 (takes 24, leaves 40)
        List<QuickDepositPlanner.MoveAction> actionsNoEmpty = QuickDepositPlanner.planDeposit(playerInv, chestInv, 27, false);
        assertEquals(1, actionsNoEmpty.size());
        assertEquals(24, actionsNoEmpty.get(0).getCount());
        assertEquals(0, actionsNoEmpty.get(0).getTargetSlot());

        // Allowing empty slots: fills slot 0 with 24, remaining 40 spills into next empty slot 1
        List<QuickDepositPlanner.MoveAction> actionsWithEmpty = QuickDepositPlanner.planDeposit(playerInv, chestInv, 27, true);
        assertEquals(2, actionsWithEmpty.size());
        assertEquals(24, actionsWithEmpty.get(0).getCount());
        assertEquals(0, actionsWithEmpty.get(0).getTargetSlot());
        assertEquals(40, actionsWithEmpty.get(1).getCount());
        assertEquals(1, actionsWithEmpty.get(1).getTargetSlot());
    }

    @Test
    public void testDepositRespectsMaxStackSize16ForEnderPearls() {
        List<ItemEntry> playerInv = List.of(
                new ItemEntry("minecraft:ender_pearl", 16, 9)
        );

        List<ItemEntry> chestInv = List.of(
                new ItemEntry("minecraft:ender_pearl", 10, 0)
        );

        // Max stack is 16: slot 0 only has room for 6 pearls
        List<QuickDepositPlanner.MoveAction> actions = QuickDepositPlanner.planDeposit(playerInv, chestInv, 27, false);
        assertEquals(1, actions.size());
        assertEquals("minecraft:ender_pearl", actions.get(0).getItemId());
        assertEquals(6, actions.get(0).getCount());
        assertEquals(0, actions.get(0).getTargetSlot());

        // With empty slots allowed: remaining 10 pearls go to empty slot 1
        List<QuickDepositPlanner.MoveAction> actionsWithEmpty = QuickDepositPlanner.planDeposit(playerInv, chestInv, 27, true);
        assertEquals(2, actionsWithEmpty.size());
        assertEquals(6, actionsWithEmpty.get(0).getCount());
        assertEquals(0, actionsWithEmpty.get(0).getTargetSlot());
        assertEquals(10, actionsWithEmpty.get(1).getCount());
        assertEquals(1, actionsWithEmpty.get(1).getTargetSlot());
    }

    @Test
    public void testDepositToolsNeverStack() {
        List<ItemEntry> playerInv = List.of(
                new ItemEntry("minecraft:diamond_sword", 1, 9)
        );

        List<ItemEntry> chestInv = List.of(
                new ItemEntry("minecraft:diamond_sword", 1, 0)
        );

        // Unstackable item (maxStack 1): cannot stack into slot 0
        List<QuickDepositPlanner.MoveAction> actionsNoEmpty = QuickDepositPlanner.planDeposit(playerInv, chestInv, 27, false);
        assertTrue(actionsNoEmpty.isEmpty(), "Tools should never stack onto existing tools");

        // With allowEmptySlots true: goes to empty slot 1
        List<QuickDepositPlanner.MoveAction> actionsWithEmpty = QuickDepositPlanner.planDeposit(playerInv, chestInv, 27, true);
        assertEquals(1, actionsWithEmpty.size());
        assertEquals(1, actionsWithEmpty.get(0).getTargetSlot());
        assertEquals(1, actionsWithEmpty.get(0).getCount());
    }

    @Test
    public void testDepositMultiplePlayerStacks() {
        List<ItemEntry> playerInv = List.of(
                new ItemEntry("minecraft:oak_planks", 64, 9),
                new ItemEntry("minecraft:oak_planks", 64, 10)
        );

        List<ItemEntry> chestInv = List.of(
                new ItemEntry("minecraft:oak_planks", 50, 0)
        );

        List<QuickDepositPlanner.MoveAction> actions = QuickDepositPlanner.planDeposit(playerInv, chestInv, 27, true);
        assertEquals(3, actions.size());

        // First stack: 14 into slot 0 (now 64), 50 into slot 1
        assertEquals(9, actions.get(0).getSourceSlot());
        assertEquals(0, actions.get(0).getTargetSlot());
        assertEquals(14, actions.get(0).getCount());

        assertEquals(9, actions.get(1).getSourceSlot());
        assertEquals(1, actions.get(1).getTargetSlot());
        assertEquals(50, actions.get(1).getCount());

        // Second stack: all 64 into slot 2
        assertEquals(10, actions.get(2).getSourceSlot());
        assertEquals(2, actions.get(2).getTargetSlot());
        assertEquals(64, actions.get(2).getCount());
    }

    @Test
    public void testDepositWithContainerSnapshot() {
        ContainerSnapshot snapshot = new ContainerSnapshot("minecraft:overworld", 100, 64, 100, ContainerType.CHEST, List.of(
                new ItemEntry("minecraft:gold_ingot", 30, 0)
        ), System.currentTimeMillis());

        List<ItemEntry> playerInv = List.of(
                new ItemEntry("minecraft:gold_ingot", 64, 9)
        );

        List<QuickDepositPlanner.MoveAction> actions = QuickDepositPlanner.planDeposit(playerInv, snapshot, false);
        assertEquals(1, actions.size());
        assertEquals(34, actions.get(0).getCount());
        assertEquals(0, actions.get(0).getTargetSlot());
    }

    @Test
    public void testDepositContainerFull() {
        // Container has limit 1, slot 0 already has 64 items
        List<ItemEntry> playerInv = List.of(
                new ItemEntry("minecraft:stone", 64, 9)
        );
        List<ItemEntry> chestInv = List.of(
                new ItemEntry("minecraft:stone", 64, 0)
        );

        List<QuickDepositPlanner.MoveAction> actions = QuickDepositPlanner.planDeposit(playerInv, chestInv, 1, true);
        assertTrue(actions.isEmpty());
    }
}

package com.slopgobbler.vaultfinder.automation;

import com.slopgobbler.vaultfinder.model.ContainerSnapshot;
import com.slopgobbler.vaultfinder.model.ItemEntry;

import java.util.*;

/**
 * Calculates optimal quick-deposit transfer transactions from player inventory to containers.
 * Respects maximum stack sizes (64 for blocks/materials, 16 for ender pearls/snowballs, 1 for tools/armor/potions),
 * matches items already present in the target container, and supports the allowEmptySlots parameter.
 */
public class QuickDepositPlanner {

    /**
     * Represents a single calculated transfer move from a player slot to a container slot.
     */
    public static class MoveAction {
        private final int sourceSlot;
        private final int targetSlot;
        private final String itemId;
        private final int count;

        public MoveAction(int sourceSlot, int targetSlot, String itemId, int count) {
            this.sourceSlot = sourceSlot;
            this.targetSlot = targetSlot;
            this.itemId = itemId != null ? itemId : "minecraft:air";
            this.count = count;
        }

        public int getSourceSlot() {
            return sourceSlot;
        }

        public int getTargetSlot() {
            return targetSlot;
        }

        public String getItemId() {
            return itemId;
        }

        public int getCount() {
            return count;
        }

        // Semantic convenience aliases
        public int getPlayerSlot() {
            return sourceSlot;
        }

        public int getContainerSlot() {
            return targetSlot;
        }

        public int getAmount() {
            return count;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MoveAction that)) return false;
            return sourceSlot == that.sourceSlot &&
                    targetSlot == that.targetSlot &&
                    count == that.count &&
                    Objects.equals(itemId, that.itemId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sourceSlot, targetSlot, itemId, count);
        }

        @Override
        public String toString() {
            return "MoveAction{" +
                    "playerSlot=" + sourceSlot +
                    " -> containerSlot=" + targetSlot +
                    ", item='" + itemId + '\'' +
                    ", count=" + count +
                    '}';
        }
    }

    private static class VirtualSlot {
        String itemId;
        int count;

        VirtualSlot(String itemId, int count) {
            this.itemId = itemId;
            this.count = count;
        }
    }

    /**
     * Plans quick-deposit transfers with an explicit container slot capacity.
     *
     * @param playerInventory    Items in player inventory
     * @param containerInventory Items in target container
     * @param containerSlotLimit Total number of slots available in container (e.g. 27 for chest, 54 for double chest)
     * @param allowEmptySlots    Whether to allow depositing items into unoccupied container slots
     * @return Sequential list of transfer actions
     */
    public static List<MoveAction> planDeposit(List<ItemEntry> playerInventory,
                                              List<ItemEntry> containerInventory,
                                              int containerSlotLimit,
                                              boolean allowEmptySlots) {
        List<MoveAction> actions = new ArrayList<>();
        if (playerInventory == null || containerInventory == null || containerSlotLimit <= 0) {
            return actions;
        }

        Map<Integer, VirtualSlot> containerSlots = new HashMap<>();
        Set<String> existingItemTypes = new HashSet<>();

        for (ItemEntry item : containerInventory) {
            if (item != null && item.getItemId() != null && !item.getItemId().equals("minecraft:air") && item.getCount() > 0) {
                int slot = item.getSlotIndex();
                if (slot >= 0 && slot < containerSlotLimit) {
                    containerSlots.put(slot, new VirtualSlot(item.getItemId(), item.getCount()));
                    existingItemTypes.add(item.getItemId());
                }
            }
        }

        for (ItemEntry playerItem : playerInventory) {
            if (playerItem == null || playerItem.getItemId() == null ||
                    playerItem.getItemId().equals("minecraft:air") || playerItem.getCount() <= 0) {
                continue;
            }

            String itemId = playerItem.getItemId();
            int remainingPlayerCount = playerItem.getCount();
            int playerSlot = playerItem.getSlotIndex();
            int maxStack = StackSizeResolver.getMaxStackSize(itemId);

            boolean isExisting = existingItemTypes.contains(itemId);
            if (!isExisting && !allowEmptySlots) {
                continue;
            }

            // Step 1: Deposit into existing non-full container slots holding the same item (stackable items only)
            if (maxStack > 1) {
                for (int slot = 0; slot < containerSlotLimit && remainingPlayerCount > 0; slot++) {
                    VirtualSlot cSlot = containerSlots.get(slot);
                    if (cSlot != null && cSlot.itemId.equals(itemId) && cSlot.count < maxStack) {
                        int space = maxStack - cSlot.count;
                        int toMove = Math.min(remainingPlayerCount, space);
                        actions.add(new MoveAction(playerSlot, slot, itemId, toMove));
                        cSlot.count += toMove;
                        remainingPlayerCount -= toMove;
                    }
                }
            }

            // Step 2: If items remain and allowEmptySlots is true, deposit into empty container slots
            if (remainingPlayerCount > 0 && allowEmptySlots) {
                for (int slot = 0; slot < containerSlotLimit && remainingPlayerCount > 0; slot++) {
                    if (!containerSlots.containsKey(slot)) {
                        int toMove = Math.min(remainingPlayerCount, maxStack);
                        actions.add(new MoveAction(playerSlot, slot, itemId, toMove));
                        containerSlots.put(slot, new VirtualSlot(itemId, toMove));
                        remainingPlayerCount -= toMove;
                    }
                }
            }
        }

        return actions;
    }

    /**
     * Convenience overload calculating deposit plan with default slot limit inferred from container contents.
     */
    public static List<MoveAction> planDeposit(List<ItemEntry> playerInventory,
                                              List<ItemEntry> containerInventory,
                                              boolean allowEmptySlots) {
        int limit = 27;
        if (containerInventory != null) {
            for (ItemEntry entry : containerInventory) {
                if (entry != null && entry.getSlotIndex() >= limit) {
                    limit = entry.getSlotIndex() + 1;
                }
            }
        }
        return planDeposit(playerInventory, containerInventory, limit, allowEmptySlots);
    }

    /**
     * Convenience overload for planning deposit into a ContainerSnapshot.
     */
    public static List<MoveAction> planDeposit(List<ItemEntry> playerInventory,
                                              ContainerSnapshot containerSnapshot,
                                              boolean allowEmptySlots) {
        if (containerSnapshot == null) {
            return Collections.emptyList();
        }
        int capacity = containerSnapshot.getType() != null ? containerSnapshot.getType().getSlotCount() : 27;
        return planDeposit(playerInventory, containerSnapshot.getItems(), capacity, allowEmptySlots);
    }
}

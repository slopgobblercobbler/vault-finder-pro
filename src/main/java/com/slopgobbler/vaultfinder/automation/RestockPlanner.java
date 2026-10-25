package com.slopgobbler.vaultfinder.automation;

import com.slopgobbler.vaultfinder.model.ContainerSnapshot;
import com.slopgobbler.vaultfinder.model.ItemEntry;

import java.util.*;

/**
 * Calculates optimal sequential restock actions from container inventories to player hotbars/inventories.
 * Identifies depleted stacks, respects maximum stack limits (e.g. 64 for blocks, 16 for pearls, 1 for tools),
 * and tracks container slot reserves across multiple restock actions without overdrawing.
 */
public class RestockPlanner {

    /**
     * Represents a single restock transfer action from a container slot to a player slot.
     */
    public static class RestockAction {
        private final int containerSlot;
        private final int playerSlot;
        private final String itemId;
        private final int amountNeeded;

        public RestockAction(int containerSlot, int playerSlot, String itemId, int amountNeeded) {
            this.containerSlot = containerSlot;
            this.playerSlot = playerSlot;
            this.itemId = itemId != null ? itemId : "minecraft:air";
            this.amountNeeded = amountNeeded;
        }

        public int getContainerSlot() {
            return containerSlot;
        }

        public int getPlayerSlot() {
            return playerSlot;
        }

        public String getItemId() {
            return itemId;
        }

        public int getAmountNeeded() {
            return amountNeeded;
        }

        // Convenience aliases
        public int getCount() {
            return amountNeeded;
        }

        public int getAmount() {
            return amountNeeded;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RestockAction that)) return false;
            return containerSlot == that.containerSlot &&
                    playerSlot == that.playerSlot &&
                    amountNeeded == that.amountNeeded &&
                    Objects.equals(itemId, that.itemId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(containerSlot, playerSlot, itemId, amountNeeded);
        }

        @Override
        public String toString() {
            return "RestockAction{" +
                    "containerSlot=" + containerSlot +
                    " -> playerSlot=" + playerSlot +
                    ", item='" + itemId + '\'' +
                    ", amount=" + amountNeeded +
                    '}';
        }
    }

    private static class VirtualSlot {
        final int slotIndex;
        final String itemId;
        int count;

        VirtualSlot(int slotIndex, String itemId, int count) {
            this.slotIndex = slotIndex;
            this.itemId = itemId;
            this.count = count;
        }
    }

    /**
     * Plans restock transfers for a player hotbar / inventory from a container's items.
     * Topping up any depleted item stacks up to their maximum stack limit.
     *
     * @param playerHotbar       Current player hotbar or inventory items
     * @param containerInventory Items available in the container
     * @return Sequential list of restock actions
     */
    public static List<RestockAction> planRestock(List<ItemEntry> playerHotbar, List<ItemEntry> containerInventory) {
        List<RestockAction> actions = new ArrayList<>();
        if (playerHotbar == null || containerInventory == null || playerHotbar.isEmpty() || containerInventory.isEmpty()) {
            return actions;
        }

        // Virtual container state indexed by slotIndex to prevent overdrawing
        Map<Integer, VirtualSlot> containerSlots = new LinkedHashMap<>();
        for (ItemEntry cItem : containerInventory) {
            if (cItem != null && cItem.getItemId() != null && !cItem.getItemId().equals("minecraft:air") && cItem.getCount() > 0) {
                containerSlots.put(cItem.getSlotIndex(), new VirtualSlot(cItem.getSlotIndex(), cItem.getItemId(), cItem.getCount()));
            }
        }

        for (ItemEntry hotbarItem : playerHotbar) {
            if (hotbarItem == null || hotbarItem.getItemId() == null || hotbarItem.getItemId().equals("minecraft:air")) {
                continue;
            }

            int currentCount = hotbarItem.getCount();
            int maxStack = StackSizeResolver.getMaxStackSize(hotbarItem.getItemId());
            if (currentCount >= maxStack) {
                continue;
            }

            int needed = maxStack - currentCount;
            String targetItemId = hotbarItem.getItemId();

            for (VirtualSlot cSlot : containerSlots.values()) {
                if (cSlot.count > 0 && cSlot.itemId.equals(targetItemId)) {
                    int toTake = Math.min(needed, cSlot.count);
                    actions.add(new RestockAction(cSlot.slotIndex, hotbarItem.getSlotIndex(), targetItemId, toTake));
                    cSlot.count -= toTake;
                    needed -= toTake;
                    if (needed <= 0) {
                        break;
                    }
                }
            }
        }

        return actions;
    }

    /**
     * Plans restock transfers to restore a target layout from a container inventory.
     *
     * @param currentLayout      Current player items
     * @param targetLayout       Desired player layout with target items and counts
     * @param containerInventory Container items available
     * @return Sequential list of restock actions
     */
    public static List<RestockAction> planRestockFromTarget(List<ItemEntry> currentLayout,
                                                           List<ItemEntry> targetLayout,
                                                           List<ItemEntry> containerInventory) {
        List<RestockAction> actions = new ArrayList<>();
        if (targetLayout == null || containerInventory == null || targetLayout.isEmpty()) {
            return actions;
        }

        // Map current items by slot
        Map<Integer, ItemEntry> currentBySlot = new HashMap<>();
        if (currentLayout != null) {
            for (ItemEntry item : currentLayout) {
                if (item != null) {
                    currentBySlot.put(item.getSlotIndex(), item);
                }
            }
        }

        // Virtual container state
        Map<Integer, VirtualSlot> containerSlots = new LinkedHashMap<>();
        for (ItemEntry cItem : containerInventory) {
            if (cItem != null && cItem.getItemId() != null && !cItem.getItemId().equals("minecraft:air") && cItem.getCount() > 0) {
                containerSlots.put(cItem.getSlotIndex(), new VirtualSlot(cItem.getSlotIndex(), cItem.getItemId(), cItem.getCount()));
            }
        }

        for (ItemEntry targetItem : targetLayout) {
            if (targetItem == null || targetItem.getItemId() == null || targetItem.getItemId().equals("minecraft:air")) {
                continue;
            }

            int slot = targetItem.getSlotIndex();
            ItemEntry current = currentBySlot.get(slot);

            int currentCount = 0;
            if (current != null && current.getItemId().equals(targetItem.getItemId())) {
                currentCount = current.getCount();
            }

            int targetCount = targetItem.getCount() > 0
                    ? targetItem.getCount()
                    : StackSizeResolver.getMaxStackSize(targetItem.getItemId());

            if (currentCount >= targetCount) {
                continue;
            }

            int needed = targetCount - currentCount;
            String targetItemId = targetItem.getItemId();

            for (VirtualSlot cSlot : containerSlots.values()) {
                if (cSlot.count > 0 && cSlot.itemId.equals(targetItemId)) {
                    int toTake = Math.min(needed, cSlot.count);
                    actions.add(new RestockAction(cSlot.slotIndex, slot, targetItemId, toTake));
                    cSlot.count -= toTake;
                    needed -= toTake;
                    if (needed <= 0) {
                        break;
                    }
                }
            }
        }

        return actions;
    }

    /**
     * Convenience overload for restocking from a ContainerSnapshot.
     */
    public static List<RestockAction> planRestock(List<ItemEntry> playerHotbar, ContainerSnapshot containerSnapshot) {
        if (containerSnapshot == null) {
            return Collections.emptyList();
        }
        return planRestock(playerHotbar, containerSnapshot.getItems());
    }
}

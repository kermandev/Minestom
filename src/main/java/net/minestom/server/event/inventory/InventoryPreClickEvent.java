package net.minestom.server.event.inventory;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.InventoryEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.event.trait.mutation.EventMutatorCancellable;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called before {@link InventoryClickEvent}, used to potentially cancel the click.
 */
public record InventoryPreClickEvent(@NotNull AbstractInventory inventory, @NotNull Player player, int slot, @NotNull ClickType clickType, @NotNull ItemStack clickedItem, @NotNull ItemStack cursorItem, boolean cancelled) implements InventoryEvent, PlayerInstanceEvent, CancellableEvent<InventoryPreClickEvent> {

    public InventoryPreClickEvent(@NotNull AbstractInventory inventory,
                                  @NotNull Player player,
                                  int slot, @NotNull ClickType clickType,
                                  @NotNull ItemStack clickedItem, @NotNull ItemStack cursorItem) {
        this(inventory, player, slot, clickType, clickedItem, cursorItem, false);
    }

    /**
     * Gets the player who is trying to click on the inventory.
     *
     * @return the player who clicked
     */
    @Override
    public @NotNull Player player() {
        return player;
    }

    /**
     * Gets the clicked slot number.
     *
     * @return the clicked slot number
     */
    @Override
    public int slot() {
        return slot;
    }

    /**
     * Gets the click type.
     *
     * @return the click type
     */
    @Override
    public @NotNull ClickType clickType() {
        return clickType;
    }

    /**
     * Gets the item who have been clicked.
     *
     * @return the clicked item
     */
    @Override
    public @NotNull ItemStack clickedItem() {
        return clickedItem;
    }

    /**
     * Gets the item who was in the player cursor.
     *
     * @return the cursor item
     */
    @Override
    public @NotNull ItemStack cursorItem() {
        return cursorItem;
    }

    @Override
    public @NotNull Mutator mutator() {
        return new Mutator(this);
    }

    public static final class Mutator extends EventMutatorCancellable.Simple<InventoryPreClickEvent> {
        private ItemStack clickedItem;
        private ItemStack cursorItem;

        public Mutator(InventoryPreClickEvent event) {
            super(event);
            this.clickedItem = event.clickedItem;
            this.cursorItem = event.cursorItem;
        }

        /**
         * Gets the item who have been clicked.
         *
         * @return the clicked item
         */
        @NotNull
        public ItemStack getClickedItem() {
            return clickedItem;
        }

        /**
         * Changes the clicked item.
         *
         * @param clickedItem the clicked item
         */
        public void setClickedItem(@NotNull ItemStack clickedItem) {
            this.clickedItem = clickedItem;
        }

        /**
         * Gets the item who was in the player cursor.
         *
         * @return the cursor item
         */
        @NotNull
        public ItemStack getCursorItem() {
            return cursorItem;
        }

        /**
         * Changes the cursor item.
         *
         * @param cursorItem the cursor item
         */
        public void setCursorItem(@NotNull ItemStack cursorItem) {
            this.cursorItem = cursorItem;
        }

        @Override
        public @NotNull InventoryPreClickEvent mutated() {
            return new InventoryPreClickEvent(this.originalEvent.inventory, this.originalEvent.player, this.originalEvent.slot, this.originalEvent.clickType, this.clickedItem, this.cursorItem, this.isCancelled());
        }
    }
}

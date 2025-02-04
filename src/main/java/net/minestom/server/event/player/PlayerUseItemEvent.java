package net.minestom.server.event.player;

import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.ItemEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.event.trait.mutation.EventMutatorCancellable;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Event when an item is used without clicking on a block.
 */
public record PlayerUseItemEvent(@NotNull Player player, @NotNull PlayerHand hand, @NotNull ItemStack itemStack, long itemUseTime, boolean cancelled) implements PlayerInstanceEvent, ItemEvent, CancellableEvent<PlayerUseItemEvent> {

    public PlayerUseItemEvent(@NotNull Player player, @NotNull PlayerHand hand, @NotNull ItemStack itemStack, long itemUseTime) {
        this(player, hand, itemStack, itemUseTime, false);
    }

    /**
     * Gets which hand the player used.
     *
     * @return the hand used
     */
    @Override
    public @NotNull PlayerHand hand() {
        return hand;
    }

    /**
     * Gets the item which has been used.
     *
     * @return the item
     */
    @Override
    public @NotNull ItemStack itemStack() {
        return itemStack;
    }

    /**
     * Gets the item usage duration. After this amount of milliseconds,
     * the animation will stop automatically and {@link net.minestom.server.event.item.PlayerFinishItemUseEvent} is called.
     *
     * @return the item use time
     */
    @Override
    public long itemUseTime() {
        return itemUseTime;
    }

    @Override
    public @NotNull Mutator mutator() {
        return new Mutator(this);
    }

    public static final class Mutator extends EventMutatorCancellable.Simple<PlayerUseItemEvent> {
        private long itemUseTime;

        public Mutator(PlayerUseItemEvent event) {
            super(event);
            this.itemUseTime = event.itemUseTime;
        }

        /**
         * Gets the item usage duration. After this amount of milliseconds,
         * the animation will stop automatically and {@link net.minestom.server.event.item.PlayerFinishItemUseEvent} is called.
         *
         * @return the item use time
         */
        public long getItemUseTime() {
            return itemUseTime;
        }

        /**
         * Changes the item usage duration.
         *
         * @param itemUseTime the new item use time
         */
        public void setItemUseTime(long itemUseTime) {
            this.itemUseTime = itemUseTime;
        }

        @Override
        public @NotNull PlayerUseItemEvent mutated() {
            return new PlayerUseItemEvent(this.originalEvent.player, this.originalEvent.hand, this.originalEvent.itemStack, this.itemUseTime, this.isCancelled());
        }
    }
}

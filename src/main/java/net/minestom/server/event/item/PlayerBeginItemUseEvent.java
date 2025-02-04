package net.minestom.server.event.item;

import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.ItemEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.event.trait.mutation.EventMutatorCancellable;
import net.minestom.server.item.ItemAnimation;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player begins using an item with the item, animation, and duration.
 *
 * <p>Setting the use duration to zero or cancelling the event will prevent consumption.</p>
 */
public value record PlayerBeginItemUseEvent(@NotNull Player player, @NotNull PlayerHand hand,
                                      @NotNull ItemStack itemStack, @NotNull ItemAnimation animation,
                                      long itemUseDuration, boolean cancelled) implements PlayerInstanceEvent, ItemEvent, CancellableEvent<PlayerBeginItemUseEvent> {

    public PlayerBeginItemUseEvent(@NotNull Player player, @NotNull PlayerHand hand,
                                   @NotNull ItemStack itemStack, @NotNull ItemAnimation animation,
                                   long itemUseDuration) {
        this(player, hand, itemStack, animation, itemUseDuration, false);
    }

    /**
     * Returns the item use duration, in ticks. A duration of zero will prevent consumption (same effect as cancellation).
     *
     * @return the current item use duration
     */
    @Override
    public long itemUseDuration() {
        return itemUseDuration;
    }

    @Override
    public @NotNull Mutator mutator() {
        return new Mutator(this);
    }

    public static class Mutator extends EventMutatorCancellable.Simple<PlayerBeginItemUseEvent> {
        private long itemUseDuration;

        public Mutator(PlayerBeginItemUseEvent event) {
            super(event);
            this.itemUseDuration = event.itemUseDuration;
        }

        /**
         * Returns the item use duration, in ticks. A duration of zero will prevent consumption (same effect as cancellation).
         *
         * @return the current item use duration
         */
        public long getItemUseDuration() {
            return itemUseDuration;
        }

        /**
         * Sets the item use duration, in ticks.
         */
        public void setItemUseDuration(long itemUseDuration) {
            Check.argCondition(itemUseDuration < 0, "Item use duration cannot be negative");
            this.itemUseDuration = itemUseDuration;
        }

        @Override
        public @NotNull PlayerBeginItemUseEvent mutated() {
            return new PlayerBeginItemUseEvent(this.originalEvent.player, this.originalEvent.hand, this.originalEvent.itemStack, this.originalEvent.animation, this.itemUseDuration, this.isCancelled());
        }
    }
}

package net.minestom.server.event.player;

import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.event.trait.mutation.EventMutatorCancellable;
import org.jetbrains.annotations.NotNull;

/**
 * Called when the player swings his hand.
 */
public record PlayerHandAnimationEvent(@NotNull Player player, @NotNull PlayerHand hand, boolean cancelled) implements PlayerInstanceEvent, CancellableEvent<PlayerHandAnimationEvent> {

    public PlayerHandAnimationEvent(@NotNull Player player, @NotNull PlayerHand hand) {
        this(player, hand, false);
    }

    /**
     * Gets the hand used.
     *
     * @return the hand
     */
    @Override
    public @NotNull PlayerHand hand() {
        return hand;
    }


    @Override
    public @NotNull Mutator mutator() {
        return new Mutator(this);
    }

    public static final class Mutator extends EventMutatorCancellable.Simple<PlayerHandAnimationEvent> {
        public Mutator(PlayerHandAnimationEvent event) {
            super(event);
        }

        @Override
        public @NotNull PlayerHandAnimationEvent mutated() {
            return new PlayerHandAnimationEvent(this.originalEvent.player, this.originalEvent.hand, this.isCancelled());
        }
    }
}

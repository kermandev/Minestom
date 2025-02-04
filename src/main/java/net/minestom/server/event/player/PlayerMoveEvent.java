package net.minestom.server.event.player;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.event.trait.mutation.EventMutatorCancellable;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player is modifying his position.
 */
public record PlayerMoveEvent(@NotNull Player player, @NotNull Pos newPosition, boolean onGround, boolean cancelled) implements PlayerInstanceEvent, CancellableEvent<PlayerMoveEvent> {

    public PlayerMoveEvent(@NotNull Player player, @NotNull Pos newPosition, boolean onGround) {
        this(player, newPosition, onGround, false);
    }

    /**
     * Gets the target position.
     *
     * @return the new position
     */
    public @NotNull Pos getNewPosition() {
        return newPosition;
    }

    /**
     * Gets if the player is now on the ground.
     * This is the original value that the client sent,
     * and is not modified by setting the new position.
     *
     * @return onGround
     */
    @Override
    public boolean onGround() {
        return onGround;
    }

    @Override
    public @NotNull Mutator mutator() {
        return new Mutator(this);
    }

    public static final class Mutator extends EventMutatorCancellable.Simple<PlayerMoveEvent> {
        private Pos newPosition;

        public Mutator(PlayerMoveEvent event) {
            super(event);
            this.newPosition = event.newPosition;
        }


        /**
         * Gets the target position.
         *
         * @return the new position
         */
        public @NotNull Pos getNewPosition() {
            return newPosition;
        }

        /**
         * Changes the target position.
         *
         * @param newPosition the new target position
         */
        public void setNewPosition(@NotNull Pos newPosition) {
            this.newPosition = newPosition;
        }

        @Override
        public @NotNull PlayerMoveEvent mutated() {
            return new PlayerMoveEvent(this.originalEvent.player, this.newPosition, this.originalEvent.onGround, this.isCancelled());
        }
    }
}

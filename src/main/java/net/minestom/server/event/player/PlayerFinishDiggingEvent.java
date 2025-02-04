package net.minestom.server.event.player;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.BlockEvent;
import net.minestom.server.event.trait.MutableEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.event.trait.mutation.EventMutator;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a {@link Player} successfully finishes digging a block
 */
public record PlayerFinishDiggingEvent(@NotNull Player player, @NotNull Block block, @NotNull BlockVec blockPosition) implements PlayerInstanceEvent, BlockEvent, MutableEvent<PlayerFinishDiggingEvent> {

    /**
     * Gets the block which was dug.
     *
     * @return the block
     */
    @Override
    public @NotNull Block block() {
        return block;
    }

    /**
     * Gets the block position.
     *
     * @return the block position
     */
    @Override
    public @NotNull BlockVec blockPosition() {
        return blockPosition;
    }

    @Override
    public @NotNull Mutator mutator() {
        return new Mutator(this);
    }

    public static final class Mutator extends EventMutator.Simple<PlayerFinishDiggingEvent> {
        private Block block;

        public Mutator(PlayerFinishDiggingEvent event) {
            super(event);
            this.block = event.block;
        }

        /**
         * Changes which block was dug
         * <p>
         * This has somewhat odd behavior;
         * If you set it from a previously solid block to a non-solid block
         * then cancel the respective {@link PlayerBlockBreakEvent}
         * it will allow the player to phase through the block and into the floor
         * (only if the player is standing on top of the block)
         *
         * @param block the block to set the result to
         */
        public void setBlock(@NotNull Block block) {
            this.block = block;
        }

        /**
         * Gets the block which was dug.
         *
         * @return the block
         */
        public @NotNull Block getBlock() {
            return block;
        }

        @Override
        public @NotNull PlayerFinishDiggingEvent mutated() {
            return new PlayerFinishDiggingEvent(this.originalEvent.player, this.block, this.originalEvent.blockPosition);
        }
    }
}

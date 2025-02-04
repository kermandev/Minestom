package net.minestom.server.event.entity;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.EntityInstanceEvent;
import net.minestom.server.event.trait.MutableEvent;
import net.minestom.server.event.trait.mutation.EventMutatorCancellable;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a velocity is applied to an entity using {@link Entity#setVelocity(Vec)}.
 */
public record EntityVelocityEvent(@NotNull Entity entity, @NotNull Vec velocity, boolean cancelled) implements EntityInstanceEvent, CancellableEvent<EntityVelocityEvent> {

    public EntityVelocityEvent(@NotNull Entity entity, @NotNull Vec velocity) {
        this(entity, velocity, false);
    }

    /**
     * Gets the enity who will be affected by {@link #velocity()}.
     *
     * @return the entity
     */
    @Override
    public @NotNull Entity entity() {
        return entity;
    }

    /**
     * Gets the velocity which will be applied.
     *
     * @return the velocity
     */
    @Override
    public @NotNull Vec velocity() {
        return velocity;
    }

    @Override
    public @NotNull Mutator mutator() {
        return new Mutator(this);
    }

    public static final class Mutator extends EventMutatorCancellable.Simple<EntityVelocityEvent> {
        private Vec velocity;

        public Mutator(EntityVelocityEvent event) {
            super(event);
            this.velocity = event.velocity;
        }

        public @NotNull Vec getVelocity() {
            return velocity;
        }

        public void setVelocity(@NotNull Vec velocity) {
            this.velocity = velocity;
        }

        @Override
        public @NotNull EntityVelocityEvent mutated() {
            return new EntityVelocityEvent(this.originalEvent.entity, this.velocity, this.isCancelled());
        }
    }
}

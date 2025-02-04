package net.minestom.server.event.entity;

import net.minestom.server.entity.Entity;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.EntityInstanceEvent;
import net.minestom.server.event.trait.mutation.EventMutatorCancellable;
import org.jetbrains.annotations.NotNull;

public record EntitySetFireEvent(@NotNull Entity entity, int fireTicks, boolean cancelled) implements EntityInstanceEvent, CancellableEvent<EntitySetFireEvent> {
    public EntitySetFireEvent(@NotNull Entity entity, int fireTicks) {
        this(entity, fireTicks, false);
    }

    @Override
    public @NotNull Mutator mutator() {
        return new Mutator(this);
    }

    public static final class Mutator extends EventMutatorCancellable.Simple<EntitySetFireEvent> {
        private int fireTicks;

        public Mutator(EntitySetFireEvent event) {
            super(event);
            this.fireTicks = event.fireTicks;
        }

        public int getFireTicks() {
            return fireTicks;
        }

        public void setFireTicks(int fireTicks) {
            this.fireTicks = fireTicks;
        }

        @Override
        public @NotNull EntitySetFireEvent mutated() {
            return new EntitySetFireEvent(this.originalEvent.entity, this.fireTicks, this.isCancelled());
        }
    }
}

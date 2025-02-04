package net.minestom.server.event.trait.mutation;

import net.minestom.server.event.Event;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.MutableEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface EventMutatorCancellable<T extends MutableEvent<T>> extends EventMutator<T> {

    /**
     * Gets if the {@link Event} should be cancelled or not.
     *
     * @return true if the event should be cancelled
     */
    boolean isCancelled();

    /**
     * Marks the {@link Event} as cancelled or not.
     *
     * @param cancel true if the event should be cancelled, false otherwise
     */
    void setCancelled(boolean cancel);

    /**
     * Marks the {@link Event} as cancelled or not.
     * <p>
     * Use @{@link CancellableEvent#mutateCancel(Supplier)} if possible to not create unnecessary mutators.
     *
     * @param cancel true if the event should be cancelled, false otherwise
     * @return self instance
     */
     default EventMutatorCancellable<T> withCancelled(boolean cancel) {
        setCancelled(cancel);
        return this;
     }

    /**
     * Marks the {@link Event} as cancelled or not.
     * <p>
     * Use @{@link CancellableEvent#mutateCancel(Supplier)} if possible to not create unnecessary mutators.
     *
     * @param supplier should the event cancel.
     * @return self instance
     */
     default EventMutatorCancellable<T> withCancelled(Supplier<Boolean> supplier) {
         return withCancelled(supplier.get());
     }

    /**
     * Marks the {@link Event} as cancelled or not.
     * <p>
     * Use @{@link CancellableEvent#mutateCancel(Supplier)} if possible to not create unnecessary mutators.
     *
     * @param function should the event cancel and the current mutator.
     * @return self instance
     */
    default EventMutatorCancellable<T> withCancelled(Function<EventMutatorCancellable<T>, Boolean> function) {
         return withCancelled(function.apply(this));
    }

    /**
     * Simple cancelable implementation of {@link EventMutatorCancellable}.
     * <p>
     * This should only be used when most fields are final.
     *
     * @param <T> Event type
     */
    abstract class Simple<T extends CancellableEvent<T>> extends EventMutator.Simple<T> implements EventMutatorCancellable<T> {
        private boolean cancelled;
        
        public Simple(@NotNull T event) {
            super(event);
            this.cancelled = event.cancelled();
        }
        
        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancel) {
            this.cancelled = cancel;
        }
    }
}

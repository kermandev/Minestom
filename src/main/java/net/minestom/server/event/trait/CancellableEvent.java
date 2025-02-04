package net.minestom.server.event.trait;

import net.minestom.server.event.Event;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.trait.mutation.EventMutatorCancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Represents an {@link Event} which can be cancelled.
 * Called using {@link EventDispatcher#callCancellable(CancellableEvent, Runnable)}.
 */
public interface CancellableEvent<E extends CancellableEvent<E>> extends MutableEvent<E>, Event {

    /**
     * Gets if the {@link Event} is cancelled or not.
     */
    boolean cancelled();

    /**
     * Gets the {@link EventMutatorCancellable} for this event.
     *
     * @return the mutator
     */
    @Override
    @NotNull
    EventMutatorCancellable<E> mutator();

    /**
     * Gets the {@link EventMutatorCancellable} that is canceled if the condition is true, null otherwise.
     * <p>
     * Useful for one-liners.
     *
     * @param shouldMutate the condition to check
     * @return the mutator if the event should mutate, null otherwise
     */
    @Nullable
    default EventMutatorCancellable<E> mutateCancel(Supplier<Boolean> shouldMutate) {
        return !this.cancelled() && shouldMutate.get() ? mutator().withCancelled(true) : null;
    }
}

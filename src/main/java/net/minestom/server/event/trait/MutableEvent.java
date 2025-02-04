package net.minestom.server.event.trait;

import net.minestom.server.event.Event;
import net.minestom.server.event.trait.mutation.EventMutator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public interface MutableEvent<E extends MutableEvent<E>> extends Event {

    /**
     * Gets the {@link EventMutator} for this event.
     *
     * @return the mutator
     */
    @NotNull EventMutator<E> mutator();

    /**
     * Gets the {@link EventMutator} that is mutated if the condition is true, null otherwise.
     * <p>
     * Useful for one-liners.
     *
     * @param condition the condition to check
     * @return the mutator, if true, null otherwise
     */
    @Nullable
    default EventMutator<E> mutatorCondition(Supplier<Boolean> condition) {
        return condition.get() ? mutator() : null;
    }
}

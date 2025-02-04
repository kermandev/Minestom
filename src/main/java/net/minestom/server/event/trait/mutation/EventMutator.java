package net.minestom.server.event.trait.mutation;

import net.minestom.server.event.trait.MutableEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface EventMutator<T extends MutableEvent<T>> {

    /**
     * Returns a newly constructed event.
     *
     * @return T the new instance.
     */
    @Contract(pure = true)
    @NotNull T mutated();

    /**
     * Simple cancelable implementation of {@link EventMutator}.
     * <p>
     * This should only be used when most fields are final.
     *
     * @param <T> Event type
     */
    abstract class Simple<T extends MutableEvent<T>> implements EventMutator<T> {
        protected final @NotNull T originalEvent;

        public Simple(@NotNull T originalEvent) {
            this.originalEvent = originalEvent;
        }
    }
}

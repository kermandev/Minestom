package net.minestom.server.event;

import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.RecursiveEvent;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents an event listener (handler) in an event graph.
 * <p>
 * A listener is responsible for executing some action based on an event triggering.
 *
 * @param <T> The event type being handled.
 */
public interface EventListener<T extends Event> {

    /**
     * The class used to filter the events to.
     * @return the event type
     */
    Class<T> eventType();

    /**
     * Runs the event and expects a {@link Result}.
     * <br>
     * For example, you can return a {@link Result#EXPIRED} to notify the node to drop this listener.
     * @param event the caller event
     * @return the {@link Result} of the listener.
     */
    Result run(T event);

    /**
     * Creates a {@link Builder} for the event type provided
     * @param eventType the class of the event to filter to.
     * @return a new builder
     * @param <T> the type of {@code eventType}
     */
    @Contract(pure = true)
    static <T extends Event> EventListener.Builder<T> builder(Class<T> eventType) {
        Objects.requireNonNull(eventType, "Event type cannot be null");
        return new EventListener.Builder<>(eventType);
    }

    /**
     * Create an event listener without any special options. The given listener will be executed
     * if the event passes all parent filtering.
     *
     * @param eventType The event type to handle
     * @param listener  The handler function
     * @param <T>       The event type to handle
     * @return An event listener with the given properties
     */
    @Contract(pure = true)
    static <T extends Event> EventListener<T> of(Class<T> eventType, Consumer<T> listener) {
        Objects.requireNonNull(eventType, "Event type cannot be null");
        Objects.requireNonNull(listener, "Listener cannot be null");
        if (CancellableEvent.class.isAssignableFrom(eventType) || RecursiveEvent.class.isAssignableFrom(eventType)) {
            return new Builder.ListenerImpl<>(eventType, event -> {
                if (event instanceof CancellableEvent cancellableEvent && cancellableEvent.isCancelled()) {
                    return Result.INVALID;
                }
                listener.accept(event);
                return Result.SUCCESS;
            });
        } else {
            return new Builder.ListenerImpl<>(eventType, event -> {
                listener.accept(event);
                return Result.SUCCESS;
            });
        }
    }

    /**
     * Builder to build event listeners. {@link #builder(Class)}
     * @param <T> the event type
     */
    class Builder<T extends Event> {
        private record ListenerImpl<T extends Event>(
                Class<T> eventType,
                Function<T, EventListener.Result> function
        ) implements EventListener<T> {
            @Override
            public Result run(T t) {
                return function.apply(t);
            }
        }

        private final Class<T> eventType;
        private final List<Predicate<T>> filters = new ArrayList<>();
        private boolean ignoreCancelled = false;
        private int expireCount;
        private @Nullable Predicate<T> expireWhen;
        private @Nullable Consumer<T> handler;

        /**
         * @deprecated Builder may become final in the future; Use {@link #builder(Class)} instead.
         */
        @ApiStatus.Obsolete(since = "1.21.8")
        protected Builder(Class<T> eventType) {
            this.eventType = eventType;
        }

        /**
         * Adds a filter to the executor of this listener. The executor will only
         * be called if this condition passes on the given event.
         */
        @Contract(value = "_ -> this")
        public EventListener.Builder<T> filter(Predicate<T> filter) {
            Objects.requireNonNull(filter, "filter cannot be null");
            this.filters.add(filter);
            return this;
        }

        /**
         * Specifies if the handler should still be called if {@link CancellableEvent#isCancelled()} returns {@code true}.
         * <p>
         * Default is set to {@code false}.
         *
         * @param ignoreCancelled True to continue processing the event when cancelled
         */
        @Contract(value = "_ -> this")
        public EventListener.Builder<T> ignoreCancelled(boolean ignoreCancelled) {
            this.ignoreCancelled = ignoreCancelled;
            return this;
        }

        /**
         * Removes this listener after it has been executed the given number of times.
         *
         * @param expireCount The number of times to execute
         * @throws IllegalArgumentException if {@code expireCount} is negative.
         */
        @Contract(value = "_ -> this")
        public EventListener.Builder<T> expireCount(int expireCount) {
            Check.argCondition(expireCount < 0, "expireCount cannot be negative");
            this.expireCount = expireCount;
            return this;
        }

        /**
         * Expires this listener when it passes the given condition. The expiration will
         * happen before the event is executed.
         *
         * @param expireWhen The condition to test
         */
        @Contract(value = "_ -> this")
        public EventListener.Builder<T> expireWhen(Predicate<T> expireWhen) {
            Objects.requireNonNull(expireWhen, "expireWhen cannot be null");
            this.expireWhen = expireWhen;
            return this;
        }

        /**
         * Sets the handler for this event listener. This will be executed if the listener passes
         * all conditions.
         */
        @Contract(value = "_ -> this")
        public EventListener.Builder<T> handler(Consumer<T> handler) {
            Objects.requireNonNull(handler, "handler cannot be null");
            this.handler = handler;
            return this;
        }

        /**
         * Builds the {@link EventListener} with all options
         * @return the new event listener typed {@link T}
         * @throws IllegalStateException if no observable actions can occur by having no consumers of the event.
         */
        @Contract(value = "-> new", pure = true)
        public EventListener<T> build() {
            final Predicate<T> expireWhen = this.expireWhen;
            final List<Predicate<T>> filters = List.copyOf(this.filters);
            final Consumer<T> handler = this.handler;
            Check.stateCondition(filters.isEmpty() && expireWhen == null && handler == null, "No observable effects for event type `{0}` are you sure this is correct?", eventType);
            final boolean ignoreCancelled = this.ignoreCancelled;
            final int expireCount = this.expireCount;
            final AtomicInteger expirationCount = expireCount > 0 ? new AtomicInteger(expireCount) : null;
            return new ListenerImpl<>(eventType, event -> {
                // Event cancellation
                if (!ignoreCancelled && event instanceof CancellableEvent cancellableEvent &&
                        cancellableEvent.isCancelled()) {
                    return Result.INVALID;
                }
                // Expiration predicate
                if (expireWhen != null && expireWhen.test(event)) {
                    return Result.EXPIRED;
                }
                // Filtering
                if (!filters.isEmpty()) {
                    for (var filter : filters) {
                        if (!filter.test(event)) {
                            // Cancelled
                            return Result.INVALID;
                        }
                    }
                }
                // Handler
                if (handler != null) {
                    handler.accept(event);
                }
                // Expiration count
                if (expirationCount != null && expirationCount.decrementAndGet() == 0) {
                    return Result.EXPIRED;
                }
                return Result.SUCCESS;
            });
        }
    }

    /**
     * Represents a result from running an event listener through {@link EventListener#run(Event)}
     * <br>
     * We use the keyword {@code consumed} as performing the actions to get the correct state.
     */
    enum Result {
        /**
         * The event was consumed fully.
         */
        SUCCESS,
        /**
         * The event did not meet the requirements to consume
         */
        INVALID,
        /**
         * The event listener has expired and is notifying the {@link EventNode} that it can be dropped.
         */
        EXPIRED,
        /**
         * The listener has thrown an exception, but not fully {@link #INVALID}.
         */
        EXCEPTION
    }
}

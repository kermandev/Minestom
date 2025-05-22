package net.minestom.server.entity;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * Descriptive how entities should be queried.
 * Offer potential indexing/spatial partitioning advantages over lazy looping.
 */
public sealed interface EntitySelector<E> extends BiPredicate<Point, E> permits EntitySelectorImpl {

    /**
     * Creates a selector.
     * <p>
     * If body is just empty, use our singletons {@link EntitySelectors#all()} or {@link EntitySelectors#players()}.
     * @param <E> the entity type
     * @param consumer a consumer that will be called with a builder
     * @return a selector
     */
    static <E> @NotNull EntitySelector<E> selector(@NotNull Consumer<@NotNull Builder> consumer) {
        EntitySelectorImpl.BuilderImpl<E> builder = new EntitySelectorImpl.BuilderImpl<>();
        consumer.accept(builder);
        return builder.build();
    }

    /**
     * Creates a selector that uses the property to match the value.
     * <p>
     * If you are doing a common operation in the builder, consider using {@link #selector(BiConsumer, T)}.
     *
     * @param <E> the entity type
     * @param <T> the property type
     * @param property the property to match
     * @param value the value to match
     * @return a selector that matches all entities
     */
    static <E, T> @NotNull EntitySelector<E> selector(@NotNull Property<?, T> property, T value) {
        return selector(builder -> builder.predicateEqual(property, value));
    }

    static <E, T> @NotNull EntitySelector<E> selector(@NotNull BiConsumer<Builder, T> property, T value) {
        return selector(builder -> property.accept(builder, value));
    }

    static <E, T> @NotNull Property<E, T> property(@NotNull String name, Class<E> type, Function<E, T> function) {
        return new EntitySelectorImpl.PropertyImpl<>(name, type, function);
    }

    static <E extends TagReadable, T> @NotNull Property<E, T> tagProperty(@NotNull Tag<T> tag) {
        //noinspection unchecked
        return EntitySelector.property(tag.getKey(), (Class<E>) TagReadable.class, e -> e.getTag(tag));
    }

    @Override
    boolean test(Point origin, E entity);

    boolean playerOnly();

    /**
     * The sorting method to use.
     * <p>
     * @implNote If the sort is {@link Sort#ARBITRARY}, the order of the entities is not guaranteed.
     * @return the sorting method
     */
    @NotNull Sort sort();

    /**
     * The maximum number of entities to return.
     * <p>
     * @implNote If the limit is 0, it means unlimited.
     * @return the limit of entities to return
     */
    int limit();

    @ApiStatus.Internal
    @Nullable Gatherer gatherer();

    interface Builder {
        void requirePlayer();

        <T> void predicate(@NotNull Property<?, T> property, @NotNull BiPredicate<Point, T> predicate);

        default <T> void predicateEqual(@NotNull Property<?, T> property, @Nullable T value) {
            predicate(property, (point, t) -> Objects.equals(t, value));
        }

        @SuppressWarnings("unchecked")
        default <T> void predicateContains(@NotNull Property<?, T> property, @NotNull T... value) {
            Set<T> valueSet = Set.of(value);
            predicate(property, (point, t) -> valueSet.contains(t));
        }

        default <T> void predicateNotEqual(@NotNull Property<?, T> property, @Nullable T value) {
            predicate(property, (point, t) -> !Objects.equals(t, value));
        }

        @SuppressWarnings("unchecked")
        default <T> void predicateExcludes(@NotNull Property<?, T> property, @NotNull T... value) {
            Set<T> valueSet = Set.of(value);
            predicate(property, (point, t) -> !valueSet.contains(t));
        }

        void id(int id);

        void uuid(@NotNull UUID uuid);

        void range(double radius);

        void chunk(int chunkX, int chunkZ);

        default void chunk(@NotNull Point chunkPosition) {
            chunk(chunkPosition.chunkX(), chunkPosition.chunkZ());
        }

        void chunkRange(int radius);

        void sort(@NotNull Sort sort);

        void limit(int limit);

        void unlimited();
    }

    enum Sort {
        ARBITRARY, FURTHEST, NEAREST, RANDOM
    }

    sealed interface Property<E, T> permits EntitySelectorImpl.PropertyImpl {
        @NotNull String name();

        @NotNull Class<E> type();

        @NotNull Function<E, T> function();
    }

    interface Finder<T> {
        <R extends T> @NotNull Stream<@NotNull R> selectEntity(@NotNull EntitySelector<R> selector, @NotNull Point origin);

        default <R extends T> @NotNull Stream<@NotNull R> selectGlobalEntity(@NotNull EntitySelector<R> selector) {
            return selectEntity(selector, Vec.ZERO);
        }

        default <R extends T> void selectEntityConsume(@NotNull EntitySelector<R> selector, @NotNull Point origin, Consumer<R> consumer) {
            final Stream<R> stream = selectEntity(selector, origin);
            stream.forEach(consumer);
        }

        default <R extends T> void selectGlobalEntityConsume(@NotNull EntitySelector<R> selector, Consumer<R> consumer) {
            final Stream<R> stream = selectGlobalEntity(selector);
            stream.forEach(consumer);
        }

        default <R extends T> @Nullable R selectEntityFirst(@NotNull EntitySelector<R> selector, @NotNull Point origin) {
            return selectEntity(selector, origin).findFirst().orElse(null);
        }

        default <R extends T> @Nullable R selectGlobalEntityFirst(@NotNull EntitySelector<R> selector) {
            return selectGlobalEntity(selector).findFirst().orElse(null);
        }

    }

    /**
     * Gathers attempt to shrink themselves to the smallest scope possible.
     */
    @ApiStatus.Internal
    sealed interface Gatherer {
        record Range(double radius) implements Gatherer {
            public Range {
                Check.argCondition(radius < 0, "Range must be positive");
                Check.argCondition(radius < Vec.EPSILON, "Range cannot just be itself, use a smaller scope!");
            }
        }
        record ChunkRange(int radius) implements Gatherer {
            public ChunkRange {
                Check.argCondition(radius < 0, "Chunk range must be positive");
                Check.argCondition(radius == 0, "Chunk range cannot just be itself, use Chunk instead!");
            }
        }
        record Chunk(long chunkIndex) implements Gatherer {}
        record Uuid(@NotNull UUID uuid) implements Gatherer {}
        record Id(int id) implements Gatherer {}
    }
}

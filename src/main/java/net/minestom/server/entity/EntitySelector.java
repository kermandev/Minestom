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
     * If you are just gathering entities consider using {@link EntitySelectors#all()} or {@link EntitySelectors#players()}.
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
        return selector(builder -> builder.predicateEquals(property, value));
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

    @NotNull Target target();

    @Nullable Gatherer gatherer();

    @NotNull Sort sort();

    int limit();

    interface Builder {
        void target(@NotNull Target target);

        default void requirePlayer() {
            target(Target.PLAYERS);
        }

        <T> void predicate(@NotNull Property<?, T> property, @NotNull BiPredicate<Point, T> predicate);

        default <T> void predicateEquals(@NotNull Property<?, T> property, @Nullable T value) {
            predicate(property, (point, t) -> Objects.equals(t, value));
        }

        default <T> void predicateNotEquals(@NotNull Property<?, T> property, @Nullable T value) {
            predicate(property, (point, t) -> !Objects.equals(t, value));
        }

        void id(int id);

        void uuid(@NotNull UUID uuid);

        void type(@NotNull EntityType @NotNull ... types);

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

    enum Target {
        ENTITIES,
        PLAYERS
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
            selectEntityConsume(selector, Vec.ZERO, consumer);
        }

        default <R extends T> @Nullable R selectEntityFirst(@NotNull EntitySelector<R> selector, @NotNull Point origin) {
            return selectEntity(selector, origin).findFirst().orElse(null);
        }

        default <R extends T> @Nullable R selectGlobalEntityFirst(@NotNull EntitySelector<R> selector) {
            return selectEntityFirst(selector, Vec.ZERO);
        }
    }
}

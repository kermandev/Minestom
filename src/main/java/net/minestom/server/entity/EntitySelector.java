package net.minestom.server.entity;

import net.minestom.server.coordinate.CoordConversion;
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
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Descriptive how entities should be queried.
 * Offer potential indexing/spatial partitioning advantages over lazy looping.
 */
public sealed interface EntitySelector<E> extends BiPredicate<Point, E> permits EntitySelectorImpl {

    static <E> @NotNull EntitySelector<E> selector(@NotNull Consumer<@NotNull Builder<E>> consumer) {
        EntitySelectorImpl.BuilderImpl<E> builder = new EntitySelectorImpl.BuilderImpl<>();
        consumer.accept(builder);
        return builder.build();
    }

    static <E, T> @NotNull EntitySelector<E> selector(@NotNull Property<E, T> property, T value) {
        return selector(builder -> builder.predicateEquals(property, value));
    }

    static <E, T> @NotNull Property<E, T> property(@NotNull String name, Function<E, T> function) {
        return new EntitySelectorImpl.PropertyImpl<>(name, function);
    }

    static <E extends TagReadable, T> @NotNull Property<E, T> tagProperty(@NotNull Tag<T> tag) {
        return property(tag.getKey(), e -> e.getTag(tag));
    }

    @Override
    boolean test(Point origin, E entity);

    @NotNull Target target();

    @Nullable Gatherer gatherer();

    @NotNull Sort sort();

    int limit();

    interface Builder<E> {
        void target(@NotNull Target target);

        default void requirePlayer() {
            target(Target.PLAYERS);
        }

        <T> void predicate(@NotNull Property<? super E, T> property, @NotNull BiPredicate<Point, T> predicate);

        default <T> void predicateEquals(@NotNull Property<? super E, T> property, @Nullable T value) {
            predicate(property, (point, t) -> Objects.equals(t, value));
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
        record Range(double radius) implements Gatherer {}
        record ChunkRange(int radius) implements Gatherer {
            public ChunkRange {
                Check.argCondition(radius < 0, "Chunk range must be positive");
                Check.argCondition(radius == 0, "Chunk range cannot just be itself, use Chunk instead!"); // TODO check this instead
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

        @NotNull Function<E, T> function();
    }

    interface Finder<T> {
        <R extends T> @NotNull Stream<@NotNull R> selectEntity(@NotNull EntitySelector<R> selector, @NotNull Point origin);

        default <R extends T> @NotNull Stream<@NotNull R> selectEntity(@NotNull EntitySelector<R> selector) {
            return selectEntity(selector, Vec.ZERO);
        }

        default <R extends T> void selectEntityConsume(@NotNull EntitySelector<R> selector, @NotNull Point origin, Consumer<R> consumer) {
            final Stream<R> stream = selectEntity(selector, origin);
            stream.forEach(consumer);
        }

        default <R extends T> void selectEntityConsume(@NotNull EntitySelector<R> selector, Consumer<R> consumer) {
            selectEntityConsume(selector, Vec.ZERO, consumer);
        }

        default <R extends T> @Nullable R selectEntityFirst(@NotNull EntitySelector<R> selector, @NotNull Point origin) {
            return selectEntity(selector, origin).findFirst().orElse(null);
        }

        default <R extends T> @Nullable R selectEntityFirst(@NotNull EntitySelector<R> selector) {
            return selectEntityFirst(selector, Vec.ZERO);
        }
    }
}

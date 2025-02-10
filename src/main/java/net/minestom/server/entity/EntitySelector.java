package net.minestom.server.entity;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Descriptive how entities should be queried.
 * Offer potential indexing/spatial partitioning advantages over lazy looping.
 */
public sealed interface EntitySelector<E> extends BiPredicate<Point, E> permits EntitySelectorImpl {

    static <E> @NotNull EntitySelector<E> selector(Target<E> target) {
        return new EntitySelectorImpl.BuilderImpl<>(target).build();
    }

    static <E> @NotNull EntitySelector<E> selector(Target<E> target, @NotNull Consumer<@NotNull Builder<E>> consumer) {
        EntitySelectorImpl.BuilderImpl<E> builder = new EntitySelectorImpl.BuilderImpl<>(target);
        consumer.accept(builder);
        return builder.build();
    }

    static @NotNull EntitySelector<Entity> entity() {
        return selector(Target.entity());
    }

    static @NotNull EntitySelector<Entity> entity(@NotNull Consumer<@NotNull Builder<Entity>> consumer) {
        return selector(Target.entity(), consumer);
    }

    static @NotNull EntitySelector<Player> player() {
        return selector(Target.player());
    }

    static @NotNull EntitySelector<Player> player(@NotNull Consumer<@NotNull Builder<Player>> consumer) {
        return selector(Target.player(), consumer);
    }

    static <E, T> @NotNull Property<E, T> property(@NotNull String name, Function<E, T> function) {
        return new EntitySelectorImpl.PropertyImpl<>(name, function);
    }

    static <E extends TagReadable, T> @NotNull Property<E, T> tagProperty(@NotNull Tag<T> tag) {
        return property(tag.getKey(), e -> e.getTag(tag));
    }

    @Override
    boolean test(Point origin, E entity);

    @NotNull Target<E> target();

    @NotNull Sort sort();

    @Nullable EntitySelector.Gather gather();

    int limit();

    interface Builder<E> {
        void target(@NotNull Target<E> target);

        <T> void predicate(@NotNull Property<? super E, T> property, @NotNull BiPredicate<Point, T> predicate);

        default <T> void predicateEquals(@NotNull Property<? super E, T> property, @Nullable T value) {
            predicate(property, (point, t) -> Objects.equals(t, value));
        }

        void gather(Gather gather);
//
//        void type(@NotNull Class<E> type);
//
//        void type(@NotNull EntityType @NotNull ... types);
//
//        void range(double radius);
//
//        void chunk(int chunkX, int chunkZ);
//
//        default void chunk(@NotNull Point chunkPosition) {
//            chunk(chunkPosition.chunkX(), chunkPosition.chunkZ());
//        }
        void limit(int limit);
//
//        void chunkRange(int radius);

        void sort(@NotNull Sort sort);
    }

    sealed interface Target<T> permits EntitySelectorImpl.TargetImpl {
        static Target<Player> player() {
            return EntitySelectorImpl.TargetImpl.PLAYERS;
        }

        static Target<Entity> entity() {
            return EntitySelectorImpl.TargetImpl.ENTITIES;
        }

        static <T> Target<T> of(Class<T> type) {
            return new EntitySelectorImpl.TargetImpl<>(type);
        }

        Class<T> type();
    }

    /**
     * Data gathering predicates.
     */
    sealed interface Gather {
        // Maybe we shouldn't implement only and that's for the user.
        record Only(int entityId) implements Gather {}
        record OnlyUuid(UUID entityUuid) implements Gather {}
        record Range(double radius) implements Gather {}
        record Chunk(int chunkX, int chunkZ) implements Gather {}

        record ChunkRange(int radius) implements Gather {}

        static Gather only(Entity entity) {
            return only(entity.getEntityId());
        }

        static Gather only(int entityId) {
            return new Only(entityId);
        }

        static Gather onlyUuid(UUID entityUuid) {
            return new OnlyUuid(entityUuid);
        }

        static Gather range(double radius) {
            return new Range(radius);
        }

        static Gather chunk(int chunkX, int chunkZ) {
            return new Chunk(chunkX, chunkZ);
        }

        static Gather chunk(@NotNull Point chunkPosition) {
            return chunk(chunkPosition.chunkX(), chunkPosition.chunkZ());
        }
        static Gather chunkRange(int radius) {
            return new ChunkRange(radius);
        }

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

package net.minestom.server.entity;

import net.minestom.server.coordinate.Point;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;

record EntitySelectorImpl<E>(@NotNull Target<? extends E> target,
                             @Nullable EntitySelector.Gather gather,
                             @Nullable EntitySelector.Sort sort,
                             int limit,
                             @Unmodifiable List<BiPredicate<Point, E>> predicates) implements EntitySelector<E> {
    public EntitySelectorImpl {
        predicates = List.copyOf(predicates);
    }

    @Override
    public boolean test(Point point, E entity) {
        for (var condition : predicates) {
            if (!condition.test(point, entity)) return false;
        }
        return true;
    }

    record PropertyImpl<E, T>(String name, Function<E, T> function) implements EntitySelector.Property<E, T> {
    }

    record TargetImpl<T>(Class<T> type) implements Target<T> {
        // We have these to prevent usage.
        final static Target<Player> PLAYERS = Target.of(Player.class);
        final static Target<Entity> ENTITIES = Target.of(Entity.class);
    }

    static final class BuilderImpl<E> implements Builder<E> {
        // TODO fix type?
        private Target<? extends E> target;
        private Gather gather = null;
        private Sort sort = Sort.ARBITRARY;
        private int limit = 0;

        private final List<BiPredicate<Point, E>> predicates = new ArrayList<>();

        BuilderImpl(@NotNull Target<E> target) {
            this.target = target;
        }

        @Override
        public void target(@NotNull Target<? extends E> target) {
            this.target = target;
        }

        @Override
        public <T> void predicate(@NotNull Property<? super E, T> property, @NotNull BiPredicate<Point, T> predicate) {
            this.predicates.add((point, entity) -> predicate.test(point, property.function().apply(entity)));
        }

        @Override
        public void gather(Gather gather) {
            this.gather = gather;
        }

        @Override
        public void type(@NotNull EntityType @NotNull ... types) {
            // No gather impl of this.
            final var allowedTypes = new HashSet<>(List.of(types));
            this.predicate(Property.class.cast(EntitySelectors.TYPE), (point, type) -> allowedTypes.contains(type));
        }

        @Override
        public void sort(@NotNull Sort sort) {
            this.sort = sort;
        }

        @Override
        public void limit(int limit) {
            Check.argCondition(limit <= 0, "Limit must be greater than 0");
            this.limit = limit;
        }

        @Override
        public <G extends E> Builder<G> reinterpret(Target<G> target) {
            final var ourTarget = this.target; // Perform runtime check.
            Check.argCondition(ourTarget.type().isAssignableFrom(target.type()), "The target type is not a subtype of " + target.type());
            //noinspection unchecked
            return (Builder<G>) this;
        }

        @Override
        public <G extends E> Builder<G> reinterpret() {
            //noinspection unchecked
            return reinterpret((Target<G>) target);
        }

        EntitySelectorImpl<E> build() {
            return new EntitySelectorImpl<>(target, gather, sort, limit, predicates);
        }

    }

//    static final class BuilderImpl<E> implements Builder<E> {
//        private Target target = Target.ALL;
//        private Sort sort = Sort.ARBITRARY;
//        private int limit = -1;
//        private final List<BiPredicate<Point, E>> predicates = new ArrayList<>();
//
//        @Override
//        public void target(@NotNull Target target) {
//            this.target = target;
//        }
//
//        @Override
//        public <T> void predicate(@NotNull Property<? super E, T> property, @NotNull BiPredicate<Point, T> predicate) {
//            this.predicates.add((point, entity) -> predicate.test(point, property.function().apply(entity)));
//        }
////
////        @Override
////        public void type(@NotNull Class<E> type) {
////            predicate(Property.class.cast(EntitySelectors.CLASS), (point, classType) -> type.isAssignableFrom((Class<?>) classType));
////        }
//
//        @Override
//        public void type(@NotNull EntityType @NotNull ... types) {
//            predicate(Property.class.cast(EntitySelectors.TYPE), (point, type) -> new HashSet<>(List.of(types)).contains(type));
//        }
//
//        @Override
//        public void range(double radius) {
//            this.<Pos>predicate(Property.class.cast(EntitySelectors.POS),
//                    (origin, coord) -> origin.distance(coord) <= radius);
//        }
//
//        @Override
//        public void chunk(int chunkX, int chunkZ) {
//            this.<Pos>predicate(Property.class.cast(EntitySelectors.POS),
//                    (origin, coord) -> coord.chunkX() == chunkX && coord.chunkZ() == chunkZ);
//        }
//
//        @Override
//        public void chunkRange(int radius) {
//            this.<Pos>predicate(Property.class.cast(EntitySelectors.POS), (origin, coord) -> {
//                final int originChunkX = origin.chunkX();
//                final int originChunkZ = origin.chunkZ();
//                final int coordChunkX = coord.chunkX();
//                final int coordChunkZ = coord.chunkZ();
//                final int deltaX = Math.abs(originChunkX - coordChunkX);
//                final int deltaZ = Math.abs(originChunkZ - coordChunkZ);
//                return deltaX <= radius && deltaZ <= radius;
//            });
//        }
//
//        @Override
//        public void sort(@NotNull Sort sort) {
//            this.sort = sort;
//        }
//
//        @Override
//        public void limit(int limit) {
//            this.limit = limit;
//        }
//
//        EntitySelectorImpl<E> build() {
//            return new EntitySelectorImpl<>(target, sort, limit, predicates);
//        }
//    }
}

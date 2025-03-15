package net.minestom.server.entity;

import net.minestom.server.coordinate.Point;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;

record EntitySelectorImpl<E>(@NotNull Class<? extends E> target,
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

    static final class BuilderImpl<E> implements Builder<E> {
        private Class<? extends E> target;
        private Gather gather = null;
        private Sort sort = Sort.ARBITRARY;
        private int limit = 0;

        private final List<BiPredicate<Point, E>> predicates = new ArrayList<>();

        BuilderImpl(@NotNull Class<E> target) {
            this.target = target;
        }

        @Override
        public void target(@NotNull Class<? extends E> target) {
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
            //noinspection unchecked
            this.predicate((Property<E, EntityType>) EntitySelectors.TYPE, (point, type) -> allowedTypes.contains(type));
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
        public <G extends E> Builder<G> reinterpret(Class<G> target) {
            final var ourTarget = this.target; // Perform runtime check.
            Check.argCondition(!target.isAssignableFrom(ourTarget), "The target type is not a subtype of " + ourTarget.getSimpleName());
            this.target(target);
            //noinspection unchecked
            return (Builder<G>) this;
        }

        @Override
        public <G extends E> Builder<G> reinterpret() {
            //noinspection unchecked
            return reinterpret((Class<G>) target);
        }

        EntitySelectorImpl<E> build() {
            return new EntitySelectorImpl<>(target, gather, sort, limit, predicates);
        }
    }
}

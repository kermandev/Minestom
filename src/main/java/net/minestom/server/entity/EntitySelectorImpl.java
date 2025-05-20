package net.minestom.server.entity;

import net.minestom.server.coordinate.CoordConversion;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;

record EntitySelectorImpl<E>(EntitySelector.Target target,
                             Gatherer gatherer,
                             EntitySelector.Sort sort,
                             int limit,
                             List<BiPredicate<Point, E>> conditions) implements EntitySelector<E> {
    public EntitySelectorImpl {
        conditions = List.copyOf(conditions);
    }

    @Override
    public boolean test(Point point, E entity) {
        for (var condition : conditions) {
            if (!condition.test(point, entity)) return false;
        }
        return true;
    }

    record PropertyImpl<E, T>(String name, Function<E, T> function) implements EntitySelector.Property<E, T> {
    }

    static final class BuilderImpl<E> implements Builder<E> {
        private Target target = Target.ENTITIES;
        private Gatherer gatherer = null;
        private Sort sort = Sort.ARBITRARY;
        private int limit = 0;
        private final List<BiPredicate<Point, E>> conditions = new ArrayList<>();

        @Override
        public void target(@NotNull Target target) {
            this.target = target;
        }

        @Override
        public <T> void predicate(@NotNull Property<? super E, T> property, @NotNull BiPredicate<Point, T> predicate) {
            this.conditions.add((point, entity) -> predicate.test(point, property.function().apply(entity)));
        }

        @Override
        public void id(int id) {
            if (upgradeGather(new Gatherer.Id(id))) return;
            this.<Integer>predicate(Property.class.cast(EntitySelectors.ID), (point, entityId) -> entityId.equals(id));
        }

        @Override
        public void uuid(@NotNull UUID uuid) {
            if (upgradeGather(new Gatherer.Uuid(uuid))) return;
            this.<UUID>predicate(Property.class.cast(EntitySelectors.UUID), (point, entityUuid) -> entityUuid.equals(uuid));
        }

        @Override
        public void type(@NotNull EntityType @NotNull ... types) {
            var typeSet = Set.of(types);
            this.<EntityType>predicate(Property.class.cast(EntitySelectors.TYPE), (point, type) -> typeSet.contains(type));
        }

        @Override
        public void range(double radius) { // TODO add specialized one, required for tests as it uses the property that is not lastPosition.
            // Attempt to upgrade the gatherer to a more specific one; we have to check bounds, because the origin is unknown.
            upgradeGather(new Gatherer.ChunkRange((int) Math.ceil(radius / 16)));
            // No matter what we have to use a predicate; TODO pop the predicate if it is not used (going to single)
            final var radiusSquared = radius * radius;
            this.<Pos>predicate(Property.class.cast(EntitySelectors.POS),
                    (origin, coord) -> origin.distanceSquared(coord) <= radiusSquared);
        }

        @Override
        public void chunk(long chunkIndex) {
            if (upgradeGather(new Gatherer.Chunk(chunkIndex))) return;
            final int chunkX = CoordConversion.chunkIndexGetX(chunkIndex);
            final int chunkZ = CoordConversion.chunkIndexGetZ(chunkIndex);
            this.<Pos>predicate(Property.class.cast(EntitySelectors.POS),
                    (origin, coord) -> coord.chunkX() == chunkX && coord.chunkZ() == chunkZ);
        }

        @Override
        public void chunkRange(int radius) {
            if (upgradeGather(new Gatherer.ChunkRange(radius))) return;
            this.<Pos>predicate(Property.class.cast(EntitySelectors.POS), (origin, coord) -> {
                final int originChunkX = origin.chunkX();
                final int originChunkZ = origin.chunkZ();
                final int coordChunkX = coord.chunkX();
                final int coordChunkZ = coord.chunkZ();
                final int deltaX = Math.abs(originChunkX - coordChunkX);
                final int deltaZ = Math.abs(originChunkZ - coordChunkZ);
                return deltaX <= radius && deltaZ <= radius;
            });
        }

        @Override
        public void sort(@NotNull Sort sort) {
            this.sort = sort;
        }

        @Override
        public void limit(int limit) {
            Check.argCondition(limit < 0, "Limit must not be negative");
            this.limit = limit;
        }

        // Returns true if the gatherer was upgraded
        boolean upgradeGather(Gatherer upgrade) {
            final Gatherer gatherer = this.gatherer != null ? this.gatherer.smallestScope(upgrade) : upgrade;
            if (gatherer == this.gatherer) return false;
            this.gatherer = gatherer;
            return true;
        }

        EntitySelectorImpl<E> build() {
            return new EntitySelectorImpl<>(target, gatherer, sort, limit, conditions);
        }
    }
}

package net.minestom.server.entity;

import net.minestom.server.coordinate.CoordConversion;
import net.minestom.server.coordinate.Point;
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

    record PropertyImpl<E, T>(String name, Class<E> type, Function<E, T> function) implements EntitySelector.Property<E, T> {
    }

    static final class BuilderImpl<E> implements Builder {
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
        public <T> void predicate(@NotNull Property<?, T> property, @NotNull BiPredicate<Point, T> predicate) {
            //noinspection unchecked
            this.conditions.add((point, entity) ->
                    // Check if the entity can be applied to this predicate, if not return false
                    property.type().isAssignableFrom(entity.getClass())
                    && predicate.test(point, ((Function<E, T>) property.function()).apply(entity)));
        }

        @Override
        public void id(int id) {
            if (upgradeGather(new Gatherer.Id(id))) return;
            this.predicateEquals(EntitySelectors.ID, id);
        }

        @Override
        public void uuid(@NotNull UUID uuid) {
            if (upgradeGather(new Gatherer.Uuid(uuid))) return;
            this.predicateEquals(EntitySelectors.UUID, uuid);
        }

        @Override
        public void type(@NotNull EntityType @NotNull ... types) {
            var typeSet = Set.of(types);
            this.predicate(EntitySelectors.TYPE, (point, type) -> typeSet.contains(type));
        }

        @Override
        public void range(double radius) {
            if (upgradeGather(new Gatherer.Range(radius))) return;
            fallbackRange(radius); // Otherwise fallback
        }

        @Override
        public void chunk(int chunkX, int chunkZ) {
            long chunkIndex = CoordConversion.chunkIndex(chunkX, chunkZ);
            if (upgradeGather(new Gatherer.Chunk(chunkIndex))) return;
            this.predicate(EntitySelectors.POS,
                    (origin, coord) -> coord.chunkX() == chunkX && coord.chunkZ() == chunkZ);
        }

        @Override
        public void chunkRange(int radius) {
            if (upgradeGather(new Gatherer.ChunkRange(radius))) return;
            this.predicate(EntitySelectors.POS, (origin, coord) -> {
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
            Check.argCondition(limit == 0, "Limit must not be 0, use unlimited() instead");
            this.limit = limit;
        }

        @Override
        public void unlimited() {
            this.limit = 0;
        }

        // Returns true if the gatherer was upgraded (to a smaller scope)
        boolean upgradeGather(Gatherer upgrade) {
            final Gatherer gatherer = this.smallestScope(upgrade);
            if (gatherer == this.gatherer) return false;
            this.gatherer = gatherer;
            return true;
        }

        void fallbackRange(double radius) {
            final var radiusSquared = radius * radius;
            this.predicate(EntitySelectors.POS,
                    (origin, coord) -> origin.distanceSquared(coord) <= radiusSquared);
        }

        Gatherer smallestScope(Gatherer other) {
            final Gatherer currentGatherer = this.gatherer;
            if (other == null) return currentGatherer; // Going back above chunk range
            if (currentGatherer == null) return other; // Must be a chunk range or lower.
            if (other.equals(currentGatherer)) return other;
            return switch (currentGatherer) {
                // Shrinking to a range
                case Gatherer.Range range when other instanceof Gatherer.Range(double radius) && range.radius() >= radius -> other;
                // Shrinking to a chunk range
                case Gatherer.Range range when other instanceof Gatherer.ChunkRange(int radius) && Math.ceil(range.radius() / 16) >= radius -> {
                    fallbackRange(radius); // Big circle across chunks, but less than the chunk range.
                    yield other;
                }
                case Gatherer.ChunkRange range when other instanceof Gatherer.ChunkRange(int radius) && range.radius() >= radius -> other;
                // Shrinking to a single chunk
                case Gatherer.Range range when other instanceof Gatherer.Chunk -> {
                    fallbackRange(range.radius()); // Case when you want a circle inside of the square.
                    yield other;
                }
                case Gatherer.ChunkRange ignored when other instanceof Gatherer.Chunk -> other;
                case Gatherer.Chunk ignored when other instanceof Gatherer.Chunk -> other; // Reassigning the chunk.
                // Shrinking to a single UUID
                case Gatherer.Range ignored when other instanceof Gatherer.Uuid -> other;
                case Gatherer.ChunkRange ignored when other instanceof Gatherer.Uuid -> other;
                case Gatherer.Chunk ignored when other instanceof Gatherer.Uuid -> other;
                case Gatherer.Uuid ignored when other instanceof Gatherer.Uuid -> other; // Reassigning the UUID.
                // Shrinking to single ID
                case Gatherer.Range ignored when other instanceof Gatherer.Id -> other;
                case Gatherer.ChunkRange ignored when other instanceof Gatherer.Id -> other;
                case Gatherer.Chunk ignored when other instanceof Gatherer.Id -> other;
                case Gatherer.Uuid ignored when other instanceof Gatherer.Id -> other;
                case Gatherer.Id ignored when other instanceof Gatherer.Id -> other; // Reassigning the ID.
                default -> currentGatherer;
            };
        }

        EntitySelectorImpl<E> build() {
            return new EntitySelectorImpl<>(target, gatherer, sort, limit, conditions);
        }
    }
}

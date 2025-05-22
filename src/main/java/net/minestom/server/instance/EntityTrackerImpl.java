package net.minestom.server.instance;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.ChunkRange;
import net.minestom.server.coordinate.CoordConversion;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntitySelector;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

final class EntityTrackerImpl implements EntityTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityTrackerImpl.class);

    private static final EntitySelector<Entity> SELECTOR = EntitySelector.selector(builder -> builder.chunkRange(ServerFlag.ENTITY_VIEW_DISTANCE));

    // Indexes
    private final Int2ObjectOpenHashMap<TrackedEntity> idIndex = new Int2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<UUID, TrackedEntity> uuidIndex = new Object2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<TrackedEntity> playerIdIndex = new Int2ObjectOpenHashMap<>();

    // Spatial partitioning
    private final Long2ObjectOpenHashMap<Set<TrackedEntity>> chunksEntities = new Long2ObjectOpenHashMap<>();

    @Override
    public synchronized void register(@NotNull Entity entity, @NotNull Point point, @Nullable Update update) {
        TrackedEntity newEntry = new TrackedEntity(entity, new AtomicReference<>(point));
        // Indexing
        TrackedEntity prevEntryWithId = idIndex.putIfAbsent(entity.getEntityId(), newEntry);
        Check.isTrue(prevEntryWithId == null, "There is already an entity registered with id {0}", entity.getEntityId());
        TrackedEntity prevEntryWithUuid = uuidIndex.putIfAbsent(entity.getUuid(), newEntry);
        Check.isTrue(prevEntryWithUuid == null, "There is already an entity registered with uuid {0}", entity.getUuid());
        if (entity instanceof Player) {
            TrackedEntity prevEntryWithPlayerId = playerIdIndex.putIfAbsent(entity.getEntityId(), newEntry);
            Check.isTrue(prevEntryWithPlayerId == null, "There is already an entity registered with player id {0}", entity.getEntityId());
        }
        // Spatial partitioning
        final long index = CoordConversion.chunkIndex(point);
        addChunkEntity(newEntry, index);
        // Update before so we don't add ourselves.
        if (update != null) {
            update.referenceUpdate(point, this);
            selectEntityConsume(SELECTOR, point, newEntity -> {
                if (newEntity == entity) return;
                update.add(newEntity);
            });
        }
    }

    @Override
    public synchronized void unregister(@NotNull Entity entity, @Nullable Update update) {
        // Indexing
        TrackedEntity entry = idIndex.remove(entity.getEntityId());
        if (entry == null) return;
        uuidIndex.remove(entity.getUuid());
        if (entity instanceof Player) {
            playerIdIndex.remove(entity.getEntityId());
        }
        // Spatial partitioning
        final Point point = entry.lastPosition().getPlain();
        final long index = CoordConversion.chunkIndex(point);
        removeChunkEntity(entry, index);
        // Update
        if (update != null) {
            update.referenceUpdate(point, null);
            selectEntityConsume(SELECTOR, point, newEntity -> {
                assert newEntity != entity : "Entity " + entity.getUuid() + " should not be in the unregister update";
                update.remove(newEntity);
            });
        }
    }

    @Override
    public synchronized void move(@NotNull Entity entity, @NotNull Point newPoint, @Nullable Update update) {
        TrackedEntity entry = idIndex.get(entity.getEntityId());
        if (entry == null) {
            LOGGER.warn("Attempted to move unregistered entity {} in the entity tracker", entity.getEntityId());
            return;
        }
        Point oldPoint = entry.lastPosition().getPlain();
        entry.lastPosition().setPlain(newPoint);
        if (oldPoint.sameChunk(newPoint)) return;
        // Chunk change, update partitions
        final long oldIndex = CoordConversion.chunkIndex(oldPoint);
        final long newIndex = CoordConversion.chunkIndex(newPoint);
        removeChunkEntity(entry, oldIndex);
        addChunkEntity(entry, newIndex);
        // Update
        if (update != null) {
            difference(oldPoint, newPoint, new Update() {
                @Override
                public void add(@NotNull Entity added) {
                    if (entity != added) update.add(added);
                }

                @Override
                public void remove(@NotNull Entity removed) {
                    if (entity != removed) update.remove(removed);
                }
            });
            update.referenceUpdate(newPoint, this);
        }
    }

    @Override
    public synchronized <R extends Entity> @NotNull Stream<@NotNull R> selectEntity(@NotNull EntitySelector<R> selector, @NotNull Point origin) {
        final boolean playersOnly = selector.target() == EntitySelector.Target.PLAYERS;
        Stream<TrackedEntity> stream = switch (selector.gatherer()) {
            case EntitySelector.Gatherer.Range(double radius) -> {
                var radiusSquared = radius * radius;
                var chunkRadius = (int) Math.ceil(radius / 16);
                var entries = gatherChunkRange(origin, chunkRadius, playersOnly);
                yield entries.filter(trackedEntity ->
                                trackedEntity.lastPosition().getPlain().distanceSquared(origin) <= radiusSquared);
            }
            case EntitySelector.Gatherer.ChunkRange(int radius) -> gatherChunkRange(origin, radius, playersOnly);
            case EntitySelector.Gatherer.Chunk(long chunkIndex) -> {
                var entry = chunksEntities.get(chunkIndex);
                if (entry == null) yield Stream.empty();
                if (!playersOnly) yield entry.stream();
                yield entry.stream().filter(trackedEntity -> trackedEntity.entity() instanceof Player);
            }
            case EntitySelector.Gatherer.Uuid(@NotNull UUID uuid) -> {
                var entry = uuidIndex.get(uuid);
                if (entry == null || (playersOnly && !(entry.entity() instanceof Player))) yield Stream.empty();
                yield Stream.of(entry);
            }
            case EntitySelector.Gatherer.Id(int id) -> {
                var entry = playersOnly ? playerIdIndex.get(id) : idIndex.get(id);
                if (entry == null) yield Stream.empty();
                yield Stream.of(entry);
            }
            case null -> playersOnly ? playerIdIndex.values().stream() : idIndex.values().stream();
        };

        {
            // noinspection unchecked
            stream = stream.filter(trackedEntity -> selector.test(origin, (R) trackedEntity.entity()));
        }

        switch (selector.sort()) {
            case ARBITRARY -> {
                // Do not sort
            }
            case FURTHEST -> stream = stream.sorted((a, b) -> {
                double distanceA = origin.distanceSquared(a.lastPosition().getPlain());
                double distanceB = origin.distanceSquared(b.lastPosition().getPlain());
                return Double.compare(distanceB, distanceA); // Sort descending by distance
            });
            case NEAREST -> stream = stream.sorted((a, b) -> {
                double distanceA = origin.distanceSquared(a.lastPosition().getPlain());
                double distanceB = origin.distanceSquared(b.lastPosition().getPlain());
                return Double.compare(distanceA, distanceB); // Sort ascending by distance
            });
            case RANDOM -> {
                var list = Arrays.asList(stream.toArray(TrackedEntity[]::new));
                Collections.shuffle(list);
                stream = list.stream();
            }
        }

        if (selector.limit() != 0) {
            stream = stream.limit(selector.limit());
        }

        // noinspection unchecked
        return (Stream<R>) stream.map(TrackedEntity::entity);
    }

    @Override
    public synchronized void trim() {
        idIndex.trim();
        uuidIndex.trim();
        playerIdIndex.trim();
        // Trim the chunk entities array sizes.
        for (var entry : chunksEntities.long2ObjectEntrySet()) {
            var key = entry.getLongKey();
            var entities = entry.getValue().toArray(new TrackedEntity[0]);
            assert entities.length > 0 : "There should be at least one entity in the chunk";
            chunksEntities.put(key, ObjectArraySet.ofUnchecked(entities));
        }
        chunksEntities.trim();
    }

    private void addChunkEntity(TrackedEntity entity, long index) {
        chunksEntities.compute(index, (ignored, entities) -> {
            // Add entity to existing chunk
            if (entities != null) {
                entities.add(entity);
                return entities;
            }
            // Otherwise, create a new entry
            return ObjectArraySet.ofUnchecked(entity);
        });
    }

    private void removeChunkEntity(TrackedEntity entity, long index) {
        chunksEntities.computeIfPresent(index, (ignored, entities) -> {
            entities.remove(entity);
            if (entities.isEmpty()) return null; // Empty chunk
            return entities;
        });
    }

    private Stream<TrackedEntity> gatherChunkRange(@NotNull Point origin, int chunkRadius, boolean playersOnly) {
        var builder = Stream.<Set<TrackedEntity>>builder();
        ChunkRange.chunksInRange(origin, chunkRadius, (chunkX, chunkZ) -> {
            var entries = chunksEntities.get(CoordConversion.chunkIndex(chunkX, chunkZ));
            if (entries == null) return;
            builder.add(entries);
        });
        if (!playersOnly) return builder.build().flatMap(Set::stream);
        return builder.build().flatMap(Set::stream)
                .filter(trackedEntity -> trackedEntity.entity() instanceof Player);
    }

    private void difference(Point oldPoint, Point newPoint, @NotNull Update update) {
        ChunkRange.chunksInRangeDiffering(newPoint.chunkX(), newPoint.chunkZ(), oldPoint.chunkX(), oldPoint.chunkZ(),
                ServerFlag.ENTITY_VIEW_DISTANCE, (chunkX, chunkZ) -> {
                    // Add
                    final Set<TrackedEntity> entities = chunksEntities.get(CoordConversion.chunkIndex(chunkX, chunkZ));
                    if (entities == null) return;
                    assert !entities.isEmpty() : "There should be at least one entity in the chunk";
                    for (TrackedEntity entry : entities) update.add(entry.entity());
                }, (chunkX, chunkZ) -> {
                    // Remove
                    final Set<TrackedEntity> entities = chunksEntities.get(CoordConversion.chunkIndex(chunkX, chunkZ));
                    if (entities == null) return;
                    assert !entities.isEmpty() : "There should be at least one entity in the chunk";
                    for (TrackedEntity entity : entities) update.remove(entity.entity());
                });
    }

    private record TrackedEntity(@NotNull Entity entity, @NotNull AtomicReference<Point> lastPosition) {
        TrackedEntity {
            Check.notNull(entity, "entity");
            Check.notNull(lastPosition, "lastPosition");
        }
    }
}

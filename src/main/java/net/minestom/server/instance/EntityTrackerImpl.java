package net.minestom.server.instance;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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
    private final Int2ObjectMap<TrackedEntity> idIndex = new Int2ObjectOpenHashMap<>();
    private final Map<UUID, TrackedEntity> uuidIndex = new Object2ObjectOpenHashMap<>();
    private final Int2ObjectMap<TrackedEntity> playerIdIndex = new Int2ObjectOpenHashMap<>();

    // Spatial partitioning
    private final Long2ObjectMap<Set<TrackedEntity>> chunksEntities = new Long2ObjectOpenHashMap<>();

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
        Stream<TrackedEntity> stream = switch (selector.gatherer()) {
            case EntitySelector.Gatherer.ChunkRange(int radius) -> {
                // Get all chunks in range
                var array = new ObjectArrayList<TrackedEntity>();
                ChunkRange.chunksInRange(origin, radius, (chunkX, chunkZ) -> {
                    var entries = chunksEntities.get(CoordConversion.chunkIndex(chunkX, chunkZ));
                    if (entries == null) return;
                    array.addAll(entries);
                });
                yield array.stream().filter(trackedEntity -> selector.target().test(trackedEntity.entity()));
            }
            case EntitySelector.Gatherer.Chunk(long chunkIndex) -> {
                var entry = chunksEntities.get(chunkIndex);
                if (entry == null) yield Stream.empty();
                yield entry.stream().filter(trackedEntity -> selector.target().test(trackedEntity.entity()));
            }
            case EntitySelector.Gatherer.Uuid(@NotNull UUID uuid) -> {
                var entry = uuidIndex.get(uuid);
                if (entry == null || selector.target().test(entry.entity())) yield Stream.empty();
                yield Stream.of(entry);
            }
            case EntitySelector.Gatherer.Id(int id) -> {
                var entry = idIndex.get(id);
                if (entry == null || selector.target().test(entry.entity())) yield Stream.empty();
                yield Stream.of(entry);
            }
            case null -> switch (selector.target()) {
                case ENTITIES -> idIndex.values().stream();
                case PLAYERS -> playerIdIndex.values().stream();
            };
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

    private void addChunkEntity(TrackedEntity entity, long index) {
        chunksEntities.compute(index, (ignored, entities) -> {
            // Add entity to existing chunk
            if (entities != null) {
                entities.add(entity);
                return entities;
            }
            // Otherwise, create a new entry
            entities = new HashSet<>(1);
            entities.add(entity);
            return entities;
        });
    }

    private void removeChunkEntity(TrackedEntity entity, long index) {
        chunksEntities.computeIfPresent(index, (ignored, entities) -> {
            entities.remove(entity);
            if (entities.isEmpty()) return null; // Empty chunk
            return entities;
        });
    }

    private TrackedEntity findNearest(Point origin, boolean player) {
        Stream<TrackedEntity> stream = player ? playerIdIndex.values().stream() : idIndex.values().stream();
        return stream.min(Comparator.comparingDouble(
                trackedEntity -> origin.distanceSquared(trackedEntity.lastPosition().getPlain())
        )).orElse(null);
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

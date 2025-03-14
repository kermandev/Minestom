package net.minestom.server.instance;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.ChunkRange;
import net.minestom.server.coordinate.CoordConversion;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntitySelector;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

final class EntityTrackerImpl implements EntityTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityTrackerImpl.class);

    private static final EntitySelector<Entity> SELECTOR = EntitySelector.entity(builder -> builder.gather(EntitySelector.Gather.chunkRange(ServerFlag.ENTITY_VIEW_DISTANCE)));

    // Class Cache TODO, probably not necessary.
    private final Object2ObjectLinkedOpenHashMap<Class<? extends Entity>, ObjectArrayList<Class<? extends Entity>>> inheritanceMapCache = new Object2ObjectLinkedOpenHashMap<>(0);

    // Indexes
    private final Object2ObjectLinkedOpenHashMap<Class<? extends Entity>, Int2ObjectMap<TrackedEntity>> classToIndex = new Object2ObjectLinkedOpenHashMap<>(0);
    private final Object2ObjectOpenHashMap<UUID, TrackedEntity> uuidIndex = new Object2ObjectOpenHashMap<>(0);

    // Spatial partitioning
    private final Long2ObjectMap<ObjectArrayList<TrackedEntity>> chunksEntities = new Long2ObjectOpenHashMap<>(0);

    @Override
    public synchronized void register(@NotNull Entity entity, @NotNull Point point, @Nullable Update update) {
        TrackedEntity newEntry = new TrackedEntity(entity, new AtomicReference<>(point));
        // Validate entity id hasnt been used.
        var idEntries = classToIndex.get(Entity.class);
        Check.isTrue(idEntries == null || !idEntries.containsKey(entity.getEntityId()), "There is already an entity registered with id {0}", entity.getEntityId());
        // Indexing
        TrackedEntity prevEntryWithUuid = uuidIndex.putIfAbsent(entity.getUuid(), newEntry);
        Check.isTrue(prevEntryWithUuid == null, "There is already an entity registered with uuid {0}", entity.getUuid());
        addEntityClass(newEntry);
        // Spatial partitioning
        final long index = CoordConversion.chunkIndex(point);
        ObjectArrayList<TrackedEntity> chunkEntities = chunksEntities.computeIfAbsent(index, t -> new ObjectArrayList<>(1));
        chunkEntities.add(newEntry);
        // Update
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
        var removed = uuidIndex.remove(entity.getUuid());
        if (removed == null) return;
        var entry = removeEntityClass(entity);
        Check.notNull(entry, "Unregistered entity does not have a valid entry!");
        // Spatial partitioning
        final Point point = entry.lastPosition().getPlain();
        final long index = CoordConversion.chunkIndex(point);
        ObjectArrayList<TrackedEntity> chunkEntities = chunksEntities.get(index);
        chunkEntities.remove(entry);
        if (chunkEntities.isEmpty()) {
            chunksEntities.remove(index); // Empty chunk
        }
        // Update
        if (update != null) {
            update.referenceUpdate(point, null);
            selectEntityConsume(SELECTOR, point, newEntity -> {
                if (newEntity == entity) return;
                update.remove(newEntity);
            });
        }
    }

    @Override
    public synchronized void move(@NotNull Entity entity, @NotNull Point newPoint, @Nullable Update update) {
        TrackedEntity entry = classToIndex.get(entity.getClass()).get(entity.getEntityId());
        if (entry == null) {
            LOGGER.warn("Attempted to move unregistered entity {} in the entity tracker", entity.getEntityId());
            return;
        }
        Point oldPoint = entry.lastPosition().getPlain();
        entry.lastPosition().setPlain(newPoint);
        if (oldPoint == null || oldPoint.sameChunk(newPoint)) return;
        Check.notNull(entry, "Entity does not have a valid entry for move!");
        // Chunk change, update partitions
        final long oldIndex = CoordConversion.chunkIndex(oldPoint);
        final long newIndex = CoordConversion.chunkIndex(newPoint);
        ObjectArrayList<TrackedEntity> oldPartition = chunksEntities.get(oldIndex);
        ObjectArrayList<TrackedEntity> newPartition = chunksEntities.computeIfAbsent(newIndex, t -> new ObjectArrayList<>(1));

        if (oldPartition != null) {
            oldPartition.remove(entry);
        } else {
            LOGGER.warn("Attempted to move unregistered chunk entity {} in the entity tracker", entity.getEntityId());
        }

        newPartition.add(entry);

        //Cleanup
        if (oldPartition != null && oldPartition.isEmpty()) {
            chunksEntities.remove(oldIndex); // Empty chunk
        }

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
        final var correctIndex = classToIndex.get(selector.target().type());
        // If we have no entities of this type, we can simply just return.
        if (correctIndex == null || correctIndex.isEmpty()) return Stream.empty();
        Stream<TrackedEntity> stream = switch (selector.gather()) {
            case EntitySelector.Gather.Only(int entityId) -> {
                final TrackedEntity trackedEntity = correctIndex.get(entityId);
                yield trackedEntity != null ? Stream.of(trackedEntity) : Stream.empty();
            }
            case EntitySelector.Gather.OnlyUuid(UUID entityUuid) -> {
                final TrackedEntity trackedEntity = uuidIndex.get(entityUuid);
                yield trackedEntity != null ? Stream.of(trackedEntity).filter(entity -> selector.target().type().isAssignableFrom(entity.entity().getClass())) : Stream.empty();
            }
            case EntitySelector.Gather.Range(double radius) -> {
                //TODO optimize for always inside chunk.
                final var doubleRadius = radius * radius;
                yield correctIndex.values().stream().filter(trackedEntity -> origin.distanceSquared(trackedEntity.lastPosition().getPlain()) < doubleRadius);
            }
            case EntitySelector.Gather.Chunk(int chunkX, int chunkZ) -> {
                final long index = CoordConversion.chunkIndex(chunkX, chunkZ);
                final ObjectArrayList<TrackedEntity> entities = chunksEntities.get(index);
                yield entities != null ? entities.stream()
                        .filter(entity -> selector.target().type().isAssignableFrom(entity.entity().getClass())) : Stream.empty();
            }
            case EntitySelector.Gather.ChunkRange(int radius) -> {
                final LongArrayList chunkIndexes = new LongArrayList(0);
                ChunkRange.chunksInRange(origin.chunkX(), origin.chunkZ(), radius, (chunkX, chunkZ) -> {
                    final long index = CoordConversion.chunkIndex(chunkX, chunkZ);
                    if (chunksEntities.containsKey(index)) {
                        chunkIndexes.add(index);
                    }
                });

                yield (ServerFlag.ENTITY_TRACKER_PARALLEL_CHUNK_STREAM ? chunkIndexes.longParallelStream() : chunkIndexes.longStream())
                        .mapToObj(chunksEntities::get)
                        .flatMap(Collection::stream)
                        .filter(entity -> selector.target().type().isAssignableFrom(entity.entity().getClass()));
            }
            // Maybe `None` condition?
            case null -> correctIndex.values().stream();
        };

        {
            // noinspection unchecked
            stream = stream.filter(
                    trackedEntity -> selector.test(origin, (R) trackedEntity.entity())
            );
        }

        // We must always sort because the limit takes the first few elements.
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

        // They have already been identified to cast to R. Lets make sure we can cast to it now.
        return stream.map(TrackedEntity::entity)
                .map(it -> selector.target().type().cast(it));
    }

    private void addEntityClass(TrackedEntity trackedEntity) {
        final var index = trackedEntity.entity().getClass();
        final var inheritance = inheritanceMapCache.computeIfAbsent(index, this::computeCacheEntry);

        inheritance.forEach(entityClass -> append(entityClass, trackedEntity));
        append(index, trackedEntity);
    }
    private void append(Class<? extends Entity> currentClass, TrackedEntity trackedEntity) {
        var out = classToIndex.computeIfAbsent(currentClass, ignored -> new Int2ObjectOpenHashMap<>());
        out.put(trackedEntity.entity().getEntityId(), trackedEntity);
    }

    private TrackedEntity removeEntityClass(Entity entity) {
        final var index = entity.getClass();
        final var id = entity.getEntityId();
        final var inheritance = inheritanceMapCache.computeIfAbsent(index, this::computeCacheEntry);

        inheritance.forEach(entityClass -> remove(entityClass, id));

        // We skip the first to come back to return it.
        return remove(index, id);
    }

    private TrackedEntity remove(Class<? extends Entity> index, int id) {
        var out = classToIndex.get(index);
        var entry = out.remove(id);
        if (out.isEmpty()){
            inheritanceMapCache.remove(index);
            classToIndex.remove(index);

            // Trim everything once we do this.
            trim();
        }
        return entry;
    }

    private void trim() {
        classToIndex.trim();
        inheritanceMapCache.trim();
        uuidIndex.trim();
    }

    private ObjectArrayList<Class<? extends Entity>> computeCacheEntry(Class<? extends Entity> entityClass) {
        ObjectArrayList<Class<? extends Entity>> set = new ObjectArrayList<>();
        // We will never store the current class, no reason to.
        Class<?> currentClass = entityClass.getSuperclass();
        while (Entity.class.isAssignableFrom(currentClass)) {
            //noinspection unchecked
            set.add((Class<? extends Entity>) currentClass);
            currentClass = currentClass.getSuperclass();
        }
        set.trim();
        return set;
    }

    private void difference(Point oldPoint, Point newPoint, @NotNull Update update) {
        ChunkRange.chunksInRangeDiffering(newPoint.chunkX(), newPoint.chunkZ(), oldPoint.chunkX(), oldPoint.chunkZ(),
                ServerFlag.ENTITY_VIEW_DISTANCE, (chunkX, chunkZ) -> {
                    // Add
                    final ObjectArrayList<TrackedEntity> entities = chunksEntities.get(CoordConversion.chunkIndex(chunkX, chunkZ));
                    if (entities == null || entities.isEmpty()) return;
                    for (TrackedEntity entity : entities) update.add(entity.entity());
                }, (chunkX, chunkZ) -> {
                    // Remove
                    final ObjectArrayList<TrackedEntity> entities = chunksEntities.get(CoordConversion.chunkIndex(chunkX, chunkZ));
                    if (entities == null || entities.isEmpty()) return;
                    for (TrackedEntity entity : entities) update.remove(entity.entity());
                });
    }

    private record TrackedEntity(Entity entity, AtomicReference<Point> lastPosition) {}
}

package net.minestom.server.instance;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
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

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

final class EntityTrackerImpl implements EntityTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityTrackerImpl.class);

    private static final EntitySelector<Entity> SELECTOR = EntitySelector.selector(EntitySelector.Target.entity(), builder -> builder.gather(EntitySelector.Gather.chunkRange(ServerFlag.ENTITY_VIEW_DISTANCE)));

    // Indexes
    private final Object2ObjectLinkedOpenHashMap<Class<? extends Entity>, Int2ObjectMap<TrackedEntity>> classToIndex = new Object2ObjectLinkedOpenHashMap<>();

    private final IntArraySet idIndex = new IntArraySet();
    private final Map<UUID, TrackedEntity> uuidIndex = new HashMap<>();
    //private final Int2ObjectMap<TrackedEntity> playerIdIndex = new Int2ObjectOpenHashMap<>();

    // Spatial partitioning
    private final Long2ObjectMap<Set<Entity>> chunksEntities = new Long2ObjectOpenHashMap<>();

    @Override
    public synchronized void register(@NotNull Entity entity, @NotNull Point point, @Nullable Update update) {
        TrackedEntity newEntry = new TrackedEntity(entity, new AtomicReference<>(point));
        // Indexing
        var exists = idIndex.add(entity.getEntityId());
        Check.isTrue(exists, "There is already an entity registered with id {0}", entity.getEntityId());
        TrackedEntity prevEntryWithUuid = uuidIndex.putIfAbsent(entity.getUuid(), newEntry);
        Check.isTrue(prevEntryWithUuid == null, "There is already an entity registered with uuid {0}", entity.getUuid());
        addEntityClass(newEntry);
        // Spatial partitioning
        final long index = CoordConversion.chunkIndex(point);
        Set<Entity> chunkEntities = chunksEntities.computeIfAbsent(index, t -> new HashSet<>());
        chunkEntities.add(entity);
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
        var removed = idIndex.remove(entity.getEntityId());
        if (!removed) return;
        uuidIndex.remove(entity.getUuid());
        var entry = removeEntityClass(entity);
        Check.notNull(entry, "Unregistered entity does not have a valid entry!");
        // Spatial partitioning
        final Point point = entry.lastPosition().getPlain();
        final long index = CoordConversion.chunkIndex(point);
        Set<Entity> chunkEntities = chunksEntities.get(index);
        chunkEntities.remove(entity);
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
        // Chunk change, update partitions
        final long oldIndex = CoordConversion.chunkIndex(oldPoint);
        final long newIndex = CoordConversion.chunkIndex(newPoint);
        Set<Entity> oldPartition = chunksEntities.computeIfAbsent(oldIndex, t -> new HashSet<>());
        Set<Entity> newPartition = chunksEntities.computeIfAbsent(newIndex, t -> new HashSet<>());
        oldPartition.remove(entity);
        newPartition.add(entity);
        if (oldPartition.isEmpty()) {
            chunksEntities.remove(oldIndex); // Empty chunk
        }
        if (newPartition.isEmpty()) {
            chunksEntities.remove(newIndex); // Empty chunk
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
                yield trackedEntity != null ? Stream.of(trackedEntity) : Stream.empty();
            }
            case EntitySelector.Gather.Range(double radius) -> {
                //TODO optimize for always inside chunk.
                final var doubleRadius = radius * radius;
                yield correctIndex.values().stream().filter(trackedEntity -> origin.distanceSquared(trackedEntity.lastPosition().getPlain()) < doubleRadius);
            }
            case EntitySelector.Gather.Chunk(int chunkX, int chunkZ) -> {
                final long index = CoordConversion.chunkIndex(chunkX, chunkZ);
                final Set<Entity> entities = chunksEntities.get(index);
                yield entities != null ? entities.stream().map(entity -> correctIndex.get(entity.getEntityId())) : Stream.empty();
            }
            case EntitySelector.Gather.ChunkRange(int radius) -> {
                final Set<Entity> entities = new ObjectArraySet<>();
                ChunkRange.chunksInRange(origin.chunkX(), origin.chunkZ(), radius, (chunkX, chunkZ) -> {
                    final long index = CoordConversion.chunkIndex(chunkX, chunkZ);
                    final Set<Entity> chunkEntities = chunksEntities.get(index);
                    if (chunkEntities != null) {
                        entities.addAll(chunkEntities);
                    }
                });
                yield entities.stream()
                        .filter(entity -> selector.target().type().isAssignableFrom(entity.getClass()))
                        .map(entity -> correctIndex.get(entity.getEntityId()));
            }
            // Maybe `None` condition?
            case null -> correctIndex.values().stream();
        };

        stream = stream.filter(trackedEntity -> {
            if (trackedEntity == null) {
                System.out.println("how");
                throw new RuntimeException("how");
            }
            return true;
        });

        {
            // noinspection unchecked
            stream = stream.filter(
                    trackedEntity -> selector.test(origin, (R) trackedEntity.entity())
            );
        }

        if (selector.limit() != 1) {
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
        }

        if (selector.limit() != -1) {
            stream = stream.limit(selector.limit());
        }

        // We will pass the error back up the chain if it's wrong.
        // noinspection unchecked
        return stream.map(TrackedEntity::entity).map(it-> (R) it);
    }

    // TODO clean these functions up
    private void addEntityClass(TrackedEntity trackedEntity) {
        Class<?> currentClass = trackedEntity.entity().getClass();
        while (Entity.class.isAssignableFrom(currentClass)) {
            var out = classToIndex.computeIfAbsent((Class<? extends Entity>) currentClass, ignored -> new Int2ObjectOpenHashMap<>());
            out.put(trackedEntity.entity().getEntityId(), trackedEntity);
            currentClass = currentClass.getSuperclass();
        }
    }

    private TrackedEntity removeEntityClass(Entity entity) {
        Class<?> currentClass = entity.getClass().getSuperclass();
        while (Entity.class.isAssignableFrom(currentClass)) {
            var out = classToIndex.get(currentClass);
            out.remove(entity.getEntityId());
            if (out.isEmpty()){
                classToIndex.remove(currentClass);
            }
            currentClass = currentClass.getSuperclass();
        }
        // We skip the first to come back to return it.
        var out = classToIndex.get(entity.getClass());
        var entry = out.remove(entity.getEntityId());
        if (out.isEmpty()){
            classToIndex.remove(entity.getClass());
        }

        return entry;
    }

    private void difference(Point oldPoint, Point newPoint, @NotNull Update update) {
        ChunkRange.chunksInRangeDiffering(newPoint.chunkX(), newPoint.chunkZ(), oldPoint.chunkX(), oldPoint.chunkZ(),
                ServerFlag.ENTITY_VIEW_DISTANCE, (chunkX, chunkZ) -> {
                    // Add
                    final Set<Entity> entities = chunksEntities.get(CoordConversion.chunkIndex(chunkX, chunkZ));
                    if (entities == null || entities.isEmpty()) return;
                    for (Entity entity : entities) update.add(entity);
                }, (chunkX, chunkZ) -> {
                    // Remove
                    final Set<Entity> entities = chunksEntities.get(CoordConversion.chunkIndex(chunkX, chunkZ));
                    if (entities == null || entities.isEmpty()) return;
                    for (Entity entity : entities) update.remove(entity);
                });
    }

    private record TrackedEntity(Entity entity, AtomicReference<Point> lastPosition) {}
}

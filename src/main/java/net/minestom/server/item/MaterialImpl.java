package net.minestom.server.item;

import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.component.DataComponent;
import net.minestom.server.component.DataComponentMap;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.component.Equippable;
import net.minestom.server.item.component.TypedCustomData;
import net.minestom.server.registry.*;
import net.minestom.server.utils.Either;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Map;

// Unfortunate cyclic issue with block keys.
record MaterialImpl(Key key, int id, String translationKey, @Nullable RegistryKey<Block> blockKey, Record record) implements Material {
    static final Registry<Material> REGISTRY = MaterialValues.load(BuiltinRegistries.ITEM);

    static @UnknownNullability Material get(RegistryKey<Material> key) {
        return REGISTRY.get(key);
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    public boolean isBlock() {
        return blockKey != null;
    }

    @Override
    public @Nullable Block block() {
        if (!isBlock()) return null;
        // NPE if not existing.
        return Block.staticRegistry().get(blockKey);
    }

    @Override
    public DataComponentMap prototype() {
        return record().prototype(key);
    }

    @Override
    public boolean isArmor() {
        final Equippable equippableComponent = prototype().get(DataComponents.EQUIPPABLE);
        final EquipmentSlot equipmentSlot = equippableComponent == null ? null : equippableComponent.slot();
        return equipmentSlot != null && equipmentSlot.isArmor();
    }

    @Override
    public @Nullable EquipmentSlot equipmentSlot() {
        final Equippable equippableComponent = prototype().get(DataComponents.EQUIPPABLE);
        return equippableComponent == null ? null : equippableComponent.slot();
    }

    @Override
    public @Nullable EntityType spawnEntityType() {
        TypedCustomData<EntityType> entityData = prototype().get(DataComponents.ENTITY_DATA);
        return entityData == null ? null : entityData.type();
    }

    // Small mutable interface until its determined how to solve the prototype issue.
    static final class Record {
        private @Nullable Either<Map<String, Object>, DataComponentMap> prototype;

        Record(Map<String, Object> map) {
            this.prototype = Either.left(map);
        }

        public DataComponentMap prototype(Key debugKey) {
            return switch (prototype) {
                case Either.Right(var dataComponentMap) ->  dataComponentMap;
                case null -> DataComponentMap.EMPTY;
                case Either.Left(var components) -> {
                    final Transcoder<Object> coder = new RegistryTranscoder<>(Transcoder.JAVA, MinecraftServer.process());
                    DataComponentMap.Builder builder = DataComponentMap.builder();
                    for (Map.Entry<String, Object> entry : components.entrySet()) {
                        //noinspection unchecked
                        DataComponent<Object> component = (DataComponent<Object>) DataComponent.fromKey(entry.getKey());
                        Check.notNull(component, "Unknown component {0} in {1}", entry.getKey(), debugKey);

                        final Result<Object> result = component.decode(coder, entry.getValue());
                        switch (result) {
                            case Result.Ok(Object ok) -> builder.set(component, ok);
                            case Result.Error(String message) ->
                                    throw new IllegalStateException("Failed to decode component " + entry.getKey() + " in " + debugKey + ": " + message);
                        }
                    }
                    final DataComponentMap prototype = builder.build();
                    this.prototype = !prototype.isEmpty() ? Either.right(prototype) : null;
                    yield prototype(debugKey);
                }
            };
        }
    }
}

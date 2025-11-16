package net.minestom.server.item;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import net.minestom.server.codec.Codec;
import net.minestom.server.component.DataComponentMap;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.registry.Registry;
import net.minestom.server.registry.RegistryData;
import net.minestom.server.registry.StaticProtocolObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Collection;

public sealed interface Material extends StaticProtocolObject<Material>, Materials permits MaterialImpl {

    NetworkBuffer.Type<Material> NETWORK_TYPE = NetworkBuffer.VAR_INT.transform(Material::fromId, Material::id);
    Codec<Material> CODEC = Codec.KEY.transform(Material::fromKey, Material::key);

    boolean isBlock();

    @Nullable Block block();

    DataComponentMap prototype();

    boolean isArmor();

    default int maxStackSize() {
        return prototype().get(DataComponents.MAX_STACK_SIZE, 64);
    }

    @Nullable EquipmentSlot equipmentSlot();

    static Collection<Material> values() {
        return MaterialImpl.REGISTRY.values();
    }

    static @Nullable Material fromKey(@KeyPattern String key) {
        return fromKey(Key.key(key));
    }

    static @Nullable Material fromKey(Key key) {
        return MaterialImpl.REGISTRY.get(key);
    }

    static @Nullable Material fromId(int id) {
        return MaterialImpl.REGISTRY.get(id);
    }

    static Registry<Material> staticRegistry() {
        return MaterialImpl.REGISTRY;
    }

    /**
     * Gets the entity type this item can spawn. Only present for spawn eggs (e.g. wolf spawn egg, skeleton spawn egg)
     *
     * @return The entity type it can spawn, or null if it is not a spawn egg
     * @deprecated Read {@link DataComponents#ENTITY_DATA} for the spawned entity data.
     */
    @Deprecated(forRemoval = true)
    @Nullable EntityType spawnEntityType();
}

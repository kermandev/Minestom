package net.minestom.server.entity;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import net.minestom.server.codec.Codec;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.registry.Registry;
import net.minestom.server.registry.RegistryData;
import net.minestom.server.registry.StaticProtocolObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public sealed interface EntityType extends StaticProtocolObject<EntityType>, EntityTypes permits EntityTypeImpl {
    NetworkBuffer.Type<EntityType> NETWORK_TYPE = NetworkBuffer.VAR_INT.transform(EntityType::fromId, EntityType::id);
    Codec<EntityType> CODEC = Codec.KEY.transform(EntityType::fromKey, EntityType::key);

    String translationKey();

    double drag();

    double acceleration();

    double horizontalAirResistance();

    double verticalAirResistance();

    boolean shouldSendAttributes();

    double eyeHeight();

    boolean fireImmune();

    int clientTrackingRange();

    /**
     * Gets the entity attachment by name. Typically, will be PASSENGER or VEHICLE, but some entities have custom attachments (e.g. WARDEN_CHEST, NAMETAG)
     *
     * @param attachmentName The attachment to retrieve
     * @return A list of 3 doubles if the attachment is defined for this entity, or null if it is not defined
     */
    @Nullable List<Double> entityAttachment(String attachmentName);

    BoundingBox boundingBox();

    double width();

    double height();

    static Collection<EntityType> values() {
        return EntityTypeImpl.REGISTRY.values();
    }

    static @Nullable EntityType fromKey(@KeyPattern String key) {
        return fromKey(Key.key(key));
    }

    static @Nullable EntityType fromKey(Key key) {
        return EntityTypeImpl.REGISTRY.get(key);
    }

    static @Nullable EntityType fromId(int id) {
        return EntityTypeImpl.REGISTRY.get(id);
    }

    static Registry<EntityType> staticRegistry() {
        return EntityTypeImpl.REGISTRY;
    }
}

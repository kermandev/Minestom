package net.minestom.server.entity;

import net.kyori.adventure.key.Key;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.registry.BuiltinRegistries;
import net.minestom.server.registry.Registry;
import net.minestom.server.registry.RegistryData;
import net.minestom.server.registry.RegistryKey;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;
import java.util.Map;

record EntityTypeImpl(Key key, int id, String translationKey, double drag, double acceleration, boolean isLiving,
                      double width, double height, double eyeHeight, int clientTrackingRange, boolean fireImmune,
                      Map<String, List<Double>> entityOffsets, BoundingBox boundingBox) implements EntityType {
    static final Registry<EntityType> REGISTRY = EntityTypeValues.load(BuiltinRegistries.ENTITY_TYPE);

    static @UnknownNullability EntityType get(RegistryKey<EntityType> key) {
        return REGISTRY.get(key);
    }

    @Override
    public String toString() {
        return name();
    }

    public double horizontalAirResistance() {
        return isLiving ? 0.91 : 0.98;
    }

    public double verticalAirResistance() {
        return 1 - drag();
    }

    public boolean shouldSendAttributes() {
        return isLiving;
    }

    @Override
    public @Nullable List<Double> entityAttachment(String attachmentName) {
        return entityOffsets.get(attachmentName);
    }
}

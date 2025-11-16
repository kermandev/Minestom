package net.minestom.server.entity;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import net.minestom.server.codec.Codec;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.registry.RegistryData;
import net.minestom.server.registry.StaticProtocolObject;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public sealed interface VillagerProfession extends StaticProtocolObject<VillagerProfession>, VillagerProfessions permits VillagerProfessionImpl {

    NetworkBuffer.Type<VillagerProfession> NETWORK_TYPE = NetworkBuffer.VAR_INT.transform(VillagerProfession::fromId, VillagerProfession::id);
    Codec<VillagerProfession> NBT_TYPE = Codec.STRING.transform(VillagerProfession::fromKey, VillagerProfession::name);

    @Contract(pure = true)
    @Nullable SoundEvent workSound();

    static Collection<VillagerProfession> values() {
        return VillagerProfessionImpl.REGISTRY.values();
    }

    static @Nullable VillagerProfession fromKey(@KeyPattern String key) {
        return fromKey(Key.key(key));
    }

    static @Nullable VillagerProfession fromKey(Key key) {
        return VillagerProfessionImpl.REGISTRY.get(key);
    }

    static @Nullable VillagerProfession fromId(int id) {
        return VillagerProfessionImpl.REGISTRY.get(id);
    }

}

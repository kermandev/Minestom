package net.minestom.server.game;


import net.kyori.adventure.key.Key;
import net.minestom.server.registry.BuiltinRegistries;
import net.minestom.server.registry.Registry;
import net.minestom.server.registry.RegistryData;
import net.minestom.server.registry.RegistryKey;
import org.jetbrains.annotations.UnknownNullability;

/**
 * Represents a game event implementation.
 * Used for a wide variety of events, from weather to bed use to game mode to demo messages.
 */
record GameEventImpl(Key key, int id, int notificationRadius) implements GameEvent {
    static final Registry<GameEvent> REGISTRY = GameEventValues.load(BuiltinRegistries.GAME_EVENT);

    public static @UnknownNullability GameEvent get(RegistryKey<GameEvent> key) {
        return REGISTRY.get(key);
    }

}
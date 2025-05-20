package net.minestom.server.entity;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntitySelector.Property;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static net.minestom.server.entity.EntitySelector.property;
import static net.minestom.server.entity.EntitySelector.selector;

public final class EntitySelectors {
    private static final EntitySelector<Entity> ALL = selector(s -> s.target(EntitySelector.Target.ENTITIES));
    private static final EntitySelector<Player> PLAYERS = selector(s -> s.target(EntitySelector.Target.PLAYERS));

    // Properties
    public static final Property<Entity, Integer> ID = property("id", Entity.class, Entity::getEntityId);
    public static final Property<Entity, UUID> UUID = property("uuid", Entity.class, Entity::getUuid);
    public static final Property<Player, String> NAME = property("name", Player.class, Player::getUsername);
    public static final Property<Entity, Pos> POS = property("coord", Entity.class, Entity::getPosition);
    public static final Property<Entity, EntityType> TYPE = property("entity_type", Entity.class, Entity::getEntityType);
    public static final Property<Player, GameMode> GAME_MODE = property("game_mode", Player.class, Player::getGameMode);
    public static final Property<Player, Integer> LEVEL = property("level", Player.class, Player::getLevel);
    public static final Property<Player, Float> EXPERIENCE = property("experience", Player.class, Player::getExp);

    public static @NotNull EntitySelector<Entity> all() {
        return ALL;
    }

    public static @NotNull EntitySelector<Player> players() {
        return PLAYERS;
    }
}

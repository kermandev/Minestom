package net.minestom.server.entity;

import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.EntityTracker;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.testing.Env;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static net.minestom.server.entity.EntitySelector.Target;
import static net.minestom.server.entity.EntitySelectors.NAME;
import static net.minestom.server.entity.EntitySelectors.TYPE;

public final class EntitySelectorTest {

    @Test
    public void playerQuery() {
        EntitySelector<Player> selectorPlayer = EntitySelector.player(builder -> {
            final var asNewPlayer = builder.reinterpret(Target.of(CustomPlayer.class));
            asNewPlayer.predicateEquals(TYPE, EntityType.PLAYER);
            asNewPlayer.predicate(NAME, (point, o) -> o.equals("TheMode"));
            asNewPlayer.limit(5);
        });

        var tracker = EntityTracker.newTracker();
        tracker.selectEntity(selectorPlayer).forEach(trackedEntity -> {
            // Do something with the entity
            trackedEntity.sendMessage("hello!");
        });

    }


    static class CustomPlayer extends Player {
        public CustomPlayer(@NotNull PlayerConnection playerConnection, @NotNull GameProfile gameProfile) {
            super(playerConnection, gameProfile);
        }
    }
}

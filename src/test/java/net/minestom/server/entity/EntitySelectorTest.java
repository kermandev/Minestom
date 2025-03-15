package net.minestom.server.entity;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.EntityTracker;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static net.minestom.server.entity.EntitySelectors.NAME;
import static net.minestom.server.entity.EntitySelectors.TYPE;

public final class EntitySelectorTest {

    @Test
    public void playerQuery() {
        var selectorPlayer = EntitySelector.selector(Player.class, builder -> {
            builder.predicateEquals(TYPE, EntityType.PLAYER);
            builder.predicate(NAME, (point, o) -> o.equals("TheMode"));
            builder.limit(5);
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

    static class CustomPlayer2 extends Player {
        public CustomPlayer2(@NotNull PlayerConnection playerConnection, @NotNull GameProfile gameProfile) {
            super(playerConnection, gameProfile);
        }
    }
}

package net.minestom.server.collision;

import net.minestom.server.utils.Direction;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@EnvTest
class EntityBlockTouchTickIntegrationTest {

    @Test
    void entityPhysicsCheckTouchTick(Env env) {
        var instance = env.createFlatInstance();

        var blockPos = Set.of(new Vec(-1, 43, 0),
                new Vec(1, 43, 0),
                new Vec(0, 43, -1),
                new Vec(0, 43, 1),
                new Vec(0, 45, 0) // Y+
        );
        var blockBelow = new Vec(0, 42, 0); // Y-

        var entity = new Entity(EntityType.ZOMBIE);
        var spawnPoint = new Pos(0.5, 43, 0.5);

        Set<Point> positions = new HashSet<>();
        var handler = new BlockHandler() {
            @Override
            public void onTouch(@NotNull Touch touch) {
                assertTrue(positions.add(touch.blockPosition()));
            }

            @Override
            public @NotNull Key getKey() {
                return Key.key("minestom:test");
            }
        };
        var customBlock = Block.STONE.withHandler(handler);

        for (var pos : blockPos) {
            instance.setBlock(pos, customBlock);
        }
        instance.setBlock(blockBelow, Block.STONE); // Regular floor as we are going to be sliding into the blocks.

        entity.setInstance(instance, spawnPoint).join();

        Arrays.stream(Direction.values())
                .filter(direction -> direction != Direction.DOWN)
                .map(Direction::vec)
                .forEachOrdered(vec -> {
            entity.setVelocity(vec.mul(10));
            entity.tick(0);
            entity.teleport(spawnPoint);
        });

        assertEquals(blockPos, positions);

        positions.clear(); // Final -Y check
        instance.setBlock(blockBelow, customBlock);
        entity.tick(0);
        assertEquals(Set.of(blockBelow), positions);
    }
    @Test
    void entityPhysicsCheckTouchTickFarPositiveXNegativeZ(Env env) {
        var instance = env.createFlatInstance();
        instance.loadChunk(new Pos(1000, 1000, -1000));

        var offset = new Vec(1000, 0, -1000);
        var blockPos = Set.of(offset.add(-1, 43, 0),
                offset.add(1, 43, 0),
                offset.add(0, 43, -1),
                offset.add(0, 43, 1),
                offset.add(0, 45, 0) // Y+
        );
        var blockBelow = offset.add(0, 42, 0); // Y-

        var entity = new Entity(EntityType.ZOMBIE);
        var spawnPoint = offset.add(0.5, 43, 0.5).asPosition();

        Set<Point> positions = new HashSet<>();
        var handler = new BlockHandler() {
            @Override
            public void onTouch(@NotNull Touch touch) {
                assertTrue(positions.add(touch.blockPosition()));
            }

            @Override
            public @NotNull Key getKey() {
                return Key.key("minestom:test");
            }
        };
        var customBlock = Block.STONE.withHandler(handler);

        for (var pos : blockPos) {
            instance.setBlock(pos, customBlock);
        }
        instance.setBlock(blockBelow, Block.STONE); // Regular floor as we are going to be sliding into the blocks.

        entity.setInstance(instance, spawnPoint).join();

        Arrays.stream(Direction.values())
                .filter(direction -> direction != Direction.DOWN)
                .map(Direction::vec)
                .forEachOrdered(vec -> {
                    entity.setVelocity(vec.mul(10));
                    entity.tick(0);
                    entity.teleport(spawnPoint);
                });

        assertEquals(blockPos, positions);

        positions.clear(); // Final -Y check
        instance.setBlock(blockBelow, customBlock);
        entity.tick(0);
        assertEquals(Set.of(blockBelow), positions);
    }

    @Test
    void entityPhysicsCheckTouchTickFarNegativeXPositiveZ(Env env) {
        var instance = env.createFlatInstance();
        instance.loadChunk(new Pos(-1000, 1000, 1000));

        var offset = new Vec(-1000, 0, 1000);
        var blockPos = Set.of(offset.add(-1, 43, 0),
                offset.add(1, 43, 0),
                offset.add(0, 43, -1),
                offset.add(0, 43, 1),
                offset.add(0, 45, 0) // Y+
        );

        var blockBelow = offset.add(0, 42, 0); // Y-

        var entity = new Entity(EntityType.ZOMBIE);
        var spawnPoint = offset.add(0.5, 43, 0.5).asPosition();

        Set<Point> positions = new HashSet<>();
        var handler = new BlockHandler() {
            @Override
            public void onTouch(@NotNull Touch touch) {
                assertTrue(positions.add(touch.blockPosition()));
            }

            @Override
            public @NotNull Key getKey() {
                return Key.key("minestom:test");
            }
        };
        var customBlock = Block.STONE.withHandler(handler);

        for (var pos : blockPos) {
            instance.setBlock(pos, customBlock);
        }
        instance.setBlock(blockBelow, Block.STONE); // Regular floor as we are going to be sliding into the blocks.

        entity.setInstance(instance, spawnPoint).join();

        Arrays.stream(Direction.values())
                .filter(direction -> direction != Direction.DOWN)
                .map(Direction::vec)
                .forEachOrdered(vec -> {
                    entity.setVelocity(vec.mul(10));
                    entity.tick(0);
                    entity.teleport(spawnPoint);
                });

        assertEquals(blockPos, positions);

        positions.clear(); // Final -Y check
        instance.setBlock(blockBelow, customBlock);
        entity.tick(0);
        assertEquals(Set.of(blockBelow), positions);
    }

    @Test
    void entityPhysicsCheckTouchTickFarPositiveXZ(Env env) {
        var instance = env.createFlatInstance();
        instance.loadChunk(new Pos(1000, 1000, 1000));

        var offset = new Vec(1000, 0, 1000);
        var blockPos = Set.of(offset.add(-1, 43, 0),
                offset.add(1, 43, 0),
                offset.add(0, 43, -1),
                offset.add(0, 43, 1),
                offset.add(0, 45, 0) // Y+
        );

        var blockBelow = offset.add(0, 42, 0); // Y-

        var entity = new Entity(EntityType.ZOMBIE);
        var spawnPoint = offset.add(0.5, 43, 0.5).asPosition();

        Set<Point> positions = new HashSet<>();
        var handler = new BlockHandler() {
            @Override
            public void onTouch(@NotNull Touch touch) {
                assertTrue(positions.add(touch.blockPosition()));
            }

            @Override
            public @NotNull Key getKey() {
                return Key.key("minestom:test");
            }
        };
        var customBlock = Block.STONE.withHandler(handler);

        for (var pos : blockPos) {
            instance.setBlock(pos, customBlock);
        }
        instance.setBlock(blockBelow, Block.STONE); // Regular floor as we are going to be sliding into the blocks.

        entity.setInstance(instance, spawnPoint).join();

        Arrays.stream(Direction.values())
                .filter(direction -> direction != Direction.DOWN)
                .map(Direction::vec)
                .forEachOrdered(vec -> {
                    entity.setVelocity(vec.mul(10));
                    entity.tick(0);
                    entity.teleport(spawnPoint);
                });

        assertEquals(blockPos, positions);

        positions.clear(); // Final -Y check
        instance.setBlock(blockBelow, customBlock);
        entity.tick(0);
        assertEquals(Set.of(blockBelow), positions);
    }

    @Test
    void entityPhysicsCheckTouchTickFarNegativeXZ(Env env) {
        var instance = env.createFlatInstance();
        instance.loadChunk(new Pos(-1000, 1000, -1000));

        var offset = new Vec(-1000, 0, -1000);
        var blockPos = Set.of(offset.add(-1, 43, 0),
                offset.add(1, 43, 0),
                offset.add(0, 43, -1),
                offset.add(0, 43, 1),
                offset.add(0, 45, 0) // Y+
        );

        var blockBelow = offset.add(0, 42, 0); // Y-

        var entity = new Entity(EntityType.ZOMBIE);
        var spawnPoint = offset.add(0.5, 43, 0.5).asPosition();

        Set<Point> positions = new HashSet<>();
        var handler = new BlockHandler() {
            @Override
            public void onTouch(@NotNull Touch touch) {
                assertTrue(positions.add(touch.blockPosition()));
            }

            @Override
            public @NotNull Key getKey() {
                return Key.key("minestom:test");
            }
        };
        var customBlock = Block.STONE.withHandler(handler);

        for (var pos : blockPos) {
            instance.setBlock(pos, customBlock);
        }
        instance.setBlock(blockBelow, Block.STONE); // Regular floor as we are going to be sliding into the blocks.

        entity.setInstance(instance, spawnPoint).join();

        Arrays.stream(Direction.values())
                .filter(direction -> direction != Direction.DOWN)
                .map(Direction::vec)
                .forEachOrdered(vec -> {
                    entity.setVelocity(vec.mul(10));
                    entity.tick(0);
                    entity.teleport(spawnPoint);
                });

        assertEquals(blockPos, positions);

        positions.clear(); // Final -Y check
        instance.setBlock(blockBelow, customBlock);
        entity.tick(0);
        assertEquals(Set.of(blockBelow), positions);
    }

    @Test
    void entityTouchFastPhysicsBadBehavior(Env env) {
        var instance = env.createFlatInstance();

        var block = Block.STONE.withHandler(new BlockHandler() {
            @Override
            public void onTouch(@NotNull Touch touch) {
                fail(touch + " should not happen");
            }

            @Override
            public @NotNull Key getKey() {
                return Key.key("minestom:test");
            }
        });
        instance.setBlock(0, 41, 0, Block.STONE);
        instance.setBlock(0, 42, 0, block);
        instance.setBlock(0, 43, 0, block);

        // Spawn entity inside existing blocks.
        var entity = new Entity(EntityType.ZOMBIE) {
            @Override
            protected boolean isFastTouch() {
                return true;
            }
        };
        entity.setInstance(instance, new Pos(0.5, 42, 0.5)).join();
        entity.tick(0);
    }
}

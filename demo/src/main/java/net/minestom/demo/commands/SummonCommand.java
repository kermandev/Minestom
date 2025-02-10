package net.minestom.demo.commands;

import net.minestom.demo.entity.ZombieCreature;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentEnum;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.registry.ArgumentEntityType;
import net.minestom.server.command.builder.arguments.number.ArgumentInteger;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.utils.location.RelativeVec;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class SummonCommand extends Command {

    private final ArgumentEntityType entity;
    private final Argument<RelativeVec> pos;
    private final Argument<EntityClass> entityClass;
    private final ArgumentInteger amount;
    private final ArgumentInteger radius;

    public SummonCommand() {
        super("summon");
        setCondition(Conditions::playerOnly);

        entity = ArgumentType.EntityType("entity type");
        pos = ArgumentType.RelativeVec3("pos").setDefaultValue(() -> new RelativeVec(
                new Vec(0, 0, 0),
                RelativeVec.CoordinateType.RELATIVE,
                true, true, true
        ));
        entityClass = ArgumentType.Enum("class", EntityClass.class)
                .setFormat(ArgumentEnum.Format.LOWER_CASED)
                .setDefaultValue(EntityClass.CREATURE);

        amount = ArgumentType.Integer("amount");
        amount.setDefaultValue(1);

        radius = ArgumentType.Integer("radius");
        radius.setDefaultValue(0);

        addSyntax(this::execute, entity, pos, entityClass, amount, radius);
        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /summon <type> <x> <y> <z> <class> [amount] [radius]"));
    }

    private void execute(@NotNull CommandSender commandSender, @NotNull CommandContext commandContext) {
        final EntityClass entityClass = commandContext.get(this.entityClass);
        final EntityType entityType = commandContext.get(this.entity);
        final Vec pos = commandContext.get(this.pos).fromSender(commandSender);
        final int amount = commandContext.get(this.amount);
        final int radius = commandContext.get(this.radius);

        final Random random = new Random();

        int spawnedGood = 0;
        for (int i = 0; i < amount; i++) {
            final Entity entity = entityClass.instantiate(entityType);
            //noinspection ConstantConditions - One couldn't possibly execute a command without being in an instance
            try {
                entity.setInstance(((Player) commandSender).getInstance(), pos.add(random.nextDouble() * 2 * radius - radius, 0, random.nextDouble() * 2 * radius - radius));
                spawnedGood++;
            } catch (Exception e) {
                System.out.println("failed spawn for " + i);
            }
        }
        System.out.println("Done! with " + spawnedGood);
    }

    @SuppressWarnings("unused")
    enum EntityClass {
        BASE(Entity::new),
        LIVING(LivingEntity::new),
        CREATURE(EntityCreature::new),
        ZOMBIE(ZombieCreature::new);
        private final EntityFactory factory;

        EntityClass(EntityFactory factory) {
            this.factory = factory;
        }

        public Entity instantiate(EntityType type) {
            return factory.newInstance(type);
        }
    }

    @FunctionalInterface
    interface EntityFactory {
        Entity newInstance(EntityType type);
    }
}

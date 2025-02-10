package net.minestom.demo.commands;

import net.minestom.demo.entity.ZombieCreature;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntitySelector;
import net.minestom.server.entity.Player;

import java.util.List;

public class EntitySelectorCommand extends Command {

    public EntitySelectorCommand() {
        super("ent");

        setDefaultExecutor((sender, context) -> sender.sendMessage("DEFAULT"));

        var argumentEntity = ArgumentType.Entity("entities");
        var argumentPlayer = ArgumentType.Player("players");
        var targetZombie = ArgumentType.Entity("targets", ZombieCreature.class);

        setArgumentCallback((sender, exception) -> exception.printStackTrace(), argumentEntity);

        addSyntax(this::executor, argumentEntity);
        addSyntax(this::playerExecutor, ArgumentType.Literal("player"), argumentPlayer);
        addSyntax(this::targetExecutor, ArgumentType.Literal("target"), targetZombie);
    }

    private void executor(CommandSender commandSender, CommandContext context) {
        EntitySelector<Entity> selector = context.get("entities");
        List<Entity> entities = commandSender.selectEntity(selector).toList();
        commandSender.sendMessage("found " + entities.size() + " entities");
    }

    private void playerExecutor(CommandSender commandSender, CommandContext context) {
        EntitySelector<Player> selector = context.get("players");
        List<Player> entities = commandSender.selectEntity(selector).toList();
        commandSender.sendMessage("found " + entities.size() + " players");
    }

    private void targetExecutor(CommandSender commandSender, CommandContext context) {
        EntitySelector<ZombieCreature> selector = context.get("targets");
        List<ZombieCreature> entities = commandSender.selectEntity(selector).toList();
        commandSender.sendMessage("found " + entities.size() + " target");
        entities.forEach(EntityCreature::kill);
    }
}

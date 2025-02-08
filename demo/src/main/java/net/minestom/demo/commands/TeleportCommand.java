package net.minestom.demo.commands;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntitySelector;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.location.RelativeVec;

public class TeleportCommand extends Command {

    public TeleportCommand() {
        super("tp");

        setCondition(Conditions::playerOnly);
        setDefaultExecutor((source, context) -> source.sendMessage(Component.text("Usage: /tp x y z")));

        var posArg = ArgumentType.RelativeVec3("pos");
        var playerArg = ArgumentType.Player("player");

        addSyntax(this::onPlayerTeleport, playerArg);
        addSyntax(this::onPositionTeleport, posArg);
    }

    private void onPlayerTeleport(CommandSender sender, CommandContext context) {
        final Player player = (Player) sender;
        final EntitySelector<Player> selector = context.get("player");

        final Player target = sender.selectEntityFirst(selector);
        if (target == null) return;
        player.teleport(target.getPosition());
        sender.sendMessage(Component.text("Teleported to player ").append(target.getName()));
    }

    private void onPositionTeleport(CommandSender sender, CommandContext context) {
        final Player player = (Player) sender;

        final RelativeVec relativeVec = context.get("pos");
        final Pos position = player.getPosition().withCoord(relativeVec.from(player));
        player.teleport(position);
        player.sendMessage(Component.text("You have been teleported to " + position));
    }
}

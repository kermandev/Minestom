package net.minestom.demo.commands;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntitySelector;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.inventory.TransactionOption;
import net.minestom.server.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static net.minestom.server.command.builder.arguments.ArgumentType.*;

public class GiveCommand extends Command {
    public GiveCommand() {
        super("give");

        setDefaultExecutor((sender, context) ->
                sender.sendMessage(Component.text("Usage: /give <target> <item> [<count>]")));

        final var player = Player("target");
        addSyntax((sender, context) -> {
            final EntitySelector<Player> selector = context.get(player);
            int count = context.get("count");
            count = Math.min(count, PlayerInventory.INVENTORY_SIZE * 64);
            ItemStack itemStack = context.get("item");

            List<ItemStack> itemStacks;
            if (count <= 64) {
                itemStack = itemStack.withAmount(count);
                itemStacks = List.of(itemStack);
            } else {
                itemStacks = new ArrayList<>();
                while (count > 64) {
                    itemStacks.add(itemStack.withAmount(64));
                    count -= 64;
                }
                itemStacks.add(itemStack.withAmount(count));
            }

            sender.selectEntity(selector, target ->
                    target.getInventory().addItemStacks(itemStacks, TransactionOption.ALL)
            );

            sender.sendMessage(Component.text("Items have been given successfully!"));

        }, player, ItemStack("item"), Integer("count").setDefaultValue(() -> 1));
    }
}

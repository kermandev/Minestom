package net.minestom.server.command.builder;

import net.minestom.server.command.CommandManager;
import net.minestom.server.command.CommandParser.Result;
import net.minestom.server.command.CommandParser.Result.KnownCommand.Invalid;
import net.minestom.server.command.CommandParser.Result.KnownCommand.Valid;
import net.minestom.server.command.CommandParser.Result.UnknownCommand;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandResult.Type;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Class responsible for parsing {@link Command}.
 */
public class CommandDispatcher {
    private final CommandManager manager;

    public CommandDispatcher(CommandManager manager) {
        this.manager = manager;
    }

    public CommandDispatcher() {
        this(new CommandManager());
    }

    /**
     * Registers a command,
     * be aware that registering a command name or alias will override the previous entry.
     *
     * @param command the command to register
     */
    public void register(Command command) {
        manager.register(command);
    }

    public void unregister(Command command) {
        manager.unregister(command);
    }

    public Set<Command> getCommands() {
        return manager.getCommands();
    }

    /**
     * Gets the command class associated with the name.
     *
     * @param commandName the command name
     * @return the {@link Command} associated with the name, null if not any
     */
    public @Nullable Command findCommand(String commandName) {
        return manager.getCommand(commandName);
    }

    /**
     * Checks if the command exists, and execute it.
     *
     * @param source        the command source
     * @param commandString the command with the argument(s)
     * @return the command result
     */
    public CommandResult execute(CommandSender source, String commandString) {
        return manager.execute(source, commandString);
    }

    /**
     * Parses the given command.
     *
     * @param commandString the command (containing the command name and the args if any)
     * @return the parsing result
     */
    public CommandResult parse(CommandSender sender, String commandString) {
        final Result test = manager.parseCommand(sender, commandString);
        return resultConverter(test, commandString);
    }

    private static CommandResult resultConverter(Result parseResult, String input) {
        Type type = switch (parseResult) {
            case UnknownCommand unknownCommand -> Type.UNKNOWN;
            case Valid valid -> Type.SUCCESS;
            case Invalid invalid -> Type.INVALID_SYNTAX;
            case null -> throw new IllegalStateException("Unknown CommandParser.Result type");
        };
        return CommandResult.of(type, input, ParsedCommand.fromExecutable(parseResult.executable()), null);
    }
}

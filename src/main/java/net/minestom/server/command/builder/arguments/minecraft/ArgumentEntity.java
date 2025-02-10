package net.minestom.server.command.builder.arguments.minecraft;

import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.*;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.Range;
import net.minestom.server.utils.StringUtils;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Represents the target selector argument.
 * <a href="https://minecraft.wiki/w/Target_selectors">Target Selectors</a>
 * <p>
 * This argument is most commonly used with the entity tracker
 * using the {@link CommandSender#selectEntity(EntitySelector, Point)} or any combination required.
 */
public class ArgumentEntity<T extends Entity> extends Argument<EntitySelector<T>> {

    public static final int INVALID_SYNTAX = -2;
    public static final int ONLY_SINGLE_ENTITY_ERROR = -3;
    public static final int ONLY_PLAYERS_ERROR = -4;
    public static final int INVALID_ARGUMENT_NAME = -5;
    public static final int INVALID_ARGUMENT_VALUE = -6;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]{1,16}");
    private static final String SELECTOR_PREFIX = "@";
    private static final List<String> SELECTOR_VARIABLES = Arrays.asList("@p", "@r", "@a", "@e", "@s", "@n");
    private static final List<String> PLAYERS_ONLY_SELECTOR = Arrays.asList("@p", "@r", "@a", "@s");
    private static final List<String> SINGLE_ONLY_SELECTOR = Arrays.asList("@p", "@r", "@s", "@n");
    private static final List<String> REQUIRES_PLAYER_UPGRADED_TYPE = Arrays.asList("@p", "@r");
    // List with all the valid arguments
    private static final List<String> VALID_ARGUMENTS = Arrays.asList(
            "x", "y", "z",
            "distance", "dx", "dy", "dz",
            "scores", "tag", "team", "limit", "sort", "level", "gamemode", "name",
            "x_rotation", "y_rotation", "type", "nbt", "advancements", "predicate");

    // List with all the easily parsable arguments which only require reading until a specific character (comma)
    private static final List<String> SIMPLE_ARGUMENTS = Arrays.asList(
            "x", "y", "z",
            "distance", "dx", "dy", "dz",
            "scores", "tag", "team", "limit", "sort", "level", "gamemode",
            "x_rotation", "y_rotation", "type", "advancements", "predicate");


    private final EntitySelector.Target<T> target;
    private final boolean onlySingleEntity;
    private final boolean onlyPlayers;

    /**
     * See {@link net.minestom.server.command.builder.arguments.ArgumentType#Entity(String)}
     *  {@link net.minestom.server.command.builder.arguments.ArgumentType#Player(String)}
     *  {@link net.minestom.server.command.builder.arguments.ArgumentType#Entity(String, Class)}
     *  for usage.
     *  <p>
     *  This is considered the raw usage.
     * @param id The id of the argument.
     * @param entityClass The class of the most generic entity types you desire.
     */
    public ArgumentEntity(@NotNull String id, Class<T> entityClass) {
        super(id, true);

        this.target = EntitySelector.Target.of(entityClass);
        this.onlySingleEntity = false;
        this.onlyPlayers = Player.class.isAssignableFrom(entityClass);
    }

    /**
     * See {@link net.minestom.server.command.builder.arguments.ArgumentType#Entity(String)}
     *  {@link net.minestom.server.command.builder.arguments.ArgumentType#Player(String)}
     *  {@link net.minestom.server.command.builder.arguments.ArgumentType#Entity(String, Class)}
     *  for usage.
     *  <p>
     *  This is considered the raw usage.
     * @param id The id of the argument.
     * @param target The entity target you desire.
     */
    public ArgumentEntity(@NotNull String id, EntitySelector.Target<T> target) {
        super(id, true);

        this.target = target;
        this.onlySingleEntity = false;
        this.onlyPlayers = Player.class.isAssignableFrom(target.type());
    }

    // We might eventually enforce a class to be passed. Either way, this signature could change in the future.
    @ApiStatus.Experimental
    public ArgumentEntity(@NotNull String id, EntitySelector.Target<T> target, boolean onlySingleEntity, boolean onlyPlayers) {
        super(id, true);

        this.target = target;
        this.onlySingleEntity = onlySingleEntity;
        this.onlyPlayers = onlyPlayers;
    }

    /**
     * Sets the argument to only select a single entity.
     * <p>
     * Used best with {@link CommandSender#selectEntityFirst(EntitySelector)}
     *
     * @return new argument instance.
     */
    @Contract(pure = true)
    public ArgumentEntity<T> single() {
        return new ArgumentEntity<>(this.getId(), target, true, onlyPlayers);
    }

    @NotNull
    @Override
    public EntitySelector<T> parse(@NotNull CommandSender sender, @NotNull String input) throws ArgumentSyntaxException {
        // Check for raw player name or UUID
        if (!input.contains(SELECTOR_PREFIX) && !input.contains(StringUtils.SPACE)) {

            // Check if the input is a valid UUID
            try {
                final UUID uuid = UUID.fromString(input);
                return EntitySelector.selector(target, builder -> builder.gather(EntitySelector.Gather.onlyUuid(uuid)));
            } catch (IllegalArgumentException ignored) {
            }

            // Check if the input is a valid player name
            if (USERNAME_PATTERN.matcher(input).matches() && onlyPlayers) {
                return EntitySelector.selector(target,builder -> builder.predicateEquals(EntitySelectors.NAME, input));
            }
        }

        // The minimum size is always 2 (for the selector variable, ex: @p)
        if (input.length() < 2)
            throw new ArgumentSyntaxException("Length needs to be > 1", input, INVALID_SYNTAX);

        // The target selector variable always start by '@'
        if (!input.startsWith(SELECTOR_PREFIX))
            throw new ArgumentSyntaxException("Target selector needs to start with @", input, INVALID_SYNTAX);

        final String selectorVariable = input.substring(0, 2);

        // Check if the selector variable used exists
        if (!SELECTOR_VARIABLES.contains(selectorVariable))
            throw new ArgumentSyntaxException("Invalid selector variable", input, INVALID_SYNTAX);

        // Check if it should only select single entity and if the selector variable valid the condition
        if (onlySingleEntity && !SINGLE_ONLY_SELECTOR.contains(selectorVariable))
            throw new ArgumentSyntaxException("Argument requires only a single entity", input, ONLY_SINGLE_ENTITY_ERROR);

        // Check if it should only select players and if the selector variable valid the condition
        if (onlyPlayers && !PLAYERS_ONLY_SELECTOR.contains(selectorVariable))
            throw new ArgumentSyntaxException("Argument requires only players", input, ONLY_PLAYERS_ERROR);

        if (!onlyPlayers && REQUIRES_PLAYER_UPGRADED_TYPE.contains(selectorVariable) && !target.type().isAssignableFrom(Player.class))
            throw new ArgumentSyntaxException("Argument requires player types.", input, ONLY_PLAYERS_ERROR);

        return EntitySelector.selector(target,builder -> {
            appendTargetSelector(sender, builder, selectorVariable);
            // The selector is a single selector variable which verify all the conditions
            if (input.length() == 2)
                return;
            // START PARSING THE STRUCTURE
            final String structure = input.substring(2);
            parseStructure(sender, input, builder, structure);
        });
    }

    @Override
    public ArgumentParserType parser() {
        return ArgumentParserType.ENTITY;
    }

    @Override
    public byte @Nullable [] nodeProperties() {
        return NetworkBuffer.makeArray(buffer -> {
            byte mask = 0;
            if (this.isOnlySingleEntity()) {
                mask |= 0x01;
            }
            if (this.isOnlyPlayers()) {
                mask |= 0x02;
            }
            buffer.write(NetworkBuffer.BYTE, mask);
        });
    }

    private void parseStructure(@NotNull CommandSender sender,
                                       @NotNull String input,
                                       @NotNull EntitySelector.Builder<T> builder,
                                       @NotNull String structure) throws ArgumentSyntaxException {
        // The structure isn't opened or closed properly
        if (!structure.startsWith("[") || !structure.endsWith("]"))
            throw new ArgumentSyntaxException("Target selector needs to start and end with brackets", input, INVALID_SYNTAX);

        // Remove brackets
        final String structureData = structure.substring(1, structure.length() - 1);
        //System.out.println("structure data: " + structureData);

        String currentArgument = "";
        for (int i = 0; i < structureData.length(); i++) {
            final char c = structureData.charAt(i);
            if (c == '=') {

                // Replace all unnecessary spaces
                currentArgument = currentArgument.trim();

                if (!VALID_ARGUMENTS.contains(currentArgument))
                    throw new ArgumentSyntaxException("Argument name '" + currentArgument + "' does not exist", input, INVALID_ARGUMENT_NAME);

                i = parseArgument(sender, builder, currentArgument, input, structureData, i);
                currentArgument = ""; // Reset current argument
            } else {
                currentArgument += c;
            }
        }
    }

    private int parseArgument(@NotNull CommandSender sender,
                                     @NotNull EntitySelector.Builder<T> builder,
                                     @NotNull String argumentName,
                                     @NotNull String input,
                                     @NotNull String structureData, int beginIndex) throws ArgumentSyntaxException {
        final char comma = ',';
        final boolean isSimple = SIMPLE_ARGUMENTS.contains(argumentName);

        int finalIndex = beginIndex + 1;
        StringBuilder valueBuilder = new StringBuilder();
        for (; finalIndex < structureData.length(); finalIndex++) {
            final char c = structureData.charAt(finalIndex);

            // Command is parsed
            if (isSimple && c == comma)
                break;

            valueBuilder.append(c);
        }

        final String value = valueBuilder.toString().trim();

        var type = new Object2BooleanOpenHashMap<EntityType>();
        var gamemode = new Object2BooleanOpenHashMap<GameMode>();
        switch (argumentName) {
            case "type": {
                final boolean include = !value.startsWith("!");
                final String entityName = include ? value : value.substring(1);
                final EntityType entityType = EntityType.fromNamespaceId(entityName);
                if (entityType == null)
                    throw new ArgumentSyntaxException("Invalid entity name", input, INVALID_ARGUMENT_VALUE);
                type.put(entityType, include);
                break;
            }
            case "gamemode": {
                final boolean include = !value.startsWith("!");
                final String gameModeName = include ? value : value.substring(1);
                try {
                    final GameMode gameMode = GameMode.valueOf(gameModeName.toUpperCase());
                    gamemode.put(gameMode, include);
                } catch (IllegalArgumentException e) {
                    throw new ArgumentSyntaxException("Invalid entity game mode", input, INVALID_ARGUMENT_VALUE);
                }
                break;
            }
            case "limit":
                int limit;
                try {
                    limit = Integer.parseInt(value);
                    builder.limit(limit);
                } catch (NumberFormatException e) {
                    throw new ArgumentSyntaxException("Invalid limit number", input, INVALID_ARGUMENT_VALUE);
                }
                if (limit <= 0) {
                    throw new ArgumentSyntaxException("Limit must be positive", input, INVALID_ARGUMENT_VALUE);
                }
                break;
            case "sort":
                try {
                    final EntitySelector.Sort entitySort = EntitySelector.Sort.valueOf(value.toUpperCase());
                    builder.sort(entitySort);
                } catch (IllegalArgumentException e) {
                    throw new ArgumentSyntaxException("Invalid entity sort", input, INVALID_ARGUMENT_VALUE);
                }
                break;
            case "level":
                if (!isOnlyPlayers()) break;
                try {
                    final Range.Int level = Argument.parse(sender, new ArgumentIntRange(value));
                    asPlayerBuilder(builder).predicate(EntitySelectors.LEVEL, (point, integer) -> level.inRange(integer));
                } catch (ArgumentSyntaxException e) {
                    throw new ArgumentSyntaxException("Invalid level number", input, INVALID_ARGUMENT_VALUE);
                }
                break;
            case "distance":
                try {
                    final Range.Int distance = Argument.parse(sender, new ArgumentIntRange(value));
                    builder.predicate(EntitySelectors.POS, (point, pos) -> {
                        final double d = point.distance(pos);
                        System.out.println(point);
                        return distance.inRange((int) d);
                    });
                } catch (ArgumentSyntaxException e) {
                    throw new ArgumentSyntaxException("Invalid distance number", input, INVALID_ARGUMENT_VALUE);
                }
                break;
        }

        if (isOnlyPlayers() && !gamemode.isEmpty()) {
            asPlayerBuilder(builder).predicate(EntitySelectors.GAME_MODE, (point, currentGameMode) -> {
                var isSet = gamemode.containsKey(currentGameMode);
                var includes = gamemode.getBoolean(currentGameMode);
                return !isSet || includes;
            });
        }

        if (!type.isEmpty()) {
            final boolean hasInversion = type.containsValue(false);
            builder.predicate(EntitySelectors.TYPE, (point, entityType) -> {
                // Case 1: Inverted & Not set -> true
                if (hasInversion && !type.containsKey(entityType)) {
                    return true;
                }

                // Case 2: See the value.
                return type.getBoolean(entityType);
            });
        }

        return finalIndex;
    }

    public boolean isOnlySingleEntity() {
        return onlySingleEntity;
    }

    public boolean isOnlyPlayers() {
        return onlyPlayers;
    }

    @Override
    public String toString() {
        if (onlySingleEntity) {
            if (onlyPlayers) {
                return String.format("Player<%s>", getId());
            }
            return String.format("Entity<%s>", getId());
        } else {
            if (onlyPlayers) {
                return String.format("Players<%s>", getId());
            }
            return String.format("Entities<%s>", getId());
        }
    }

    private void appendTargetSelector(@NotNull CommandSender sender,
                                             EntitySelector.Builder<T> builder,
                                             @NotNull String selectorVariable) {
        switch (selectorVariable) {
            case "@p", "@n" -> {
                builder.sort(EntitySelector.Sort.NEAREST);
                builder.limit(1);
            }
            case "@r" -> {
                builder.sort(EntitySelector.Sort.RANDOM);
                builder.limit(1);
            }
            case "@a", "@e" -> {
                // Ignored as targets already use an all gatherer.
            }
            case "@s" -> builder.gather(EntitySelector.Gather.only((Entity) sender));
            default -> throw new IllegalArgumentException("Weird selector variable: " + selectorVariable);
        }
    }

    private EntitySelector.Builder<Player> asPlayerBuilder(EntitySelector.Builder<T> builder) {
        Check.argCondition(isOnlyPlayers(), "This argument is not player-only");
        // noinspection unchecked
        return (EntitySelector.Builder<Player>) builder;
    }
}

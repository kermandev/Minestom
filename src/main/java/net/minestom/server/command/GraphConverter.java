package net.minestom.server.command;

import net.minestom.server.command.builder.arguments.*;
import net.minestom.server.command.builder.arguments.minecraft.SuggestionType;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;

final class GraphConverter {
    private final CommandManager commandManager;
    private final @Nullable Player player;
    private final List<NodeBuilder> nodes = new ArrayList<>();
    private final List<BiConsumer<Graph, Integer>> redirects = new ArrayList<>();
    private final Map<Argument<?>, Integer> argToPacketId = new IdentityHashMap<>();

    private GraphConverter(CommandManager commandManager, @Nullable Player player) {
        this.commandManager = commandManager;
        this.player = player;
    }

    @Contract("_, _, _ -> new")
    public static DeclareCommandsPacket createPacket(CommandManager commandManager, Graph graph, @Nullable Player player) {
        GraphConverter converter = new GraphConverter(commandManager, player);
        List<Integer> rootIds = new ArrayList<>();
        converter.append(graph.root(), null, rootIds);
        final int rootId = rootIds.getFirst();
        for (var r : converter.redirects) {
            r.accept(graph, rootId);
        }
        List<DeclareCommandsPacket.Node> finalNodes = new ArrayList<>(converter.nodes.size());
        for (NodeBuilder node : converter.nodes) {
            finalNodes.add(node.build());
        }
        return new DeclareCommandsPacket(finalNodes, rootId);
    }

    private void append(Graph.Node graphNode, @Nullable List<NodeBuilder> redirectGroup, List<Integer> parentChildren) {
        if (!shouldInclude(graphNode)) return;

        final Argument<?> argument = graphNode.argument();
        final List<Graph.Node> children = graphNode.next();

        List<Integer> childIds = new ArrayList<>();
        for (Graph.Node child : children) {
            appendChild(child, redirectGroup, childIds);
        }

        boolean isExecutable = graphNode.execution() != null && graphNode.execution().executor() != null;

        switch (argument) {
            case ArgumentLiteral literal -> appendLiteral(literal, childIds, isExecutable, redirectGroup, parentChildren);
            case ArgumentCommand argCmd -> appendCommand(argCmd, childIds, isExecutable, parentChildren);
            case ArgumentEnum<?> argEnum -> appendEnum(argEnum.entries(), childIds, isExecutable, redirectGroup, parentChildren);
            case ArgumentWord word when word.hasRestrictions() -> {
                final String[] restrictions = word.getRestrictions();
                appendEnum(restrictions != null ? Arrays.asList(restrictions) : List.of(), childIds, isExecutable, redirectGroup, parentChildren);
            }
            case ArgumentGroup special -> appendGroup(special, childIds, redirectGroup, parentChildren);
            case ArgumentLoop<?> special -> appendLoop(special, parentChildren);
            default -> appendDefault(argument, childIds, isExecutable, redirectGroup, parentChildren);
        }
    }

    private boolean shouldInclude(Graph.Node node) {
        final Graph.Execution execution = node.execution();
        return player == null || execution == null || execution.test(player);
    }

    private void appendChild(Graph.Node child, @Nullable List<NodeBuilder> redirectGroup, List<Integer> childIds) {
        int start = childIds.size();
        append(child, redirectGroup, childIds);
        if (childIds.size() > start) {
            argToPacketId.put(child.argument(), childIds.get(start));
        }
    }

    private void appendLiteral(ArgumentLiteral literal, List<Integer> childIds, boolean isExecutable, @Nullable List<NodeBuilder> redirectGroup, List<Integer> parentChildren) {
        final NodeBuilder node = new NodeBuilder().children(childIds);
        if (literal.getId().isEmpty()) {
            node.flags(DeclareCommandsPacket.Node.IS_ROOT);
        } else {
            byte flags = DeclareCommandsPacket.Node.IS_LITERAL;
            if (isExecutable) flags |= DeclareCommandsPacket.Node.IS_EXECUTABLE;
            if (redirectGroup != null) {
                flags |= DeclareCommandsPacket.Node.HAS_REDIRECT;
                redirectGroup.add(node);
            }
            node.flags(flags).name(literal.getId());
        }
        node.addTo(nodes);
        parentChildren.add(node.id);
    }

    private void appendCommand(ArgumentCommand argCmd, List<Integer> childIds, boolean isExecutable, List<Integer> parentChildren) {
        byte flags = DeclareCommandsPacket.Node.IS_LITERAL;
        if (isExecutable) flags |= DeclareCommandsPacket.Node.IS_EXECUTABLE;
        flags |= DeclareCommandsPacket.Node.HAS_REDIRECT;

        final NodeBuilder node = new NodeBuilder()
                .children(childIds)
                .flags(flags)
                .name(argCmd.getId());
        final String shortcut = argCmd.getShortcut();
        if (shortcut.isEmpty()) {
            redirects.add((_, root) -> node.redirectedNode(root));
        } else {
            redirects.add(createShortcutRedirectHandler(node, shortcut));
        }
        node.addTo(nodes);
        parentChildren.add(node.id);
    }

    private BiConsumer<Graph, Integer> createShortcutRedirectHandler(NodeBuilder node, String shortcut) {
        return (graph, _) -> {
            var sender = player == null ? commandManager.getConsoleSender() : player;
            final List<Argument<?>> args = CommandParser.parser().parse(sender, graph, shortcut).args();
            final Argument<?> last = args.getLast();
            if (last.allowSpace()) {
                node.redirectedNode(argToPacketId.get(args.get(args.size() - 2)));
            } else {
                node.redirectedNode(argToPacketId.get(last));
            }
        };
    }

    private void appendEnum(Collection<String> entries, List<Integer> childIds, boolean isExecutable, @Nullable List<NodeBuilder> redirectGroup, List<Integer> parentChildren) {
        for (String entry : entries) {
            final NodeBuilder subNode = new NodeBuilder()
                    .children(childIds)
                    .name(entry);
            byte flags = DeclareCommandsPacket.Node.IS_LITERAL;
            if (isExecutable) flags |= DeclareCommandsPacket.Node.IS_EXECUTABLE;
            if (redirectGroup != null) {
                flags |= DeclareCommandsPacket.Node.HAS_REDIRECT;
                redirectGroup.add(subNode);
            }
            subNode.flags(flags).addTo(nodes);
            parentChildren.add(subNode.id);
        }
    }

    /**
     * Appends a sequence of argument group nodes. The group's elements are chained together
     * in sequence, with the first element linked as a child of the parent command node.
     */
    private void appendGroup(ArgumentGroup groupArg, List<Integer> childIds, @Nullable List<NodeBuilder> redirectGroup, List<Integer> parentChildren) {
        List<Argument<?>> entries = groupArg.group();
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Argument group must not be empty");
        }

        List<Integer> previousLayer = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            Argument<?> entry = entries.get(i);
            boolean isLast = (i == entries.size() - 1);
            
            List<Integer> currentLayer = new ArrayList<>();
            append(new GraphImpl.NodeImpl(entry, null, List.of()), isLast ? redirectGroup : null, currentLayer);

            if (i == 0) {
                parentChildren.addAll(currentLayer);
            }

            linkLayers(previousLayer, currentLayer);
            if (isLast) {
                linkLayers(currentLayer, childIds);
            } else {
                previousLayer = currentLayer;
            }
        }
    }

    private void linkLayers(List<Integer> from, List<Integer> to) {
        for (int id : from) {
            final NodeBuilder node = nodes.get(id);
            node.children.addAll(to);
        }
    }

    /**
     * Appends a loop argument, recursively setting up the loop elements so that their
     * redirect targets point to the target ID of the loop's post-completion step.
     */
    private void appendLoop(ArgumentLoop<?> loopArg, List<Integer> parentChildren) {
        List<NodeBuilder> redirectNodes = new ArrayList<>();
        for (Argument<?> arg : loopArg.arguments()) {
            append(new GraphImpl.NodeImpl(arg, null, List.of()), redirectNodes, parentChildren);
        }
        int targetId = nodes.size();
        for (NodeBuilder n : redirectNodes) {
            n.redirectedNode(targetId);
        }
    }

    private void appendDefault(Argument<?> argument, List<Integer> childIds, boolean isExecutable, @Nullable List<NodeBuilder> redirectGroup, List<Integer> parentChildren) {
        final boolean hasSuggestion = argument.hasSuggestion();
        final NodeBuilder node = new NodeBuilder()
                .children(childIds)
                .name(argument.getId())
                .parser(argument.parser())
                .properties(argument.nodeProperties());
        byte flags = DeclareCommandsPacket.Node.IS_ARGUMENT;
        if (isExecutable) flags |= DeclareCommandsPacket.Node.IS_EXECUTABLE;
        if (hasSuggestion) flags |= DeclareCommandsPacket.Node.HAS_SUGGESTION_TYPE;
        if (redirectGroup != null) {
            flags |= DeclareCommandsPacket.Node.HAS_REDIRECT;
            redirectGroup.add(node);
        }
        node.flags(flags);
        if (hasSuggestion) {
            final SuggestionType suggestionType = argument.suggestionType();
            Objects.requireNonNull(suggestionType, "suggestionType");
            node.suggestionsType(suggestionType.getIdentifier());
        }
        node.addTo(nodes);
        parentChildren.add(node.id);
    }

    private static final class NodeBuilder {
        int id;
        byte flags;
        final List<Integer> children = new ArrayList<>();
        int redirectedNode;
        @Nullable String name;
        @Nullable ArgumentParserType parser;
        byte @Nullable [] properties;
        @Nullable String suggestionsType;

        NodeBuilder flags(int flags) {
            this.flags = (byte) flags;
            return this;
        }

        NodeBuilder children(Collection<Integer> children) {
            this.children.addAll(children);
            return this;
        }

        NodeBuilder redirectedNode(int redirectedNode) {
            this.redirectedNode = redirectedNode;
            return this;
        }

        NodeBuilder name(@Nullable String name) {
            this.name = name;
            return this;
        }

        NodeBuilder parser(@Nullable ArgumentParserType parser) {
            this.parser = parser;
            return this;
        }

        NodeBuilder properties(byte @Nullable [] properties) {
            this.properties = properties;
            return this;
        }

        NodeBuilder suggestionsType(@Nullable String suggestionsType) {
            this.suggestionsType = suggestionsType;
            return this;
        }

        NodeBuilder addTo(List<NodeBuilder> to) {
            this.id = to.size();
            to.add(this);
            return this;
        }

        DeclareCommandsPacket.Node build() {
            return new DeclareCommandsPacket.Node(
                flags, 
                children.stream().mapToInt(Integer::intValue).toArray(), 
                redirectedNode, 
                name, 
                parser, 
                properties, 
                suggestionsType
            );
        }
    }
}

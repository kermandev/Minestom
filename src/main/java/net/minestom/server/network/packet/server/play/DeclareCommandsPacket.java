package net.minestom.server.network.packet.server.play;

import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.server.ServerPacket;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.minestom.server.network.NetworkBuffer.*;

public record DeclareCommandsPacket(List<Node> nodes,
                                    int rootIndex) implements ServerPacket.Play {
    public static final int MAX_NODES = Short.MAX_VALUE;
    public static final NetworkBuffer.Type<DeclareCommandsPacket> SERIALIZER = NetworkBufferTemplate.template(
            Node.SERIALIZER.list(MAX_NODES), DeclareCommandsPacket::nodes,
            VAR_INT, DeclareCommandsPacket::rootIndex,
            DeclareCommandsPacket::new
    );

    public DeclareCommandsPacket {
        nodes = List.copyOf(nodes);
    }

    /**
     * Represents a command node
     *
     * @see <a href="https://minecraft.wiki/w/Java_Edition_protocol/Command_data">Command Data</a>
     * @param flags           the flags see above
     * @param children        the children indexes, from the node list
     * @param redirectedNode  Only if flags 0x08
     * @param name            Only for literal and argument
     * @param parser          Only for argument
     * @param properties      Only for argument
     * @param suggestionsType Only if flags 0x10
     */
    public record Node(@MagicConstant(flagsFromClass = Node.class) byte flags, int[] children, int redirectedNode, @Nullable String name,
                       @Nullable ArgumentParserType parser, byte @Nullable [] properties,
                       @Nullable String suggestionsType) {
        public static final int IS_ROOT = 0x00;
        public static final int IS_LITERAL = 0x01;
        public static final int IS_ARGUMENT = 0x02;
        public static final int IS_EXECUTABLE = 0x04;
        public static final int HAS_REDIRECT = 0x08;
        public static final int HAS_SUGGESTION_TYPE = 0x10;
        public static final int IS_RESTRICTED = 0x20;
        public static final int MAX_CHILDREN = 262114;

        // The writing/reading impl is pretty gross, Sorry!
        public static final Type<Node> SERIALIZER = new Type<>() {
            @Override
            public void write(NetworkBuffer writer, Node value) {
                writer.write(BYTE, value.flags);

                if (value.children.length > MAX_CHILDREN) {
                    throw new RuntimeException("Children length " + value.children.length + " is bigger than the maximum allowed " + MAX_CHILDREN);
                }
                writer.write(VAR_INT_ARRAY, value.children);

                if ((value.flags & HAS_REDIRECT) != 0) {
                    writer.write(VAR_INT, value.redirectedNode);
                }

                if ((value.flags & IS_LITERAL) != 0 || (value.flags & IS_ARGUMENT) != 0) {
                    writer.write(STRING, value.name);
                }

                if ((value.flags & IS_ARGUMENT) != 0) {
                    writer.write(ArgumentParserType.NETWORK_TYPE, value.parser);
                    if (value.properties != null) {
                        writer.write(RAW_BYTES, value.properties);
                    }
                }

                if ((value.flags & HAS_SUGGESTION_TYPE) != 0) {
                    writer.write(STRING, value.suggestionsType);
                }
            }

            public Node read(NetworkBuffer reader) {
                byte flags = reader.read(BYTE);
                int[] children = reader.read(VAR_INT_ARRAY);
                int redirectedNode = 0;
                if ((flags & HAS_REDIRECT) != 0) {
                    redirectedNode = reader.read(VAR_INT);
                }

                String name = "";
                if ((flags & IS_LITERAL) != 0 || (flags & IS_ARGUMENT) != 0) {
                    name = reader.read(STRING);
                }

                ArgumentParserType parser = null;
                byte[] properties = null;
                if ((flags & IS_ARGUMENT) != 0) {
                    parser = reader.read(ArgumentParserType.NETWORK_TYPE);
                    properties = getProperties(reader, parser);
                }

                String suggestionsType = null;
                if ((flags & HAS_SUGGESTION_TYPE) != 0) {
                    suggestionsType = reader.read(STRING);
                }

                return new Node(flags, children, redirectedNode, name, parser, properties, suggestionsType);
            }
        };

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Node(
                    byte flags1, int[] children1, int redirectedNode1, String name1, ArgumentParserType parser1,
                    byte[] properties1, String type
            ))) return false;
            return flags() == flags1 && redirectedNode() == redirectedNode1 && Objects.equals(name(), name1) && Arrays.equals(children(), children1) && Objects.equals(suggestionsType(), type) && parser() == parser1 && Arrays.equals(properties(), properties1);
        }

        @Override
        public int hashCode() {
            int result = flags();
            result = 31 * result + Arrays.hashCode(children());
            result = 31 * result + redirectedNode();
            result = 31 * result + Objects.hashCode(name());
            result = 31 * result + Objects.hashCode(parser());
            result = 31 * result + Arrays.hashCode(properties());
            result = 31 * result + Objects.hashCode(suggestionsType());
            return result;
        }
    }

    private static byte[] getProperties(NetworkBuffer reader, ArgumentParserType parser) {
        final Function<Consumer<NetworkBuffer>, byte[]> minMaxExtractor = (via) -> reader.extractBytes((extractor) -> {
            byte flags = extractor.read(BYTE);
            if ((flags & 0x01) == 0x01) {
                via.accept(extractor); // min
            }
            if ((flags & 0x02) == 0x02) {
                via.accept(extractor); // max
            }
        });
        return switch (parser) {
            case DOUBLE -> minMaxExtractor.apply(b -> b.read(DOUBLE));
            case INTEGER -> minMaxExtractor.apply(b -> b.read(INT));
            case FLOAT -> minMaxExtractor.apply(b -> b.read(FLOAT));
            case LONG -> minMaxExtractor.apply(b -> b.read(LONG));
            case STRING -> reader.extractBytes(b -> b.read(VAR_INT));
            case ENTITY, SCORE_HOLDER -> reader.extractBytes(b -> b.read(BYTE));
            case TIME -> reader.extractBytes(b -> b.read(INT));
            case RESOURCE_OR_TAG, RESOURCE_OR_TAG_KEY, RESOURCE, RESOURCE_KEY, RESOURCE_SELECTOR -> reader.extractBytes(b -> b.read(STRING));
            default -> new byte[0]; // unknown
        };
    }

}

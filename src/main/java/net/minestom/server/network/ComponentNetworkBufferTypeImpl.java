package net.minestom.server.network;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.*;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import net.kyori.adventure.text.object.SpriteObjectContents;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.adventure.serializer.nbt.NbtDataComponentValue;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.dialog.Dialog;
import net.minestom.server.registry.Registries;
import net.minestom.server.registry.RegistryTranscoder;
import net.minestom.server.utils.nbt.BinaryTagWriter;
import net.minestom.server.utils.validate.Check;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static net.minestom.server.network.NetworkBuffer.*;

record ComponentNetworkBufferTypeImpl() implements NetworkBuffer.Type<Component, NetworkContext> {

    @Override
    public void write(NetworkBuffer buffer, Component value, NetworkContext context) {
        Check.notNull(value, "Component cannot be null");

        buffer.write(BYTE, TAG_COMPOUND, context);
        writeInnerComponent(buffer, value, context);
    }

    @Override
    public Component read(NetworkBuffer buffer, NetworkContext context) {
        final Transcoder<BinaryTag> coder;
        if (context instanceof Registries.Provider provider) {
            coder = new RegistryTranscoder<>(Transcoder.NBT, provider.registries());
        } else {
            coder = Transcoder.NBT;
        }
        final BinaryTag tag = buffer.read(NBT, context);
        return Codec.COMPONENT.decode(coder, tag).orElseThrow();
    }

    // WRITING IMPL, pretty gross. Would not recommend reading.

    private static final byte TAG_END = 0;
    private static final byte TAG_BYTE = 1;
    private static final byte TAG_INT = 3;
    private static final byte TAG_STRING = 8;
    private static final byte TAG_LIST = 9;
    private static final byte TAG_COMPOUND = 10;
    private static final byte TAG_INT_ARRAY = 11;

    private void writeInnerComponent(NetworkBuffer buffer, Component component, NetworkContext context) {
        buffer.write(BYTE, TAG_STRING, context); // Start first tag (always the type)
        buffer.write(STRING_IO_UTF8, "type", context);
        switch (component) {
            case TextComponent text -> {
                buffer.write(STRING_IO_UTF8, "text", context);

                buffer.write(BYTE, TAG_STRING, context); // Start "text" tag
                buffer.write(STRING_IO_UTF8, "text", context);
                buffer.write(STRING_IO_UTF8, text.content(), context);
            }
            case TranslatableComponent translatable -> {
                buffer.write(STRING_IO_UTF8, "translatable", context);

                buffer.write(BYTE, TAG_STRING, context); // Start "translate" tag
                buffer.write(STRING_IO_UTF8, "translate", context);
                buffer.write(STRING_IO_UTF8, translatable.key(), context);

                final String fallback = translatable.fallback();
                if (fallback != null) {
                    buffer.write(BYTE, TAG_STRING, context);
                    buffer.write(STRING_IO_UTF8, "fallback", context);
                    buffer.write(STRING_IO_UTF8, fallback, context);
                }

                final List<TranslationArgument> args = translatable.arguments();
                if (!args.isEmpty()) {
                    buffer.write(BYTE, TAG_LIST, context);
                    buffer.write(STRING_IO_UTF8, "with", context);
                    buffer.write(BYTE, TAG_COMPOUND, context); // List type
                    buffer.write(INT, args.size(), context);
                    for (final TranslationArgument arg : args)
                        writeInnerComponent(buffer, arg.asComponent(), context);
                }
            }
            case ScoreComponent score -> {
                buffer.write(STRING_IO_UTF8, "score", context);

                buffer.write(BYTE, TAG_COMPOUND, context); // Start "score" tag
                buffer.write(STRING_IO_UTF8, "score", context);
                {
                    buffer.write(BYTE, TAG_STRING, context);
                    buffer.write(STRING_IO_UTF8, "name", context);
                    buffer.write(STRING_IO_UTF8, score.name(), context);

                    buffer.write(BYTE, TAG_STRING, context);
                    buffer.write(STRING_IO_UTF8, "objective", context);
                    buffer.write(STRING_IO_UTF8, score.objective(), context);
                }
                buffer.write(BYTE, TAG_END, context); // End "score" tag

            }
            case SelectorComponent selector -> {
                buffer.write(STRING_IO_UTF8, "selector", context);

                buffer.write(BYTE, TAG_STRING, context);
                buffer.write(STRING_IO_UTF8, "selector", context);
                buffer.write(STRING_IO_UTF8, selector.pattern(), context);

                final Component separator = selector.separator();
                if (separator != null) {
                    buffer.write(BYTE, TAG_COMPOUND, context);
                    buffer.write(STRING_IO_UTF8, "separator", context);
                    writeInnerComponent(buffer, separator, context);
                }
            }
            case KeybindComponent keybind -> {
                buffer.write(STRING_IO_UTF8, "keybind", context);

                buffer.write(BYTE, TAG_STRING, context);
                buffer.write(STRING_IO_UTF8, "keybind", context);
                buffer.write(STRING_IO_UTF8, keybind.keybind(), context);
            }
            case NBTComponent<?, ?> nbt -> {
                //todo
                throw new UnsupportedOperationException("NBTComponent is not implemented yet");
            }
            case ObjectComponent object -> {
                buffer.write(STRING_IO_UTF8, "object", context);

                switch (object.contents()) {
                    case SpriteObjectContents sprite -> {
                        if (!sprite.atlas().equals(SpriteObjectContents.DEFAULT_ATLAS)) {
                            buffer.write(BYTE, TAG_STRING, context);
                            buffer.write(STRING_IO_UTF8, "atlas", context);
                            buffer.write(STRING_IO_UTF8, sprite.atlas().asMinimalString(), context);
                        }

                        buffer.write(BYTE, TAG_STRING, context);
                        buffer.write(STRING_IO_UTF8, "sprite", context);
                        buffer.write(STRING_IO_UTF8, sprite.sprite().asMinimalString(), context);
                    }
                    case PlayerHeadObjectContents player -> {
                        buffer.write(BYTE, TAG_COMPOUND, context); // Start "player" tag
                        buffer.write(STRING_IO_UTF8, "player", context);
                        {
                            final String name = player.name();
                            if (name != null) {
                                buffer.write(BYTE, TAG_STRING, context);
                                buffer.write(STRING_IO_UTF8, "name", context);
                                buffer.write(STRING_IO_UTF8, name, context);
                            }

                            final UUID id = player.id();
                            if (id != null) {
                                buffer.write(BYTE, TAG_INT_ARRAY, context);
                                buffer.write(STRING_IO_UTF8, "id", context);
                                buffer.write(INT, 4, context);

                                final long uuidMost = id.getMostSignificantBits();
                                final long uuidLeast = id.getLeastSignificantBits();
                                buffer.write(INT, (int) (uuidMost >> 32), context);
                                buffer.write(INT, (int) uuidMost, context);
                                buffer.write(INT, (int) (uuidLeast >> 32), context);
                                buffer.write(INT, (int) uuidLeast, context);
                            }

                            int propertyCount = player.profileProperties().size();
                            if (propertyCount > 0) {
                                buffer.write(BYTE, TAG_LIST, context);
                                buffer.write(STRING_IO_UTF8, "properties", context);
                                buffer.write(BYTE, TAG_COMPOUND, context); // List type
                                buffer.write(INT, propertyCount, context);

                                for (PlayerHeadObjectContents.ProfileProperty property : player.profileProperties()) {
                                    buffer.write(BYTE, TAG_STRING, context);
                                    buffer.write(STRING_IO_UTF8, "name", context);
                                    buffer.write(STRING_IO_UTF8, property.name(), context);

                                    buffer.write(BYTE, TAG_STRING, context);
                                    buffer.write(STRING_IO_UTF8, "value", context);
                                    buffer.write(STRING_IO_UTF8, property.value(), context);

                                    final String signature = property.signature();
                                    if (signature != null) {
                                        buffer.write(BYTE, TAG_STRING, context);
                                        buffer.write(STRING_IO_UTF8, "signature", context);
                                        buffer.write(STRING_IO_UTF8, signature, context);
                                    }

                                    buffer.write(BYTE, TAG_END, context); // End property object
                                }
                            }

                            final Key texture = player.texture();
                            if (texture != null) {
                                buffer.write(BYTE, TAG_STRING, context);
                                buffer.write(STRING_IO_UTF8, "body", context);
                                buffer.write(STRING_IO_UTF8, texture.asMinimalString(), context);
                            }
                        }
                        buffer.write(BYTE, TAG_END, context); // End "player" tag

                        if (!player.hat()) {
                            buffer.write(BYTE, TAG_BYTE, context);
                            buffer.write(STRING_IO_UTF8, "hat", context);
                            buffer.write(BYTE, (byte) 0, context);
                        }
                    }
                    default -> throw new UnsupportedOperationException("Unknown object contents: " + object.contents());
                }
            }
            default -> throw new UnsupportedOperationException("Unsupported component type: " + component.getClass());
        }

        // Children
        if (!component.children().isEmpty()) {
            buffer.write(BYTE, TAG_LIST, context);
            buffer.write(STRING_IO_UTF8, "extra", context);
            buffer.write(BYTE, TAG_COMPOUND, context); // List type

            buffer.write(INT, component.children().size(), context);
            for (final Component child : component.children())
                writeInnerComponent(buffer, child, context);
        }

        // Formatting/Interactivity
        writeComponentStyle(buffer, component.style(), context);

        buffer.write(BYTE, TAG_END, context);
    }

    private void writeComponentStyle(NetworkBuffer buffer, Style style, NetworkContext context) {
        final TextColor color = style.color();
        if (color != null) {
            buffer.write(BYTE, TAG_STRING, context);
            buffer.write(STRING_IO_UTF8, "color", context);
            if (color instanceof NamedTextColor namedColor)
                buffer.write(STRING_IO_UTF8, namedColor.toString(), context);
            else buffer.write(STRING_IO_UTF8, color.asHexString(), context);
        }

        final ShadowColor shadowColor = style.shadowColor();
        if (shadowColor != null) {
            buffer.write(BYTE, TAG_INT, context);
            buffer.write(STRING_IO_UTF8, "shadow_color", context);
            buffer.write(INT, shadowColor.value(), context);
        }

        final Key font = style.font();
        if (font != null) {
            buffer.write(BYTE, TAG_STRING, context);
            buffer.write(STRING_IO_UTF8, "font", context);
            buffer.write(STRING_IO_UTF8, font.asString(), context);
        }

        final TextDecoration.State bold = style.decoration(TextDecoration.BOLD);
        if (bold != TextDecoration.State.NOT_SET) {
            buffer.write(BYTE, TAG_BYTE, context);
            buffer.write(STRING_IO_UTF8, "bold", context);
            buffer.write(BYTE, bold == TextDecoration.State.TRUE ? (byte) 1 : (byte) 0, context);
        }

        final TextDecoration.State italic = style.decoration(TextDecoration.ITALIC);
        if (italic != TextDecoration.State.NOT_SET) {
            buffer.write(BYTE, TAG_BYTE, context);
            buffer.write(STRING_IO_UTF8, "italic", context);
            buffer.write(BYTE, italic == TextDecoration.State.TRUE ? (byte) 1 : (byte) 0, context);
        }

        final TextDecoration.State underlined = style.decoration(TextDecoration.UNDERLINED);
        if (underlined != TextDecoration.State.NOT_SET) {
            buffer.write(BYTE, TAG_BYTE, context);
            buffer.write(STRING_IO_UTF8, "underlined", context);
            buffer.write(BYTE, underlined == TextDecoration.State.TRUE ? (byte) 1 : (byte) 0, context);
        }

        final TextDecoration.State strikethrough = style.decoration(TextDecoration.STRIKETHROUGH);
        if (strikethrough != TextDecoration.State.NOT_SET) {
            buffer.write(BYTE, TAG_BYTE, context);
            buffer.write(STRING_IO_UTF8, "strikethrough", context);
            buffer.write(BYTE, strikethrough == TextDecoration.State.TRUE ? (byte) 1 : (byte) 0, context);
        }

        final TextDecoration.State obfuscated = style.decoration(TextDecoration.OBFUSCATED);
        if (obfuscated != TextDecoration.State.NOT_SET) {
            buffer.write(BYTE, TAG_BYTE, context);
            buffer.write(STRING_IO_UTF8, "obfuscated", context);
            buffer.write(BYTE, obfuscated == TextDecoration.State.TRUE ? (byte) 1 : (byte) 0, context);
        }

        final String insertion = style.insertion();
        if (insertion != null) {
            buffer.write(BYTE, TAG_STRING, context);
            buffer.write(STRING_IO_UTF8, "insertion", context);
            buffer.write(STRING_IO_UTF8, insertion, context);
        }

        final ClickEvent clickEvent = style.clickEvent();
        if (clickEvent != null) writeClickEvent(buffer, clickEvent, context);

        final HoverEvent<?> hoverEvent = style.hoverEvent();
        if (hoverEvent != null) writeHoverEvent(buffer, hoverEvent, context);
    }

    private void writeClickEvent(NetworkBuffer buffer, ClickEvent clickEvent, NetworkContext context) {
        buffer.write(BYTE, TAG_COMPOUND, context);
        buffer.write(STRING_IO_UTF8, "click_event", context);

        buffer.write(BYTE, TAG_STRING, context);
        buffer.write(STRING_IO_UTF8, "action", context);
        buffer.write(STRING_IO_UTF8, clickEvent.action().name().toLowerCase(Locale.ROOT), context);

        switch (clickEvent.action()) {
            case OPEN_URL -> {
                final ClickEvent.Payload.Text payload = checkPayload(clickEvent, ClickEvent.Payload.Text.class);
                buffer.write(BYTE, TAG_STRING, context);
                buffer.write(STRING_IO_UTF8, "url", context);
                buffer.write(STRING_IO_UTF8, payload.value(), context);
            }
            case OPEN_FILE -> {
                final ClickEvent.Payload.Text payload = checkPayload(clickEvent, ClickEvent.Payload.Text.class);
                buffer.write(BYTE, TAG_STRING, context);
                buffer.write(STRING_IO_UTF8, "path", context);
                buffer.write(STRING_IO_UTF8, payload.value(), context);
            }
            case RUN_COMMAND, SUGGEST_COMMAND -> {
                final ClickEvent.Payload.Text payload = checkPayload(clickEvent, ClickEvent.Payload.Text.class);
                buffer.write(BYTE, TAG_STRING, context);
                buffer.write(STRING_IO_UTF8, "command", context);
                buffer.write(STRING_IO_UTF8, payload.value(), context);
            }
            case CHANGE_PAGE -> {
                final ClickEvent.Payload.Int payload = checkPayload(clickEvent, ClickEvent.Payload.Int.class);
                buffer.write(BYTE, TAG_INT, context);
                buffer.write(STRING_IO_UTF8, "page", context);
                buffer.write(INT, payload.integer(), context);
            }
            case COPY_TO_CLIPBOARD -> {
                final ClickEvent.Payload.Text payload = checkPayload(clickEvent, ClickEvent.Payload.Text.class);
                buffer.write(BYTE, TAG_STRING, context);
                buffer.write(STRING_IO_UTF8, "value", context);
                buffer.write(STRING_IO_UTF8, payload.value(), context);
            }
            case SHOW_DIALOG -> {
                final ClickEvent.Payload.Dialog payload = checkPayload(clickEvent, ClickEvent.Payload.Dialog.class);

                try {
                    final Transcoder<BinaryTag> coder;
                    if (context instanceof Registries.Provider provider) {
                        coder = new RegistryTranscoder<>(Transcoder.NBT, provider.registries());
                    } else {
                        coder = Transcoder.NBT;
                    }
                    final BinaryTag dialog = Dialog.CODEC.encode(coder, Dialog.unwrap(payload.dialog())).orElseThrow();

                    final BinaryTagWriter nbtWriter = new BinaryTagWriter(buffer.ioView());
                    nbtWriter.writeNamed("dialog", dialog);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to write dialog click event payload", e);
                }
            }
            case CUSTOM -> {
                final ClickEvent.Payload.Custom payload = checkPayload(clickEvent, ClickEvent.Payload.Custom.class);
                buffer.write(BYTE, TAG_STRING, context);
                buffer.write(STRING_IO_UTF8, "id", context);
                buffer.write(STRING_IO_UTF8, payload.key().asString(), context);

                try {
                    final BinaryTagWriter nbtWriter = new BinaryTagWriter(buffer.ioView());
                    nbtWriter.writeNamed("payload", MinestomAdventure.unwrapNbt(payload.nbt()));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to write custom click event payload", e);
                }
            }
            default -> throw new UnsupportedOperationException("Unknown click event action: " + clickEvent.action());
        }

        buffer.write(BYTE, TAG_END, context);
    }

    private <T extends ClickEvent.Payload> T checkPayload(ClickEvent clickEvent, Class<T> expected) {
        final ClickEvent.Payload payload = clickEvent.payload();
        if (!expected.isInstance(payload))
            throw new IllegalArgumentException(
                    "Expected " + expected.getSimpleName() + " for " + clickEvent.action() + ", got: " + payload.getClass());
        return expected.cast(payload);
    }

    @SuppressWarnings("unchecked")
    private void writeHoverEvent(NetworkBuffer buffer, HoverEvent<?> hoverEvent, NetworkContext context) {
        buffer.write(BYTE, TAG_COMPOUND, context);
        buffer.write(STRING_IO_UTF8, "hover_event", context);

        buffer.write(BYTE, TAG_STRING, context);
        buffer.write(STRING_IO_UTF8, "action", context);
        buffer.write(STRING_IO_UTF8, hoverEvent.action().toString().toLowerCase(Locale.ROOT), context);

        if (hoverEvent.action() == HoverEvent.Action.SHOW_TEXT) {
            buffer.write(BYTE, TAG_COMPOUND, context);
            buffer.write(STRING_IO_UTF8, "value", context);
            writeInnerComponent(buffer, (Component) hoverEvent.value(), context);
        } else if (hoverEvent.action() == HoverEvent.Action.SHOW_ITEM) {
            var value = ((HoverEvent<HoverEvent.ShowItem>) hoverEvent).value();

            buffer.write(BYTE, TAG_STRING, context);
            buffer.write(STRING_IO_UTF8, "id", context);
            buffer.write(STRING_IO_UTF8, value.item().asString(), context);

            buffer.write(BYTE, TAG_INT, context);
            buffer.write(STRING_IO_UTF8, "count", context);
            buffer.write(INT, value.count(), context);

            buffer.write(BYTE, TAG_COMPOUND, context);
            buffer.write(STRING_IO_UTF8, "components", context);
            final Map<Key, NbtDataComponentValue> dataComponents = value.dataComponentsAs(NbtDataComponentValue.class);
            if (!dataComponents.isEmpty()) {
                final BinaryTagWriter nbtWriter = new BinaryTagWriter(buffer.ioView());
                try {
                    for (final Map.Entry<Key, NbtDataComponentValue> entry : dataComponents.entrySet()) {
                        final BinaryTag dataComponentValue = entry.getValue().value();
                        if (dataComponentValue == null) {
                            buffer.write(BYTE, TAG_COMPOUND, context);
                            buffer.write(STRING_IO_UTF8, "!" + entry.getKey().asString(), context);
                            buffer.write(BYTE, TAG_END, context);
                        } else {
                            nbtWriter.writeNamed(entry.getKey().asString(), dataComponentValue);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            buffer.write(BYTE, TAG_END, context);
        } else if (hoverEvent.action() == HoverEvent.Action.SHOW_ENTITY) {
            var value = ((HoverEvent<HoverEvent.ShowEntity>) hoverEvent).value();

            final Component name = value.name();
            if (name != null) {
                buffer.write(BYTE, TAG_COMPOUND, context);
                buffer.write(STRING_IO_UTF8, "name", context);
                writeInnerComponent(buffer, name, context);
            }

            buffer.write(BYTE, TAG_STRING, context);
            buffer.write(STRING_IO_UTF8, "id", context);
            buffer.write(STRING_IO_UTF8, value.type().asString(), context);

            buffer.write(BYTE, TAG_STRING, context);
            buffer.write(STRING_IO_UTF8, "uuid", context);
            buffer.write(STRING_IO_UTF8, value.id().toString(), context);
        } else {
            throw new UnsupportedOperationException("Unknown hover event action: " + hoverEvent.action());
        }

        buffer.write(BYTE, TAG_END, context);
    }
}

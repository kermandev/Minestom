package net.minestom.server.codec.stream;

final class StreamCodecImpl {
    private StreamCodecImpl() {
    }

    record PrimitiveImpl<T>(StreamEncoder<T> encoder, StreamDecoder<T> decoder) implements StreamCodec<T> {
        @Override
        public T decode(StreamReader stream) throws RuntimeException {
            return decoder.decode(stream);
        }

        @Override
        public void encode(StreamWriter stream, T value) throws RuntimeException {
            encoder.encode(stream, value);
        }
    }
}

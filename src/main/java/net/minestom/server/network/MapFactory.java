package net.minestom.server.network;

public interface MapFactory<K, V, M> {
    Object create(int size);

    void put(Object map, K key, V value);

    M finish(Object map);
}

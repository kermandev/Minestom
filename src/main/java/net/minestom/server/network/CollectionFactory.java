package net.minestom.server.network;

public interface CollectionFactory<E, C> {
    Object create(int size);

    void add(Object collection, E value);

    C finish(Object collection);
}

package net.minestom.demo.entity;

import net.minestom.server.entity.ItemEntity;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class TestItemEntity extends ItemEntity {
    public TestItemEntity(@NotNull ItemStack itemStack) {
        super(itemStack);
    }
}

package com.ming.victus.item;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BlankAspectItem extends Item {
    public BlankAspectItem() {
        super(new Properties());
    }

    @Override
    @SuppressWarnings("null")
    public boolean onEntityItemUpdate(@javax.annotation.Nonnull ItemStack stack, @javax.annotation.Nonnull ItemEntity entity) {
        if (!entity.isOnFire()) return false;

        // 按堆叠数量1:1转换空白之心为虚空之心
        int count = stack.getCount();
        for (int i = 0; i < count; i++) {
            entity.spawnAtLocation(new ItemStack(VictusItems.VOID_HEART_ASPECT.get()));
        }
        entity.discard();
        return true;
    }
}

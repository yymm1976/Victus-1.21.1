package com.ming.victus.components;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class VictusComponents {
    @SuppressWarnings("null")
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, com.ming.victus.VictusMain.MOD_ID);

    @SuppressWarnings("null")
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HeartData>> HEART_DATA =
        COMPONENTS.register("heart_data", () ->
            DataComponentType.<HeartData>builder()
                .persistent(HeartData.CODEC)
                .networkSynchronized(HeartData.STREAM_CODEC)
                .build()
        );
}

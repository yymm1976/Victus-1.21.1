package com.ming.victus.entity;

import com.ming.victus.VictusMain;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class VictusEntityTypes {
    @SuppressWarnings("null")
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, VictusMain.MOD_ID);

    @SuppressWarnings("null")
    public static final DeferredHolder<EntityType<?>, EntityType<DragonBreathCloud>> DRAGON_BREATH_CLOUD =
        ENTITY_TYPES.register("dragon_breath_cloud", () ->
            EntityType.Builder.<DragonBreathCloud>of(DragonBreathCloud::new, MobCategory.MISC)
                .sized(6.0F, 0.5F)
                .clientTrackingRange(10)
                .updateInterval(10)
                .build(VictusMain.MOD_ID + ":dragon_breath_cloud")
        );
}

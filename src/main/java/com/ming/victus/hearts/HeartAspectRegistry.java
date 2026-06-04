package com.ming.victus.hearts;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public class HeartAspectRegistry {
    private static final Map<ResourceLocation, HeartAspect.Type> REGISTRY = new HashMap<>();

    public static void registerDefaults() {
        register(com.ming.victus.hearts.content.GrilledAspect.TYPE);
        register(com.ming.victus.hearts.content.BundleAspect.TYPE);
        register(com.ming.victus.hearts.content.CreeperAspect.TYPE);
        register(com.ming.victus.hearts.content.DiamondAspect.TYPE);
        register(com.ming.victus.hearts.content.LightAspect.TYPE);
        register(com.ming.victus.hearts.content.OceanAspect.TYPE);
        register(com.ming.victus.hearts.content.TotemAspect.TYPE);
        register(com.ming.victus.hearts.content.PotionAspect.TYPE);
        register(com.ming.victus.hearts.content.ArcheryAspect.TYPE);
        register(com.ming.victus.hearts.content.BlazingAspect.TYPE);
        register(com.ming.victus.hearts.content.DraconicAspect.TYPE);
        register(com.ming.victus.hearts.content.EmeraldAspect.TYPE);
        register(com.ming.victus.hearts.content.EvokingAspect.TYPE);
        register(com.ming.victus.hearts.content.GoldenAspect.TYPE);
        register(com.ming.victus.hearts.content.IcyAspect.TYPE);
        register(com.ming.victus.hearts.content.IronAspect.TYPE);
        register(com.ming.victus.hearts.content.LapisAspect.TYPE);
        register(com.ming.victus.hearts.content.SweetAspect.TYPE);
        register(com.ming.victus.hearts.content.CheeseAspect.TYPE);
    }

    public static void register(HeartAspect.Type type) {
        if (REGISTRY.containsKey(type.id())) throw new IllegalArgumentException("Tried to register " + type.id() + " twice!");
        REGISTRY.put(type.id(), type);
    }

    public static HeartAspect forId(ResourceLocation id, Player holder) {
        if (!REGISTRY.containsKey(id)) return null;
        return REGISTRY.get(id).factory().apply(holder);
    }
}

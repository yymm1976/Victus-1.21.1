package com.ming.victus.hearts;

import com.ming.victus.hearts.content.ArcheryAspect;
import com.ming.victus.hearts.content.BlazingAspect;
import com.ming.victus.hearts.content.BundleAspect;
import com.ming.victus.hearts.content.CheeseAspect;
import com.ming.victus.hearts.content.CreeperAspect;
import com.ming.victus.hearts.content.DiamondAspect;
import com.ming.victus.hearts.content.DraconicAspect;
import com.ming.victus.hearts.content.EmeraldAspect;
import com.ming.victus.hearts.content.EvokingAspect;
import com.ming.victus.hearts.content.GoldenAspect;
import com.ming.victus.hearts.content.GrilledAspect;
import com.ming.victus.hearts.content.IcyAspect;
import com.ming.victus.hearts.content.IronAspect;
import com.ming.victus.hearts.content.LapisAspect;
import com.ming.victus.hearts.content.LightAspect;
import com.ming.victus.hearts.content.OceanAspect;
import com.ming.victus.hearts.content.PotionAspect;
import com.ming.victus.hearts.content.SweetAspect;
import com.ming.victus.hearts.content.TotemAspect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.Map;

public class HeartAspectRegistry {
    /** 使用 LinkedHashMap 保证注册顺序，便于按序遍历（如 UI 显示） */
    private static final Map<ResourceLocation, HeartAspect.Type> REGISTRY = new LinkedHashMap<>();

    public static void registerDefaults() {
        register(GrilledAspect.TYPE);
        register(BundleAspect.TYPE);
        register(CreeperAspect.TYPE);
        register(DiamondAspect.TYPE);
        register(LightAspect.TYPE);
        register(OceanAspect.TYPE);
        register(TotemAspect.TYPE);
        register(PotionAspect.TYPE);
        register(ArcheryAspect.TYPE);
        register(BlazingAspect.TYPE);
        register(DraconicAspect.TYPE);
        register(EmeraldAspect.TYPE);
        register(EvokingAspect.TYPE);
        register(GoldenAspect.TYPE);
        register(IcyAspect.TYPE);
        register(IronAspect.TYPE);
        register(LapisAspect.TYPE);
        register(SweetAspect.TYPE);
        register(CheeseAspect.TYPE);
    }

    public static void register(HeartAspect.Type type) {
        if (REGISTRY.containsKey(type.id())) throw new IllegalArgumentException("Tried to register " + type.id() + " twice!");
        REGISTRY.put(type.id(), type);
    }

    public static HeartAspect forId(ResourceLocation id, Player holder) {
        HeartAspect.Type type = REGISTRY.get(id);
        if (type == null) return null;
        return type.factory().apply(holder);
    }
}

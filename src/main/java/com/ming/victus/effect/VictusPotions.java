package com.ming.victus.effect;

import com.ming.victus.VictusMain;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

@SuppressWarnings("null")
public class VictusPotions {
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(net.minecraft.core.registries.Registries.POTION, VictusMain.MOD_ID);

    public static final DeferredHolder<Potion, Potion> BLEEDING_HEART = POTIONS.register("bleeding_heart",
        () -> new Potion(new MobEffectInstance(VictusEffects.BLEEDING_HEART, 600)));
}

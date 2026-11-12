package com.ming.victus.effect;

import com.ming.victus.VictusMain;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

@SuppressWarnings("null")
public class VictusEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(net.minecraft.core.registries.Registries.MOB_EFFECT, VictusMain.MOD_ID);

    /**
     * Bleeding Heart: 标记效果 — 携带此效果的实体死亡时会掉落空白之心。
     * applyEffectTick 不造成额外伤害，仅作为死亡掉落判定的标记。
     */
    public static final DeferredHolder<MobEffect, MobEffect> BLEEDING_HEART = EFFECTS.register("bleeding_heart",
        () -> new MobEffect(MobEffectCategory.NEUTRAL, 0xCC0000) {
            @Override
            public boolean applyEffectTick(net.minecraft.world.entity.LivingEntity entity, int amplifier) {
                // 标记效果：不需要每 tick 施加额外效果，死亡时通过 VictusEvents.onLivingDeath 判定掉落
                return true;
            }

            @Override
            public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
                // 仅在每 20 tick（1秒）时调用一次 applyEffectTick，减少不必要的开销
                return tickCount % 20 == 0;
            }
        });
}

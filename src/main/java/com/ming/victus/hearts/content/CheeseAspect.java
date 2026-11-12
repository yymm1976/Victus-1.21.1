package com.ming.victus.hearts.content;

import com.ming.victus.VictusMain;
import com.ming.victus.hearts.HeartAspect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Holder;

import java.util.ArrayList;
import java.util.List;

public class CheeseAspect extends HeartAspect {
    public static final HeartAspect.Type TYPE = new HeartAspect.Type(
        ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "cheese"),
        19, 600, 0xFFDA00, HeartAspect.NEVER_UPDATE, CheeseAspect::new
    );

    public CheeseAspect(Player player) {
        super(player, TYPE);
    }

    @Override
    @SuppressWarnings("null")
    public boolean handleBreak(DamageSource source, float damage, float originalHealth) {
        List<Holder<MobEffect>> effectsToRemove = new ArrayList<>();

        this.player.getActiveEffectsMap().forEach((effect, instance) -> {
            if (effect != null && effect.value().getCategory() == MobEffectCategory.HARMFUL) {
                effectsToRemove.add(effect);
            }
        });

        for (Holder<MobEffect> effect : effectsToRemove) {
            this.player.removeEffect(effect);
        }

        return false;
    }
}

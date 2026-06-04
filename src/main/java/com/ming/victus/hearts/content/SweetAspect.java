package com.ming.victus.hearts.content;

import com.ming.victus.VictusMain;
import com.ming.victus.hearts.HeartAspect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class SweetAspect extends HeartAspect {
    public static final HeartAspect.Type TYPE = new HeartAspect.Type(
        ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "sweet"),
        13, 50, 0xB5C2F2, HeartAspect.NEVER_UPDATE, SweetAspect::new
    );

    public SweetAspect(Player player) {
        super(player, TYPE);
    }

    @Override
    @SuppressWarnings("null")
    public boolean handleBreak(DamageSource source, float damage, float originalHealth) {
        this.player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 150, 1, true, false));
        this.player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 150, 1, true, false));
        return false;
    }
}

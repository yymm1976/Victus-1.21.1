package com.ming.victus.hearts.content;

import com.ming.victus.VictusMain;
import com.ming.victus.hearts.HeartAspect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import com.ming.victus.entity.DragonBreathCloud;
import net.minecraft.world.entity.player.Player;

public class DraconicAspect extends HeartAspect {
    public static final HeartAspect.Type TYPE = new HeartAspect.Type(
        ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "draconic"),
        11, 100, 0xFF55FF, HeartAspect.NEVER_UPDATE, DraconicAspect::new
    );

    public DraconicAspect(Player player) {
        super(player, TYPE);
    }

    @Override
    @SuppressWarnings("null")
    public boolean handleBreak(DamageSource source, float damage, float originalHealth) {
        DragonBreathCloud areaEffectCloud = new DragonBreathCloud(this.player.level(), this.player.getX(), this.player.getY(), this.player.getZ());
        
        areaEffectCloud.setOwner(this.player);
        areaEffectCloud.setParticle(ParticleTypes.DRAGON_BREATH);
        areaEffectCloud.setRadius(2.0F);
        areaEffectCloud.setDuration(150);
        areaEffectCloud.setRadiusPerTick((7.0F - areaEffectCloud.getRadius()) / (float)areaEffectCloud.getDuration());
        
        areaEffectCloud.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
        
        this.player.level().addFreshEntity(areaEffectCloud);
        
        return false;
    }
}

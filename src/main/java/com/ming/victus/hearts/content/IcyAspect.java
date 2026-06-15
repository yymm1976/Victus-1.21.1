package com.ming.victus.hearts.content;

import com.ming.victus.VictusMain;
import com.ming.victus.hearts.HeartAspect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class IcyAspect extends HeartAspect {
    public static final HeartAspect.Type TYPE = new HeartAspect.Type(
        ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "icy"),
        10, 40, 0x00A2D5, HeartAspect.NEVER_UPDATE, IcyAspect::new
    );

    public IcyAspect(Player player) {
        super(player, TYPE);
    }

    @Override
    @SuppressWarnings("null")
    public boolean handleBreak(DamageSource source, float damage, float originalHealth) {
        java.util.List<net.minecraft.world.entity.LivingEntity> entities = this.player.level().getEntitiesOfClass(
            net.minecraft.world.entity.LivingEntity.class, 
            this.player.getBoundingBox().inflate(6.0D), 
            entity -> {
                if (entity == this.player || !entity.isAlive()) return false;
                // 排除玩家驯服的动物
                if (entity instanceof net.minecraft.world.entity.TamableAnimal tamable) {
                    return !tamable.isOwnedBy(this.player);
                }
                return true;
            }
        );
        for (net.minecraft.world.entity.LivingEntity entity : entities) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 2, true, false));
        }
        return false;
    }
}

package com.ming.victus.entity; 
 
import net.minecraft.world.effect.MobEffectInstance; 
import net.minecraft.world.entity.AreaEffectCloud; 
import net.minecraft.world.entity.LivingEntity; 
import net.minecraft.world.level.Level; 
 
import java.util.ArrayList;
import java.util.List;

public class DragonBreathCloud extends AreaEffectCloud { 
 
    private final List<MobEffectInstance> customEffects = new ArrayList<>();

    public DragonBreathCloud(Level level, double x, double y, double z) { 
        super(level, x, y, z); 
    } 
 
    @Override
    @SuppressWarnings("null")
    public void addEffect(MobEffectInstance effect) {
        this.customEffects.add(effect);
    }

    @Override 
    @SuppressWarnings("null")
    public void tick() { 
        super.tick();
        
        if (!this.level().isClientSide && this.tickCount % 5 == 0) {
            for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox())) {
                // 仅排除龙息云的主人，允许伤害其他玩家（PvP场景）
                if (victim == getOwner()) continue;
                if (victim.isAffectedByPotions()) {
                    for (MobEffectInstance effect : this.customEffects) {
                        victim.addEffect(new MobEffectInstance(effect));
                    }
                }
            }
        }
    } 
} 

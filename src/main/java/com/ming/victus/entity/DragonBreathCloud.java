package com.ming.victus.entity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class DragonBreathCloud extends AreaEffectCloud {

    private final List<MobEffectInstance> customEffects = new ArrayList<>();

    /**
     * EntityType 工厂兼容构造器 — 由注册系统调用，位置通过 moveTo() 设置
     */
    public DragonBreathCloud(EntityType<? extends AreaEffectCloud> type, Level level) {
        super(type, level);
    }

    public DragonBreathCloud(Level level, double x, double y, double z) {
        super(level, x, y, z);
    }

    /**
     * 覆写 addEffect：仅存入 customEffects，不写入父类 effects 列表。
     * 这确保 super.tick() 中的父类效果施加逻辑为空操作，
     * 效果仅由本类的自定义 tick 逻辑施加一次，避免双重施加。
     */
    @Override
    @SuppressWarnings("null")
    public void addEffect(MobEffectInstance effect) {
        this.customEffects.add(effect);
    }

    @Override
    @SuppressWarnings("null")
    public void tick() {
        // super.tick() 处理云实体的生命周期（半径衰减、持续时间递减、实体移除等），
        // 由于 addEffect() 覆写不会向父类 effects 列表添加效果，
        // super.tick() 中的效果施加逻辑为空操作，不会导致双重施加。
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

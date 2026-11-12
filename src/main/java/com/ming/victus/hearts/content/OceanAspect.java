package com.ming.victus.hearts.content;

import com.ming.victus.VictusMain;
import com.ming.victus.hearts.HeartAspect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class OceanAspect extends HeartAspect {
    public static final HeartAspect.Type TYPE = new HeartAspect.Type(
        ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "ocean"),
        4, 1200, 0x00A2D5, HeartAspect.ALWAYS_UPDATE, OceanAspect::new
    );

    private int waterTicks = 0;
    private static final int WATER_TICKS_TO_BREAK = 60; // 在水中持续3秒后触发效果

    public OceanAspect(Player player) {
        super(player, TYPE);
    }

    @Override
    @SuppressWarnings("null")
    protected void update() {
        if (this.player.isUnderWater()) {
            this.waterTicks++;
            if (this.waterTicks >= WATER_TICKS_TO_BREAK) {
                this.waterTicks = 0;
                // 在水中持续足够时间后，主动触发破碎并赋予水下增益效果
                com.ming.victus.capability.PlayerHeartCapability cap = com.ming.victus.capability.VictusAttachments.getHearts(this.player);
                if (cap != null) {
                    int index = cap.findFirstIndex(TYPE, com.ming.victus.hearts.HeartAspect.IS_ACTIVE);
                    if (index != -1 && cap.getAspect(index) == this) {
                        cap.damageAspect(index, this.player.damageSources().magic(), 2.0F, this.player.getHealth());
                    }
                }
            }
        } else {
            this.waterTicks = 0;
        }
    }

    @Override
    @SuppressWarnings("null")
    public boolean handleBreak(DamageSource source, float damage, float originalHealth) {
        this.player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 600, 0));
        this.player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 600, 0));
        return false; // 纯服务端效果，无需客户端回调
    }

    @Override
    protected void readCustomData(CompoundTag nbt) {
        this.waterTicks = nbt.getInt("WaterTicks");
    }

    @Override
    protected void writeCustomData(CompoundTag nbt) {
        nbt.putInt("WaterTicks", this.waterTicks);
    }
}

package com.ming.victus.item;

import com.ming.victus.capability.PlayerHeartCapability;
import com.ming.victus.capability.VictusAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class BrokenHeartItem extends EdibleItem {
    @SuppressWarnings("null")
    public BrokenHeartItem() {
        super(new Properties()
            .stacksTo(1));
    }

    @Override
    @SuppressWarnings("null")
    public @javax.annotation.Nonnull InteractionResultHolder<ItemStack> use(@javax.annotation.Nonnull Level level, @javax.annotation.Nonnull Player player, @javax.annotation.Nonnull InteractionHand hand) {
        PlayerHeartCapability aspects = VictusAttachments.getHearts(player);
        if (aspects == null || aspects.empty()) {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(@javax.annotation.Nonnull ItemStack stack, @javax.annotation.Nonnull LivingEntity entity) {
        return 32;
    }

    @Override
    @SuppressWarnings("null")
    public @javax.annotation.Nonnull UseAnim getUseAnimation(@javax.annotation.Nonnull ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    @SuppressWarnings("null")
    void onEaten(ItemStack stack, Level level, Player eater) {
        PlayerHeartCapability aspects = VictusAttachments.getHearts(eater);
        if (aspects != null && !aspects.empty()) {
            // 重要：必须先移除所有心相，再施加伤害。
            // 若顺序反转（先受伤再移除心相），LivingDamageEvent.Post 会触发 damageAspect，
            // 导致所有心相同时破碎，产生严重的连锁效果。
            while (!aspects.empty()) {
                com.ming.victus.hearts.HeartAspect removed = aspects.removeAspect();
                if (removed != null) {
                    eater.spawnAtLocation(removed.asItem());
                }
            }
            eater.hurt(level.damageSources().magic(), 15.0F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, 
                    eater.getX(), eater.getY() + eater.getBbHeight() * 0.5, eater.getZ(), 
                    10, 0.5, 0.5, 0.5, 0.1);
            }
        }
        // 消耗物品（非创造模式）
        if (!eater.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }
}

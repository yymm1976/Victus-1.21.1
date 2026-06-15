package com.ming.victus.item;

import com.ming.victus.capability.PlayerHeartCapability;
import com.ming.victus.capability.VictusAttachments;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class VoidAspectItem extends EdibleItem {
    @SuppressWarnings("null")
    public VoidAspectItem() {
        super(new Properties()
            .fireResistant()
            .stacksTo(1));
    }

    @Override
    @SuppressWarnings("null")
    public @javax.annotation.Nonnull InteractionResultHolder<ItemStack> use(@javax.annotation.Nonnull Level level, @javax.annotation.Nonnull Player player, @javax.annotation.Nonnull InteractionHand hand) {
        PlayerHeartCapability hearts = VictusAttachments.getHearts(player);
        if (hearts == null || hearts.empty()) return InteractionResultHolder.fail(player.getItemInHand(hand));
        
        ItemStack playerStack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(playerStack);
    }

    @Override
    public int getUseDuration(@javax.annotation.Nonnull ItemStack stack, @javax.annotation.Nonnull net.minecraft.world.entity.LivingEntity entity) {
        return 32;
    }

    @Override
    @SuppressWarnings("null")
    public @javax.annotation.Nonnull net.minecraft.world.item.UseAnim getUseAnimation(@javax.annotation.Nonnull ItemStack stack) {
        return net.minecraft.world.item.UseAnim.EAT;
    }

    @Override
    @SuppressWarnings("null")
    void onEaten(ItemStack stack, Level level, Player eater) {
        PlayerHeartCapability hearts = VictusAttachments.getHearts(eater);
        if (hearts != null) {
            com.ming.victus.hearts.HeartAspect removed = hearts.removeAspect();
            if (removed != null) {
                eater.spawnAtLocation(removed.asItem());
            }
            // 计算移除心相后应扣除的生命值，确保伤害不为负数
            float targetHealth = hearts.effectiveSize() * 2.0F;
            float damage = Math.max(0.0F, eater.getHealth() + 1.0F - targetHealth);
            if (damage > 0.0F) {
                eater.hurt(level.damageSources().magic(), damage);
            }
        }
        if (!eater.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, 
                eater.getX(), eater.getY() + eater.getBbHeight() * 0.5, eater.getZ(), 
                10, 0.5, 0.5, 0.5, 0.05);
        }
    }
}

package com.ming.victus.item;

import com.ming.victus.capability.PlayerHeartCapability;
import com.ming.victus.capability.VictusAttachments;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
        PlayerHeartCapability aspects = VictusAttachments.getHearts(eater);
        if (aspects != null && !aspects.empty()) {
            while (!aspects.empty()) {
                com.ming.victus.hearts.HeartAspect removed = aspects.removeAspect();
                if (removed != null) {
                    eater.spawnAtLocation(removed.asItem());
                }
            }
            eater.hurt(level.damageSources().magic(), 15.0F);
            if (!level.isClientSide()) {
                net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level;
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR, 
                    eater.getX(), eater.getY() + eater.getBbHeight() * 0.5, eater.getZ(), 
                    10, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }
}

package com.ming.victus.item;

import com.ming.victus.capability.PlayerHeartCapability;
import com.ming.victus.capability.VictusAttachments;
import com.ming.victus.hearts.HeartAspect;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeartAspectItem extends EdibleItem {
    private static final Map<HeartAspect.Type, HeartAspectItem> HEART_ASPECT_ITEMS = new HashMap<>();

    private final HeartAspect.Type aspectType;

    @SuppressWarnings("null")
    public HeartAspectItem(HeartAspect.Type aspectType) {
        super(new Properties()
            .food(new FoodProperties.Builder()
                .nutrition(aspectType == com.ming.victus.hearts.content.GrilledAspect.TYPE ? 8 : 0)
                .saturationModifier(aspectType == com.ming.victus.hearts.content.GrilledAspect.TYPE ? 0.8F : 0.0F)
                .alwaysEdible()
                .build())
            .stacksTo(1));
        this.aspectType = aspectType;

        HEART_ASPECT_ITEMS.put(aspectType, this);
    }

    @Override
    @SuppressWarnings("null")
    public @javax.annotation.Nonnull InteractionResultHolder<ItemStack> use(@javax.annotation.Nonnull Level level, @javax.annotation.Nonnull Player player, @javax.annotation.Nonnull InteractionHand hand) {
        ItemStack playerStack = player.getItemInHand(hand);
        PlayerHeartCapability aspectComponent = VictusAttachments.getHearts(player);

        if (aspectComponent == null || !aspectComponent.acceptsNew()) return InteractionResultHolder.fail(playerStack);

        if (this.aspectType == com.ming.victus.hearts.content.PotionAspect.TYPE) {
            if (aspectComponent.hasAspect(com.ming.victus.hearts.content.PotionAspect.TYPE, a -> true)) {
                if (!level.isClientSide()) {
                    player.displayClientMessage(Component.translatable("message.victus.potion_aspect_limit").withStyle(ChatFormatting.RED), true);
                }
                return InteractionResultHolder.fail(playerStack);
            }
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(playerStack);
    }

    @Override
    public int getUseDuration(@javax.annotation.Nonnull ItemStack stack, @javax.annotation.Nonnull LivingEntity entity) {
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
        PlayerHeartCapability aspectComponent = VictusAttachments.getHearts(eater);
        if (aspectComponent != null) {
            aspectComponent.addAspect(this.aspectType.factory().apply(eater));
        }

        if (!eater.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (!level.isClientSide()) {
            net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level;
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, 
                eater.getX(), eater.getY() + eater.getBbHeight() * 0.5, eater.getZ(), 
                5, 0.5, 0.5, 0.5, 0.1);
        }
    }

    @Override
    @SuppressWarnings("null")
    public @javax.annotation.Nonnull Component getName(@javax.annotation.Nonnull ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack)); // TODO: colored translation
    }

    @Override
    @SuppressWarnings("null")
    public void appendHoverText(@javax.annotation.Nonnull ItemStack stack, @javax.annotation.Nonnull TooltipContext context, @javax.annotation.Nonnull List<Component> tooltip, @javax.annotation.Nonnull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.literal(" "));
        tooltip.add(Component.translatable("text.victus.recharge_duration", this.aspectType.standardRechargeDuration() / 20.0F).withStyle(ChatFormatting.BLUE));
    }

    public static HeartAspectItem getItem(HeartAspect.Type type) {
        return HEART_ASPECT_ITEMS.get(type);
    }
}
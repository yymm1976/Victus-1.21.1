package com.ming.victus.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.ChatFormatting;
import java.util.List;

public abstract class EdibleItem extends Item {
    public EdibleItem(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("null")
    public @javax.annotation.Nonnull InteractionResultHolder<ItemStack> use(@javax.annotation.Nonnull Level level, @javax.annotation.Nonnull Player player, @javax.annotation.Nonnull InteractionHand hand) {
        ItemStack playerStack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(playerStack);
    }

    @Override
    @SuppressWarnings("null")
    public @javax.annotation.Nonnull ItemStack finishUsingItem(@javax.annotation.Nonnull ItemStack stack, @javax.annotation.Nonnull Level level, @javax.annotation.Nonnull LivingEntity livingEntity) {
        if (!level.isClientSide() && livingEntity instanceof Player player) {
            onEaten(stack, level, player);
        }
        // 不调 super：子类 onEaten 已负责所有消耗逻辑；
        // Item 默认实现会对 FoodProperties 的物品触发 player.eat()，导致重复消耗
        return stack;
    }

    @Override
    @SuppressWarnings("null")
    public void appendHoverText(@javax.annotation.Nonnull ItemStack stack, @javax.annotation.Nonnull TooltipContext context, @javax.annotation.Nonnull List<Component> tooltip, @javax.annotation.Nonnull TooltipFlag flag) {
        tooltip.add(Component.translatable(this.getDescriptionId(stack) + ".description").withStyle(ChatFormatting.GRAY));
    }

    abstract void onEaten(ItemStack stack, Level level, Player eater);
}
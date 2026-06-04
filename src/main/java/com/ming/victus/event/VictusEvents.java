package com.ming.victus.event;

import com.ming.victus.VictusMain;
import com.ming.victus.capability.PlayerHeartCapability;
import com.ming.victus.capability.VictusAttachments;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

@EventBusSubscriber(modid = VictusMain.MOD_ID)
public class VictusEvents {

    /**
     * 公共辅助方法：将玩家的心相数据同步到客户端
     */
    @SuppressWarnings("null")
    private static void syncHeartsToPlayer(net.minecraft.server.level.ServerPlayer targetPlayer) {
        PlayerHeartCapability cap = VictusAttachments.getHearts(targetPlayer);
        if (cap != null) {
            net.minecraft.nbt.CompoundTag tag = cap.serializeNBT(targetPlayer.registryAccess());
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(targetPlayer, new com.ming.victus.network.SyncAspectsPacket(tag));
        }
    }

    /**
     * 公共辅助方法：将目标玩家的心相数据同步给观察者
     */
    @SuppressWarnings("null")
    private static void syncHeartsToObserver(net.minecraft.server.level.ServerPlayer targetPlayer, net.minecraft.server.level.ServerPlayer observer) {
        PlayerHeartCapability cap = VictusAttachments.getHearts(targetPlayer);
        if (cap != null) {
            net.minecraft.nbt.CompoundTag tag = cap.serializeNBT(targetPlayer.registryAccess());
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(observer, new com.ming.victus.network.SyncAspectsPacket(tag));
        }
    }

    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onPlayerClone(net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            PlayerHeartCapability original = event.getOriginal().getData(VictusAttachments.PLAYER_HEARTS);
            PlayerHeartCapability current = event.getEntity().getData(VictusAttachments.PLAYER_HEARTS);
            if (original != null && current != null) {
                current.deserializeNBT(event.getEntity().registryAccess(), original.serializeNBT(event.getEntity().registryAccess()));
            }
        }
    }

    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onRegisterBrewingRecipes(RegisterBrewingRecipesEvent event) {
        event.getBuilder().addMix(
            Potions.AWKWARD, 
            Items.SWEET_BERRIES, 
            com.ming.victus.effect.VictusPotions.BLEEDING_HEART
        );
    }

    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onPlayerLoggedIn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            syncHeartsToPlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onPlayerChangedDimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            syncHeartsToPlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onStartTracking(net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof net.minecraft.server.level.ServerPlayer targetPlayer && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer tracker) {
            syncHeartsToObserver(targetPlayer, tracker);
        }
    }

    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onPlayerRespawn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            syncHeartsToPlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.LivingEntity entity && !entity.level().isClientSide()) {
            if (entity.hasEffect(com.ming.victus.effect.VictusEffects.BLEEDING_HEART)) {
                entity.spawnAtLocation(com.ming.victus.item.VictusItems.BLANK_HEART_ASPECT.get());
            }
        }

        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            PlayerHeartCapability cap = VictusAttachments.getHearts(player);
            if (cap != null && !cap.empty()) {
                // 检查是否有激活的图腾之心，如果有则取消死亡并触发其效果
                int totemIndex = cap.findFirstIndex(
                    com.ming.victus.hearts.content.TotemAspect.TYPE,
                    com.ming.victus.hearts.HeartAspect.IS_ACTIVE
                );
                if (totemIndex != -1) {
                    event.setCanceled(true);
                    // 主动触发图腾之心的破碎效果（消耗图腾、回血）
                    com.ming.victus.hearts.HeartAspect totemAspect = cap.getAspect(totemIndex);
                    if (totemAspect != null) {
                        float lethalDamage = player.getMaxHealth(); // 致命伤害用最大生命值估算
                        totemAspect.onBroken(player.damageSources().magic(), lethalDamage, player.getMaxHealth());
                        cap.sync();
                        // 发送破碎包到客户端
                        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                                serverPlayer,
                                new com.ming.victus.network.AspectBrokenPacket(totemIndex, true)
                            );
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onLivingDamagePre(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            PlayerHeartCapability cap = VictusAttachments.getHearts(player);
            if (cap != null && !cap.empty()) {
                if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
                    int lightIndex = cap.findLastIndex(com.ming.victus.hearts.content.LightAspect.TYPE, com.ming.victus.hearts.HeartAspect.IS_ACTIVE);
                    if (lightIndex != -1) {
                        float originalDamage = event.getNewDamage();
                        event.setNewDamage(0);
                        com.ming.victus.hearts.HeartAspect aspect = cap.getAspect(lightIndex);
                        if (aspect != null) {
                            // 传递原始伤害值（而非修改后的0），让 handleBreak 能获取真实伤害信息
                            boolean callClient = aspect.onBroken(event.getSource(), originalDamage, player.getHealth());
                            cap.sync();
                            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer, new com.ming.victus.network.AspectBrokenPacket(lightIndex, callClient));
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            PlayerHeartCapability cap = VictusAttachments.getHearts(player);
            if (cap != null && !cap.empty()) {
                float damage = event.getNewDamage();
                if (damage <= 0) return;

                float health = player.getHealth();
                float originalHealth = health + damage;

                int firstAffectedHeart = (int) Math.ceil((originalHealth - damage) / 2) - 1;
                int lastAffectedHeart = (int) Math.ceil(originalHealth / 2) - 1;

                if (firstAffectedHeart < 0) firstAffectedHeart = 0;
                if (lastAffectedHeart >= cap.effectiveSize()) lastAffectedHeart = cap.effectiveSize() - 1;
                
                if (lastAffectedHeart >= firstAffectedHeart) {
                    cap.damageAspect(lastAffectedHeart, event.getSource(), damage, originalHealth);
                }
            }
        }
    }

    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().getData(VictusAttachments.NO_DROPS)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPickupXp(PlayerXpEvent.PickupXp event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || event.getOrb().getValue() < 3) return;
        if (player.getMaxHealth() - player.getHealth() < 1.0F) return;

        PlayerHeartCapability cap = VictusAttachments.getHearts(player);
        if (cap != null) {
            int lapisIndex = cap.findFirstIndex(com.ming.victus.hearts.content.LapisAspect.TYPE, com.ming.victus.hearts.HeartAspect.IS_ACTIVE);
            if (lapisIndex != -1) {
                player.heal(2.0F);
                
                int xpAmount = event.getOrb().getValue();
                event.getOrb().value = xpAmount - 3;
                if (event.getOrb().getValue() <= 0) {
                    event.getOrb().discard();
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        PlayerHeartCapability cap = VictusAttachments.getHearts(player);
        if (cap != null) {
            cap.tick();
            
            if (!player.level().isClientSide()) {
                // Diamond Aspect armor logic
                int diamondCount = 0;
                for (com.ming.victus.hearts.HeartAspect aspect : cap.getEquippedHearts()) {
                    if (aspect instanceof com.ming.victus.hearts.content.DiamondAspect && aspect.active()) {
                        diamondCount++;
                    }
                }
                
                var armorAttr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
                if (armorAttr != null) {
                    var modifier = armorAttr.getModifier(com.ming.victus.hearts.content.DiamondAspect.ARMOR_MODIFIER_ID);
                    double expectedArmor = diamondCount * 2.0D;
                    if ((modifier == null && expectedArmor > 0) || (modifier != null && modifier.amount() != expectedArmor)) {
                        armorAttr.removeModifier(com.ming.victus.hearts.content.DiamondAspect.ARMOR_MODIFIER_ID);
                        if (expectedArmor > 0) {
                            armorAttr.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(com.ming.victus.hearts.content.DiamondAspect.ARMOR_MODIFIER_ID, expectedArmor, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
                        }
                    }
                }
                
                var toughnessAttr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
                if (toughnessAttr != null) {
                    var modifier = toughnessAttr.getModifier(com.ming.victus.hearts.content.DiamondAspect.TOUGHNESS_MODIFIER_ID);
                    double expectedToughness = diamondCount * 0.5D; // 1 toughness per 2 hearts
                    if ((modifier == null && expectedToughness > 0) || (modifier != null && modifier.amount() != expectedToughness)) {
                        toughnessAttr.removeModifier(com.ming.victus.hearts.content.DiamondAspect.TOUGHNESS_MODIFIER_ID);
                        if (expectedToughness > 0) {
                            toughnessAttr.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(com.ming.victus.hearts.content.DiamondAspect.TOUGHNESS_MODIFIER_ID, expectedToughness, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
                        }
                    }
                }

                // Iron Aspect targeting logic
                if (cap.hasAspect(com.ming.victus.hearts.content.IronAspect.TYPE, com.ming.victus.hearts.HeartAspect.IS_NOT_ACTIVE)) {
                    LivingEntity attacker = player.getLastHurtByMob();
                    if (attacker != null && attacker != player && !(attacker instanceof IronGolem)) {
                        AABB box = player.getBoundingBox().inflate(10.0D);
                        List<IronGolem> golems = player.level().getEntitiesOfClass(IronGolem.class, box, entity -> entity.isAlive());
                        for (IronGolem golem : golems) {
                            if (golem.getPersistentData().hasUUID("VictusOwner") && player.getUUID().equals(golem.getPersistentData().getUUID("VictusOwner"))) {
                                golem.setTarget(attacker);
                            }
                        }
                    }
                    LivingEntity target = player.getLastHurtMob();
                    if (target != null && target != player && !(target instanceof IronGolem)) {
                        AABB box = player.getBoundingBox().inflate(10.0D);
                        List<IronGolem> golems = player.level().getEntitiesOfClass(IronGolem.class, box, entity -> entity.isAlive());
                        for (IronGolem golem : golems) {
                            if (golem.getPersistentData().hasUUID("VictusOwner") && player.getUUID().equals(golem.getPersistentData().getUUID("VictusOwner"))) {
                                golem.setTarget(target);
                            }
                        }
                    }
                }

                // Evoking Aspect targeting logic
                if (cap.hasAspect(com.ming.victus.hearts.content.EvokingAspect.TYPE, com.ming.victus.hearts.HeartAspect.IS_NOT_ACTIVE)) {
                    LivingEntity attacker = player.getLastHurtByMob();
                    if (attacker != null) {
                        AABB box = player.getBoundingBox().inflate(10.0D);
                        List<net.minecraft.world.entity.monster.Vex> vexes = player.level().getEntitiesOfClass(net.minecraft.world.entity.monster.Vex.class, box, entity -> entity.isAlive());
                        for (net.minecraft.world.entity.monster.Vex vex : vexes) {
                            if (vex.getPersistentData().hasUUID("VictusOwner") && player.getUUID().equals(vex.getPersistentData().getUUID("VictusOwner"))) {
                                vex.setTarget(attacker);
                            }
                        }
                    }
                    LivingEntity target = player.getLastHurtMob();
                    if (target != null) {
                        AABB box = player.getBoundingBox().inflate(10.0D);
                        List<net.minecraft.world.entity.monster.Vex> vexes = player.level().getEntitiesOfClass(net.minecraft.world.entity.monster.Vex.class, box, entity -> entity.isAlive());
                        for (net.minecraft.world.entity.monster.Vex vex : vexes) {
                            if (vex.getPersistentData().hasUUID("VictusOwner") && player.getUUID().equals(vex.getPersistentData().getUUID("VictusOwner"))) {
                                vex.setTarget(target);
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.monster.Vex vex && event.getNewAboutToBeSetTarget() instanceof Player player) {
            if (vex.getPersistentData().hasUUID("VictusOwner") && player.getUUID().equals(vex.getPersistentData().getUUID("VictusOwner"))) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onLivingUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            if (event.getItem().getItem() instanceof net.minecraft.world.item.PotionItem) {
                PlayerHeartCapability cap = VictusAttachments.getHearts(player);
                if (cap != null) {
                    int potionIndex = cap.findFirstIndex(com.ming.victus.hearts.content.PotionAspect.TYPE, com.ming.victus.hearts.HeartAspect.IS_ACTIVE);
                    if (potionIndex != -1) {
                        com.ming.victus.hearts.HeartAspect aspect = cap.getAspect(potionIndex);
                        if (aspect instanceof com.ming.victus.hearts.content.PotionAspect potionAspect) {
                            PotionContents contents = event.getItem().getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                            potionAspect.setPotion(contents.potion());
                            potionAspect.getCustomEffects().clear();
                            contents.customEffects().forEach(effect -> potionAspect.getCustomEffects().add(new net.minecraft.world.effect.MobEffectInstance(effect)));
                            cap.sync();
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            PlayerHeartCapability cap = VictusAttachments.getHearts(player);
            if (cap != null && cap.recharging()) {
                float healAmount = event.getAmount();
                float maxHealth = player.getMaxHealth();
                cap.rechargeAllByPercentage(healAmount / maxHealth, healAmount);
            }
        }
    }
}

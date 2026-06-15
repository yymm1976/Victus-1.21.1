package com.ming.victus.event;

import com.ming.victus.VictusMain;
import com.ming.victus.capability.PlayerHeartCapability;
import com.ming.victus.capability.VictusAttachments;
import com.ming.victus.effect.VictusEffects;
import com.ming.victus.effect.VictusPotions;
import com.ming.victus.hearts.HeartAspect;
import com.ming.victus.hearts.content.DiamondAspect;
import com.ming.victus.hearts.content.EvokingAspect;
import com.ming.victus.hearts.content.IronAspect;
import com.ming.victus.hearts.content.LapisAspect;
import com.ming.victus.hearts.content.LightAspect;
import com.ming.victus.hearts.content.PotionAspect;
import com.ming.victus.hearts.content.TotemAspect;
import com.ming.victus.item.VictusItems;
import com.ming.victus.network.AspectBrokenPacket;
import com.ming.victus.network.SyncAspectsPacket;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.AABB;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

@EventBusSubscriber(modid = VictusMain.MOD_ID)
public class VictusEvents {

    /**
     * 公共辅助方法：将玩家的心相数据同步到客户端
     */
    @SuppressWarnings("null")
    private static void syncHeartsToPlayer(ServerPlayer targetPlayer) {
        PlayerHeartCapability cap = VictusAttachments.getHearts(targetPlayer);
        if (cap != null) {
            CompoundTag tag = cap.serializeNBT(targetPlayer.registryAccess());
            PacketDistributor.sendToPlayer(targetPlayer, new SyncAspectsPacket(tag));
        }
    }

    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // 死亡克隆时的数据复制已由 VictusAttachments.PLAYER_HEARTS 的 copyOnDeath() 自动处理
        // 此处仅处理非死亡克隆场景（如从末地返回），确保心相数据不丢失
        if (!event.isWasDeath()) {
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
            VictusPotions.BLEEDING_HEART
        );
    }

    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncHeartsToPlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncHeartsToPlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncHeartsToPlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof LivingEntity entity && !entity.level().isClientSide()) {
            if (entity.hasEffect(VictusEffects.BLEEDING_HEART)) {
                entity.spawnAtLocation(VictusItems.BLANK_HEART_ASPECT.get());
            }
        }

        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            PlayerHeartCapability cap = VictusAttachments.getHearts(player);
            if (cap != null && !cap.empty()) {
                // 检查是否有激活的图腾之心，如果有则取消死亡并触发其效果
                int totemIndex = cap.findFirstIndex(
                    TotemAspect.TYPE,
                    HeartAspect.IS_ACTIVE
                );
                if (totemIndex != -1) {
                    event.setCanceled(true);
                    // 主动触发图腾之心的破碎效果（消耗图腾、回血）
                    HeartAspect totemAspect = cap.getAspect(totemIndex);
                    if (totemAspect != null) {
                        float lethalDamage = player.getMaxHealth(); // 致命伤害用最大生命值估算
                        totemAspect.onBroken(player.damageSources().magic(), lethalDamage, player.getMaxHealth());
                        cap.sync();
                        // 发送破碎包到客户端
                        if (player instanceof ServerPlayer serverPlayer) {
                            PacketDistributor.sendToPlayer(
                                serverPlayer,
                                new AspectBrokenPacket(totemIndex, true)
                            );
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            PlayerHeartCapability cap = VictusAttachments.getHearts(player);
            if (cap != null && !cap.empty()) {
                if (event.getSource().is(DamageTypeTags.IS_FALL)) {
                    int lightIndex = cap.findLastIndex(LightAspect.TYPE, HeartAspect.IS_ACTIVE);
                    if (lightIndex != -1) {
                        float originalDamage = event.getNewDamage();
                        event.setNewDamage(0);
                        HeartAspect aspect = cap.getAspect(lightIndex);
                        if (aspect != null) {
                            // 传递原始伤害值（而非修改后的0），让 handleBreak 能获取真实伤害信息
                            boolean callClient = aspect.onBroken(event.getSource(), originalDamage, player.getHealth());
                            cap.sync();
                            if (player instanceof ServerPlayer serverPlayer) {
                                PacketDistributor.sendToPlayer(serverPlayer, new AspectBrokenPacket(lightIndex, callClient));
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

                // 防御性钳制：防止极端值（如通过命令设置 HP > 2000）导致溢出
                if (firstAffectedHeart < 0) firstAffectedHeart = 0;
                if (firstAffectedHeart >= cap.effectiveSize()) firstAffectedHeart = cap.effectiveSize() - 1;
                if (lastAffectedHeart >= cap.effectiveSize()) lastAffectedHeart = cap.effectiveSize() - 1;
                if (lastAffectedHeart < 0) lastAffectedHeart = 0;
                
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
    @SuppressWarnings("null")
    public static void onPickupXp(PlayerXpEvent.PickupXp event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || event.getOrb().getValue() < 3) return;
        if (player.getMaxHealth() - player.getHealth() < 1.0F) return;

        PlayerHeartCapability cap = VictusAttachments.getHearts(player);
        if (cap != null) {
            int lapisIndex = cap.findFirstIndex(LapisAspect.TYPE, HeartAspect.IS_ACTIVE);
            if (lapisIndex != -1) {
                player.heal(2.0F);

                int remainingXp = event.getOrb().getValue() - 3;
                if (remainingXp <= 0) {
                    // 经验值全部消耗，取消拾取并移除经验球
                    event.setCanceled(true);
                    event.getOrb().discard();
                } else {
                    // 取消原事件，用新的经验球替代（避免直接修改 ExperienceOrb.value 字段）
                    event.setCanceled(true);
                    event.getOrb().discard();
                    ExperienceOrb newOrb = new ExperienceOrb(
                        player.level(), player.getX(), player.getY(), player.getZ(), remainingXp);
                    player.level().addFreshEntity(newOrb);
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
                updateDiamondArmorModifiers(player, cap);
                updateIronGolemTargeting(player, cap);
                updateVexTargeting(player, cap);
            }
        }
    }

    /**
     * 钻石心相护甲/韧性属性修饰符更新。
     * 每颗激活的钻石心提供 +2 护甲值和 +0.5 韧性。
     */
    @SuppressWarnings("null")
    private static void updateDiamondArmorModifiers(Player player, PlayerHeartCapability cap) {
        int diamondCount = 0;
        for (HeartAspect aspect : cap.getEquippedHearts()) {
            if (aspect instanceof DiamondAspect && aspect.active()) {
                diamondCount++;
            }
        }

        var armorAttr = player.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            var modifier = armorAttr.getModifier(DiamondAspect.ARMOR_MODIFIER_ID);
            double expectedArmor = diamondCount * 2.0D;
            if ((modifier == null && expectedArmor > 0) || (modifier != null && modifier.amount() != expectedArmor)) {
                armorAttr.removeModifier(DiamondAspect.ARMOR_MODIFIER_ID);
                if (expectedArmor > 0) {
                    armorAttr.addTransientModifier(new AttributeModifier(
                        DiamondAspect.ARMOR_MODIFIER_ID,
                        expectedArmor, AttributeModifier.Operation.ADD_VALUE));
                }
            }
        }

        var toughnessAttr = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughnessAttr != null) {
            var modifier = toughnessAttr.getModifier(DiamondAspect.TOUGHNESS_MODIFIER_ID);
            double expectedToughness = diamondCount * 0.5D;
            if ((modifier == null && expectedToughness > 0) || (modifier != null && modifier.amount() != expectedToughness)) {
                toughnessAttr.removeModifier(DiamondAspect.TOUGHNESS_MODIFIER_ID);
                if (expectedToughness > 0) {
                    toughnessAttr.addTransientModifier(new AttributeModifier(
                        DiamondAspect.TOUGHNESS_MODIFIER_ID,
                        expectedToughness, AttributeModifier.Operation.ADD_VALUE));
                }
            }
        }
    }

    /**
     * 铁心相：当铁心破碎中，指挥附近属于该玩家的铁傀儡攻击伤害来源或攻击目标。
     */
    @SuppressWarnings("null")
    private static void updateIronGolemTargeting(Player player, PlayerHeartCapability cap) {
        if (!cap.hasAspect(IronAspect.TYPE, HeartAspect.IS_NOT_ACTIVE)) {
            return;
        }

        AABB box = player.getBoundingBox().inflate(10.0D);
        List<IronGolem> golems = player.level().getEntitiesOfClass(IronGolem.class, box, entity -> entity.isAlive());

        LivingEntity attacker = player.getLastHurtByMob();
        if (attacker != null && attacker != player && !(attacker instanceof IronGolem)) {
            for (IronGolem golem : golems) {
                if (golem.getPersistentData().hasUUID("VictusOwner")
                        && player.getUUID().equals(golem.getPersistentData().getUUID("VictusOwner"))) {
                    golem.setTarget(attacker);
                }
            }
        }

        LivingEntity target = player.getLastHurtMob();
        if (target != null && target != player && !(target instanceof IronGolem)) {
            for (IronGolem golem : golems) {
                if (golem.getPersistentData().hasUUID("VictusOwner")
                        && player.getUUID().equals(golem.getPersistentData().getUUID("VictusOwner"))) {
                    golem.setTarget(target);
                }
            }
        }
    }

    /**
     * 唤魔心相：当唤魔心破碎中，指挥附近属于该玩家的恼鬼攻击伤害来源或攻击目标。
     */
    @SuppressWarnings("null")
    private static void updateVexTargeting(Player player, PlayerHeartCapability cap) {
        if (!cap.hasAspect(EvokingAspect.TYPE, HeartAspect.IS_NOT_ACTIVE)) {
            return;
        }

        AABB box = player.getBoundingBox().inflate(10.0D);
        List<Vex> vexes = player.level().getEntitiesOfClass(
            Vex.class, box, entity -> entity.isAlive());

        LivingEntity attacker = player.getLastHurtByMob();
        if (attacker != null) {
            for (Vex vex : vexes) {
                if (vex.getPersistentData().hasUUID("VictusOwner")
                        && player.getUUID().equals(vex.getPersistentData().getUUID("VictusOwner"))) {
                    vex.setTarget(attacker);
                }
            }
        }

        LivingEntity target = player.getLastHurtMob();
        if (target != null) {
            for (Vex vex : vexes) {
                if (vex.getPersistentData().hasUUID("VictusOwner")
                        && player.getUUID().equals(vex.getPersistentData().getUUID("VictusOwner"))) {
                    vex.setTarget(target);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Vex vex && event.getNewAboutToBeSetTarget() instanceof Player player) {
            if (vex.getPersistentData().hasUUID("VictusOwner") && player.getUUID().equals(vex.getPersistentData().getUUID("VictusOwner"))) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onLivingUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            if (event.getItem().getItem() instanceof PotionItem) {
                PlayerHeartCapability cap = VictusAttachments.getHearts(player);
                if (cap != null) {
                    int potionIndex = cap.findFirstIndex(PotionAspect.TYPE, HeartAspect.IS_ACTIVE);
                    if (potionIndex != -1) {
                        HeartAspect aspect = cap.getAspect(potionIndex);
                        if (aspect instanceof PotionAspect potionAspect) {
                            PotionContents contents = event.getItem().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                            potionAspect.setPotion(contents.potion());
                            potionAspect.getCustomEffects().clear();
                            contents.customEffects().forEach(effect -> potionAspect.getCustomEffects().add(new MobEffectInstance(effect)));
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

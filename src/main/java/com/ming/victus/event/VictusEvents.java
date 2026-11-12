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
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
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
                        // 先发送破碎动画/音效包，再同步权威状态；与 damageAspect 路径保持一致。
                        if (player instanceof ServerPlayer serverPlayer) {
                            PacketDistributor.sendToPlayer(
                                serverPlayer,
                                new AspectBrokenPacket(totemIndex, true)
                            );
                        }
                        cap.sync();
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
                            // 先发送破碎动画/音效包，再同步权威状态；与 damageAspect 路径保持一致。
                            if (player instanceof ServerPlayer serverPlayer) {
                                PacketDistributor.sendToPlayer(serverPlayer, new AspectBrokenPacket(lightIndex, callClient));
                            }
                            cap.sync();
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
                    // Phase 3 修复（LapisAspect "3 XP → 2 HP" 转换）：
                    //
                    // 原实现：discard 原 100 XP orb + 在玩家脚下 addFreshEntity(remainingXp orb)
                    //  问题：玩家下一 tick 立即重新拾取新 orb，循环触发 33 次，
                    //        单次拾取产生 33 次 heal(2) + 33 次实体创建销毁 + 33 次事件回调。
                    //
                    // 备选方案评估（已通过 javap 验证 1.21.1 Parchment API）：
                    //   ① 直接修改 orb.value（public 字段，javap 已确认）:
                    //      不可行——事件 cancel 后 orb 仍存在，玩家持续接近导致 onPickupXp
                    //      持续触发（同 tick 取消+漂移+再触发 死循环）。
                    //   ② 在偏移位置 addFreshEntity + pickupDelay:
                    //      不可行——1.21.1 ExperienceOrb 没有 pickupDelay 字段（javap 已确认）；
                    //      玩家 takeXpDelay（public 字段，javap 已确认）是 Player 字段而非 orb 字段，
                    //      即便设置也只是延迟玩家拾取，对已被丢回原 orb 的链式场景无帮助。
                    //   ③ 取消事件 + discard + giveExperiencePoints(remainingXp)（当前选择）:
                    //      玩家净获得 97 XP + 2 HP（3 XP 换 2 HP，保留设计意图）；
                    //      无 orb 重建、无实体 churn、无链式触发；
                    //      副作用：玩家失去原版"经验球被玩家吸收"的视觉动画。
                    //
                    // giveExperiencePoints(int) 是 Parchment 1.21.1 中的 public 方法（javap 已确认）。
                    // 方法语义：增加 totalExperience，负数则扣减，跨等级时自动 level up。
                    // 无需反射、无需 AT、无需访问私有字段。
                    event.setCanceled(true);
                    event.getOrb().discard();
                    player.giveExperiencePoints(remainingXp);
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
            // 客户端也推进本地 cooldown，使 HUD 充能进度条平滑显示；
            // 服务端为权威方，并在关键节点（充满/加速）通过 SyncAspectsPacket 纠偏
            cap.tick();

            if (!player.level().isClientSide()) {
                updateDiamondArmorModifiers(player, cap);
                // 召唤物目标更新涉及附近实体扫描（热路径），降频到每 10 tick 一次，
                // 减少多人/多召唤物场景下的性能压力；目标重定向延迟 ≤0.5s 可接受
                if (player.tickCount % 10 == 0) {
                    updateIronGolemTargeting(player, cap);
                    updateVexTargeting(player, cap);
                }
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
        if (attacker != null && attacker != player) {
            for (Vex vex : vexes) {
                if (vex.getPersistentData().hasUUID("VictusOwner")
                        && player.getUUID().equals(vex.getPersistentData().getUUID("VictusOwner"))) {
                    vex.setTarget(attacker);
                }
            }
        }

        LivingEntity target = player.getLastHurtMob();
        if (target != null && target != player) {
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

    /**
     * GameTest-only: pre-register an empty 1x1x1 structure template for
     * {@code @GameTest(template = "empty")}. Without this, GameTest fails
     * with "Missing test structure: victus:empty" because the NBT resource
     * is not found by the ResourceManager in the server environment.
     * Gated by {@link GameTestHooks#isGametestEnabled()} so it has no effect
     * on normal {@code runClient} / {@code runServer} (no player/world/registry
     * state is mutated).
     */
    @SubscribeEvent
    public static void onServerStarting(ServerAboutToStartEvent event) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        event.getServer().getStructureManager()
            .getOrCreate(ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "empty"));
    }
}

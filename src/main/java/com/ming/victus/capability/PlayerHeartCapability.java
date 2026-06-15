package com.ming.victus.capability;

import com.ming.victus.hearts.HeartAspect;
import com.ming.victus.hearts.HeartAspectRegistry;
import com.ming.victus.network.AspectBrokenPacket;
import com.ming.victus.network.SyncAspectsPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.INBTSerializable;

import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class PlayerHeartCapability implements INBTSerializable<CompoundTag> {
    private final Player provider;
    private final List<HeartAspect> aspects = new ArrayList<>();
    private int rechargeBoostTicks = 0;
    /** 整数累加器：每 4 个加速 tick 额外充能 1 tick，消除浮点精度隐患 */
    private int fractionalRechargeTicks = 0;
    /**
     * 脏标记：当内部状态发生变化时设为 true，由 tick() 在每 tick 末尾统一 flush 同步，
     * 避免在单次 tick 中多次 add/remove/damage 操作都发送完整 NBT 数据包。
     * 外部调用方（事件处理器等）应继续使用 sync() 做即时同步。
     */
    private boolean dirty = false;

    public PlayerHeartCapability(Player provider) {
        this.provider = provider;
    }

    public void addRechargeBoostTicks(int ticks) {
        this.rechargeBoostTicks += ticks;
        markDirty();
    }

    /**
     * 设置脏标记，延迟到 tick() 末尾统一同步。
     */
    private void markDirty() {
        this.dirty = true;
    }

    public List<HeartAspect> getEquippedHearts() {
        return Collections.unmodifiableList(aspects);
    }

    public boolean acceptsNew() {
        return (this.aspects.size() < capacity());
    }

    public boolean addAspect(HeartAspect aspect) {
        if (this.aspects.size() >= capacity()) return false;
        this.aspects.add(aspect);
        markDirty();
        return true;
    }

    public HeartAspect removeAspect() {
        if (this.aspects.isEmpty()) return null;
        HeartAspect removedAspect = this.aspects.remove(this.aspects.size() - 1);
        markDirty();
        return removedAspect;
    }

    public void tick() {
        HeartAspect targetAspect = null;
        for (int i = 0; i < effectiveSize(); i++) {
            HeartAspect aspect = this.aspects.get(i);
            if (!aspect.active()) {
                float healthRequired = (i + 1) * 2.0F;
                if (this.provider.getHealth() >= healthRequired) {
                    targetAspect = aspect;
                }
                break;
            } else {
                aspect.tick();
            }
        }

        if (targetAspect != null) {
            int tickAmount = 1;
            if (this.rechargeBoostTicks > 0) {
                this.rechargeBoostTicks--;
                this.fractionalRechargeTicks++;
                if (this.fractionalRechargeTicks >= 4) {
                    tickAmount += this.fractionalRechargeTicks / 4;
                    this.fractionalRechargeTicks %= 4;
                }
                markDirty();
            }
            for (int t = 0; t < tickAmount; t++) {
                targetAspect.tick();
            }

            // 心相在本 tick 内完成充能，清除剩余加速 tick
            if (targetAspect.active()) {
                this.rechargeBoostTicks = 0;
                this.fractionalRechargeTicks = 0;
                markDirty();
            }
        }

        // 本 tick 结束，统一 flush 脏标记为一次网络同步
        if (this.dirty) {
            sync();
            this.dirty = false;
        }
    }

    public boolean recharging() {
        for (int i = 0; i < effectiveSize(); i++) {
            if (!this.aspects.get(i).active()) return true;
        }
        return false;
    }

    public void rechargeAllByPercentage(float percentage, float expectedExtraHealth) {
        int totalTicks = 0;
        for (HeartAspect aspect : this.aspects) {
            if (!aspect.active()) {
                totalTicks += (int) (aspect.getRechargeDuration() * percentage);
            }
        }
        
        float effectiveHealth = this.provider.getHealth() + expectedExtraHealth;

        for (int i = 0; i < effectiveSize(); i++) {
            HeartAspect aspect = this.aspects.get(i);
            if (!aspect.active()) {
                float healthRequired = (i + 1) * 2.0F;
                if (effectiveHealth >= healthRequired) {
                    while (!aspect.active() && totalTicks > 0) {
                        aspect.tick();
                        totalTicks--;
                    }
                    if (totalTicks <= 0) {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        sync();
    }

    public void damageAspect(int index, DamageSource source, float damage, float originalHealth) {
        HeartAspect aspect = getAspect(index);
        if (aspect == null) return;

        // 仅对活跃（已激活）的心相触发破碎，避免重置正在充能中的心相进度
        if (!aspect.active()) return;

        float healthAfterThisHeart = index * 2.0F;
        float currentHealth = originalHealth - damage;

        int nextIndex = index - 1;
        // 使用严格小于（<）而非小于等于（<=），避免健康值恰好处于心相边界时多碎一颗心
        if (nextIndex >= 0 && currentHealth < healthAfterThisHeart) {
            damageAspect(nextIndex, source, damage, originalHealth);
        }

        boolean callClient = aspect.onBroken(source, damage, originalHealth);
        // damageAspect 是事件驱动的即时操作，保持 sync() 确保破碎动画立即到达客户端
        sync();
        if (this.provider instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new AspectBrokenPacket(index, callClient));
        }
    }

    @Nullable
    public HeartAspect getAspect(int index) {
        return (index < 0 || index > effectiveSize() - 1) ? null : this.aspects.get(index);
    }

    public int findFirstIndex(HeartAspect.Type type, Predicate<HeartAspect> filter) {
        for (int i = 0; i < effectiveSize(); i++) {
            HeartAspect aspect = getAspect(i);
            if (aspect != null && aspect.getType() == type && filter.test(aspect)) return i;
        }
        return -1;
    }

    public int findLastIndex(HeartAspect.Type type, Predicate<HeartAspect> filter) {
        for (int i = effectiveSize() - 1; i >= 0; i--) {
            HeartAspect aspect = getAspect(i);
            if (aspect != null && aspect.getType() == type && filter.test(aspect)) return i;
        }
        return -1;
    }

    public boolean hasAspect(HeartAspect.Type type, Predicate<HeartAspect> filter) {
        for (HeartAspect aspect : this.aspects) {
            if (aspect.getType() == type && filter.test(aspect)) return true;
        }
        return false;
    }

    public int capacity() {
        return (int) Math.round(this.provider.getMaxHealth() / 2.0D);
    }

    public int effectiveSize() {
        return Math.min(this.aspects.size(), capacity());
    }

    public int realSize() {
        return this.aspects.size();
    }

    public boolean empty() {
        return this.aspects.isEmpty();
    }

    @SuppressWarnings("null")
    public void sync() {
        if (this.provider instanceof ServerPlayer serverPlayer) {
            CompoundTag tag = this.serializeNBT(serverPlayer.registryAccess());
            PacketDistributor.sendToPlayer(serverPlayer, new SyncAspectsPacket(tag));
        }
    }

    @Override
    @SuppressWarnings("null")
    public CompoundTag serializeNBT(@javax.annotation.Nonnull HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (HeartAspect heart : aspects) {
            list.add(heart.toNbt());
        }
        tag.put("Aspects", list);
        tag.putInt("RechargeBoostTicks", this.rechargeBoostTicks);
        tag.putInt("FractionalRechargeTicks", this.fractionalRechargeTicks);
        return tag;
    }

    @Override
    public void deserializeNBT(@javax.annotation.Nonnull HolderLookup.Provider provider, @javax.annotation.Nonnull CompoundTag tag) {
        this.aspects.clear();
        ListTag list = tag.getList("Aspects", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag compound = list.getCompound(i);
            String typeString = compound.getString("Type");
            if (typeString.isEmpty()) continue;
            ResourceLocation id = ResourceLocation.tryParse(typeString);
            if (id == null) continue;
            HeartAspect aspect = HeartAspectRegistry.forId(id, this.provider);
            if (aspect == null) continue;
            aspect.readNbt(compound);
            this.aspects.add(aspect);
        }
        this.rechargeBoostTicks = tag.getInt("RechargeBoostTicks");
        // 兼容旧存档：优先读取新的整数键，旧版 float 键不存在时 getInt 返回 0
        this.fractionalRechargeTicks = tag.contains("FractionalRechargeTicks", Tag.TAG_INT)
            ? tag.getInt("FractionalRechargeTicks")
            : 0;
    }
}

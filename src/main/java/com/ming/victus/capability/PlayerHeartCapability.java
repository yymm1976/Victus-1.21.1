package com.ming.victus.capability;

import com.ming.victus.hearts.HeartAspect;
import com.ming.victus.hearts.HeartAspectRegistry;
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
    private float fractionalRecharge = 0.0F;

    public PlayerHeartCapability(Player provider) {
        this.provider = provider;
    }

    public void addRechargeBoostTicks(int ticks) {
        this.rechargeBoostTicks += ticks;
        sync();
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
        sync();
        return true;
    }

    public HeartAspect removeAspect() {
        if (this.aspects.isEmpty()) return null;
        HeartAspect removedAspect = this.aspects.remove(this.aspects.size() - 1);
        sync();
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
                this.fractionalRecharge += 0.25F;
                if (this.fractionalRecharge >= 1.0F) {
                    tickAmount += (int) this.fractionalRecharge;
                    this.fractionalRecharge -= (int) this.fractionalRecharge;
                }
            }
            for (int t = 0; t < tickAmount; t++) {
                targetAspect.tick();
            }
            
            // If the target aspect finished charging during this tick, clear the remaining boost ticks
            if (targetAspect.active()) {
                this.rechargeBoostTicks = 0;
                this.fractionalRecharge = 0.0F;
                sync();
            }
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
        
        float healthAfterThisHeart = index * 2.0F;
        float currentHealth = originalHealth - damage;
        
        int nextIndex = index - 1;
        if (nextIndex >= 0 && currentHealth <= healthAfterThisHeart) {
            damageAspect(nextIndex, source, damage, originalHealth);
        }

        boolean callClient = aspect.onBroken(source, damage, originalHealth);
        sync();
        if (this.provider instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new com.ming.victus.network.AspectBrokenPacket(index, callClient));
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
            PacketDistributor.sendToPlayer(serverPlayer, new com.ming.victus.network.SyncAspectsPacket(tag));
        }
    }

    @Override
    @SuppressWarnings("null")
    public CompoundTag serializeNBT(@javax.annotation.Nonnull net.minecraft.core.HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (HeartAspect heart : aspects) {
            list.add(heart.toNbt());
        }
        tag.put("Aspects", list);
        return tag;
    }

    @Override
    public void deserializeNBT(@javax.annotation.Nonnull net.minecraft.core.HolderLookup.Provider provider, @javax.annotation.Nonnull CompoundTag tag) {
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
    }
}

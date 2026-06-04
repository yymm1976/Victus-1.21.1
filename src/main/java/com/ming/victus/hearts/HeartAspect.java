package com.ming.victus.hearts;

import com.ming.victus.VictusMain;
import com.ming.victus.capability.VictusAttachments;
import com.ming.victus.item.HeartAspectItem;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Function;
import java.util.function.Predicate;

public class HeartAspect {
    public static final Predicate<HeartAspect> IS_ACTIVE = HeartAspect::active;
    public static final Predicate<HeartAspect> IS_NOT_ACTIVE = heartAspect -> !heartAspect.active();

    public static final ResourceLocation HEART_ATLAS_TEXTURE = ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "textures/gui/hearts.png");

    protected static final Predicate<Player> NEVER_UPDATE = p -> false;
    protected static final Predicate<Player> ALWAYS_UPDATE = p -> true;

    protected final Player player;
    private final Type type;
    private int cooldown;

    public HeartAspect(Player player, Type type) {
        this.player = player;
        this.type = type;
        this.cooldown = getRechargeDuration();
    }

    @SuppressWarnings("null")
    public final CompoundTag toNbt() {
        CompoundTag containerNbt = new CompoundTag();
        containerNbt.putString("Type", this.type.id().toString());
        containerNbt.putInt("Cooldown", this.cooldown);
        writeCustomData(containerNbt);
        return containerNbt;
    }

    public final void readNbt(CompoundTag nbt) {
        this.cooldown = nbt.getInt("Cooldown");
        readCustomData(nbt);
    }

    protected void readCustomData(CompoundTag nbt) {}

    protected void writeCustomData(CompoundTag nbt) {}

    public void tick() {
        if (this.cooldown > -1) this.cooldown -= 1;
        if (this.cooldown < -1) {
            this.cooldown = -1;
        } else if (active() && this.type.tickUpdateCondition().test(this.player)) {
            update();
        }
    }

    protected void update() {}

    protected static Predicate<Player> belowHealth(float health) {
        return p -> (p.getHealth() <= health);
    }

    protected static Predicate<Player> belowHealthPercentage(float percentage) {
        return p -> (p.getHealth() <= p.getMaxHealth() * percentage);
    }

    @OnlyIn(Dist.CLIENT)
    public final void onBrokenClient(boolean callHandler) {
        if (active() && callHandler) handleBreakClient();
        this.cooldown = getRechargeDuration();
    }

    @OnlyIn(Dist.CLIENT)
    public static Runnable createBreakEvent(Minecraft client, int index, boolean callHandler) {
        return () -> {
            if (client.player != null) {
                var cap = VictusAttachments.getHearts(client.player);
                if (cap != null) {
                    var aspect = cap.getAspect(index);
                    if (aspect != null) {
                        aspect.onBrokenClient(callHandler);
                    }
                }
            }
        };
    }

    public final boolean onBroken(DamageSource source, float damage, float originalHealth) {
        boolean shouldCallClient = false;
        if (active()) shouldCallClient = handleBreak(source, damage, originalHealth);

        this.cooldown = getRechargeDuration();
        return shouldCallClient;
    }

    public final boolean active() {
        return (this.cooldown == -1);
    }

    public final void rechargeByPercentage(float percentage) {
        if (active()) return;
        this.cooldown = (int) (this.cooldown - getRechargeDuration() * percentage);
        if (this.cooldown < -1) this.cooldown = -1;
    }

    public final float getRechargeProgress() {
        return (this.cooldown != -1) ? ((float) (getRechargeDuration() - this.cooldown) / getRechargeDuration()) : 1.0F;
    }

    public int getCooldown() {
        return this.cooldown;
    }

    public int getRechargeDuration() {
        return this.type.standardRechargeDuration();
    }

    public ResourceLocation getAtlas() {
        return HEART_ATLAS_TEXTURE;
    }

    public final Type getType() {
        return this.type;
    }

    public boolean handleBreak(DamageSource source, float damage, float originalHealth) {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    protected void handleBreakClient() {}

    public int getTextureIndex() {
        return this.type.textureIndex();
    }

    public Item asItem() {
        Item item = HeartAspectItem.getItem(this.type);
        if (item == null) {
            return net.minecraft.world.item.Items.AIR;
        }
        return item;
    }

    @SuppressWarnings("null")
    public net.minecraft.network.chat.Component getName() {
        return net.minecraft.network.chat.Component.translatable(this.type.translationKey());
    }

    public record Type(ResourceLocation id, int textureIndex, int standardRechargeDuration, int color, Predicate<Player> tickUpdateCondition, Function<Player, HeartAspect> factory) {
        @SuppressWarnings("null")
        public String translationKey() {
            return "item." + id.getNamespace() + "." + id.getPath();
        }
    }
}

package com.ming.victus.hearts;

import com.ming.victus.VictusMain;
import com.ming.victus.capability.VictusAttachments;
import com.ming.victus.item.HeartAspectItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Function;
import java.util.function.Predicate;

public class HeartAspect {
    public static final Predicate<HeartAspect> IS_ACTIVE = HeartAspect::active;
    public static final Predicate<HeartAspect> IS_NOT_ACTIVE = heartAspect -> !heartAspect.active();

    public static final ResourceLocation HEART_ATLAS_TEXTURE = ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "textures/gui/hearts.png");
    /** 心形纹理图集的尺寸（像素），当前支持 8×8 = 64 个纹理槽位 */
    public static final int ATLAS_SIZE = 64;

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
        // 使用独立 if 而非 else-if，确保 cooldown 递减到 -1 的同一 tick 也能执行 update
        if (this.cooldown < -1) {
            this.cooldown = -1;
        }
        // 客户端只推进本地 cooldown（保证 HUD 进度条平滑显示）；
        // update() 只在服务端执行，避免客户端通过 OceanAspect.update() 等路径
        // 误改本地 capability（cooldown 跳跃、本地"破碎"事件、damageAspect 等副作用），
        // 服务端为权威方，关键节点（破碎/加速）通过 SyncAspectsPacket 纠偏。
        if (active()
            && !this.player.level().isClientSide()
            && this.type.tickUpdateCondition().test(this.player)) {
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

    /**
     * 客户端接收到服务端权威的 AspectBrokenPacket 后触发。
     *
     * <p>不再依赖 {@link #active()} 判断：服务端为权威方，可能已先通过
     * {@link com.ming.victus.network.SyncAspectsPacket} 把心相 cooldown 置为
     * 充能状态，若此时仍用 {@code active() && callHandler} 判断，客户端会因为
     * active() == false 而跳过 {@link #handleBreakClient()}，导致破碎动画/音效丢失。</p>
     *
     * <p>只要服务端指示 {@code callHandler == true}，客户端就应当播放一次客户端特效；
     * cooldown 重置放在最后，确保即使服务端已将状态置为充能，本方法仍保持幂等。</p>
     */
    @OnlyIn(Dist.CLIENT)
    public final void onBrokenClient(boolean callHandler) {
        if (callHandler) handleBreakClient();
        this.cooldown = getRechargeDuration();
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
            return Items.AIR;
        }
        return item;
    }

    @SuppressWarnings("null")
    public Component getName() {
        return Component.translatable(this.type.translationKey());
    }

    public record Type(ResourceLocation id, int textureIndex, int standardRechargeDuration, int color, Predicate<Player> tickUpdateCondition, Function<Player, HeartAspect> factory) {
        /**
         * 翻译键拼接策略：lang 文件实际 key 为 {@code item.victus.<path>_heart_aspect}
         * （与 Type id 的短名 {@code victus:grilled} 形式不一致），故在 path 末追加
         * {@code _heart_aspect} 后缀；若 path 已以 {@code _heart_aspect} 结尾则不重复追加。
         *
         * <p>NBT 兼容性：id（ResourceLocation）保持原短名，存档中序列化形式不变；
         * 仅翻译键在显示时追加后缀。</p>
         */
        @SuppressWarnings("null")
        public String translationKey() {
            String path = id.getPath();
            if (!path.endsWith("_heart_aspect")) {
                path = path + "_heart_aspect";
            }
            return "item." + id.getNamespace() + "." + path;
        }
    }
}

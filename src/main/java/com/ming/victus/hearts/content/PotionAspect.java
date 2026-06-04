package com.ming.victus.hearts.content;

import com.ming.victus.VictusMain;
import com.ming.victus.hearts.HeartAspect;
import com.ming.victus.hearts.OverlaySpriteProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PotionAspect extends HeartAspect implements OverlaySpriteProvider {
    public static final HeartAspect.Type TYPE = new HeartAspect.Type(
        ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "potion"),
        6, 50, 0xE95EF1, HeartAspect.ALWAYS_UPDATE, PotionAspect::new
    );

    private Optional<Holder<Potion>> potion = Optional.empty();
    private final List<MobEffectInstance> customEffects = new ArrayList<>();

    public PotionAspect(Player player) {
        super(player, TYPE);
    }

    public Optional<Holder<Potion>> getPotion() {
        return this.potion;
    }

    public void setPotion(Optional<Holder<Potion>> potion) {
        this.potion = potion;
    }

    public List<MobEffectInstance> getCustomEffects() {
        return this.customEffects;
    }

    @Override
    @SuppressWarnings("null")
    public boolean handleBreak(DamageSource source, float damage, float originalHealth) {
        // 施加基础药水类型的效果
        if (this.potion.isPresent()) {
            for (MobEffectInstance instance : this.potion.get().value().getEffects()) {
                applyEffect(instance);
            }
        }
        // 施加自定义效果（来自模组药水或命令添加的额外效果）
        for (MobEffectInstance instance : this.customEffects) {
            applyEffect(instance);
        }
        return false;
    }

    private void applyEffect(MobEffectInstance instance) {
        if (instance.getEffect().value().isInstantenous()) {
            instance.getEffect().value().applyInstantenousEffect(this.player, this.player, this.player, instance.getAmplifier(), 1.0D);
        } else {
            this.player.addEffect(new MobEffectInstance(instance));
        }
    }

    @Override
    @SuppressWarnings("null")
    protected void readCustomData(CompoundTag nbt) {
        ResourceLocation id = ResourceLocation.tryParse(nbt.getString("Potion"));
        if (id != null && !id.getPath().equals("empty")) {
            this.potion = BuiltInRegistries.POTION.getHolder(id).map(h -> h);
        } else {
            this.potion = Optional.empty();
        }
        // 读取自定义效果列表
        this.customEffects.clear();
        if (nbt.contains("CustomEffects", Tag.TAG_LIST)) {
            ListTag list = nbt.getList("CustomEffects", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag effectTag = list.getCompound(i);
                MobEffectInstance effect = MobEffectInstance.load(effectTag);
                if (effect != null) {
                    this.customEffects.add(effect);
                }
            }
        }
    }

    @Override
    @SuppressWarnings("null")
    protected void writeCustomData(CompoundTag nbt) {
        nbt.putString("Potion", this.potion.flatMap(Holder::unwrapKey).map(key -> key.location().toString()).orElse("minecraft:empty"));
        // 写入自定义效果列表
        if (!this.customEffects.isEmpty()) {
            ListTag list = new ListTag();
            for (MobEffectInstance effect : this.customEffects) {
                // 1.21.x 中 save() 不再接受参数，直接返回 CompoundTag
                list.add(effect.save());
            }
            nbt.put("CustomEffects", list);
        }
    }

    @Override
    public int getOverlayIndex() {
        return 8;
    }

    @Override
    @SuppressWarnings("null")
    public int getOverlayTint() {
        if (this.potion.isEmpty()) return 0xFFFFFF;
        return PotionContents.getColor(this.potion.get().value().getEffects());
    }
}

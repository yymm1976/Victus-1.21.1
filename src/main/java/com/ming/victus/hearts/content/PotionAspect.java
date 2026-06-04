package com.ming.victus.hearts.content;

import com.ming.victus.VictusMain;
import com.ming.victus.hearts.HeartAspect;
import com.ming.victus.hearts.OverlaySpriteProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.Optional;

public class PotionAspect extends HeartAspect implements OverlaySpriteProvider {
    public static final HeartAspect.Type TYPE = new HeartAspect.Type(
        ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "potion"),
        6, 50, 0xE95EF1, HeartAspect.ALWAYS_UPDATE, PotionAspect::new
    );

    private Optional<Holder<Potion>> potion = Optional.empty();
    private final java.util.List<MobEffectInstance> customEffects = new java.util.ArrayList<>();

    public PotionAspect(Player player) {
        super(player, TYPE);
    }

    public Optional<Holder<Potion>> getPotion() {
        return this.potion;
    }

    public void setPotion(Optional<Holder<Potion>> potion) {
        this.potion = potion;
    }

    public java.util.List<MobEffectInstance> getCustomEffects() {
        return this.customEffects;
    }

    @Override
    @SuppressWarnings("null")
    public boolean handleBreak(DamageSource source, float damage, float originalHealth) {
        if (this.potion.isPresent()) {
            for (MobEffectInstance instance : this.potion.get().value().getEffects()) {
                if (instance.getEffect().value().isInstantenous()) {
                    instance.getEffect().value().applyInstantenousEffect(this.player, this.player, this.player, instance.getAmplifier(), 1.0D);
                } else {
                    this.player.addEffect(new MobEffectInstance(instance));
                }
            }
        }
        return false;
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
    }

    @Override
    @SuppressWarnings("null")
    protected void writeCustomData(CompoundTag nbt) {
        nbt.putString("Potion", this.potion.flatMap(Holder::unwrapKey).map(key -> key.location().toString()).orElse("minecraft:empty"));
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

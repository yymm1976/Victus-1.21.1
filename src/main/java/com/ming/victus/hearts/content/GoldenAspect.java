package com.ming.victus.hearts.content;

import com.ming.victus.VictusMain;
import com.ming.victus.capability.PlayerHeartCapability;
import com.ming.victus.capability.VictusAttachments;
import com.ming.victus.hearts.HeartAspect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class GoldenAspect extends HeartAspect {
    public static final HeartAspect.Type TYPE = new HeartAspect.Type(
        ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "golden"),
        16, 100, 0xFFF9FB, HeartAspect.NEVER_UPDATE, GoldenAspect::new
    );

    public GoldenAspect(Player player) {
        super(player, TYPE);
    }

    @Override
    @SuppressWarnings("null")
    public boolean handleBreak(DamageSource source, float damage, float originalHealth) {
        PlayerHeartCapability aspects = VictusAttachments.getHearts(this.player);
        if (aspects == null) return false;

        int index = findIndex(aspects);
        if (index == -1) return false;

        float percentage = 1.0F - (index + 0.0F) / (this.player.getMaxHealth() / 2.0F);
        int level = Math.max(0, Math.round(percentage * 5.0F));

        this.player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, level));

        return false;
    }

    private int findIndex(PlayerHeartCapability component) {
        for (int i = 0; i < component.effectiveSize(); i++) {
            if (component.getAspect(i) == this) return i;
        }
        return -1;
    }
}

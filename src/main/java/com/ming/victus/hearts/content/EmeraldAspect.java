package com.ming.victus.hearts.content;

import com.ming.victus.VictusMain;
import com.ming.victus.capability.PlayerHeartCapability;
import com.ming.victus.capability.VictusAttachments;
import com.ming.victus.hearts.HeartAspect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class EmeraldAspect extends HeartAspect {
    public static final HeartAspect.Type TYPE = new HeartAspect.Type(
        ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "emerald"),
        18, 40, 0x07D069, HeartAspect.NEVER_UPDATE, EmeraldAspect::new
    );

    public EmeraldAspect(Player player) {
        super(player, TYPE);
    }

    @Override
    public boolean handleBreak(DamageSource source, float damage, float originalHealth) {
        PlayerHeartCapability cap = VictusAttachments.getHearts(this.player);
        if (cap != null) {
            int totalRemaining = 0;
            for (HeartAspect aspect : cap.getEquippedHearts()) {
                if (!aspect.active()) {
                    totalRemaining += aspect.getCooldown();
                }
            }
            cap.addRechargeBoostTicks(totalRemaining);
        }
        return true;
    }
}

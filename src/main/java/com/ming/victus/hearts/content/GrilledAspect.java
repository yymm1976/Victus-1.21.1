package com.ming.victus.hearts.content;

import com.ming.victus.VictusMain;
import com.ming.victus.hearts.HeartAspect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

public class GrilledAspect extends HeartAspect {
    public static final HeartAspect.Type TYPE = new HeartAspect.Type(
        ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "grilled"),
        3, 60, 0x71403B, HeartAspect.NEVER_UPDATE, GrilledAspect::new
    );

    public GrilledAspect(Player player) {
        super(player, TYPE);
    }

    @Override
    @SuppressWarnings("null")
    public boolean handleBreak(DamageSource source, float damage, float originalHealth) {
        this.player.getFoodData().eat(8, 0.8F);
        return false;
    }
}

package com.ming.victus.hearts.content;

import com.ming.victus.VictusMain;
import com.ming.victus.hearts.HeartAspect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

public class LapisAspect extends HeartAspect {
    public static final HeartAspect.Type TYPE = new HeartAspect.Type(
        ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "lapis"),
        15, 20, 0x0064B8, HeartAspect.NEVER_UPDATE, LapisAspect::new
    );

    public LapisAspect(Player player) {
        super(player, TYPE);
    }

    @Override
    public boolean handleBreak(DamageSource source, float damage, float originalHealth) {
        // Functionality is handled in PlayerXpEvent.PickupXp
        return false;
    }
}

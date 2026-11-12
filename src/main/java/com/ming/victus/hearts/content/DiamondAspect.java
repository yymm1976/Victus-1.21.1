package com.ming.victus.hearts.content;

import com.ming.victus.VictusMain;
import com.ming.victus.hearts.HeartAspect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class DiamondAspect extends HeartAspect {
    @SuppressWarnings("null")
    public static final HeartAspect.Type TYPE = new HeartAspect.Type(
        ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "diamond"),
        1, 50, 0x00D4C9, HeartAspect.NEVER_UPDATE, DiamondAspect::new
    );

    @SuppressWarnings("null")
    public static final ResourceLocation ARMOR_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "diamond_armor");
    @SuppressWarnings("null")
    public static final ResourceLocation TOUGHNESS_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "diamond_toughness");

    public DiamondAspect(Player player) {
        super(player, TYPE);
    }
}

package com.ming.victus.hearts.content;

import com.ming.victus.VictusMain;
import com.ming.victus.capability.VictusAttachments;
import com.ming.victus.hearts.HeartAspect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;

public class IronAspect extends HeartAspect {
    public static final HeartAspect.Type TYPE = new HeartAspect.Type(
        ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "iron"),
        17, 300, 0xB5C2F2, HeartAspect.NEVER_UPDATE, IronAspect::new
    );

    public IronAspect(Player player) {
        super(player, TYPE);
    }

    @Override
    @SuppressWarnings("null")
    public boolean handleBreak(DamageSource source, float damage, float originalHealth) {
        IronGolem golem = EntityType.IRON_GOLEM.create(this.player.level());
        if (golem != null) {
            golem.setPlayerCreated(true);
            golem.setData(VictusAttachments.NO_DROPS, true);
            golem.getPersistentData().putUUID("VictusOwner", this.player.getUUID());
            golem.moveTo(this.player.getX(), this.player.getY(), this.player.getZ(), 0.0F, 0.0F);
            this.player.level().addFreshEntity(golem);
        }
        return false;
    }
}

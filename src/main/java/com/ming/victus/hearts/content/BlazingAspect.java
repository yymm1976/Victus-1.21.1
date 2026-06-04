package com.ming.victus.hearts.content;

import com.ming.victus.VictusMain;
import com.ming.victus.hearts.HeartAspect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class BlazingAspect extends HeartAspect {
    public static final HeartAspect.Type TYPE = new HeartAspect.Type(
        ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "blazing"),
        9, 50, 0xFD4A2C, HeartAspect.NEVER_UPDATE, BlazingAspect::new
    );

    public BlazingAspect(Player player) {
        super(player, TYPE);
    }

    @Override
    @SuppressWarnings("null")
    public boolean handleBreak(DamageSource source, float damage, float originalHealth) {
        // TODO: VictusParticleEvents.BLAZING_FLAMES.spawn(this.player.level(), this.player.position());

        @SuppressWarnings("null")
        AABB box = this.player.getBoundingBox().inflate(4.0D);
        List<LivingEntity> entities = this.player.level().getEntitiesOfClass(LivingEntity.class, box, entity -> {
            if (entity == this.player) return false;
            // Simplified check: ignore tamed entities of this player
            if (entity instanceof net.minecraft.world.entity.TamableAnimal tamable) {
                return !tamable.isOwnedBy(this.player);
            }
            return true;
        });

        for (int i = 0; i < 4; i++) {
            if (entities.isEmpty()) return false;
            LivingEntity target = entities.remove(this.player.getRandom().nextInt(entities.size()));
            target.hurt(this.player.damageSources().inFire(), 3.0F);
            target.igniteForSeconds(4.0F);
        }

        return false;
    }
}

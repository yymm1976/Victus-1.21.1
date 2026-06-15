package com.ming.victus.hearts.content;

import com.ming.victus.VictusMain;
import com.ming.victus.hearts.HeartAspect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class CreeperAspect extends HeartAspect {
    public static final HeartAspect.Type TYPE = new HeartAspect.Type(
        ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "creeper"),
        6, 200, 0x53BCAE, HeartAspect.NEVER_UPDATE, CreeperAspect::new
    );

    public CreeperAspect(Player player) {
        super(player, TYPE);
    }

    @Override
    @SuppressWarnings("null")
    public boolean handleBreak(DamageSource source, float damage, float originalHealth) {
        if (!this.player.level().isClientSide()) {
            this.player.level().explode(
                this.player,
                this.player.damageSources().explosion(this.player, null),
                new ExplosionDamageCalculator() {
                    @Override
                    public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
                        if (entity == CreeperAspect.this.player) return false;
                        // 排除玩家驯服的动物
                        if (entity instanceof net.minecraft.world.entity.TamableAnimal tamable) {
                            return !tamable.isOwnedBy(CreeperAspect.this.player);
                        }
                        return true;
                    }
                },
                this.player.getX(), this.player.getY(), this.player.getZ(),
                5.0F, false, Level.ExplosionInteraction.NONE
            );
        }
        return false;
    }
}

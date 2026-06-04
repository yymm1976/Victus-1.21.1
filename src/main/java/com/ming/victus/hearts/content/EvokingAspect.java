package com.ming.victus.hearts.content;

import com.ming.victus.VictusMain;
import com.ming.victus.hearts.HeartAspect;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

public class EvokingAspect extends HeartAspect {
    public static final HeartAspect.Type TYPE = new HeartAspect.Type(
        ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "evoking"),
        12, 100, 0x4853DF, HeartAspect.NEVER_UPDATE, EvokingAspect::new
    );

    public EvokingAspect(Player player) {
        super(player, TYPE);
    }

    @Override
    @SuppressWarnings("null")
    public boolean handleBreak(DamageSource source, float damage, float originalHealth) {
        for (int i = 0; i < 3; i++) {
            Vex vex = new Vex(EntityType.VEX, this.player.level());
            
            vex.getPersistentData().putUUID("VictusOwner", this.player.getUUID());
            
            net.minecraft.world.item.ItemStack sword = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
            var sharpness = this.player.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolder(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS);
            sharpness.ifPresent(enchantment -> sword.enchant(enchantment, 5));
            vex.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, sword);
            vex.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0f);

            if (source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker) {
                vex.setTarget(attacker);
            }

            Vec3 vexPos = this.player.position().add(
                (this.player.getRandom().nextDouble() - 0.5) * 4.0, 
                2.0, 
                (this.player.getRandom().nextDouble() - 0.5) * 4.0
            );
            vex.moveTo(vexPos.x, vexPos.y, vexPos.z, 0.0F, 0.0F);

            if (this.player.level() instanceof ServerLevelAccessor serverLevel) {
                vex.finalizeSpawn(serverLevel, this.player.level().getCurrentDifficultyAt(BlockPos.containing(vexPos)), MobSpawnType.EVENT, null);
            }
            vex.setLimitedLife(250);

            this.player.level().addFreshEntity(vex);
            vex.playAmbientSound();
        }

        this.player.level().playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(),
            SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, 1.0F);

        return false;
    }
}

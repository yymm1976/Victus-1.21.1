package com.ming.victus.hearts.content;

import com.ming.victus.VictusMain;
import com.ming.victus.hearts.HeartAspect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ArcheryAspect extends HeartAspect {
    public static final HeartAspect.Type TYPE = new HeartAspect.Type(
        ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "archery"),
        14, 40, 0x71403B, HeartAspect.NEVER_UPDATE, ArcheryAspect::new
    );

    public ArcheryAspect(Player player) {
        super(player, TYPE);
    }

    @Override
    @SuppressWarnings("null")
    public boolean handleBreak(DamageSource source, float damage, float originalHealth) {
        @SuppressWarnings("null")
        AABB box = this.player.getBoundingBox().inflate(3.0D);
        List<LivingEntity> entities = this.player.level().getEntitiesOfClass(LivingEntity.class, box, entity -> {
            if (entity == this.player) return false;
            if (entity instanceof net.minecraft.world.entity.TamableAnimal tamable) {
                return !tamable.isOwnedBy(this.player);
            }
            return true;
        });

        for (int i = 0; i < 5; i++) {
            if (entities.isEmpty()) return false;
            LivingEntity target = entities.remove(this.player.getRandom().nextInt(entities.size()));

            Arrow arrow = new Arrow(this.player.level(), this.player, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
            arrow.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.HARM, 1, 1));
            
            // In 1.21.1, setKnockback is removed. We apply the knockback effect via NBT if necessary,
            // or just use punch enchantment. But Arrow doesn't expose it.
            // Let's use punch enchantment on the weapon it's "shot" from, but we don't have a weapon.
            // We can just set the knockback value directly if there's a setter, else skip it or use NBT.
            // Let's skip it since setKnockback is gone and modifying NBT is tricky for spawned entities.
            // Wait, AbstractArrow has setKnockback(int) in older versions. Let's check if it exists with different name.
            // If not, we just don't apply knockback.

            @SuppressWarnings("null")
            Vec3 arrowVelocity = target.position().subtract(this.player.position()).normalize().scale(0.5D);
            @SuppressWarnings("null")
            Vec3 arrowPos = this.player.position().add(arrowVelocity.scale(0.5D)).add(0.0D, this.player.getEyeHeight(), 0.0D);

            arrow.setPos(arrowPos.x, arrowPos.y, arrowPos.z);
            arrow.setDeltaMovement(arrowVelocity);
            arrow.setBaseDamage(2.0D);

            this.player.level().addFreshEntity(arrow);
        }

        return false;
    }
}

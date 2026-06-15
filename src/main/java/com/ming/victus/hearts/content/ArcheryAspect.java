package com.ming.victus.hearts.content;

import com.ming.victus.VictusMain;
import com.ming.victus.hearts.HeartAspect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
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
        // 包装为可变列表，防止 getEntitiesOfClass 返回不可变列表导致 UnsupportedOperationException
        List<LivingEntity> entities = new ArrayList<>(this.player.level().getEntitiesOfClass(LivingEntity.class, box, entity -> {
            if (entity == this.player) return false;
            if (entity instanceof TamableAnimal tamable) {
                return !tamable.isOwnedBy(this.player);
            }
            return true;
        }));

        for (int i = 0; i < 5; i++) {
            if (entities.isEmpty()) return false;
            LivingEntity target = entities.remove(this.player.getRandom().nextInt(entities.size()));

            Arrow arrow = new Arrow(this.player.level(), this.player, new ItemStack(Items.ARROW), null);
            arrow.addEffect(new MobEffectInstance(MobEffects.HARM, 1, 1));

            @SuppressWarnings("null")
            Vec3 direction = target.position().subtract(this.player.position());
            // 零向量防御：目标与玩家位置重合时 normalize() 会产生无效方向，
            // 改用玩家视线方向作为 fallback
            if (direction.lengthSqr() < 0.0001D) {
                direction = this.player.getLookAngle();
            }
            Vec3 arrowVelocity = direction.normalize().scale(0.5D);
            @SuppressWarnings("null")
            Vec3 arrowPos = this.player.position().add(arrowVelocity.scale(0.5D)).add(0.0D, this.player.getEyeHeight(), 0.0D);

            arrow.setPos(arrowPos.x, arrowPos.y, arrowPos.z);
            arrow.setDeltaMovement(arrowVelocity);
            arrow.setBaseDamage(2.0D);
            // 通过增大箭矢速度模拟击退效果（1.21.1 中 setKnockback 已移除）
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(1.5D));

            this.player.level().addFreshEntity(arrow);
        }

        return false;
    }
}

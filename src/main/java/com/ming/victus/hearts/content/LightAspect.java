package com.ming.victus.hearts.content;

import com.ming.victus.VictusMain;
import com.ming.victus.hearts.HeartAspect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class LightAspect extends HeartAspect {
    public static final HeartAspect.Type TYPE = new HeartAspect.Type(
        ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "light"),
        5, 1200, 0xFFFFFF, HeartAspect.NEVER_UPDATE, LightAspect::new
    );

    public LightAspect(Player player) {
        super(player, TYPE);
    }

    @Override
    @SuppressWarnings("null")
    public boolean handleBreak(DamageSource source, float damage, float originalHealth) {
        this.player.level().playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 2.0F);
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("null")
    protected void handleBreakClient() {
        net.minecraft.client.Minecraft.getInstance().particleEngine.createTrackingEmitter(this.player, ParticleTypes.CLOUD, 10);
        for (int i = 0; i < 40; i++) {
            net.minecraft.client.Minecraft.getInstance().level.addParticle(
                ParticleTypes.CLOUD,
                this.player.getRandomX(1.0D), this.player.getRandomY() + 1.0D, this.player.getRandomZ(1.0D),
                0.0D, 0.1D, 0.0D
            );
        }
    }
}

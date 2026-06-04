package com.ming.victus.hearts.content;

import com.ming.victus.VictusMain;
import com.ming.victus.hearts.HeartAspect;
import com.ming.victus.item.VictusItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class TotemAspect extends HeartAspect {
    public static final HeartAspect.Type TYPE = new HeartAspect.Type(
        ResourceLocation.fromNamespaceAndPath(VictusMain.MOD_ID, "totem"),
        0, 3000, 0xFFD2AD, HeartAspect.NEVER_UPDATE, TotemAspect::new
    );

    private boolean hadTotem = false;

    public TotemAspect(Player player) {
        super(player, TYPE);
    }

    @Override
    @SuppressWarnings("null")
    public boolean handleBreak(DamageSource source, float damage, float originalHealth) {
        Inventory inventory = this.player.getInventory();

        int removed = inventory.clearOrCountMatchingItems(stack -> stack.is(Items.TOTEM_OF_UNDYING), 1, this.player.inventoryMenu.getCraftSlots());
        if (removed > 0) {
            this.player.setHealth(this.player.getHealth() + 15.0F);
            this.hadTotem = true;
        } else {
            this.player.setHealth(this.player.getHealth() + 5.0F);
            this.hadTotem = false;
        }

        this.player.level().playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(),
            SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);

        return true;
    }

    @Override
    public int getRechargeDuration() {
        return this.hadTotem ? 40 : getType().standardRechargeDuration();
    }

    @Override
    protected void readCustomData(CompoundTag nbt) {
        this.hadTotem = nbt.getBoolean("HadTotem");
    }

    @Override
    protected void writeCustomData(CompoundTag nbt) {
        nbt.putBoolean("HadTotem", this.hadTotem);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("null")
    protected void handleBreakClient() {
        Minecraft.getInstance().gameRenderer.displayItemActivation(new ItemStack(VictusItems.TOTEM_HEART_ASPECT.get()));
        Minecraft.getInstance().particleEngine.createTrackingEmitter(this.player, ParticleTypes.TOTEM_OF_UNDYING, 30);
    }
}

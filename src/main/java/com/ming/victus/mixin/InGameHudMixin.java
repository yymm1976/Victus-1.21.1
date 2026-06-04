package com.ming.victus.mixin;

import com.ming.victus.capability.PlayerHeartCapability;
import com.ming.victus.capability.VictusAttachments;
import com.ming.victus.client.VictusHudRenderer;
import com.ming.victus.hearts.HeartAspect;
import com.ming.victus.hearts.OverlaySpriteProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.gui.Gui.class)
public abstract class InGameHudMixin {

    @Shadow @Final protected Minecraft minecraft;

    @Unique
    private int heartX;
    @Unique
    private int heartY;
    @Unique
    private int heartIndex;
    @Unique
    private PlayerHeartCapability aspectComponent = null;

    @Inject(method = "renderHealthLevel", at = @At("HEAD"))
    private void storeAspectComponent(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (this.minecraft.player != null) {
            this.aspectComponent = VictusAttachments.getHearts(this.minecraft.player);
        }
        this.heartIndex = 0; // Reset index before rendering
    }

    @Inject(method = "renderHealthLevel", at = @At("RETURN"))
    private void releaseAspectComponent(GuiGraphics guiGraphics, CallbackInfo ci) {
        this.aspectComponent = null;
    }

    @SuppressWarnings("null")
    @Inject(method = "renderHeart", at = @At("HEAD"))
    private void captureHeartRender(GuiGraphics guiGraphics, net.minecraft.client.gui.Gui.HeartType type, int x, int y, boolean hardcore, boolean blinking, boolean halfHeart, CallbackInfo ci) {
        if (this.aspectComponent == null || this.minecraft.level.getLevelData().isHardcore()) return;

        // The renderHeart method is called for each heart. 
        // We can track the index using heartIndex.
        // Wait, renderHeart is called multiple times per heart (background, then foreground).
        // Let's just use heartIndex to keep track.
    }

    @SuppressWarnings("null")
    @Inject(method = "renderHearts", at = @At("TAIL"))
    private void renderCustomHearts(GuiGraphics guiGraphics, Player player, int x, int y, int height, int offsetHeartIndex, float maxHealth, int currentHealth, int displayHealth, int absorptionAmount, boolean renderHighlight, CallbackInfo ci) {
        if (this.aspectComponent == null || this.minecraft.level.getLevelData().isHardcore()) return;

        int healthHearts = net.minecraft.util.Mth.ceil(maxHealth / 2.0F);
        int absorptionHearts = net.minecraft.util.Mth.ceil(absorptionAmount / 2.0F);
        int totalHearts = healthHearts + absorptionHearts;

        // Add 1 extra heart rendering pass if expectedExtraHealth requires it, 
        // but here we just render what's in the capability.
        int effectiveHearts = this.aspectComponent.effectiveSize();
        if (effectiveHearts > healthHearts) {
            healthHearts = effectiveHearts;
            totalHearts = healthHearts + absorptionHearts;
        }

        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create();
        random.setSeed((long)(this.minecraft.gui.getGuiTicks() * 312871));

        int healthForShake = net.minecraft.util.Mth.ceil(Math.max((float)currentHealth, (float)displayHealth));

        for (int i = totalHearts - 1; i >= healthHearts; --i) {
            if (healthForShake <= 4) {
                random.nextInt(2);
            }
        }

        for (int i = healthHearts - 1; i >= 0; --i) {
            HeartAspect aspect = this.aspectComponent.getAspect(i);

            int heartX = x + i % 10 * 8;
            int heartY = y - (i / 10) * height;
            
            if (healthForShake <= 4) {
                heartY += random.nextInt(2);
            }

            if (i == offsetHeartIndex) {
                heartY -= 2;
            }

            if (aspect == null) continue;

            boolean hasHealth = false;
            boolean isHalf = false;
            if (renderHighlight && i * 2 + 1 < displayHealth) {
                hasHealth = true;
            } else if (i * 2 + 1 < currentHealth) {
                hasHealth = true;
            } else if (i * 2 + 1 == currentHealth) {
                hasHealth = true;
                isHalf = true;
            }

            if (hasHealth) {
                VictusHudRenderer.renderAspect(guiGraphics, heartX, heartY, aspect.getTextureIndex(), aspect.getRechargeProgress(), isHalf);

                if (aspect instanceof OverlaySpriteProvider overlayProvider && overlayProvider.shouldRenderOverlay()) {
                    com.mojang.blaze3d.systems.RenderSystem.setShaderColor(
                        VictusHudRenderer.getComponent(overlayProvider.getOverlayTint(), 16),
                        VictusHudRenderer.getComponent(overlayProvider.getOverlayTint(), 8),
                        VictusHudRenderer.getComponent(overlayProvider.getOverlayTint(), 0),
                        1f
                    );
                    
                    VictusHudRenderer.renderAspect(guiGraphics, heartX, heartY, overlayProvider.getOverlayIndex(), aspect.getRechargeProgress(), isHalf);
                    com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                }
            }
        }
    }
    // Forced modification for gradle to pick up changes
}

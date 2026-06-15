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

/**
 * ===== Mixin 审计注释 =====
 *
 * 目标类: net.minecraft.client.gui.Gui (1.21.1 NeoForge)
 *
 * 本 Mixin 注入以下方法，版本升级时需验证其签名是否变化：
 *
 * 1. renderHealthLevel(GuiGraphics, CallbackInfo)
 *    - 用途: 渲染生命值行的入口方法，HEAD 处获取 PlayerHeartCapability，RETURN 处释放。
 *    - 1.21.1 签名: private void renderHealthLevel(GuiGraphics guiGraphics)
 *
 * 2. renderHearts(GuiGraphics, Player, int, int, int, int, float, int, int, int, boolean, CallbackInfo)
 *    - 用途: TAIL 注入自定义心相渲染，在原版心形之上叠加自定义心相纹理（视觉上覆盖原版心）。
 *    - 1.21.1 签名: private void renderHearts(GuiGraphics guiGraphics, Player player,
 *        int x, int y, int height, int offsetHeartIndex, float maxHealth,
 *        int currentHealth, int displayHealth, int absorptionAmount, boolean renderHighlight)
 *
 * 如果 Mojang 在后续版本中重命名或更改上述方法的参数，Mixin 编译将失败，
 * 届时需更新本注释中的签名信息并调整注入点。
 */
@Mixin(net.minecraft.client.gui.Gui.class)
public abstract class InGameHudMixin {

    @Shadow @Final protected Minecraft minecraft;

    @Unique
    private PlayerHeartCapability aspectComponent = null;

    @Inject(method = "renderHealthLevel", at = @At("HEAD"))
    private void storeAspectComponent(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (this.minecraft.player != null) {
            this.aspectComponent = VictusAttachments.getHearts(this.minecraft.player);
        }
    }

    @Inject(method = "renderHealthLevel", at = @At("RETURN"))
    private void releaseAspectComponent(GuiGraphics guiGraphics, CallbackInfo ci) {
        this.aspectComponent = null;
    }

    @SuppressWarnings("null")
    @Inject(method = "renderHearts", at = @At("TAIL"))
    private void renderCustomHearts(GuiGraphics guiGraphics, Player player, int x, int y, int height, int offsetHeartIndex, float maxHealth, int currentHealth, int displayHealth, int absorptionAmount, boolean renderHighlight, CallbackInfo ci) {
        if (this.aspectComponent == null || this.minecraft.level.getLevelData().isHardcore()) return;

        int healthHearts = net.minecraft.util.Mth.ceil(maxHealth / 2.0F);
        int absorptionHearts = net.minecraft.util.Mth.ceil(absorptionAmount / 2.0F);
        int totalHearts = healthHearts + absorptionHearts;

        int effectiveHearts = this.aspectComponent.effectiveSize();
        if (effectiveHearts > healthHearts) {
            healthHearts = effectiveHearts;
            totalHearts = healthHearts + absorptionHearts;
        }

        // 使用与原版一致的随机种子，确保低血量时心形抖动与原版一致
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create();
        random.setSeed((long)(this.minecraft.gui.getGuiTicks() * 312871));

        int healthForShake = net.minecraft.util.Mth.ceil(Math.max((float)currentHealth, (float)displayHealth));

        // 消耗吸收心部分的随机数序列，使其不影响后续健康心的抖动
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
}

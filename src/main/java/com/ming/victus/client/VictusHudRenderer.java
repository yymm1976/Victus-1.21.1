package com.ming.victus.client;

import com.ming.victus.hearts.HeartAspect;
import net.minecraft.client.gui.GuiGraphics;

public class VictusHudRenderer {

    @SuppressWarnings("null")
    public static void renderAspect(GuiGraphics context, int x, int y, int textureIndex, float rechargeProgress, boolean isHalf) {
        int u = (textureIndex % 8) * 8;
        int v = (textureIndex / 8) * 8;
        // 半心宽 4 像素、整心宽 8 像素；按充能进度裁剪渲染宽度，实现进度条效果
        int width = isHalf ? 4 : 8;
        context.blit(HeartAspect.HEART_ATLAS_TEXTURE, x + 1, y + 1, u, v, Math.round(rechargeProgress * width), 8, HeartAspect.ATLAS_SIZE, HeartAspect.ATLAS_SIZE);
    }

    public static float getComponent(int rgb, int shift) {
        return ((rgb >> shift) & 0xFF) / 255.0F;
    }
}

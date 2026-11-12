package com.ming.victus.client.compat;

import com.ming.victus.capability.PlayerHeartCapability;
import com.ming.victus.capability.VictusAttachments;
import com.ming.victus.client.VictusHudRenderer;
import com.ming.victus.hearts.HeartAspect;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * 已装备心相面板。
 *
 * <p>在物品栏界面点击 L2Tabs 心相标签后打开，显示当前装备的所有心相及充能状态。
 * 鼠标悬停可查看心相名称、激活状态与充能进度。</p>
 */
public class HeartAspectScreen extends Screen {

	/** 每个心相格子的像素尺寸（含间距） */
	private static final int CELL_SIZE = 16;
	/** 每行心相数量 */
	private static final int COLS = 9;
	/** 面板内边距 */
	private static final int PADDING = 8;

	private int panelX;
	private int panelY;
	private static final int PANEL_WIDTH = 176;
	private static final int PANEL_HEIGHT = 80;

	public HeartAspectScreen(Component title) {
		super(title);
	}

	@Override
	protected void init() {
		panelX = (this.width - PANEL_WIDTH) / 2;
		panelY = (this.height - PANEL_HEIGHT) / 2;
	}

	private PlayerHeartCapability getCapability() {
		var player = Minecraft.getInstance().player;
		if (player == null) return null;
		return VictusAttachments.getHearts(player);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
		// 全屏半透明遮罩
		g.fill(0, 0, this.width, this.height, 0x66000000);

		// 面板边框与背景（仿原版物品栏配色）
		g.fill(panelX - 2, panelY - 2, panelX + PANEL_WIDTH + 2, panelY + PANEL_HEIGHT + 2, 0xFF373737);
		g.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFFC6C6C6);

		// 标题
		g.drawCenteredString(this.font, this.getTitle(), this.width / 2, panelY + PADDING, 0x404040);

		PlayerHeartCapability cap = getCapability();
		if (cap == null || cap.empty()) {
			g.drawCenteredString(this.font,
				Component.translatable("victus.screen.hearts.empty"),
				this.width / 2, panelY + 40, 0x808080);
			return;
		}

		var hearts = cap.getEquippedHearts();
		int gridStartX = panelX + (PANEL_WIDTH - COLS * CELL_SIZE) / 2;
		int gridStartY = panelY + 28;

		// 渲染心相格子
		for (int i = 0; i < hearts.size(); i++) {
			HeartAspect aspect = hearts.get(i);
			int cellX = gridStartX + (i % COLS) * CELL_SIZE;
			int cellY = gridStartY + (i / COLS) * CELL_SIZE;

			// 格子背景：浅灰边框 + 深色槽位
			g.fill(cellX, cellY, cellX + CELL_SIZE, cellY + CELL_SIZE, 0xFF8B8B8B);
			g.fill(cellX + 1, cellY + 1, cellX + CELL_SIZE - 1, cellY + CELL_SIZE - 1, 0xFF373737);

			// 心相图标：renderAspect 内部偏移 +1，传入 cellX+3 使 8x8 心相居中于 16x16 格子
			VictusHudRenderer.renderAspect(g, cellX + 3, cellY + 3,
				aspect.getTextureIndex(), aspect.getRechargeProgress(), false);
		}

		// 悬停 tooltip
		int hoverIndex = getHoveredHeartIndex(mouseX, mouseY, hearts.size(), gridStartX, gridStartY);
		if (hoverIndex >= 0) {
			HeartAspect aspect = hearts.get(hoverIndex);
			List<FormattedCharSequence> lines = new ArrayList<>();
			lines.add(aspect.getName().getVisualOrderText());
			if (aspect.active()) {
				lines.add(Component.translatable("victus.screen.hearts.active")
					.withStyle(ChatFormatting.GREEN).getVisualOrderText());
			} else {
				int percent = (int) (aspect.getRechargeProgress() * 100);
				lines.add(Component.translatable("victus.screen.hearts.recharging", percent)
					.withStyle(ChatFormatting.GOLD).getVisualOrderText());
			}
			g.renderTooltip(this.font, lines, mouseX, mouseY);
		}
	}

	private int getHoveredHeartIndex(int mouseX, int mouseY, int count, int gridStartX, int gridStartY) {
		for (int i = 0; i < count; i++) {
			int cellX = gridStartX + (i % COLS) * CELL_SIZE;
			int cellY = gridStartY + (i / COLS) * CELL_SIZE;
			if (mouseX >= cellX && mouseX < cellX + CELL_SIZE
				&& mouseY >= cellY && mouseY < cellY + CELL_SIZE) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/**
	 * 支持 E 键（物品栏键）关闭面板，与原版物品栏行为一致。
	 * Esc 由父类 Screen 默认处理。
	 */
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.minecraft != null && this.minecraft.player != null
			&& this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
			this.onClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
}

package com.ming.victus.client.compat;

import com.ming.victus.item.VictusItems;
import dev.xkmc.l2tabs.tabs.core.TabBase;
import dev.xkmc.l2tabs.tabs.core.TabManager;
import dev.xkmc.l2tabs.tabs.core.TabToken;
import dev.xkmc.l2tabs.tabs.inventory.InvTabData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * L2Tabs 心相面板标签按钮。
 *
 * <p>继承 L2Tabs 的 {@link TabBase}，点击后打开 {@link HeartAspectScreen}
 * 显示当前已装备的心相列表及充能状态。</p>
 *
 * <p>本类仅在客户端被 {@link dev.xkmc.l2tabs.tabs.core.TabManager} 实例化，
 * 服务端不会加载本类（TabManager 是客户端专属逻辑）。</p>
 */
public class HeartAspectTab extends TabBase<InvTabData, HeartAspectTab> {

	public HeartAspectTab(int index, TabToken<InvTabData, HeartAspectTab> token,
						  TabManager<InvTabData> manager, Component title) {
		super(index, token, manager, title);
	}

	@Override
	public void onTabClicked() {
		Minecraft.getInstance().setScreen(new HeartAspectScreen(this.getMessage()));
	}

	/**
	 * 覆写图标渲染：使用 BrokenHeart 物品图标。
	 *
	 * <p>避免依赖 L2Tabs 的 DataMap icon 系统（需额外生成 JSON 数据包），
	 * 直接用 {@link com.ming.victus.item.VictusItems#BROKEN_HEART} 作为标签图标。
	 * 借用 {@code token.getType().drawIcon()} 处理不同标签位置（上/下/左/右）的图标偏移。</p>
	 */
	@Override
	protected void renderIcon(GuiGraphics g) {
		ItemStack stack = new ItemStack(VictusItems.BROKEN_HEART.get());
		token.getType().drawIcon(g, getX(), getY(), stack);
	}
}

package com.ming.victus.client.compat;

import net.neoforged.fml.ModList;

/**
 * L2Tabs 可选兼容入口。
 *
 * <p>本类由 {@link com.ming.victus.VictusMain} 通过反射调用 {@link #init()}，
 * 确保 L2Tabs 未安装时不会触发 L2Tabs 相关类加载。</p>
 *
 * <p><b>关键约束</b>：本类不得在字段类型、方法签名或超类中引用任何 L2Tabs 类型，
 * 所有 L2Tabs 引用必须限制在方法体内部（延迟到方法执行时才解析类）。
 * 这样 {@code Class.forName("VictusL2TabsCompat")} 触发的类加载不会加载 L2Tabs 类，
 * 只有当 {@link #init()} 实际执行且 L2Tabs 已安装时才会加载。</p>
 *
 * <p><b>注册时机</b>：本方法在 ModConfigEvent 时被调用（配置加载后、RegisterEvent 前）。
 * 内部调用 {@code L2Tabs.TAB_REG.reg()} 只是向 DeferredRegister 添加待注册条目，
 * 真正注册（执行 GROUP.registerTab 创建 TabToken）在后续 RegisterEvent 时完成。</p>
 */
public class VictusL2TabsCompat {

	private static final String L2TABS_MODID = "l2tabs";

	/**
	 * 在配置加载后通过反射调用。
	 * 若 L2Tabs 已安装，注册心相面板 TabToken。
	 *
	 * <p>配置检查（enableL2TabsCompat）已在 VictusMain.initL2TabsCompat() 中完成，
	 * 此处不再重复检查。</p>
	 */
	public static void init() {
		if (!ModList.get().isLoaded(L2TABS_MODID)) {
			return;
		}
		registerHeartTab();
	}

	/**
	 * 注册心相面板 TabToken。
	 *
	 * <p>本方法内所有 L2Tabs 类型引用仅在方法执行时解析，
	 * 因此 VictusL2TabsCompat 类加载不会触发 L2Tabs 类加载。
	 * 注册的 TabToken 会被 L2Tabs 的 TabInventory 监听器自动包含到物品栏界面的标签栏中
	 * （TabManager 遍历 GROUP.getTabs() 获取所有注册的 TabToken）。</p>
	 *
	 * <p><b>SR 内部机制</b>：SR 包装 DeferredRegister，reg() 返回 Val（DeferredHolder）。
	 * Val 内部 supplier 在 RegisterEvent 时执行，调用 GROUP.registerTab() 将 TabToken
	 * 加入 GROUP 的 tokens 列表。SR/DeferredRegister 内部持有 Val 引用，无需额外持有。</p>
	 */
	private static void registerHeartTab() {
		// 所有 L2Tabs 引用限于本方法体；不存储返回的 Val（DeferredRegister 内部持有引用）
		dev.xkmc.l2tabs.init.L2Tabs.TAB_REG.reg(
			"victus_hearts",
			() -> dev.xkmc.l2tabs.init.L2Tabs.GROUP.registerTab(
				() -> HeartAspectTab::new,
				net.minecraft.network.chat.Component.translatable("victus.tabs.hearts")
			)
		);
	}
}

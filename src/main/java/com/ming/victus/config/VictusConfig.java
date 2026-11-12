package com.ming.victus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Victus 模组配置。
 *
 * <p>使用 COMMON 类型配置，服务端和客户端都会加载。
 * 配置文件位于 {@code config/victus-common.toml}。</p>
 *
 * <p><b>生效规则</b>：所有配置项修改后需要重启游戏才能生效，
 * 因为相关注册在游戏启动早期完成（RegisterEvent 阶段），无法在运行时动态切换。</p>
 */
public class VictusConfig {

	public static final ModConfigSpec SPEC;

	/** 是否启用 L2Library/L2Tabs 兼容（物品栏心相面板标签） */
	public static final ModConfigSpec.BooleanValue ENABLE_L2TABS_COMPAT;

	static {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

		builder.comment("Compat settings for third-party mods", "第三方模组兼容设置")
			.push("compat");

		ENABLE_L2TABS_COMPAT = builder
			.comment(
				"Enable L2Library/L2Tabs integration.",
				"When enabled and L2Library is installed, an extra tab is added to the inventory screen",
				"showing all equipped heart aspects with their recharge progress.",
				"Requires restart to take effect.",
				"启用 L2Library/L2Tabs 兼容。",
				"启用后若已安装 L2Library，物品栏界面会新增一个标签，",
				"显示所有已装备心相及其充能进度。",
				"修改后需要重启游戏生效。"
			)
			.define("enableL2TabsCompat", true);

		builder.pop();

		SPEC = builder.build();
	}
}

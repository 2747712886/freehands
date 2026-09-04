package com.yourname.freehands;

import com.mojang.logging.LogUtils;
import com.yourname.freehands.registry.ModCreativeTabs;
import com.yourname.freehands.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Free Hands 主类（NeoForge 1.21.1）。
 * <p>
 * 在模组事件总线上注册物品与创造模式标签页；游戏事件由 {@code FreeHandEvents} 的
 * {@code @EventBusSubscriber} 自动注册到 NeoForge 游戏总线。
 */
@Mod(FreeHands.MODID)
public class FreeHands {
    public static final String MODID = "freehands";
    public static final String FREE_HAND_SLOT = "free_hand";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FreeHands(IEventBus modBus, ModContainer modContainer) {
        ModItems.register(modBus);
        ModCreativeTabs.register(modBus);
        LOGGER.info("Free Hands loaded (NeoForge 1.21.1)");
    }
}

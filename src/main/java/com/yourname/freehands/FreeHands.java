package com.yourname.freehands;

import com.mojang.logging.LogUtils;
import com.yourname.freehands.registry.ModCreativeTabs;
import com.yourname.freehands.registry.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(FreeHands.MODID)
public class FreeHands {
    public static final String MODID = "freehands";
    public static final String FREE_HAND_SLOT = "free_hand";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FreeHands(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Free Hands loaded");
    }
}

package com.yourname.coretrinkets;

import com.mojang.logging.LogUtils;
import com.yourname.coretrinkets.registry.ModCreativeTabs;
import com.yourname.coretrinkets.registry.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CoreTrinkets.MODID)
public class CoreTrinkets {
    public static final String MODID = "coretrinkets";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CoreTrinkets(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Core Trinkets loaded");
    }
}

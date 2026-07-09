package com.yourname.coretrinkets.registry;

import com.yourname.coretrinkets.CoreTrinkets;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CoreTrinkets.MODID);

    public static final RegistryObject<CreativeModeTab> CORE_TRINKETS = TABS.register("core_trinkets", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .title(Component.translatable("itemGroup.coretrinkets.core_trinkets"))
            .icon(() -> ModItems.IRON_CORE.get().getDefaultInstance())
            .displayItems((parameters, output) -> output.accept(ModItems.IRON_CORE.get()))
            .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}

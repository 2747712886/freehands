package com.yourname.freehands.registry;

import com.yourname.freehands.FreeHands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FreeHands.MODID);

    public static final RegistryObject<CreativeModeTab> FREE_HANDS = TABS.register("free_hands", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .title(Component.translatable("itemGroup.freehands.free_hands"))
            .icon(() -> ModItems.IRON_TRINKET.get().getDefaultInstance())
            .displayItems((parameters, output) -> output.accept(ModItems.IRON_TRINKET.get()))
            .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}

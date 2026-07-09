package com.yourname.coretrinkets.registry;

import com.yourname.coretrinkets.CoreTrinkets;
import com.yourname.coretrinkets.core.CoreAbility;
import com.yourname.coretrinkets.item.CoreTrinketItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CoreTrinkets.MODID);

    public static final RegistryObject<Item> IRON_CORE = ITEMS.register("iron_core", () -> new CoreTrinketItem(new Item.Properties().stacksTo(1), CoreAbility.IRON));

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

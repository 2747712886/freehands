package com.yourname.freehands.registry;

import com.yourname.freehands.FreeHands;
import com.yourname.freehands.ability.FreeHandAbility;
import com.yourname.freehands.item.AbilityTrinketItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FreeHands.MODID);

    public static final RegistryObject<Item> IRON_TRINKET = ITEMS.register("iron_trinket", () -> new AbilityTrinketItem(new Item.Properties().stacksTo(1), FreeHandAbility.IRON_TRINKET));

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

package com.yourname.freehands.registry;

import com.yourname.freehands.FreeHands;
import com.yourname.freehands.ability.FreeHandAbility;
import com.yourname.freehands.item.AbilityTrinketItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 物品注册（NeoForge 1.21.1）：三种能力饰品（铁/钻石/下界合金）。
 */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, FreeHands.MODID);

    public static final DeferredHolder<Item, AbilityTrinketItem> IRON_TRINKET =
            ITEMS.register("iron_trinket", () -> new AbilityTrinketItem(
                    new Item.Properties().stacksTo(1).durability(2048), FreeHandAbility.IRON_TRINKET));
    public static final DeferredHolder<Item, AbilityTrinketItem> DIAMOND_TRINKET =
            ITEMS.register("diamond_trinket", () -> new AbilityTrinketItem(
                    new Item.Properties().stacksTo(1).durability(4096), FreeHandAbility.DIAMOND_TRINKET));
    public static final DeferredHolder<Item, AbilityTrinketItem> NETHERITE_TRINKET =
            ITEMS.register("netherite_trinket", () -> new AbilityTrinketItem(
                    new Item.Properties().stacksTo(1).durability(8192), FreeHandAbility.NETHERITE_TRINKET));

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

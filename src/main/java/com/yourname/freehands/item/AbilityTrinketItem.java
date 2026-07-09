package com.yourname.freehands.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.yourname.freehands.FreeHands;
import com.yourname.freehands.ability.FreeHandAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;

public class AbilityTrinketItem extends Item implements ICurioItem {
    private final FreeHandAbility ability;

    public AbilityTrinketItem(Properties properties, FreeHandAbility ability) {
        super(properties);
        this.ability = ability;
    }

    public FreeHandAbility ability() {
        return ability;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return FreeHands.FREE_HAND_SLOT.equals(slotContext.identifier());
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return canEquip(slotContext, stack);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        return HashMultimap.create();
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, ItemStack stack) {
        tooltips.add(Component.translatable("tooltip.freehands.slot.free_hand"));
        return tooltips;
    }
}

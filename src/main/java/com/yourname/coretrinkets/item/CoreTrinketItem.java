package com.yourname.coretrinkets.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.yourname.coretrinkets.core.CoreAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;

public class CoreTrinketItem extends Item implements ICurioItem {
    private static final UUID ATTACK_DAMAGE_UUID = UUID.fromString("3d2d5c06-8d37-4d85-a872-2f6afdbfd8e1");
    private static final UUID ARMOR_UUID = UUID.fromString("65f77bb2-6d3a-4269-93e1-94ea9e9b89de");
    private static final UUID ARMOR_TOUGHNESS_UUID = UUID.fromString("f0151ffc-e94d-42f1-b8bc-64a4c4ce495d");

    private final CoreAbility ability;

    public CoreTrinketItem(Properties properties, CoreAbility ability) {
        super(properties);
        this.ability = ability;
    }

    public CoreAbility ability() {
        return ability;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "core".equals(slotContext.identifier());
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return canEquip(slotContext, stack);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        if (!canEquip(slotContext, stack)) {
            return modifiers;
        }

        modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(ATTACK_DAMAGE_UUID, "Iron core attack damage", ability.attackDamage(), AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.ARMOR, new AttributeModifier(ARMOR_UUID, "Iron core armor", ability.armor(), AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(ARMOR_TOUGHNESS_UUID, "Iron core armor toughness", ability.armorToughness(), AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, ItemStack stack) {
        tooltips.add(Component.translatable("tooltip.coretrinkets.slot.core"));
        return tooltips;
    }
}

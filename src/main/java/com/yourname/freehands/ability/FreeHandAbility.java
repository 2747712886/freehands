package com.yourname.freehands.ability;

public record FreeHandAbility(
        int harvestLevel,
        float miningSpeed,
        double attackDamage,
        double armor,
        double armorToughness,
        double knockbackResistance,
        int enchantmentValue
) {
    public static final FreeHandAbility IRON_TRINKET = new FreeHandAbility(2, 6.0F, 6.0D, 15.0D, 0.0D, 0.0D, 14);
    public static final FreeHandAbility DIAMOND_TRINKET = new FreeHandAbility(3, 8.0F, 7.0D, 20.0D, 8.0D, 0.0D, 10);
    public static final FreeHandAbility NETHERITE_TRINKET = new FreeHandAbility(4, 9.0F, 8.0D, 20.0D, 12.0D, 0.4D, 15);
}

package com.yourname.freehands.ability;

public record FreeHandAbility(
        int harvestLevel,
        float miningSpeed,
        double attackDamage,
        double armor,
        double armorToughness
) {
    public static final FreeHandAbility IRON_TRINKET = new FreeHandAbility(2, 6.0F, 6.0D, 6.0D, 0.0D);
}

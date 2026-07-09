package com.yourname.coretrinkets.core;

public record CoreAbility(
        int harvestLevel,
        float miningSpeed,
        double attackDamage,
        double armor,
        double armorToughness
) {
    public static final CoreAbility IRON = new CoreAbility(2, 6.0F, 6.0D, 6.0D, 0.0D);
}

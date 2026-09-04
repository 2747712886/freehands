package com.yourname.freehands.mixin;

import com.yourname.freehands.event.FreeHandEvents;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 抢夺附魔 Mixin（NeoForge 1.21.1）。
 * <p>
 * 1.20.1 的 {@code LootingLevelEvent} 在 NeoForge 1.21.1 已被移除；抢夺等级现在由战利品结算时
 * {@link EnchantmentHelper#getEnchantmentLevel(Holder, LivingEntity)} 查询攻击者装备得出，
 * 而该方法只读原版装备槽（抢夺仅 {@code mainhand}），看不到解放槽武器。
 * <p>
 * 这里只在查询抢夺、且玩家解放槽武器的抢夺等级更高时抬高原版结果：其余附魔、其余实体、
 * 以及原版主手路径完全不受影响（只升不降）。
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
    @Inject(
            method = "getEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/LivingEntity;)I",
            at = @At("RETURN"),
            cancellable = true)
    private static void freehands$applyFreeHandLooting(Holder<Enchantment> enchantment, LivingEntity entity,
                                                       CallbackInfoReturnable<Integer> callback) {
        if (!(entity instanceof Player player) || !enchantment.is(Enchantments.LOOTING)) {
            return;
        }

        int freeHandLooting = FreeHandEvents.freeHandEnchantmentLevel(player, Enchantments.LOOTING);
        if (freeHandLooting > callback.getReturnValueI()) {
            callback.setReturnValue(freeHandLooting);
        }
    }
}

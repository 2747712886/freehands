package com.yourname.freehands.mixin;

import com.yourname.freehands.compat.VirtualMainHandContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 实体物品获取 Mixin：将解放槽虚拟工具注入到主手物品查询中。
 * <p>
 * 当玩家通过解放槽使用工具时，getMainHandItem() 和 getItemInHand(MAIN_HAND)
 * 会返回虚拟工具，使得挖掘速度计算、耐久损耗等逻辑能正常工作。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /**
     * 注入 getMainHandItem，在有虚拟工具时返回虚拟堆叠。
     */
    @Inject(method = "getMainHandItem", at = @At("RETURN"), cancellable = true)
    private void freehands$useVirtualMiningMainHand(CallbackInfoReturnable<ItemStack> callback) {
        if ((Object) this instanceof Player player) {
            VirtualMainHandContext.getVirtualMainHand(player).ifPresent(callback::setReturnValue);
        }
    }

    /**
     * 注入 getItemInHand，在主手交互时返回虚拟堆叠。
     */
    @Inject(method = "getItemInHand", at = @At("RETURN"), cancellable = true)
    private void freehands$useVirtualMainHandForInteractions(InteractionHand hand,
                                                               CallbackInfoReturnable<ItemStack> callback) {
        if (hand == InteractionHand.MAIN_HAND && (Object) this instanceof Player player) {
            VirtualMainHandContext.getVirtualMainHand(player).ifPresent(callback::setReturnValue);
        }
    }
}

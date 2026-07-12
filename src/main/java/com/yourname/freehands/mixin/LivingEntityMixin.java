package com.yourname.freehands.mixin;

import com.yourname.freehands.compat.VirtualMainHandContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getMainHandItem", at = @At("RETURN"), cancellable = true)
    private void freehands$useVirtualMiningMainHand(CallbackInfoReturnable<ItemStack> callback) {
        if ((Object) this instanceof Player player) {
            VirtualMainHandContext.getVirtualMainHand(player).ifPresent(callback::setReturnValue);
        }
    }
}

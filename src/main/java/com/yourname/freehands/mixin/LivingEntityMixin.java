package com.yourname.freehands.mixin;

import com.yourname.freehands.compat.VirtualMainHandContext;
import com.yourname.freehands.event.FreeHandEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getMainHandItem", at = @At("RETURN"), cancellable = true)
    private void freehands$useVirtualMiningMainHand(CallbackInfoReturnable<ItemStack> callback) {
        if ((Object) this instanceof Player player) {
            VirtualMainHandContext.getVirtualMainHand(player).ifPresent(callback::setReturnValue);
        }
    }

    @Inject(method = "getItemInHand", at = @At("RETURN"), cancellable = true)
    private void freehands$useVirtualMainHandForInteractions(InteractionHand hand,
                                                               CallbackInfoReturnable<ItemStack> callback) {
        if (hand == InteractionHand.MAIN_HAND && (Object) this instanceof Player player) {
            VirtualMainHandContext.getVirtualMainHand(player).ifPresent(callback::setReturnValue);
        }
    }

    /**
     * 在护甲减伤前（免疫检查后、getDamageAfterArmorAbsorb 输入处）把解放槽最高武器的攻击加成叠加到原始伤害，
     * 与 1.21.1 的 LivingIncomingDamageEvent 时机一致：加成会被目标护甲/韧性正常削减。
     * 仅当伤害直接来源是玩家近战、且服务端时生效；玩家 TNT/抛射物等间接伤害不算近战。
     */
    @ModifyVariable(method = "getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float freehands$addFreeHandAttackDamageBeforeArmor(float amount, DamageSource source, float ignored) {
        if (source == null || ((LivingEntity) (Object) this).level().isClientSide()) {
            return amount;
        }
        if (source.getDirectEntity() instanceof Player player) {
            return amount + FreeHandEvents.addFreeHandAttackDamage(player, (LivingEntity) (Object) this);
        }
        return amount;
    }
}

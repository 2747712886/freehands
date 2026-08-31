package com.yourname.freehands.mixin;

import com.yourname.freehands.compat.VirtualMainHandContext;
import com.yourname.freehands.event.FreeHandEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow
    protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void freehands$beginVirtualMining(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        VirtualMainHandContext.beginMining(player, player.level().getBlockState(pos));
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void freehands$endVirtualMining(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        VirtualMainHandContext.endMining(player);
    }

    @ModifyVariable(method = "useItemOn", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private ItemStack freehands$replaceRightClickStack(ItemStack original) {
        return VirtualMainHandContext.getVirtualMainHand(player).orElse(original);
    }

    @Inject(method = "useItemOn", at = @At("RETURN"), cancellable = true)
    private void freehands$endVirtualToolUse(ServerPlayer player, Level level, ItemStack stack,
                                             InteractionHand hand, BlockHitResult hitResult,
                                             CallbackInfoReturnable<InteractionResult> callback) {
        if (VirtualMainHandContext.getVirtualMainHand(player).isPresent()) {
            return;
        }

        if (hand == InteractionHand.MAIN_HAND) {
            if (callback.getReturnValue() != InteractionResult.PASS || !player.getOffhandItem().isEmpty()) {
                return;
            }

            InteractionResult result = freehands$useFreeHandTool(player, level, hitResult);
            if (result != InteractionResult.PASS) {
                FreeHandEvents.recordFreeHandHandled(player, hitResult.getBlockPos());
                player.swing(InteractionHand.MAIN_HAND, true);
                callback.setReturnValue(InteractionResult.CONSUME);
            }
            return;
        }

        if (hand != InteractionHand.OFF_HAND || callback.getReturnValue() != InteractionResult.PASS) {
            return;
        }

        if (FreeHandEvents.consumeFreeHandHandled(player, hitResult.getBlockPos())) {
            return;
        }

        InteractionResult result = freehands$useFreeHandTool(player, level, hitResult);
        if (result != InteractionResult.PASS) {
            FreeHandEvents.recordFreeHandHandled(player, hitResult.getBlockPos());
            player.swing(InteractionHand.MAIN_HAND, true);
            callback.setReturnValue(InteractionResult.CONSUME);
        }
    }

    @Unique
    private InteractionResult freehands$useFreeHandTool(ServerPlayer player, Level level, BlockHitResult hitResult) {
        for (ItemStack freeHandStack : FreeHandEvents.freeHandUseStacks(player)) {
            VirtualMainHandContext.beginUsing(player, freeHandStack);
            try {
                InteractionResult result = ((ServerPlayerGameMode) (Object) this).useItemOn(
                        player, level, freeHandStack, InteractionHand.MAIN_HAND, hitResult);
                if (result != InteractionResult.PASS) {
                    return result;
                }
            } finally {
                VirtualMainHandContext.endUsing(player);
            }
        }
        return InteractionResult.PASS;
    }
}

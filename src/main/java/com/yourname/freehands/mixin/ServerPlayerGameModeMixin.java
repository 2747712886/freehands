package com.yourname.freehands.mixin;

import com.yourname.freehands.compat.VirtualMainHandContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
}

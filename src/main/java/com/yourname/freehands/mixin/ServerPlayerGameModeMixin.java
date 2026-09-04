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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 服务端玩家游戏模式 Mixin：处理解放槽工具的挖掘和右键交互。
 * <p>
 * 主要功能：
 * <ul>
 *   <li>在 destroyBlock 前后激活/停用虚拟挖掘上下文</li>
 *   <li>在 useItemOn 中替换主手物品为虚拟工具</li>
 *   <li>在主手和副手都返回 PASS 时，尝试使用解放槽物品</li>
 * </ul>
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow
    protected ServerPlayer player;

    /**
     * 在破坏方块前激活虚拟挖掘上下文。
     */
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void freehands$beginVirtualMining(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        VirtualMainHandContext.beginMining(player, player.level().getBlockState(pos));
    }

    /**
     * 在破坏方块后停用虚拟挖掘上下文。
     */
    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void freehands$endVirtualMining(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        VirtualMainHandContext.endMining(player);
    }

    /**
     * 修改 useItemOn 的主手物品参数，若有虚拟工具则替换。
     */
    @ModifyVariable(method = "useItemOn", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private ItemStack freehands$replaceRightClickStack(ItemStack original) {
        return VirtualMainHandContext.getVirtualMainHand(player).orElse(original);
    }

    /**
     * 在 useItemOn 返回后，若主手和副手都未处理，尝试解放槽物品。
     */
    @Inject(method = "useItemOn", at = @At("RETURN"), cancellable = true)
    private void freehands$endVirtualToolUse(ServerPlayer player, Level level, ItemStack stack,
                                             InteractionHand hand, BlockHitResult hitResult,
                                             CallbackInfoReturnable<InteractionResult> callback) {
        // 若仍有虚拟工具（嵌套调用），跳过
        if (VirtualMainHandContext.getVirtualMainHand(player).isPresent()) {
            return;
        }

        // 主手逻辑：若返回非 PASS 或副手有物品，由原版处理；否则尝试解放槽
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

        // 副手逻辑：若返回非 PASS，跳过；若已被主手解放槽处理过，跳过
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

    /**
     * 依次尝试解放槽中的可用物品，直到有一个成功处理交互。
     */
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

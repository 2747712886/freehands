package com.yourname.freehands.mixin;

import com.yourname.freehands.compat.VirtualMainHandContext;
import com.yourname.freehands.event.FreeHandEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbultimine.FTBUltimine", remap = false)
public abstract class FTBUltimineMixin {
    @Unique
    private static final ThreadLocal<Boolean> freehands$dispatching = ThreadLocal.withInitial(() -> false);

    @Inject(method = "blockRightClick", at = @At("RETURN"), cancellable = true)
    private void freehands$retryWithOffHandAndAccessories(Player player, InteractionHand hand, BlockPos pos,
                                                          net.minecraft.core.Direction direction,
                                                          CallbackInfoReturnable<?> callback) {
        if (!(player instanceof ServerPlayer serverPlayer) || freehands$dispatching.get()) {
            return;
        }

        Object result = callback.getReturnValue();
        if (freehands$handled(result)) {
            return;
        }

        freehands$dispatching.set(true);
        try {
            if (hand == InteractionHand.MAIN_HAND) {
                ItemStack offStack = serverPlayer.getOffhandItem();
                if (!offStack.isEmpty() && freehands$isSupportedRightClickTool(offStack)) {
                    result = freehands$invokeBlockRightClick(player, InteractionHand.OFF_HAND, pos, direction);
                    if (freehands$handled(result)) {
                        freehands$setReturnValue(callback, result);
                        return;
                    }
                }
            }

            for (var accessoryStack : FreeHandEvents.ultimineUseStacks(serverPlayer)) {
                VirtualMainHandContext.beginUsing(serverPlayer, accessoryStack);
                try {
                    result = freehands$invokeBlockRightClick(player, InteractionHand.MAIN_HAND, pos, direction);
                } finally {
                    VirtualMainHandContext.endUsing(serverPlayer);
                }
                if (freehands$handled(result)) {
                    freehands$setReturnValue(callback, result);
                    return;
                }
            }
        } finally {
            freehands$dispatching.remove();
        }
    }

    @Unique
    private static boolean freehands$isSupportedRightClickTool(ItemStack stack) {
        return stack.canPerformAction(net.minecraftforge.common.ToolActions.HOE_TILL)
                || stack.canPerformAction(net.minecraftforge.common.ToolActions.AXE_STRIP)
                || stack.canPerformAction(net.minecraftforge.common.ToolActions.SHOVEL_FLATTEN);
    }

    @Unique
    private Object freehands$invokeBlockRightClick(Player player, InteractionHand hand, BlockPos pos,
                                                    net.minecraft.core.Direction direction) {
        try {
            Method method = this.getClass().getMethod("blockRightClick", Player.class, InteractionHand.class,
                    BlockPos.class, net.minecraft.core.Direction.class);
            return method.invoke(this, player, hand, pos, direction);
        } catch (IllegalAccessException | NoSuchMethodException exception) {
            throw new IllegalStateException("Unable to invoke FTB Ultimine right-click handler", exception);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("FTB Ultimine right-click handler failed", exception.getCause());
        }
    }

    @Unique
    private static boolean freehands$handled(Object result) {
        try {
            return (boolean) result.getClass().getMethod("interruptsFurtherEvaluation").invoke(result);
        } catch (IllegalAccessException | NoSuchMethodException exception) {
            throw new IllegalStateException("Unable to inspect FTB Ultimine right-click result", exception);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("Unable to inspect FTB Ultimine right-click result", exception.getCause());
        }
    }

    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void freehands$setReturnValue(CallbackInfoReturnable<?> callback, Object result) {
        ((CallbackInfoReturnable) callback).setReturnValue(result);
    }
}
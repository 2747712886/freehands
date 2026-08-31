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
        boolean originalHandled = freehands$handled(result);
        if (originalHandled) {
            if (hand == InteractionHand.MAIN_HAND) {
                FreeHandEvents.recordFreeHandHandled(serverPlayer, pos);
            }
            return;
        }
        if (hand == InteractionHand.OFF_HAND && FreeHandEvents.isFreeHandHandled(serverPlayer, pos)) {
            // 同一次物理右键的副手空包：主手包已执行过解放槽连锁，跳过避免二次动作。
            return;
        }

        freehands$dispatching.set(true);
        try {
            if (hand == InteractionHand.MAIN_HAND) {
                ItemStack offStack = serverPlayer.getOffhandItem();
                if (!offStack.isEmpty() && freehands$isSupportedRightClickTool(offStack)) {
                    result = freehands$invokeBlockRightClick(player, InteractionHand.OFF_HAND, pos, direction);
                    if (freehands$handled(result)) {
                        FreeHandEvents.recordFreeHandHandled(serverPlayer, pos);
                        serverPlayer.swing(InteractionHand.MAIN_HAND, true);
                        freehands$setReturnValue(callback, result);
                        return;
                    }
                }
            }

            var stacks = FreeHandEvents.ultimineUseStacks(serverPlayer);
            for (var accessoryStack : stacks) {
                boolean accessoryHandled;
                Object handledResult = null;
                if (accessoryStack.getItem() instanceof com.yourname.freehands.item.AbilityTrinketItem) {
                    // Ultimine 的斧分支会把物品强转为自己的 AxeItemAccess 混入接口，
                    // 多合一饰品无法走其分支；改为对连锁位置逐个执行饰品自身的单块逻辑
                    // （去皮/刮锈/去蜡/铲平/熄营火/成熟藤蔓一次覆盖）。
                    accessoryHandled = freehands$useTrinketOnCachedChain(serverPlayer, accessoryStack, pos);
                    if (accessoryHandled) {
                        handledResult = freehands$interruptResult();
                    }
                } else {
                    VirtualMainHandContext.beginUsing(serverPlayer, accessoryStack);
                    try {
                        result = freehands$invokeBlockRightClick(player, InteractionHand.MAIN_HAND, pos, direction);
                    } finally {
                        VirtualMainHandContext.endUsing(serverPlayer);
                    }
                    accessoryHandled = freehands$handled(result);
                    if (accessoryHandled) {
                        handledResult = result;
                    }
                }
                if (accessoryHandled) {
                    FreeHandEvents.recordFreeHandHandled(serverPlayer, pos);
                    // Ultimine 内部 swing 只广播给其他玩家（updateSelf=false），
                    // 客户端又无法预知解放槽动作，须由服务端把挥臂动画发给玩家自己。
                    serverPlayer.swing(InteractionHand.MAIN_HAND, true);
                    freehands$setReturnValue(callback, handledResult);
                    return;
                }
            }
        } finally {
            freehands$dispatching.remove();
        }
    }

    @SuppressWarnings("unchecked")
    @Unique
    private boolean freehands$useTrinketOnCachedChain(ServerPlayer player, ItemStack trinket, BlockPos clickedPos) {
        java.util.Collection<BlockPos> positions;
        try {
            Class<?> ultimineClass = Class.forName("dev.ftb.mods.ftbultimine.FTBUltimine");
            Object instance = ultimineClass.getField("instance").get(null);
            Object data = ultimineClass.getMethod("getOrCreatePlayerData", Player.class).invoke(instance, player);
            if (!(boolean) data.getClass().getMethod("isPressed").invoke(data)) {
                return false;
            }
            java.lang.reflect.Field cachedPosField = data.getClass().getDeclaredField("cachedPos");
            cachedPosField.setAccessible(true);
            if (!clickedPos.equals(cachedPosField.get(data))) {
                // 缓存位置来自上一次点击（本次调用未走到 updateBlocks），不使用旧缓存。
                return false;
            }
            positions = (java.util.Collection<BlockPos>) data.getClass().getMethod("cachedPositions").invoke(data);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to read FTB Ultimine chain positions", exception);
        }
        if (positions == null || positions.isEmpty()) {
            return false;
        }
        boolean anyHandled = false;
        for (BlockPos chainPos : positions) {
            net.minecraft.world.phys.BlockHitResult chainHit = new net.minecraft.world.phys.BlockHitResult(
                    net.minecraft.world.phys.Vec3.atCenterOf(chainPos), net.minecraft.core.Direction.UP, chainPos, false);
            VirtualMainHandContext.beginUsing(player, trinket);
            try {
                net.minecraft.world.item.context.UseOnContext context =
                        new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, chainHit);
                if (trinket.useOn(context).consumesAction()) {
                    anyHandled = true;
                }
            } finally {
                VirtualMainHandContext.endUsing(player);
            }
        }
        return anyHandled;
    }

    @Unique
    private static Object freehands$interruptResult() {
        try {
            return Class.forName("dev.architectury.event.EventResult").getMethod("interruptFalse").invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to create FTB Ultimine right-click result", exception);
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
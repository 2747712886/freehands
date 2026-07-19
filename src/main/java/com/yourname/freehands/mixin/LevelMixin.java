package com.yourname.freehands.mixin;

import com.yourname.freehands.compat.VirtualMainHandContext;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Level.class)
public abstract class LevelMixin {
    @ModifyVariable(method = "playSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Player freehands$includeVirtualToolUserInSound(Player excludedPlayer) {
        if (excludedPlayer != null && VirtualMainHandContext.isUsing(excludedPlayer)) {
            return null;
        }
        return excludedPlayer;
    }
}

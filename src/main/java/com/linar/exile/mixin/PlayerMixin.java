package com.linar.exile.mixin;

import com.linar.exile.LinarExileMod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void linarExile$boostMiningSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;
        if (!player.level().isClientSide() && LinarExileMod.isLinar(player) && LinarExileMod.isAbilityActive(player)) {
            cir.setReturnValue(cir.getReturnValue() * 1.5f);
        }
    }
}

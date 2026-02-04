package com.linar.exile.mixin;

import com.linar.exile.LinarExileMod;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float linarExile$reduceLinarDamage(float amount, DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof Player player && LinarExileMod.isLinar(player)) {
            return amount * 0.9f;
        }
        return amount;
    }
}

package com.linar.exile;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;

public class LinarExileClient implements ClientModInitializer {
    private static KeyMapping abilityKey;
    private static Double baseGamma = null;

    @Override
    public void onInitializeClient() {
        abilityKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.linar_exile.activate_ability",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "category.linar_exile"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LocalPlayer player = client.player;
            if (player != null && LinarExileMod.isLinar(player)) {
                applyGammaBoost(client);
                while (abilityKey.consumeClick()) {
                    ClientPlayNetworking.send(new LinarExileMod.ActivateAbilityPayload());
                }
            } else {
                restoreGamma(client);
            }
        });
    }

    private static void applyGammaBoost(Minecraft client) {
        Options options = client.options;
        if (options == null) {
            return;
        }
        if (baseGamma == null) {
            baseGamma = options.gamma().get();
        }
        double boosted = Math.min(1.0, baseGamma * 1.3);
        if (options.gamma().get() != boosted) {
            options.gamma().set(boosted);
        }
    }

    private static void restoreGamma(Minecraft client) {
        if (baseGamma == null) {
            return;
        }
        Options options = client.options;
        if (options != null) {
            options.gamma().set(baseGamma);
        }
        baseGamma = null;
    }
}

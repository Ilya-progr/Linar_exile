package com.linar.exile;

import com.linar.exile.state.AbilityState;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class LinarExileMod implements ModInitializer {
    public static final String MOD_ID = "linar_exile";
    public static final ResourceLocation ACTIVATE_ABILITY = ResourceLocation.fromNamespaceAndPath(MOD_ID, "activate_ability");
    private static final String LINAR_NAME = "Linar_li";
    private static final ResourceLocation SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linar_speed");
    private static final double SPEED_MULTIPLIER = 0.5;
    private static final int ABILITY_DURATION_TICKS = 10 * 20;
    private static final int ABILITY_COOLDOWN_TICKS = 15 * 60 * 20;

    private static final Map<UUID, AbilityState> ABILITY_STATES = new ConcurrentHashMap<>();
    private static long serverTicks = 0;

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(ActivateAbilityPayload.TYPE, ActivateAbilityPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ActivateAbilityPayload.TYPE, (payload, context) -> {
            MinecraftServer server = context.server();
            ServerPlayer player = context.player();
            server.execute(() -> tryActivateAbility(server, player));
        });

        ServerTickEvents.END_SERVER_TICK.register(LinarExileMod::onServerTick);
    }

    public static boolean isLinar(Player player) {
        return player != null && LINAR_NAME.equals(player.getName().getString());
    }

    public static boolean isAbilityActive(Player player) {
        AbilityState state = ABILITY_STATES.get(player.getUUID());
        return state != null && state.isActive(serverTicks);
    }

    private static void tryActivateAbility(MinecraftServer server, ServerPlayer player) {
        if (!isLinar(player)) {
            return;
        }

        AbilityState state = ABILITY_STATES.computeIfAbsent(player.getUUID(), uuid -> new AbilityState());
        if (state.isOnCooldown(serverTicks)) {
            return;
        }

        state.activate(serverTicks, ABILITY_DURATION_TICKS, ABILITY_COOLDOWN_TICKS);
        removeNegativeEffects(player);
        applySpeedModifier(player);
        player.sendSystemMessage(Component.translatable("message.linar_exile.ability_activated"), true);
    }

    private static void onServerTick(MinecraftServer server) {
        serverTicks++;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!isLinar(player)) {
                continue;
            }

            applyMaxHealthPenalty(player);

            AbilityState state = ABILITY_STATES.computeIfAbsent(player.getUUID(), uuid -> new AbilityState());
            if (state.isActive(serverTicks)) {
                removeNegativeEffects(player);
                applySpeedModifier(player);
            } else {
                removeSpeedModifier(player);
            }
        }
    }

    private static void applyMaxHealthPenalty(ServerPlayer player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        if (maxHealth.getBaseValue() != 18.0) {
            maxHealth.setBaseValue(18.0);
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    private static void applySpeedModifier(ServerPlayer player) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            return;
        }
        if (movementSpeed.getModifier(SPEED_MODIFIER_ID) == null) {
            movementSpeed.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_ID, SPEED_MULTIPLIER, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void removeSpeedModifier(ServerPlayer player) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            return;
        }
        AttributeModifier modifier = movementSpeed.getModifier(SPEED_MODIFIER_ID);
        if (modifier != null) {
            movementSpeed.removeModifier(modifier);
        }
    }

    private static void removeNegativeEffects(ServerPlayer player) {
        for (MobEffectInstance effectInstance : player.getActiveEffects()) {
            Holder<MobEffect> effect = effectInstance.getEffect();
            if (!effect.value().isBeneficial()) {
                player.removeEffect(effect);
            }
        }
    }

    public record ActivateAbilityPayload() implements CustomPacketPayload {
        public static final Type<ActivateAbilityPayload> TYPE = new Type<>(ACTIVATE_ABILITY);
        public static final StreamCodec<RegistryFriendlyByteBuf, ActivateAbilityPayload> CODEC =
            StreamCodec.unit(new ActivateAbilityPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}

package de.castcrafter.travelanchors;

import de.castcrafter.travelanchors.config.CommonConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public class ManaIntegration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManaIntegration.class);
    private static final String ISS_MOD_ID = "irons_spellbooks";
    private static boolean isIssLoaded = false;
    private static Method getPlayerMagicDataMethod = null;
    private static Method getManaMethod = null;
    private static Method setManaMethod = null;
    private static Method sendToPlayerMethod = null;
    private static java.lang.reflect.Constructor<?> clientboundSyncManaConstructor = null;
    private static Class<?> magicDataClass = null; // Store class for constructor
    private static Method getClientManaMethod = null;

    static {
        if (ModList.get().isLoaded(ISS_MOD_ID)) {
            try {
                magicDataClass = Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
                Class<?> livingEntityClass = Class.forName("net.minecraft.world.entity.LivingEntity");
                getPlayerMagicDataMethod = magicDataClass.getMethod("getPlayerMagicData", livingEntityClass);
                getManaMethod = magicDataClass.getMethod("getMana");
                setManaMethod = magicDataClass.getMethod("setMana", float.class);

                Class<?> messagesClass = Class.forName("io.redspace.ironsspellbooks.setup.Messages");
                Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
                // Note: sendToPlayer is generic, so we need to find it by parameter types Object and ServerPlayer
                sendToPlayerMethod = messagesClass.getMethod("sendToPlayer", Object.class, serverPlayerClass);

                Class<?> clientboundSyncManaClass = Class.forName("io.redspace.ironsspellbooks.network.ClientboundSyncMana");
                clientboundSyncManaConstructor = clientboundSyncManaClass.getConstructor(magicDataClass);

                if (FMLEnvironment.dist == Dist.CLIENT) {
                    try {
                        Class<?> clientMagicDataClass = Class.forName("io.redspace.ironsspellbooks.player.ClientMagicData");
                        getClientManaMethod = clientMagicDataClass.getMethod("getPlayerMana");
                    } catch (ClassNotFoundException | NoSuchMethodException e) {
                        LOGGER.error("Failed to initialize client-side mana check for Iron's Spells 'n Spellbooks.", e);
                    }
                }

                isIssLoaded = true;
                LOGGER.info("Iron's Spells 'n Spellbooks found. Mana cost for short teleports will be enabled if configured.");
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                LOGGER.error("Failed to initialize integration with Iron's Spells 'n Spellbooks. Mana cost will not be applied or synced.", e);
                isIssLoaded = false;
            }
        } else {
            LOGGER.info("Iron's Spells 'n Spellbooks not found. Mana cost for short teleports will not be applied.");
        }
    }

    public static boolean isModLoaded() {
        return isIssLoaded;
    }

    public static boolean hasEnoughMana(Player player, int cost) {
        if (!isIssLoaded || getPlayerMagicDataMethod == null || getManaMethod == null) {
            return true; // If mod not loaded or methods not found, assume enough mana (or feature is off)
        }
        try {
            Object magicData = getPlayerMagicDataMethod.invoke(null, player);
            if (magicData != null) {
                float currentMana = (float) getManaMethod.invoke(magicData);
                return currentMana >= cost;
            }
        } catch (Exception e) {
            LOGGER.error("Error checking mana for player {}: {}", player.getName().getString(), e.getMessage());
        }
        return false; // Default to false if there's an error, to prevent free teleports if integration fails
    }

    public static boolean consumeMana(Player player, int cost) {
        if (!isIssLoaded || getPlayerMagicDataMethod == null || getManaMethod == null || setManaMethod == null || !(player instanceof ServerPlayer)) {
            return false; // Cannot consume if mod not loaded, methods not found, or not a server player
        }
        try {
            Object magicData = getPlayerMagicDataMethod.invoke(null, player);
            if (magicData != null) {
                float currentMana = (float) getManaMethod.invoke(magicData);
                if (currentMana >= cost) {
                    setManaMethod.invoke(magicData, currentMana - cost);
                    if (sendToPlayerMethod != null && clientboundSyncManaConstructor != null) {
                        Object packet = clientboundSyncManaConstructor.newInstance(magicData);
                        sendToPlayerMethod.invoke(null, packet, player);
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error consuming mana or syncing for player {}: {}", player.getName().getString(), e.getMessage());
        }
        return false;
    }

    public static boolean canTeleportWithMana(Player player) {
        if (!CommonConfig.short_tp_mana_cost_enabled || !isModLoaded()) {
            return true; // Mana cost not enabled or mod not loaded
        }
        if (!(player instanceof ServerPlayer)) {
           return true;
       }
        return hasEnoughMana(player, CommonConfig.short_tp_mana_cost_amount);
    }

    public static void tryConsumeMana(Player player) {
        if (CommonConfig.short_tp_mana_cost_enabled && isModLoaded() && player instanceof ServerPlayer) {
            consumeMana(player, CommonConfig.short_tp_mana_cost_amount);
        }
    }

    public static boolean clientHasEnoughMana() {
        if (!CommonConfig.short_tp_mana_cost_enabled || !isModLoaded()) {
            return true;
        }
        try {
            int currentMana = (int) getClientManaMethod.invoke(null);
            return currentMana >= CommonConfig.short_tp_mana_cost_amount;
        } catch (Exception e) {
            LOGGER.error("Error checking client-side mana: {}", e.getMessage());
        }
        return false;
    }
}

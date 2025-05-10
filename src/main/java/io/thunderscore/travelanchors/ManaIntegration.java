package io.thunderscore.travelanchors;

import io.thunderscore.travelanchors.config.ServerConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.network.ClientboundSyncMana;
import io.redspace.ironsspellbooks.setup.Messages;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManaIntegration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManaIntegration.class);
    private static final String ISS_MOD_ID = "irons_spellbooks";
    private static boolean isIssLoaded = false;

    static {
        isIssLoaded = ModList.get().isLoaded(ISS_MOD_ID);
        if (isIssLoaded) {
            LOGGER.info("Iron's Spells 'n Spellbooks integration enabled. Mana cost for short teleports will be applied if configured.");
        } else {
            LOGGER.info("Iron's Spells 'n Spellbooks not found. Mana cost for short teleports will not be applied.");
        }
    }

    public static boolean isModLoaded() {
        return isIssLoaded;
    }

    public static boolean hasEnoughMana(Player player, int cost) {
        if (!isIssLoaded) {
            return true; // If mod not loaded or feature is off, assume enough mana
        }
        try {
            MagicData magicData = MagicData.getPlayerMagicData(player);
            if (magicData != null) {
                return magicData.getMana() >= cost;
            }
        } catch (Throwable t) {
            LOGGER.error("Error checking mana for player {}: {}. Disabling ISS integration for safety.", player.getName().getString(), t.getMessage(), t);
            isIssLoaded = false;
        }
        return false;
    }

    public static boolean consumeMana(Player player, int cost) {
        if (!isIssLoaded || !(player instanceof ServerPlayer)) {
            return false; // Cannot consume if mod not loaded, or not a server player
        }
        ServerPlayer serverPlayer = (ServerPlayer) player;
        try {
            MagicData magicData = MagicData.getPlayerMagicData(serverPlayer);
            if (magicData != null) {
                if (magicData.getMana() >= cost) {
                    magicData.setMana(magicData.getMana() - cost);
                    ClientboundSyncMana packet = new ClientboundSyncMana(magicData);
                    Messages.sendToPlayer(packet, serverPlayer);
                    return true;
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Error consuming/syncing mana for player {}: {}. Disabling ISS integration for safety.", player.getName().getString(), t.getMessage(), t);
            isIssLoaded = false;
        }
        return false;
    }

    public static boolean canTeleportWithMana(Player player) {
        if (!ServerConfig.SHORT_TP_MANA_COST_ENABLED.get() || !isModLoaded()) {
            return true; // Mana cost not enabled or mod not loaded
        }
        if (!(player instanceof ServerPlayer)) {
           return true;
       }
        return hasEnoughMana(player, ServerConfig.SHORT_TP_MANA_COST_AMOUNT.get());
    }

    public static void tryConsumeMana(Player player) {
        if (ServerConfig.SHORT_TP_MANA_COST_ENABLED.get() && isModLoaded() && player instanceof ServerPlayer) {
            consumeMana(player, ServerConfig.SHORT_TP_MANA_COST_AMOUNT.get());
        }
    }

    public static boolean clientHasEnoughMana() {
        if (!ServerConfig.SHORT_TP_MANA_COST_ENABLED.get() || !isModLoaded()) {
            return true;
        }

        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                float currentMana = ClientMagicData.getPlayerMana();
                return currentMana >= ServerConfig.SHORT_TP_MANA_COST_AMOUNT.get();
            } catch (Throwable t) {
                LOGGER.error("Client-side error checking mana for Iron's Spells 'n Spellbooks: {}. Client UI might not reflect mana accurately.", t.getMessage(), t);
            }
        }
        
        return false;
    }
}

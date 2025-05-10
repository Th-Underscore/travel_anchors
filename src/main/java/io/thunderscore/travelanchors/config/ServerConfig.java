package io.thunderscore.travelanchors.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ServerConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue MAX_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue MAX_SHORT_TP_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue FIRE_TELEPORT_EVENT;
    public static final ForgeConfigSpec.IntValue SHORT_TP_COOLDOWN;
    public static final ForgeConfigSpec.BooleanValue SHORT_TP_MANA_COST_ENABLED;
    public static final ForgeConfigSpec.IntValue SHORT_TP_MANA_COST_AMOUNT;

    static {
        BUILDER.comment("Travel Anchors Server Configuration"); // Updated comment
        BUILDER.push("general");

        MAX_DISTANCE = BUILDER
                .comment("The maximum distance you are allowed to teleport.")
                .defineInRange("max_distance", 64.0, 1.0, Double.MAX_VALUE);

        MAX_SHORT_TP_DISTANCE = BUILDER
                .comment("The maximum distance you can short-range teleport with shift-click.")
                .defineInRange("max_short_tp_distance", 7.0, 2.0, 15.0);

        FIRE_TELEPORT_EVENT = BUILDER
                .comment("""
                        Fire an EntityTeleportEvent before allowing the teleport.
                        This allows other mods to prevent the teleport or change the destination.""")
                .define("fireTeleportEvent", true);
        
        SHORT_TP_COOLDOWN = BUILDER
                .comment("The cooldown in ticks for short-range teleports (shift-click). 20 ticks = 1 second.")
                .defineInRange("short_tp_cooldown", 30, 0, Integer.MAX_VALUE);

        BUILDER.pop();
        BUILDER.push("mana_integration");

        SHORT_TP_MANA_COST_ENABLED = BUILDER
                .comment("""
                        Enable mana cost for short-range teleports when 'Iron's Spells 'n Spellbooks' is installed.
                        This requires 'Iron's Spells 'n Spellbooks' to be present.""")
                .define("short_tp_mana_cost_enabled", true);

        SHORT_TP_MANA_COST_AMOUNT = BUILDER
                .comment("The amount of mana to consume for a short-range teleport if mana cost is enabled.")
                .defineInRange("short_tp_mana_cost_amount", 25, 0, Integer.MAX_VALUE);
        
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}

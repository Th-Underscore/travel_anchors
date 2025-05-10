package io.thunderscore.travelanchors.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue DISABLE_ELEVATION;
    public static final ForgeConfigSpec.BooleanValue KEEP_VELOCITY_ON_TELEPORT;

    static {
        BUILDER.comment("Travel Anchors Client Configuration");
        BUILDER.push("general");

        DISABLE_ELEVATION = BUILDER
                .comment("""
                        When this is set, you won't be able to use the elevation feature of travel anchors,
                        but you'll teleport to the anchor you're looking at when jumping on another travel anchor.
                        This is a client option so each player can adjust it as they prefer.""")
                .define("disable_elevation", false);

        KEEP_VELOCITY_ON_TELEPORT = BUILDER
                .comment("""
                        When this is set, you'll keep your velocity by default after using the short-range teleport.
                        Otherwise, your velocity will be reset by default. This behavior can be inverted by holding Ctrl.
                        This is a client option so each player can adjust it as they prefer.""")
                .define("keepVelocityOnTeleport", false);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}

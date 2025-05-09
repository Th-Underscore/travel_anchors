package de.castcrafter.travelanchors.config;

import org.moddingx.libx.annotation.config.RegisterConfig;
import org.moddingx.libx.config.Config;

@RegisterConfig(value = "client", client = true)
public class ClientConfig {
    
    @Config({
            "When this is set, you won't be able to use the elevation feature of travel anchors",
            "but you'll teleport to the anchor you're looking at when jumping on another travel anchor",
            "This is a client option so each player can adjust it as they prefer."
    })
    public static boolean disable_elevation = false;

    @Config({
            "When this is set, you'll keep your velocity by default after using the short-range teleport.",
            "Otherwise, your velocity will be reset by default. This behavior can be inverted by holding Ctrl.",
            "This is a client option so each player can adjust it as they prefer."
    })
    public static boolean keepVelocityOnTeleport = false;
}

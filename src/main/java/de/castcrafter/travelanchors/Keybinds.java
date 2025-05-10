package de.castcrafter.travelanchors;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class Keybinds {

    public static final String KEY_CATEGORY_TRAVELANCHORS = "key.categories.travelanchors";
    public static final String KEY_INVERT_VELOCITY = "key.travelanchors.invert_velocity";
    public static final String KEY_SHORT_TELEPORT = "key.travelanchors.short_teleport";

    public static KeyMapping INVERT_VELOCITY_KEY;
    public static KeyMapping SHORT_TELEPORT_KEY;

    public static void registerKeyMappings(final RegisterKeyMappingsEvent event) {
        INVERT_VELOCITY_KEY = new KeyMapping(
                KEY_INVERT_VELOCITY,
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_CONTROL, // Default to Left Control
                KEY_CATEGORY_TRAVELANCHORS
        );
        event.register(INVERT_VELOCITY_KEY);

        SHORT_TELEPORT_KEY = new KeyMapping(
                KEY_SHORT_TELEPORT,
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_ENTER, // Default to Keypad Enter
                KEY_CATEGORY_TRAVELANCHORS
        );
        event.register(SHORT_TELEPORT_KEY);
    }
}

package io.thunderscore.travelanchors;

import io.thunderscore.travelanchors.item.ItemTravelStaff;
import net.minecraft.world.item.Item;
import org.moddingx.libx.annotation.registration.RegisterClass;
import org.moddingx.libx.base.ItemBase;

@RegisterClass(registry = "ITEM")
public class ModItems {

    public static final ItemBase travelStaff = new ItemTravelStaff(TravelAnchors.getInstance(), new Item.Properties().stacksTo(1));
}

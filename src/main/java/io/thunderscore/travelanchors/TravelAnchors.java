package io.thunderscore.travelanchors;

import io.thunderscore.travelanchors.config.ClientConfig;
import io.thunderscore.travelanchors.config.ServerConfig;
import io.thunderscore.travelanchors.data.BlockLootProvider;
import io.thunderscore.travelanchors.data.BlockStatesProvider;
import io.thunderscore.travelanchors.data.ItemModelsProvider;
import io.thunderscore.travelanchors.data.RecipesProvider;
import io.thunderscore.travelanchors.network.Networking;
import io.thunderscore.travelanchors.render.TravelAnchorRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import org.moddingx.libx.datagen.DatagenSystem;
import org.moddingx.libx.mod.ModXRegistration;
import org.moddingx.libx.registration.RegistrationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

@Mod("travelanchors")
public final class TravelAnchors extends ModXRegistration {

    public static final Logger LOGGER = LoggerFactory.getLogger(TravelAnchors.class);

    private static TravelAnchors instance;
    private static Networking network;
    private static Tab tab;

    public TravelAnchors() {
        // super();

        instance = this;
        network = new Networking(this);
        tab = new Tab(this);

        // Register Forge configs
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "travelanchors-client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC, "travelanchors-server.toml");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::serverSetup);
        modEventBus.addListener(this::clientSetup);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modEventBus.addListener(Keybinds::registerKeyMappings));

        MinecraftForge.EVENT_BUS.register(new EventListener());
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.addListener(TravelAnchorRenderer::renderAnchors));

        // NOTE: DatagenSystem is from LibX. This might need to be adapted or removed.
        // For now, keeping it as the focus is on config.
        DatagenSystem.create(this, system -> {
            system.addDataProvider(BlockStatesProvider::new);
            system.addDataProvider(ItemModelsProvider::new);
            system.addDataProvider(BlockLootProvider::new);
            system.addDataProvider(RecipesProvider::new);
        });
    }

    @Nonnull
    public static TravelAnchors getInstance() {
        return instance;
    }

    @Nonnull
    public static Networking getNetwork() {
        return network;
    }

    @Nonnull
    public static Tab getTab() {
        return tab;
    }

    @Override
    protected void initRegistration(RegistrationBuilder builder) {
    //
    }

    @Override
    protected void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Loading TravelAnchors");
    }

    protected void serverSetup(final FMLDedicatedServerSetupEvent event) {
        LOGGER.info("Loading TravelAnchors server");
    }

    @Override
    protected void clientSetup(final FMLClientSetupEvent event) {

    }
}

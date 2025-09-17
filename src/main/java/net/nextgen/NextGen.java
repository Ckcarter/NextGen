package net.nextgen;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.item.CreativeModeTabs;
import net.nextgen.menu.ModMenuTypes;
import net.nextgen.network.ModMessages;
import net.nextgen.client.screen.CompanionSkinScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.nextgen.entity.ModEntityTypes;
import org.slf4j.Logger;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.nextgen.client.render.CompanionRenderer;
import net.nextgen.item.CompanionSummonerItem;



// The value here should match an entry in the META-INF/mods.toml file
@Mod(NextGen.MOD_ID)
public class NextGen {

    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "nextgen";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();


    // Register for all custom items that belong to the mod
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    // Item that summons and stores the combat companion
    public static final RegistryObject<Item> COMPANION_SUMMONER = ITEMS.register("companion_summoner",
            () -> new CompanionSummonerItem(new Item.Properties().stacksTo(1)));



    public NextGen() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register item and entity types
        ITEMS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModMenuTypes.register(modEventBus);


        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModMessages::register);

    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {

        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(COMPANION_SUMMONER);
        }
    }
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            event.enqueueWork(() -> MenuScreens.register(ModMenuTypes.COMPANION_SKIN.get(), CompanionSkinScreen::new));
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntityTypes.COMPANION.get(), CompanionRenderer::new);
        }
    }
}

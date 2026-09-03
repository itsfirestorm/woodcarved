package com.itsfirestorm.woodcarved;

import com.itsfirestorm.woodcarved.crafting.ModRecipeSerializers;
import com.itsfirestorm.woodcarved.registries.ModDataComponents;
import com.itsfirestorm.woodcarved.registries.ModMenuTypes;
import com.itsfirestorm.woodcarved.registries.ModRecipeTypes;
import com.itsfirestorm.woodcarved.registries.ModVillagers;
import com.itsfirestorm.woodcarved.registries.blocks.ModBlockEntities;
import com.itsfirestorm.woodcarved.registries.blocks.ModBlocks;
import com.itsfirestorm.woodcarved.registries.items.ModItems;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(woodcarved.MODID)
public class woodcarved {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "woodcarved";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public woodcarved(IEventBus modEventBus, ModContainer modContainer) {
        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (woodcarved) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the items to creative tabs
        modEventBus.addListener(this::addCreative);

        // Register mod items
        ModItems.ITEMS.register(modEventBus);

        // Register blocks
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);

        // Register recipe types
        ModRecipeTypes.RECIPE_TYPES.register(modEventBus);

        // Register recipe serializers
        ModRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);

        // Register mod menus
        ModMenuTypes.register(modEventBus);

        // Register villagers & poi
        ModVillagers.register(modEventBus);

        // Register data components
        ModDataComponents.register(modEventBus);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.CARVING_TABLE.asItem());
        }

        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.CARVING_BLADE);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}

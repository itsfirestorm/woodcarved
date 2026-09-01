package com.itsfirestorm.woodcarved.registries;

import com.itsfirestorm.woodcarved.trades.SerializedItemListing;
import com.itsfirestorm.woodcarved.trades.TradeDefinition;
import com.itsfirestorm.woodcarved.trades.VillagerTradeReloadListener;
import com.itsfirestorm.woodcarved.woodcarved;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;

@EventBusSubscriber(modid = woodcarved.MODID)
public class ModEvents {
    @SubscribeEvent
    public static void registerReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new VillagerTradeReloadListener());
    }

    @SubscribeEvent
    public static void addCustomVillagerTrades(VillagerTradesEvent event) {
        ResourceLocation professionId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(event.getType());

        for (int level = 1; level <= 5; level++) {
            List<TradeDefinition> definitionList = VillagerTradeReloadListener.getTrades(professionId, level);
            if (definitionList.isEmpty()) continue;

            List<VillagerTrades.ItemListing> listings = event.getTrades().get(level);
            for (TradeDefinition definition : definitionList) {
                listings.add(new SerializedItemListing(definition));
            }
        }
    }
}

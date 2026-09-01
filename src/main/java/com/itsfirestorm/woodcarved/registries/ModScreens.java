package com.itsfirestorm.woodcarved.registries;


import com.itsfirestorm.woodcarved.screen.carving_blade.CarvingBladeScreen;
import com.itsfirestorm.woodcarved.screen.carving_table.CarvingTableScreen;
import com.itsfirestorm.woodcarved.woodcarved;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = woodcarved.MODID, value = Dist.CLIENT)
public class ModScreens {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(
                ModMenuTypes.CARVING_TABLE.get(),
                CarvingTableScreen::new
        );
        event.register(
                ModMenuTypes.CARVING_BLADE.get(),
                CarvingBladeScreen::new
        );
    }
}

package com.itsfirestorm.woodcarved.registries;

import com.itsfirestorm.woodcarved.woodcarved;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, woodcarved.MODID);

    // Stored block ID from the block clicked before selection
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> CARVE_ORIGIN =
            DATA_COMPONENT_TYPES.register("carve_origin", () ->
                    DataComponentType.<ResourceLocation>builder()
                            .persistent(ResourceLocation.CODEC)
                            .networkSynchronized(ResourceLocation.STREAM_CODEC)
                            .build());

    // Stores block ID from last selected result from carving blade menu
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> SELECTED_CARVE_RESULT =
            DATA_COMPONENT_TYPES.register("selected_carve_result", () ->
                    DataComponentType.<ResourceLocation>builder()
                            .persistent(ResourceLocation.CODEC)
                            .networkSynchronized(ResourceLocation.STREAM_CODEC)
                            .build());

    public static void register(IEventBus modBus) {
        DATA_COMPONENT_TYPES.register(modBus);
    }
}

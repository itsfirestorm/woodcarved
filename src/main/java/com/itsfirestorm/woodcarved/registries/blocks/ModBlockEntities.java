package com.itsfirestorm.woodcarved.registries.blocks;

import com.itsfirestorm.woodcarved.block.entities.CarvingTableBlockEntity;
import com.itsfirestorm.woodcarved.woodcarved;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, woodcarved.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CarvingTableBlockEntity>> CARVING_TABLE =
            BLOCK_ENTITIES.register("carving_table_be",
                    () -> BlockEntityType.Builder.of(
                            CarvingTableBlockEntity::new,
                            ModBlocks.CARVING_TABLE.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
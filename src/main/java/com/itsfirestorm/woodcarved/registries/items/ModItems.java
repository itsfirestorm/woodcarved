package com.itsfirestorm.woodcarved.registries.items;

import com.itsfirestorm.woodcarved.block.CarvingTable;
import com.itsfirestorm.woodcarved.item.CarvingBlade;
import com.itsfirestorm.woodcarved.woodcarved;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(woodcarved.MODID);

    public static final DeferredItem<Item> CARVING_BLADE = ITEMS.register("carving_blade",
            () -> new CarvingBlade(new Item.Properties().durability(250).setNoRepair()));
}

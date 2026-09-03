package com.itsfirestorm.woodcarved.screen.carving_blade;

import com.itsfirestorm.woodcarved.crafting.CarvingRecipe;
import com.itsfirestorm.woodcarved.registries.ModDataComponents;
import com.itsfirestorm.woodcarved.registries.ModMenuTypes;
import com.itsfirestorm.woodcarved.registries.ModRecipeTypes;
import com.itsfirestorm.woodcarved.registries.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.*;

import java.util.List;

public class CarvingBladeMenu extends AbstractContainerMenu {

    public static final int CARVE_CONFIRMATION_ID = 1000; // This is high to not conflict with any recipe ID.

    private final Level level;
    private final BlockPos targetPos;
    private final Block originalBlock;
    private final InteractionHand hand;

    private final List<RecipeHolder<CarvingRecipe>> recipes;
    private int selectedRecipe = -1;
    private final ContainerData data;

    public CarvingBladeMenu(int id, Inventory inv, Level level, BlockPos targetPos,
                            InteractionHand interactionHand, ResourceLocation storedResult)
    {
        super(ModMenuTypes.CARVING_BLADE.get(), id);
        this.level = level;
        this.targetPos = targetPos;
        this.hand = interactionHand;
        this.originalBlock = level.getBlockState(targetPos).getBlock();
        this.recipes = buildRecipeList();
        this.selectedRecipe = findSelectedRecipe(storedResult);

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> selectedRecipe;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    selectedRecipe = value;
                }
            }

            @Override
            public int getCount() {
                return 1;
            }
        };
        this.addDataSlots(data);
    }

    // Client-side constructor, used for MenuType factory
    public CarvingBladeMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, inv.player.level(), buf.readBlockPos(), buf.readEnum(InteractionHand.class), readStoredResult(buf));
    }

    private static ResourceLocation readStoredResult(FriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readResourceLocation() : null;
    }

    private int findSelectedRecipe(ResourceLocation storedResult) {
        if (storedResult == null) return -1;

        for (int i = 0; i < recipes.size(); i++) {
            CarvingRecipe recipe = recipes.get(i).value();

            ItemStack result = recipe.getResultItem(level.registryAccess());

            if (result.isEmpty()) continue;

            Block resultBlock = Block.byItem(result.getItem());

            ResourceLocation resultId =
                    BuiltInRegistries.BLOCK.getKey(resultBlock);

            if (storedResult.equals(resultId)) {
                return i;
            }
        }

        return -1;
    }

    private List<RecipeHolder<CarvingRecipe>> buildRecipeList() {
        ItemStack asItem = new ItemStack(this.originalBlock.asItem());
        if (asItem.isEmpty()) {
            return List.of();
        }
        return this.level.getRecipeManager().getRecipesFor(
                ModRecipeTypes.CARVING.get(), new SingleRecipeInput(asItem), this.level)
                .stream()
                .filter(holder -> Block.byItem(
                        holder.value().getResultItem(this.level.registryAccess()).getItem()) != Blocks.AIR)
                .toList();
    }

    public List<RecipeHolder<CarvingRecipe>> getRecipes() {
        return this.recipes;
    }

    public int getNumRecipes() {
        return this.recipes.size();
    }

    public int getSelectedRecipe() {
        return this.selectedRecipe;
    }

    public void setSelectedRecipe(int index) {
        this.selectedRecipe = index;
    }

    private boolean isValidRecipe(int id) {
        return id >= 0 && id < this.recipes.size();
    }

    /**
     * @param player - Player Entity
     * @param id - Button ID
     * @return
     *
     * This simply controls the action to take when the player clicks a button in the screen.
     * The most obvious out of these are the confirm and cancel buttons.
     * If the player clicks the check button, it confirms the carve action.
     * Otherwise, it closes the menu.
     * <br/>
     * However, it also controls which recipe the player chooses and promptly selects it so the screen can
     * update accordingly.
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == CARVE_CONFIRMATION_ID) {
            this.confirmSelection(player);
            return true;
        }

        if (this.isValidRecipe(id)) {
            this.selectedRecipe = id;
            this.data.set(0, id);
            return true;
        }

        return false;
    }

    private void confirmSelection(Player player) {
        if (this.level.isClientSide) return;
        if (!this.isValidRecipe(this.selectedRecipe)) return;

        ItemStack tool = player.getItemInHand(this.hand);
        if (!tool.is(ModItems.CARVING_BLADE.get())) {
            player.closeContainer();
            return;
        }

        RecipeHolder<CarvingRecipe> recipe = this.recipes.get(this.selectedRecipe);
        Block resultBlock = Block.byItem(recipe.value().getResultItem(this.level.registryAccess()).getItem());
        if (resultBlock == Blocks.AIR) {
            player.closeContainer();
            return;
        }

        ResourceLocation originId = BuiltInRegistries.BLOCK.getKey(this.originalBlock);
        ResourceLocation resultId = BuiltInRegistries.BLOCK.getKey(resultBlock);

        tool.set(ModDataComponents.CARVE_ORIGIN.get(), originId);
        tool.set(ModDataComponents.SELECTED_CARVE_RESULT.get(), resultId);

        player.closeContainer();
    }

    @Override
    public boolean stillValid(Player player) {
        if (!this.level.getBlockState(this.targetPos).is(this.originalBlock)) {
            return false;
        }
        return this.targetPos.closerToCenterThan(player.position(), 8.0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }
}

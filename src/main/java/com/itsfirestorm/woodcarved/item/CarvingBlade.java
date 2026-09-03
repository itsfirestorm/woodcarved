package com.itsfirestorm.woodcarved.item;

import com.itsfirestorm.woodcarved.crafting.CarvingRecipe;
import com.itsfirestorm.woodcarved.registries.ModDataComponents;
import com.itsfirestorm.woodcarved.registries.ModRecipeTypes;
import com.itsfirestorm.woodcarved.screen.carving_blade.CarvingBladeMenu;
import com.itsfirestorm.woodcarved.util.CarveHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CarvingBlade extends Item {

    public CarvingBlade(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        ItemStack tool = context.getItemInHand();

        if (player == null) {
            return InteractionResult.FAIL;
        }

        ResourceLocation storedOrigin = tool.get(ModDataComponents.CARVE_ORIGIN.get());
        ResourceLocation storedResult = tool.get(ModDataComponents.SELECTED_CARVE_RESULT.get());
        ResourceLocation clickedBlock = BuiltInRegistries.BLOCK.getKey(state.getBlock());

        if (player.isShiftKeyDown()) {
            if (clickedBlock.equals(storedOrigin)) {
                return openSelectionMenu(level, pos, state, player, context.getHand(), storedResult);
            }
            return clearSelection(level, player, tool);
        }

        if (storedOrigin != null && storedResult != null) {
            return applySelection(level, pos, state, player, context.getHand(),
                    storedOrigin, storedResult, context.getClickLocation());
        }

        return openSelectionMenu(level, pos, state, player, context.getHand(), storedResult);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack tool = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            clearSelection(level, player, tool);
            return InteractionResultHolder.sidedSuccess(tool, level.isClientSide);
        }
        return InteractionResultHolder.pass(tool);
    }

    private InteractionResult clearSelection(Level level, Player player, ItemStack tool) {
        if (!level.isClientSide) {
            tool.remove(ModDataComponents.CARVE_ORIGIN.get());
            tool.remove(ModDataComponents.SELECTED_CARVE_RESULT.get());
            player.displayClientMessage(Component.translatable("message.woodcarved.selection_cleared"), true);
            level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult openSelectionMenu(Level level, BlockPos pos, BlockState blockState,
                                                Player player, InteractionHand hand, ResourceLocation storedResult)
    {
        ItemStack blockAsItem = new ItemStack(blockState.getBlock().asItem());
        if (blockAsItem.isEmpty()) {
           return InteractionResult.FAIL;
        }

        List<RecipeHolder<CarvingRecipe>> recipes = level.getRecipeManager()
                .getRecipesFor(ModRecipeTypes.CARVING.get(), new SingleRecipeInput(blockAsItem), level);

        if (recipes.isEmpty()) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new CarvingBladeMenu(id, inv, level, pos, hand, storedResult),
                    Component.translatable("menu.woodcarved.carving_blade")
            ), buf -> {
                buf.writeBlockPos(pos);
                buf.writeEnum(hand);

                if (storedResult != null) {
                    buf.writeBoolean(true);
                    buf.writeResourceLocation(storedResult);
                } else {
                    buf.writeBoolean(false);
                }
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult applySelection(Level level, BlockPos pos, BlockState blockState, Player player,
                                             InteractionHand hand, ResourceLocation storedOrigin,
                                             ResourceLocation storedResult, Vec3 hitPos)
    {
        ResourceLocation clickedId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());

        if (!clickedId.equals(storedOrigin)) {
            return fail(level, player);
        }

        ItemStack blockAsItem = new ItemStack(blockState.getBlock().asItem());
        if (blockAsItem.isEmpty()) {
            return fail(level, player);
        }

        List<RecipeHolder<CarvingRecipe>> recipes = level.getRecipeManager()
                .getRecipesFor(ModRecipeTypes.CARVING.get(), new SingleRecipeInput(blockAsItem), level);

        RecipeHolder<CarvingRecipe> matched = recipes.stream()
                .filter(holder -> {
                    Block resultBlock = Block.byItem(
                            holder.value().getResultItem(level.registryAccess()).getItem());
                    return storedResult.equals(BuiltInRegistries.BLOCK.getKey(resultBlock));
                })
                .findFirst()
                .orElse(null);

        if (matched == null) {
            return fail(level, player);
        }

        if (!level.isClientSide) {
            CarveHelper.carve(level, pos, blockState.getBlock(), matched, player, hand, hitPos);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult fail(Level level, Player player) {
        if (!level.isClientSide) {
            player.displayClientMessage(
                    Component.translatable("message.woodcarved.cannot_apply").withStyle(ChatFormatting.RED),
                    true
            );
            level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.PLAYERS, 1.0F, 0.2F);
        }
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag)
    {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        ResourceLocation originId = stack.get(ModDataComponents.CARVE_ORIGIN.get());
        ResourceLocation resultId = stack.get(ModDataComponents.SELECTED_CARVE_RESULT.get());

        if (originId == null) {
            tooltipComponents.add(Component.translatable("tooltip.woodcarved.carving_blade.origin", "N/A")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        if (resultId == null) {
            tooltipComponents.add(Component.translatable("tooltip.woodcarved.carving_blade.result", "N/A")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        Block originBlock = BuiltInRegistries.BLOCK.get(originId);
        Block resultBlock = BuiltInRegistries.BLOCK.get(resultId);

        tooltipComponents.add(Component.translatable("tooltip.woodcarved.carving_blade.origin", originBlock.getName())
                .withStyle(ChatFormatting.AQUA));
        tooltipComponents.add(Component.translatable("tooltip.woodcarved.carving_blade.result", resultBlock.getName())
                .withStyle(ChatFormatting.GOLD));
    }
}

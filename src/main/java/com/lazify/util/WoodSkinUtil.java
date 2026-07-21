package com.lazify.util;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Maps placed wood blocks to Bordic activeWoodType ids.
 */
public final class WoodSkinUtil {

    private WoodSkinUtil() {}

    public static boolean isHoldingWood(EntityPlayer player) {
        return player != null && fromItemStack(player.getHeldItem()) != null;
    }

    public static String fromItemStack(ItemStack stack) {
        if (stack == null) return null;
        Item item = stack.getItem();
        int meta = stack.getMetadata() & 7;

        if (item == Item.getItemFromBlock(Blocks.planks)) {
            return plankId(meta);
        }
        if (item == Item.getItemFromBlock(Blocks.log)) {
            return oldLogId(meta);
        }
        if (item == Item.getItemFromBlock(Blocks.log2)) {
            return newLogId(meta);
        }
        return null;
    }

    public static boolean isWood(IBlockState state) {
        return fromBlockState(state) != null;
    }

    public static String fromStateId(int stateId) {
        try {
            return fromBlockState(Block.getStateById(stateId));
        } catch (Exception e) {
            return null;
        }
    }

    public static String fromBlockState(IBlockState state) {
        if (state == null) return null;
        Block block = state.getBlock();

        if (block == Blocks.planks) {
            return plankId(block.getMetaFromState(state));
        }

        if (block == Blocks.log) {
            return oldLogId(block.getMetaFromState(state) & 3);
        }

        if (block == Blocks.log2) {
            return newLogId(block.getMetaFromState(state) & 1);
        }

        return null;
    }

    private static String plankId(int meta) {
        switch (meta) {
            case 0: return "woodSkin_oak";
            case 1: return "woodSkin_spruce";
            case 2: return "woodSkin_birch";
            case 3: return "woodSkin_jungle";
            case 4: return "woodSkin_acacia";
            case 5: return "woodSkin_dark_oak";
            default: return null;
        }
    }

    private static String oldLogId(int type) {
        switch (type) {
            case 0: return "woodSkin_oak_log";
            case 1: return "woodSkin_spruce_log";
            case 2: return "woodSkin_birch_log";
            case 3: return "woodSkin_jungle_log";
            default: return null;
        }
    }

    private static String newLogId(int type) {
        switch (type) {
            case 0: return "woodSkin_acacia_log";
            case 1: return "woodSkin_dark_oak_log";
            default: return null;
        }
    }
}

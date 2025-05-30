package com.github.jikoo.captcha.util;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Observer;
import org.bukkit.block.data.type.RedstoneRail;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

public enum BlockUtil {
  ;

  public static boolean hasRightClickFunction(@NotNull PlayerInteractEvent event) {

    if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
      return false;
    }

    Block block = event.getClickedBlock();
    Material blockType = block.getType();

    if (blockType.isInteractable()) {
      return true;
    }

    ItemStack hand = ItemUtil.getHeldItem(event);
    Material handType = hand.getType();

    if (blockType == Material.END_STONE) {
      // Special case: player is probably attempting to bottle dragon's breath
      return block.getWorld().getEnvironment() == World.Environment.THE_END
          && handType == Material.GLASS_BOTTLE;
    }

    if (handType == Material.FLINT_AND_STEEL || handType == Material.FIRE_CHARGE) {
      return true;
    }
    if (Tag.ITEMS_AXES.isTagged(handType)
        && (Tag.LOGS.isTagged(blockType) || blockType == Material.BAMBOO_BLOCK || blockType.getKey().getKey().startsWith("waxed_"))) {
      return true;
    }
    if (Tag.ITEMS_SHOVELS.isTagged(handType)
        && (Tag.DIRT.isTagged(blockType) || Tag.CAMPFIRES.isTagged(blockType))) {
      // DIRT isn't entirely accurate, includes extras like mud, but whatever.
      return true;
    }
    if (Tag.ITEMS_HOES.isTagged(handType) && Tag.DIRT.isTagged(blockType)) {
      // As above, some extras, but it probably isn't a big deal.
      return true;
    }

    BlockData blockData = block.getBlockData();
    if (blockData instanceof Bed || blockData instanceof Levelled) {
      return true;
    }

    if (blockData instanceof Powerable) {
      return !(blockData instanceof Observer || blockData instanceof RedstoneRail);
    }

    if (blockData instanceof Waterlogged
        && (handType == Material.BUCKET || handType == Material.WATER_BUCKET)) {
      return true;
    }

    BlockState state = block.getState(false);
    if (state instanceof InventoryHolder || state instanceof TileState) {
      // TileState also covers cases like BrushableBlock.
      return true;
    }

    if (handType == Material.GLASS_BOTTLE) {
      RayTraceResult rayTraceResult =
          event.getPlayer().rayTraceBlocks(6, FluidCollisionMode.ALWAYS);
      if (rayTraceResult == null) {
        return false;
      }
      Block hitBlock = rayTraceResult.getHitBlock();
      return hitBlock != null
          && (hitBlock.getType() == Material.WATER || hitBlock.getType() == Material.CAULDRON);
    }

    return false;
  }

}

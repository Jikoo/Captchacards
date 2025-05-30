package com.github.jikoo.captcha.util;

import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A set of useful methods for inventory functions.
 */
public enum ItemUtil {
  ;

  public static int getAddFailures(Map<Integer, ItemStack> failures) {
    int count = 0;
    for (ItemStack is : failures.values()) {
      count += is.getAmount();
    }
    return count;
  }

  /**
   * Reduces an ItemStack by the given quantity. If the ItemStack will have a quantity of 0,
   * returns air.
   *
   * @param itemStack the ItemStack to reduce
   * @param amount the amount to reduce the ItemStack by
   * @return the reduced ItemStack
   */
  public @NotNull static ItemStack decrement(@Nullable ItemStack itemStack, int amount) {
    if (itemStack == null || itemStack.getType() == Material.AIR) {
      return ItemStack.of(Material.AIR);
    }
    if (itemStack.getAmount() > amount) {
      itemStack.setAmount(itemStack.getAmount() - amount);
    } else {
      itemStack = ItemStack.of(Material.AIR);
    }
    return itemStack;
  }

  public static void decrementHeldItem(@NotNull PlayerInteractEvent event, int amount) {
    boolean main = isMainHand(event);
    PlayerInventory inv = event.getPlayer().getInventory();
    setHeldItem(inv, main, decrement(getHeldItem(inv, main), amount));
  }

  public static boolean isMainHand(@NotNull PlayerInteractEvent event) {
    return event.getHand() == EquipmentSlot.HAND;
  }

  public @NotNull static ItemStack getHeldItem(@NotNull PlayerInteractEvent event) {
    return getHeldItem(event.getPlayer().getInventory(), isMainHand(event));
  }

  private @NotNull static ItemStack getHeldItem(@NotNull PlayerInventory inv, boolean mainHand) {
    return mainHand ? inv.getItemInMainHand() : inv.getItemInOffHand();
  }

  public static void setHeldItem(
      @NotNull PlayerInventory inv, boolean mainHand, @Nullable ItemStack item) {
    if (mainHand) {
      inv.setItemInMainHand(item);
    } else {
      inv.setItemInOffHand(item);
    }
  }

  /**
   * Checks if there is space in an Inventory to add an ItemStack.
   *
   * @param is the ItemStack
   * @param inv the Inventory to check
   * @return true if the ItemStack can be fully added
   */
  public static boolean hasSpaceFor(@Nullable ItemStack is, @NotNull Inventory inv) {
    if (is == null || is.getType() == Material.AIR) {
      return true;
    }
    int toAdd = is.getAmount();
    int maxStack = is.getMaxStackSize();
    for (ItemStack invStack : inv.getStorageContents()) {
      if (invStack == null || invStack.getType() == Material.AIR) {
        return true;
      }
      if (!invStack.isSimilar(is)) {
        continue;
      }
      toAdd -= maxStack - invStack.getAmount();
      if (toAdd <= 0) {
        return true;
      }
    }
    return false;
  }

}

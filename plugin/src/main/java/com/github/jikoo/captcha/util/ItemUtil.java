package com.github.jikoo.captcha.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
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
  public static @NotNull ItemStack decrement(@Nullable ItemStack itemStack, int amount) {
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

}

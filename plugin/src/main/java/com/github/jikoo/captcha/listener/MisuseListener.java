package com.github.jikoo.captcha.listener;

import com.github.jikoo.captcha.CaptchaManager;
import com.google.errorprone.annotations.Keep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MisuseListener implements Listener {

  @Keep
  @EventHandler(ignoreCancelled = true)
  private void onInventoryClick(InventoryClickEvent event) {
    // Block inserting into an anvil or enchanting table.
    Inventory topInv = event.getView().getTopInventory();
    if (!(topInv instanceof AnvilInventory) && !(topInv instanceof EnchantingInventory)) {
      return;
    }
    boolean isTopSlot = event.getRawSlot() == event.getView().convertSlot(event.getRawSlot());
    switch (event.getClick()) {
      case NUMBER_KEY -> {
        // Hotbar swap: item on hotbar is swapped with the item in the hovered slot.
        ItemStack hotbar = event.getView().getBottomInventory().getItem(event.getHotbarButton());
        if (isTopSlot && CaptchaManager.isCaptcha(hotbar)) {
          event.setCancelled(true);
        }
      }
      case LEFT, RIGHT -> {
        // Left/right click: stack insert/swap or single insert.
        if (isTopSlot && CaptchaManager.isCaptcha(event.getCursor())) {
          event.setCancelled(true);
        }
      }
      case SHIFT_LEFT, SHIFT_RIGHT -> {
        // Shift click: push from one inventory to another.
        if (!isTopSlot && CaptchaManager.isCaptcha(event.getCurrentItem())) {
          event.setCancelled(true);
        }
      }
      default -> {}
    }
  }

  @Keep
  @EventHandler(ignoreCancelled = true)
  private void onInventoryDrag(InventoryDragEvent event) {
    // Block dragging into an anvil or enchanting table.
    Inventory topInv = event.getView().getTopInventory();
    if (!(topInv instanceof AnvilInventory) && !(topInv instanceof EnchantingInventory)) {
      return;
    }
    event.getRawSlots().removeIf(integer -> integer < topInv.getSize());
  }

  @Keep
  @EventHandler
  private void onPrepareItemEnchant(@NotNull PrepareItemEnchantEvent event) {
    // Block enchanting.
    if (CaptchaManager.isCaptcha(event.getItem())) {
      event.setCancelled(true);
    }
  }

}

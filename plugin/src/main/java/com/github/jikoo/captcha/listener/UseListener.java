package com.github.jikoo.captcha.listener;

import com.github.jikoo.captcha.CaptchaManager;
import com.github.jikoo.captcha.util.BlockUtil;
import com.github.jikoo.captcha.util.ItemUtil;
import com.google.errorprone.annotations.Keep;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class UseListener implements Listener {

  private final @NotNull CaptchaManager captchas;
  private final @NotNull Logger logger;

  public UseListener(@NotNull CaptchaManager captchas, @NotNull Logger logger) {
    this.captchas = captchas;
    this.logger = logger;
  }

  @Keep
  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  private void handleCaptcha(@NotNull InventoryClickEvent event) {
    boolean hotbar = false;
    switch (event.getClick()) {
      case NUMBER_KEY:
        hotbar = true;
        break;
      case LEFT:
      case RIGHT:
        if (event.getCursor().getType() == Material.AIR
            || event.getCurrentItem() == null
            || event.getCurrentItem().getType() == Material.AIR) {
          return;
        }
        break;
      case CONTROL_DROP:
      case CREATIVE:
      case DOUBLE_CLICK:
      case DROP:
      case MIDDLE:
      case SHIFT_LEFT:
      case SHIFT_RIGHT:
      case WINDOW_BORDER_LEFT:
      case WINDOW_BORDER_RIGHT:
      case UNKNOWN:
      default:
        return;
    }
    ItemStack blankCaptcha;
    ItemStack toCaptcha;
    if (hotbar) {
      blankCaptcha = event.getView().getBottomInventory().getItem(event.getHotbarButton());
      toCaptcha = event.getCurrentItem();
    } else {
      blankCaptcha = event.getCurrentItem();
      toCaptcha = event.getCursor();
    }

    if (toCaptcha == null
        || !CaptchaManager.isBlankCaptcha(blankCaptcha)
        || captchas.canNotCaptcha(toCaptcha)
        || CaptchaManager.isBlankCaptcha(toCaptcha)) {
      return;
    }

    ItemStack captchaItem = captchas.getCaptchaForItem(toCaptcha);
    event.setResult(Event.Result.DENY);

    if (captchaItem == null) {
      return;
    }

    // Decrement captcha stack
    if (hotbar) {
      event
          .getView()
          .getBottomInventory()
          .setItem(event.getHotbarButton(), ItemUtil.decrement(blankCaptcha, 1));
      event.setCurrentItem(null);
    } else {
      event.setCurrentItem(ItemUtil.decrement(blankCaptcha, 1));
      // No alternative. Functions fine.
      event.setCursor(null);
    }

    // Add to bottom inventory first
    int leftover =
        ItemUtil.getAddFailures(event.getView().getBottomInventory().addItem(captchaItem));
    if (leftover > 0) {
      // Add to top, bottom was full.
      leftover = ItemUtil.getAddFailures(event.getView().getTopInventory().addItem(captchaItem));
    }
    if (leftover > 0) {
      if (hotbar) {
        // Drop rather than delete.
        event.getWhoClicked().getWorld().dropItem(event.getWhoClicked().getLocation(), captchaItem);
      } else {
        // Set cursor to captcha.
        event.setCursor(captchaItem);
      }
    }
  }

  @Keep
  @EventHandler(priority = EventPriority.HIGH)
  private void onPlayerInteract(@NotNull PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_AIR
        && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }

    EquipmentSlot hand = event.getHand();

    if (hand == null) {
      return;
    }

    PlayerInventory inventory = event.getPlayer().getInventory();
    ItemStack held = inventory.getItem(hand);
    if (!CaptchaManager.isUsedCaptcha(held) || BlockUtil.hasRightClickFunction(event)) {
      return;
    }

    ItemStack captchaStack = captchas.getItemByCaptcha(held);
    if (captchaStack == null || captchaStack.isSimilar(held)) {
      String hash = CaptchaManager.getHashFromCaptcha(held);
      logger.warning(() -> "Invalid captcha belonging to " + event.getPlayer().getName() + ": " + (hash == null ? held.toString() : hash));
      return;
    }

    // TODO for last captcha, always place in slot
    if (decrementedHandIsEmpty(held, inventory, hand)) {
      inventory.setItem(hand, captchaStack);
    } else if (ItemUtil.hasSpaceFor(captchaStack, event.getPlayer().getInventory())) {
      event.getPlayer().getInventory().addItem(captchaStack);
    } else {
      event
          .getPlayer()
          .getWorld()
          .dropItem(event.getPlayer().getEyeLocation(), captchaStack)
          .setVelocity(event.getPlayer().getLocation().getDirection().multiply(0.4));
    }

    event.getPlayer().updateInventory();
  }

  private boolean decrementedHandIsEmpty(@NotNull ItemStack held, @NotNull PlayerInventory inventory, @NotNull EquipmentSlot hand) {
    int amount = held.getAmount() - 1;
    if (amount > 0) {
      held.setAmount(amount);
    } else {
      // We'll clobber item anyway.
      return false;
    }
    inventory.setItem(hand, held);
    return true;
  }

}

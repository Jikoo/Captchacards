package com.github.jikoo.captcha.listener;

import com.github.jikoo.captcha.CaptchaManager;
import com.github.jikoo.captcha.util.BlockUtil;
import com.github.jikoo.captcha.util.ItemUtil;
import com.google.errorprone.annotations.Keep;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
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
        // TODO fire event and respect cancellation
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

    Player player = event.getPlayer();
    PlayerInventory inventory = player.getInventory();
    ItemStack held = inventory.getItem(hand);
    if (!CaptchaManager.isUsedCaptcha(held) || BlockUtil.hasRightClickFunction(event)) {
      return;
    }

    ItemStack captchaStack = captchas.getItemByCaptcha(held);
    if (captchaStack == null || captchaStack.isSimilar(held)) {
      String hash = CaptchaManager.getHashFromCaptcha(held);
      logger.warning(() -> "Invalid captcha belonging to " + player.getName() + ": " + (hash == null ? held.toString() : hash));
      return;
    }

    if (decrementedHandIsEmpty(inventory, hand, held)) {
      // If this was the last captcha, place the contents directly in the same slot.
      inventory.setItem(hand, captchaStack);
      return;
    }

    ItemStack[] contents = inventory.getStorageContents();
    if (addOrDrop(player, contents, captchaStack)) {
      // Update inventory contents.
      inventory.setStorageContents(contents);
    } else {
      // If dropping excess was denied, cannot open. Undo hand modification.
      held.setAmount(held.getAmount() + 1);
      inventory.setItem(hand, held);
    }
  }

  private boolean decrementedHandIsEmpty(
      @NotNull PlayerInventory inventory,
      @NotNull EquipmentSlot hand,
      @NotNull ItemStack held
  ) {
    int amount = held.getAmount() - 1;

    if (amount <= 0) {
      // Don't bother setting slot to air - we'll clobber consumed card with contents.
      return true;
    }

    // Update item.
    held.setAmount(amount);
    inventory.setItem(hand, held);
    return false;
  }

  private boolean addOrDrop(@NotNull Player player, ItemStack @NotNull [] contents, @NotNull ItemStack added) {
    if (added.getType() == Material.AIR) {
      // Air is always "added" successfully.
      return true;
    }

    // Add to existing stacks first.
    addToLikeStacks(contents, added);

    // Add any remainder to first empty slot.
    if (added.getAmount() > 0 && !addToEmptyStack(contents, added)) {
      // Drop any remainder.
      Item item = player.dropItem(added);
      // If item was not added to world, drop was cancelled.
      return item != null;
    }

    return true;
  }

  private void addToLikeStacks(ItemStack @NotNull [] contents, @NotNull ItemStack added) {
    int remainderToAdd = added.getAmount();

    for (ItemStack content : contents) {
      // If the stack is not similar, skip.
      if (content == null || !content.isSimilar(added)) {
        continue;
      }

      int contentAmount = content.getAmount();
      int addable = content.getMaxStackSize() - contentAmount;

      // If the stack is full, skip.
      if (addable <= 0) {
        continue;
      }

      if (addable < remainderToAdd) {
        // Add number possible to existing stack.
        content.setAmount(content.getMaxStackSize());
        remainderToAdd -= addable;
      } else {
        // If entire amount to add fits, finish.
        content.setAmount(contentAmount + remainderToAdd);
        remainderToAdd = 0;
        break;
      }
    }

    added.setAmount(remainderToAdd);
  }

  private boolean addToEmptyStack(ItemStack @NotNull [] contents, @NotNull ItemStack added) {
    for (int i = 0; i < contents.length; ++i) {
      ItemStack content = contents[i];
      if (content == null || content.isEmpty()) {
        // There should be no oversized stacks here - the original contents were an in-inventory item.
        // Directly set item rather than worry about spreading out oversized.
        contents[i] = added;
        return true;
      }
    }

    // No free slots.
    return false;
  }

}

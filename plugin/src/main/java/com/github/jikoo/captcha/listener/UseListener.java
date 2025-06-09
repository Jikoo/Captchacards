package com.github.jikoo.captcha.listener;

import com.github.jikoo.captcha.CaptchaManager;
import com.github.jikoo.captcha.util.BlockUtil;
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
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    CaptchaAction action = switch (event.getClick()) {
      case LEFT, RIGHT -> new NormalCaptcha(event);
      case NUMBER_KEY -> new HotbarCaptcha(event);
      case SWAP_OFFHAND -> new OffHandCaptcha(event);
      default -> null;
    };

    if (action == null) {
      return;
    }

    ItemStack blank = action.getBlank();
    ItemStack content = action.getContent();

    if (captchas.canNotCaptcha(content)
        || !CaptchaManager.isBlankCaptcha(blank)
        || CaptchaManager.isBlankCaptcha(content)) {
      return;
    }

    ItemStack captchaItem = captchas.getCaptchaForItem(content);
    event.setResult(Event.Result.DENY);

    if (captchaItem == null) {
      return;
    }

    // Consume item and decrement captcha stack.
    action.setContent(null);
    blank.setAmount(blank.getAmount() - 1);
    action.setBlank(blank.isEmpty() ? ItemStack.of(Material.AIR) : blank);

    if (addToInventories(event, captchaItem) || action.addOverflowSafe(captchaItem)) {
      return;
    }

    // Dropping item failed for hotbar/off-hand swap. Undo changes.
    blank.setAmount(blank.getAmount() + 1);
    action.setBlank(blank);
    action.setContent(content);
  }

  /**
   * This method is specifically for adding an item with amount 1! Higher amounts may cause inconsistent losses.
   */
  private boolean addToInventories(@NotNull InventoryClickEvent event, ItemStack captchaItem) {
    InventoryView view = event.getView();
    Inventory[] inventories;
    if (event.getSlot() == event.getRawSlot()) {
      // If the mouse is in the top inventory, prefer it.
      inventories = new Inventory[2];
      inventories[0] = view.getTopInventory();
      inventories[1] = view.getBottomInventory();
    } else if (view.getType() != InventoryType.CRAFTING) {
      // If the mouse is in the bottom inventory and the view is not the player's default view, use both.
      inventories = new Inventory[2];
      inventories[0] = view.getBottomInventory();
      inventories[1] = view.getTopInventory();
    } else {
      // Otherwise, the upper inventory is the player's 2x2 crafting grid and should not be used.
      inventories = new Inventory[1];
      inventories[0] = view.getBottomInventory();
    }

    ItemStack[][] allContents = new ItemStack[inventories.length][0];

    for (int i = 0; i < inventories.length; ++i) {
      Inventory inventory = inventories[i];
      ItemStack[] contents = inventory.getStorageContents();

      // Store contents for later use.
      allContents[i] = contents;

      // Add to existing stacks.
      addToLikeStacks(contents, captchaItem);
      if (captchaItem.isEmpty()) {
        inventory.setStorageContents(contents);
        return true;
      }
    }

    // Add to empty slot.
    for (int i = 0; i < inventories.length; ++i) {
      Inventory inventory = inventories[i];
      ItemStack[] contents = allContents[i];
      if (addToEmptyStack(contents, captchaItem)) {
        inventory.setStorageContents(contents);
        return true;
      }
    }

    // No slots where item can fit. Should never happen - we always consume an item.
    return false;
  }

  private interface CaptchaAction {

    @Nullable ItemStack getBlank();

    @Nullable ItemStack getContent();

    void setBlank(@Nullable ItemStack blank);

    void setContent(@Nullable ItemStack content);

    boolean addOverflowSafe(@NotNull ItemStack captcha);

  }

  private record NormalCaptcha(InventoryClickEvent event) implements CaptchaAction {

    @Override
    public @Nullable ItemStack getBlank() {
      return event.getCurrentItem();
    }

    @Override
    public @NotNull ItemStack getContent() {
      return event.getCursor();
    }

    @Override
    public void setBlank(@Nullable ItemStack blank) {
      event.setCurrentItem(blank);
    }

    @Override
    public void setContent(@Nullable ItemStack content) {
      event.getView().setCursor(content);
    }

    @Override
    public boolean addOverflowSafe(@NotNull ItemStack captcha) {
      event.getView().setCursor(captcha);
      return true;
    }

  }

  private record HotbarCaptcha(InventoryClickEvent event) implements CaptchaAction {

    @Override
    public ItemStack getBlank() {
      return event.getView().getBottomInventory().getItem(event.getHotbarButton());
    }

    @Override
    public ItemStack getContent() {
      return event.getCurrentItem();
    }

    @Override
    public void setBlank(@Nullable ItemStack blank) {
      event.getView().getBottomInventory().setItem(event.getHotbarButton(), blank);
    }

    @Override
    public void setContent(@Nullable ItemStack content) {
      event.setCurrentItem(content);
    }

    @Override
    public boolean addOverflowSafe(@NotNull ItemStack captcha) {
      return event.getWhoClicked().dropItem(captcha) != null;
    }

  }

  private record OffHandCaptcha(InventoryClickEvent event) implements CaptchaAction {

    @Override
    public @NotNull ItemStack getBlank() {
      return event.getWhoClicked().getInventory().getItemInOffHand();
    }

    @Override
    public @Nullable ItemStack getContent() {
      return event.getCurrentItem();
    }

    @Override
    public void setBlank(@Nullable ItemStack blank) {
      event.getWhoClicked().getInventory().setItemInOffHand(blank);
    }

    @Override
    public void setContent(@Nullable ItemStack content) {
      event.setCurrentItem(content);
    }

    @Override
    public boolean addOverflowSafe(@NotNull ItemStack captcha) {
      return event.getWhoClicked().dropItem(captcha) != null;
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

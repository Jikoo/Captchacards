package com.github.jikoo.captcha.command;

import com.github.jikoo.captcha.CaptchaManager;
import com.github.jikoo.captcha.CaptchaPlugin;
import com.github.jikoo.captcha.util.lang.QuantityReplacement;
import com.github.jikoo.captcha.util.lang.ComponentLangManager;
import com.github.jikoo.captcha.util.lang.Messages;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class BatchCaptchaCommand extends Command implements PluginIdentifiableCommand {

  private static final String PERM_BASE = "captcha.command.batch.";
  private static final String PERM_FREE = PERM_BASE + "free";

  private final @NotNull CaptchaPlugin plugin;
  private final @NotNull ComponentLangManager lang;
  private final @NotNull CaptchaManager captcha;

  public BatchCaptchaCommand(
      @NotNull CaptchaPlugin plugin,
      @NotNull ComponentLangManager lang,
      @NotNull CaptchaManager captcha
  ) {
    super("batchcaptcha");
    setDescription("Captcha in bulk!");
    setPermission(PERM_BASE + "use");
    this.plugin = plugin;
    this.lang = lang;
    this.captcha = captcha;
  }

  @Override
  public boolean execute(
      @NotNull CommandSender sender,
      @NotNull String commandLabel,
      @NotNull String @NotNull [] args
  ) {
    if (!(sender instanceof Player player)) {
      lang.sendComponent(sender, Messages.COMMAND_DENIAL_REQUIRES_PLAYER);
      return true;
    }

    ItemStack item = player.getInventory().getItemInMainHand();

    // If the held item cannot be put into a captcha, fail.
    // For quality of life purposes, the item does not have to be a max stack.
    if (captcha.canNotCaptcha(item, false)) {
      lang.sendComponent(sender, Messages.COMMAND_DENIAL_CANNOT_CAPTCHA);
      return true;
    }

    PlayerInventory inventory = player.getInventory();
    ItemStack[] contents = inventory.getStorageContents();

    // Match blank captchacards in the inventory.
    MatchedItems blanks = matchBlanks(args, player, contents);

    if (blanks.amount <= 0) {
      lang.sendComponent(sender, Messages.COMMAND_BATCH_DENIAL_NO_BLANKS);
      return true;
    }

    // Check if blank captchacards are being stored.
    boolean storingBlanks = CaptchaManager.isBlankCaptcha(item);
    int itemMaxStack = item.getMaxStackSize();

    // Attempt to create a filled captchacard for the item being stored.
    ItemStack captchaForItem = getFilledCaptcha(storingBlanks, item, itemMaxStack);
    if (captchaForItem == null) {
      lang.sendComponent(sender, Messages.COMMAND_DENIAL_CANNOT_CAPTCHA);
      return true;
    }

    MatchedItems similar;
    if (storingBlanks) {
      if (blanks.amount == Integer.MAX_VALUE) {
        // If blanks are free, we never assembled them in the first place.
        similar = matchItems(contents, CaptchaManager::isBlankCaptcha);
      } else {
        // Otherwise, the similar items are the blanks, and we are consuming an extra blank stack to fill.
        itemMaxStack += 1;
        similar = blanks;
      }
    } else {
      // Otherwise, normal item.
      similar = matchItems(contents, item::isSimilar);
    }

    // If there aren't enough items for at least one stack, deny.
    if (similar.amount < itemMaxStack) {
      lang.sendComponent(sender, Messages.COMMAND_BATCH_DENIAL_NOT_MAX);
      return true;
    }

    // Number of cards that can be created is minimum of blanks and number of stacks.
    int stacks = Math.min(blanks.amount, similar.amount / itemMaxStack);

    // Remove consumed stacks.
    remove(stacks * itemMaxStack, similar, contents);

    // If the item being stored is not a blank and blanks are not free, remove the blanks consumed.
    if (!storingBlanks && blanks.amount != Integer.MAX_VALUE) {
      remove(stacks, blanks, contents);
    }

    // Update inventory contents with items removed.
    inventory.setStorageContents(contents);

    // Add filled card stack to inventory, dropping any failures.
    captchaForItem.setAmount(stacks);
    for (Map.Entry<Integer, ItemStack> failure : inventory.addItem(captchaForItem).entrySet()) {
      player.getWorld().dropItem(player.getLocation(), failure.getValue()).setPickupDelay(0);
    }

    lang.sendComponent(sender, Messages.COMMAND_BATCH_SUCCESS, new QuantityReplacement(stacks));
    return true;
  }

  private @Nullable ItemStack getFilledCaptcha(boolean storingBlanks, @NotNull ItemStack itemStack, int itemMaxStack) {
    if (storingBlanks) {
      // If storing blanks, store the most up-to-date blank possible.
      itemStack = captcha.newBlankCaptcha();
    } else {
      // Otherwise, store a copy of the item.
      itemStack = itemStack.clone();
    }
    // Set amount to max - we only store max stacks.
    itemStack.setAmount(itemMaxStack);

    return captcha.getCaptchaForItem(itemStack);
  }

  private @NotNull MatchedItems matchBlanks(
      @NotNull String @NotNull [] args,
      @NotNull Player player,
      @Nullable ItemStack @NotNull [] contents
  ) {
    MatchedItems blanks;
    if (player.getGameMode() == GameMode.CREATIVE
        || (
            player.hasPermission(PERM_FREE)
            && args.length >= 1
            && args[0].equalsIgnoreCase(lang.getValue(player, Messages.COMMAND_BATCH_FREE))
        )
    ) {
      // If the player is in creative or using the free parameter, blanks are free.
      blanks = new MatchedItems(Integer.MAX_VALUE, Set.of());
    } else {
      // Otherwise, count blanks.
      blanks = matchItems(contents, CaptchaManager::isBlankCaptcha);
    }
    return blanks;
  }

  private MatchedItems matchItems(
      @Nullable ItemStack @NotNull [] contents,
      @NotNull Predicate<@NotNull ItemStack> predicate
  ) {
    int quantity = 0;
    Set<Integer> slots = new HashSet<>();

    for (int index = 0; index < contents.length; ++index) {
      ItemStack content = contents[index];
      if (content != null && predicate.test(content)) {
        // Record matching item amount and index.
        quantity += content.getAmount();
        slots.add(index);
      }
    }

    return new MatchedItems(quantity, slots);
  }

  private void remove(
      int amount,
      @NotNull BatchCaptchaCommand.MatchedItems locations,
      @Nullable ItemStack @NotNull [] contents
  ) {
    for (int index : locations.slots()) {
      ItemStack content = contents[index];
      if (content == null) {
        // Shouldn't be possible.
        continue;
      }

      int stack = content.getAmount();
      if (stack <= amount) {
        contents[index] = null;
        amount -= stack;
      } else {
        content.setAmount(stack - amount);
        amount = 0;
      }

      if (amount <= 0) {
        return;
      }
    }

    throw new IllegalStateException("Unable to remove previously summed items!");
  }

  private record MatchedItems(int amount, Set<Integer> slots) {}

  @Override
  public @NotNull List<String> tabComplete(
      @NotNull CommandSender sender,
      @NotNull String alias,
      @NotNull String @NotNull [] args
  ) throws IllegalArgumentException {
    if (args.length != 1) {
      return List.of();
    }

    if (!sender.hasPermission(PERM_FREE)) {
      return List.of();
    }

    String value = lang.getValue(sender, Messages.COMMAND_BATCH_FREE);
    if (value != null) {
      return List.of(value);
    }

    return List.of();
  }

  @Override
  public @NotNull Plugin getPlugin() {
    return plugin;
  }

}

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

import java.util.List;

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
    if (captcha.canNotCaptcha(item)) {
      lang.sendComponent(sender, Messages.COMMAND_DENIAL_CANNOT_CAPTCHA);
      return true;
    }

    if (item.getAmount() != item.getType().getMaxStackSize()) {
      lang.sendComponent(sender, Messages.COMMAND_BATCH_DENIAL_NOT_MAX);
      return true;
    }

    PlayerInventory inventory = player.getInventory();
    ItemStack blankCaptcha = captcha.getBlankCaptchacard();

    int max;
    if (player.getGameMode() == GameMode.CREATIVE
        || (
            player.hasPermission(PERM_FREE)
            && args.length >= 1
            && args[0].equalsIgnoreCase(lang.getValue(player, Messages.COMMAND_BATCH_FREE))
        )
    ) {
      max = Integer.MAX_VALUE;
    } else {
      max = 0;
      for (int i = 0; i < inventory.getSize(); i++) {
        if (i == inventory.getHeldItemSlot()) {
          // Skip hand, it's the target stack.
          continue;
        }
        ItemStack slot = inventory.getItem(i);
        if (blankCaptcha.isSimilar(slot)) {
          max += slot.getAmount();
        }
      }
    }

    if (max == 0) {
      lang.sendComponent(sender, Messages.COMMAND_BATCH_DENIAL_NO_BLANKS);
      return true;
    }

    // TODO improve general handling
    //  - allow command to run on non-max stacks, total stacks
    //  - pre-calculate blank handling
    boolean blank = CaptchaManager.isBlankCaptcha(item);

    int count = 0;
    for (int i = 0; count < max && i < inventory.getSize(); i++) {
      if (item.equals(inventory.getItem(i))) {
        inventory.setItem(i, null);
        if (blank && max != Integer.MAX_VALUE && !inventory.removeItem(blankCaptcha).isEmpty()) {
          // Blank captchas are required.
          // If they're being stored as well, we need to store as we go or risk running out.
          inventory.setItem(i, item.clone());
          break;
        }
        count++;
      }
    }

    if (!blank && max != Integer.MAX_VALUE) {
      blankCaptcha.setAmount(count);
      // Not bothering catching failed removals here, there should be none.
      inventory.removeItem(blankCaptcha);
    }
    item = captcha.getCaptchaForItem(item);
    if (item != null) {
      item.setAmount(count);
      player.getInventory().addItem(item);
    }

    lang.sendComponent(sender, Messages.COMMAND_BATCH_SUCCESS, new QuantityReplacement(count));
    return true;
  }

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

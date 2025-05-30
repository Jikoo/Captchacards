package com.github.jikoo.captcha.command;

import com.github.jikoo.captcha.CaptchaManager;
import com.github.jikoo.captcha.CaptchaPlugin;
import com.github.jikoo.captcha.util.lang.ComponentLangManager;
import com.github.jikoo.captcha.util.lang.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class CaptchaCommand extends Command implements PluginIdentifiableCommand {

  private final @NotNull CaptchaPlugin plugin;
  private final @NotNull ComponentLangManager lang;
  private final @NotNull CaptchaManager captcha;

  public CaptchaCommand(
      @NotNull CaptchaPlugin plugin,
      @NotNull ComponentLangManager lang,
      @NotNull CaptchaManager captcha
  ) {
    super("captcha");
    setDescription("Captcha management command");
    setUsage("/captcha <get <code>|unique>");
    setPermission("captcha.command.admin"); // TODO
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
    if (args.length < 1) {
      // TODO
      return false;
    }

    args[0] = args[0].toLowerCase(Locale.ROOT);

    if ("get".equals(args[0])) {
      if (args.length < 2) {
        // TODO
        return false;
      }

      ItemStack item = captcha.getCaptchaForHash(args[1]);
      if (item == null) {
        lang.sendComponent(sender, Messages.COMMAND_GET_DENIAL_INVALID);
        return true;
      }

      player.getWorld().dropItem(player.getLocation(), item).setPickupDelay(0);
      lang.sendComponent(sender,  Messages.COMMAND_GET_SUCCESS);
      return true;
    }

    if ("unique".equals(args[0])) {
      ItemStack item = player.getInventory().getItemInMainHand();
      if (!CaptchaManager.isUsedCaptcha(item)) {
        lang.sendComponent(sender, Messages.COMMAND_UNIQUE_DENIAL_NOT_CAPTCHA);
        return true;
      }
      ItemMeta itemMeta = item.getItemMeta();
      if (itemMeta != null) {
        itemMeta.getPersistentDataContainer().set(
            CaptchaManager.KEY_SKIP_CONVERT,
            PersistentDataType.BYTE,
            (byte) 1);
      }
      item.setItemMeta(itemMeta);
      player.getInventory().setItemInMainHand(item);
      lang.sendComponent(sender,  Messages.COMMAND_UNIQUE_SUCCESS);
      return true;
    }

    // TODO
    return false;
  }

  // TODO tab completion

  @Override
  public @NotNull Plugin getPlugin() {
    return plugin;
  }

}

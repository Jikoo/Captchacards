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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CaptchaCommand extends Command implements PluginIdentifiableCommand {

  private final @NotNull CaptchaPlugin plugin;
  private final @NotNull ComponentLangManager lang;
  private final Map<String, Command> subcommands = new HashMap<>();

  public CaptchaCommand(
      @NotNull CaptchaPlugin plugin,
      @NotNull ComponentLangManager lang,
      @NotNull CaptchaManager captcha
  ) {
    super("captcha");
    setDescription("Captcha management command");
    setUsage("/captcha <get <code>|unique>"); // TODO
    setPermission("captcha.command.admin"); // TODO
    this.plugin = plugin;
    this.lang = lang;
    subcommands.put("get", new CaptchaGetCommand(lang, captcha));
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

    Command subcommand = subcommands.get(args[0]);
    if (subcommand != null) {
      String[] subArgs = new String[args.length - 1];
      System.arraycopy(args, 1, subArgs, 0, subArgs.length);
      return subcommand.execute(sender, commandLabel, subArgs);
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

  @Override
  public @NotNull List<String> tabComplete(
      @NotNull CommandSender sender,
      @NotNull String alias,
      @NotNull String @NotNull [] args
  ) throws IllegalArgumentException {
    if (args.length < 1) {
      return List.of();
    }

    if (args.length == 1) {
      // TODO subcommand names
      return List.of();
    }

    Command subcommand = subcommands.get(args[0].toLowerCase(Locale.ROOT));

    if (subcommand == null) {
      return List.of();
    }

    String[] subArgs = new String[args.length - 1];
    System.arraycopy(args, 1, subArgs, 0, subArgs.length);
    return subcommand.tabComplete(sender, alias, subArgs);
  }

  @Override
  public @NotNull Plugin getPlugin() {
    return plugin;
  }

}

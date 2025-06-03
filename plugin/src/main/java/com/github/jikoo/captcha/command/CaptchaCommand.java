package com.github.jikoo.captcha.command;

import com.github.jikoo.captcha.CaptchaManager;
import com.github.jikoo.captcha.CaptchaPlugin;
import com.github.jikoo.captcha.util.lang.ComponentLangManager;
import com.github.jikoo.captcha.util.lang.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.entity.Player;
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
    this.plugin = plugin;
    this.lang = lang;

    subcommands.put("get", new CaptchaGetCommand(lang, captcha));
    subcommands.put("unique", new CaptchaUniqueCommand(lang));

    setDescription("General captchacard management command");
    setPermissions();
    setUsages();
  }

  private void setPermissions() {
    StringBuilder builder = new StringBuilder();

    for (Command subcommand : subcommands.values()) {
      String permission = subcommand.getPermission();

      if (permission == null || permission.isEmpty()) {
        throw new IllegalStateException("Permission must be set!");
      }

      builder.append(permission).append(';');
    }

    setPermission(builder.substring(0, builder.length() - 1));
  }

  private void setUsages() {
    StringBuilder builder = new StringBuilder("/captcha <");

    for (Map.Entry<String, Command> entry : subcommands.entrySet()) {
      builder.append(entry.getKey());

      String usage = entry.getValue().getUsage();

      if (!usage.isEmpty()) {
        int space = usage.indexOf(' ');

        if (space >= 0) {
          builder.append(usage.substring(space));
        }
      }

      builder.append('|');
    }

    builder.setCharAt(builder.length() - 1, '>');

    setUsage(builder.toString());
  }

  @Override
  public boolean execute(
      @NotNull CommandSender sender,
      @NotNull String commandLabel,
      @NotNull String @NotNull [] args
  ) {
    if (!(sender instanceof Player)) {
      lang.sendComponent(sender, Messages.COMMAND_DENIAL_REQUIRES_PLAYER);
      return true;
    }
    if (args.length < 1) {
      return false;
    }

    args[0] = args[0].toLowerCase(Locale.ROOT);

    Command subcommand = subcommands.get(args[0]);
    if (subcommand != null) {
      String[] subArgs = new String[args.length - 1];
      System.arraycopy(args, 1, subArgs, 0, subArgs.length);
      if (!subcommand.execute(sender, commandLabel, subArgs)) {
        sender.sendMessage(Component.text().content(getUsage(args[0], subcommand)).color(TextColor.color(0xFF5555)).build());
      }
      return true;
    }

    return false;
  }

  private @NotNull String getUsage(@NotNull String name, @NotNull Command subcommand) {
    String usage = subcommand.getUsage();
    StringBuilder builder = new StringBuilder("/captcha ").append(name);

    if (usage.isEmpty()) {
      return builder.toString();
    }

    int space = usage.indexOf(' ');
    if (space < 0) {
      return builder.toString();
    }

    return builder.append(usage.substring(space)).toString();
  }

  @Override
  public @NotNull List<String> tabComplete(
      @NotNull CommandSender sender,
      @NotNull String alias,
      @NotNull String @NotNull [] args
  ) throws IllegalArgumentException {
    // Shouldn't be possible, should always be at minimum an empty string.
    if (args.length < 1) {
      return List.of();
    }

    args[0] = args[0].toLowerCase(Locale.ROOT);

    if (args.length == 1) {
      // Completing subcommand names.
      return subcommands.entrySet()
          .stream()
          .filter(entry -> entry.getKey().startsWith(args[0]) && entry.getValue().testPermissionSilent(sender))
          .map(Map.Entry::getKey)
          .toList();
    }

    // Find subcommand being completed.
    Command subcommand = subcommands.get(args[0]);

    if (subcommand == null) {
      return List.of();
    }

    String[] subArgs = new String[args.length - 1];
    System.arraycopy(args, 1, subArgs, 0, subArgs.length);
    // Complete subcommand with sub-arguments.
    return subcommand.tabComplete(sender, alias, subArgs);
  }

  @Override
  public @NotNull Plugin getPlugin() {
    return plugin;
  }

}

package com.github.jikoo.captcha.command;

import com.github.jikoo.captcha.CaptchaManager;
import com.github.jikoo.captcha.CaptchaPlugin;
import com.github.jikoo.captcha.util.lang.QuantityReplacement;
import com.github.jikoo.captcha.util.lang.ComponentLangManager;
import com.github.jikoo.captcha.util.lang.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UpdateCaptchaCommand extends Command implements PluginIdentifiableCommand {

  private final @NotNull CaptchaPlugin plugin;
  private final @NotNull ComponentLangManager lang;
  private final @NotNull CaptchaManager captcha;

  public UpdateCaptchaCommand(
      @NotNull CaptchaPlugin plugin,
      @NotNull ComponentLangManager lang,
      @NotNull CaptchaManager captcha
  ) {
    super("updatecaptcha");
    setDescription("Convert captchas whose hashes have changed.");
    setPermission("captcha.command.update");
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

    lang.sendComponent(sender, Messages.COMMAND_BATCH_SUCCESS, new QuantityReplacement(captcha.convert(player)));
    return true;
  }

  @Override
  public @NotNull List<String> tabComplete(
      @NotNull CommandSender sender,
      @NotNull String alias,
      @NotNull String @NotNull [] args
  ) throws IllegalArgumentException {
    return List.of();
  }

  @Override
  public @NotNull Plugin getPlugin() {
    return plugin;
  }

}

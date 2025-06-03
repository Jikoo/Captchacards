package com.github.jikoo.captcha.command;

import com.github.jikoo.captcha.CaptchaManager;
import com.github.jikoo.captcha.util.lang.ComponentLangManager;
import com.github.jikoo.captcha.util.lang.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class CaptchaGetCommand extends Command {

  private final @NotNull ComponentLangManager lang;
  private final @NotNull CaptchaManager captcha;

  CaptchaGetCommand(@NotNull ComponentLangManager lang, @NotNull CaptchaManager captcha) {
    super("get");
    this.lang = lang;
    this.captcha = captcha;
    setPermission("captcha.command.get.use");
    setUsage(" <hash>");
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

  @Override
  public @NotNull List<String> tabComplete(
      @NotNull CommandSender sender,
      @NotNull String alias,
      @NotNull String @NotNull [] args
  ) throws IllegalArgumentException {
    if (args.length != 1) {
      return List.of();
    }

    try (Stream<Path> pathStream = Files.list(captcha.getDataDir())){
      return pathStream
          .map(path -> {
            String string = path.getFileName().toString();
            if (!string.endsWith(".nbt")) {
              return null;
            }
            return string.substring(0, string.length() - 4);
          })
          .filter(Objects::nonNull)
          .toList();
    } catch (IOException e) {
      return List.of();
    }
  }

}

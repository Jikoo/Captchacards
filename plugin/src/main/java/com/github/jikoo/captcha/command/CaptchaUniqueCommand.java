package com.github.jikoo.captcha.command;

import com.github.jikoo.captcha.CaptchaManager;
import com.github.jikoo.captcha.util.lang.ComponentLangManager;
import com.github.jikoo.captcha.util.lang.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CaptchaUniqueCommand extends Command {

  private final @NotNull ComponentLangManager lang;

  CaptchaUniqueCommand(@NotNull ComponentLangManager lang) {
    super("unique");
    this.lang = lang;
    setPermission("captcha.command.unique.use");
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
    if (!CaptchaManager.isUsedCaptcha(item)) {
      lang.sendComponent(sender, Messages.COMMAND_UNIQUE_DENIAL_NOT_CAPTCHA);
      return true;
    }

    item.editMeta(ItemMeta.class, itemMeta -> itemMeta.getPersistentDataContainer().set(
        CaptchaManager.KEY_SKIP_CONVERT,
        PersistentDataType.BYTE,
        (byte) 1
    ));

    player.getInventory().setItemInMainHand(item);
    lang.sendComponent(sender,  Messages.COMMAND_UNIQUE_SUCCESS);
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

}

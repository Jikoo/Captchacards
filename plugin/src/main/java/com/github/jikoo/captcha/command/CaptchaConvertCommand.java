package com.github.jikoo.captcha.command;

import com.github.jikoo.captcha.CaptchaManager;
import com.github.jikoo.captcha.util.lang.ComponentLangManager;
import com.github.jikoo.captcha.util.lang.Messages;
import com.github.jikoo.captcha.util.lang.QuantityReplacement;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class CaptchaConvertCommand extends Command {

  private final @NotNull ComponentLangManager lang;
  private final @NotNull CaptchaManager captcha;

  CaptchaConvertCommand(@NotNull ComponentLangManager lang, @NotNull CaptchaManager captcha) {
    super("updatecaptcha");
    setDescription("Convert captchas whose hashes have changed.");
    setPermission("captcha.command.update.use");
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

    lang.sendComponent(sender, Messages.COMMAND_BATCH_SUCCESS, new QuantityReplacement(convert(player)));
    return true;
  }

  private int convert(@NotNull Player player) {
    int conversions = 0;
    List<Integer> depthAmounts = new LinkedList<>();
    for (int i = 0; i < player.getInventory().getSize(); i++) {
      ItemStack baseItem = player.getInventory().getItem(i);

      String originalHash = getHash(baseItem);
      // If there's no hash, not a captcha.
      if (originalHash == null) {
        continue;
      }

      // Start storing amounts of each layer to rebuild.
      depthAmounts.clear();
      depthAmounts.add(baseItem.getAmount());

      ItemStack storedItem = deconstructCaptcha(originalHash, depthAmounts);

      // If stored item is null, final captcha is invalid. Ignore.
      if (storedItem == null) {
        continue;
      }

      storedItem = reconstructCaptcha(storedItem, depthAmounts);

      // If reconstructed item is null, can't recreate for some reason.
      if (storedItem == null) {
        continue;
      }

      ItemMeta newMeta = storedItem.getItemMeta();
      if (newMeta == null) {
        continue;
      }

      String newHash = newMeta
          .getPersistentDataContainer()
          .get(CaptchaManager.KEY_HASH, PersistentDataType.STRING);
      if (!originalHash.equals(newHash)) {
        player.getInventory().setItem(i, storedItem);
        conversions += storedItem.getAmount();
      }
    }
    return conversions;
  }

  private @Nullable ItemStack reconstructCaptcha(ItemStack storedItem, List<Integer> depthAmounts) {
    // If the store item is a blank captcha, update it as well.
    if (CaptchaManager.isBlankCaptcha(storedItem)) {
      int amount = storedItem.getAmount();
      storedItem = captcha.newBlankCaptcha();
      storedItem.setAmount(amount);
    }

    ListIterator<Integer> depthIterator = depthAmounts.listIterator(depthAmounts.size());
    // Fully re-captcha stored item.
    while (depthIterator.hasPrevious()) {
      storedItem = captcha.getCaptchaForItem(storedItem);

      // Problem creating new captcha, ignore.
      if (storedItem == null) {
        break;
      }

      storedItem.setAmount(depthIterator.previous());
    }
    return storedItem;
  }

  private @Nullable ItemStack deconstructCaptcha(String originalHash, List<Integer> depthAmounts) {
    String hash = originalHash;
    ItemStack storedItem;
    // Fully de-captcha stored item.
    while ((hash = getHash((storedItem = captcha.getItemByHash(hash)))) != null) {
      depthAmounts.add(storedItem.getAmount());
    }
    return storedItem;
  }

  private @Nullable String getHash(@Nullable ItemStack potentialCaptcha) {
    if (potentialCaptcha == null) {
      return null;
    }

    AtomicReference<String> hash = new AtomicReference<>();
    Predicate<PersistentDataContainer> predicate = pdc -> {
      if (pdc.has(CaptchaManager.KEY_SKIP_CONVERT)) {
        return false;
      }
      hash.set(pdc.get(CaptchaManager.KEY_HASH, PersistentDataType.STRING));
      return true;
    };

    if (CaptchaManager.isCaptcha(potentialCaptcha, predicate)) {
      return hash.get();
    }

    return null;
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

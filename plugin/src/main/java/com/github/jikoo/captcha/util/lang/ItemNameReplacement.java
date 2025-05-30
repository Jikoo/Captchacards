package com.github.jikoo.captcha.util.lang;

import com.github.jikoo.planarwrappers.lang.Replacement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class ItemNameReplacement implements Replacement {

  private final @NotNull ItemStack itemStack;

  public ItemNameReplacement(@NotNull ItemStack itemStack) {
    this.itemStack = itemStack;
  }

  @Override
  public String getPlaceholder() {
    return "{content}";
  }

  @Override
  public String getValue() {
    if (itemStack.hasItemMeta()) {
      ItemMeta itemMeta = itemStack.getItemMeta();
      if (itemMeta != null && itemMeta.hasCustomName()) {
        Component customName = itemMeta.customName();
        if (customName != null) {
          return MiniMessage.miniMessage().serialize(customName);
        }
      }
    }
    return "<lang:" + itemStack.translationKey() + ">";
  }

}

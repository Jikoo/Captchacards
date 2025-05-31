package com.github.jikoo.captcha.util.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class ItemNameReplacement extends SimpleReplacement {

  public ItemNameReplacement(@NotNull ItemStack itemStack) {
    this("content", itemStack);
  }

  public ItemNameReplacement(@NotNull String placeholder, @NotNull ItemStack itemStack) {
    super(placeholder, getNameString(itemStack));
  }

  private static @NotNull String getNameString(@NotNull ItemStack itemStack) {
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

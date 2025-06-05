package com.github.jikoo.captcha.listener;

import com.github.jikoo.captcha.CaptchaManager;
import com.github.jikoo.captcha.util.lang.ComponentLangManager;
import com.github.jikoo.captcha.util.lang.Messages;
import com.google.errorprone.annotations.Keep;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@SuppressWarnings("MultipleNullnessAnnotations") // False positive for non-null array with nullable elements.
public class CraftingListener implements Listener {

  private final @NotNull ComponentLangManager lang;
  private final @NotNull CaptchaManager captchas;

  public CraftingListener(@NotNull ComponentLangManager lang, @NotNull CaptchaManager captchas) {
    this.lang = lang;
    this.captchas = captchas;
  }

  @Keep
  @EventHandler
  private void handleDiscover(@NotNull PlayerRecipeDiscoverEvent event) {
    // Disallow discovering the uncaptcha recipe. The recipe book can't show an accurate result.
    if (event.getRecipe().equals(CaptchaManager.KEY_UNCAPTCHA_RECIPE)) {
      event.setCancelled(true);
    }
  }

  @Keep
  @EventHandler
  private void onPrepareItemCraft(@NotNull PrepareItemCraftEvent event) {
    CraftingInventory inventory = event.getInventory();
    // Handle captchas in recipes and the uncaptcha crafting recipe.
    onRecipeUse(event.getRecipe(), inventory.getMatrix(), inventory::setResult, () -> inventory.setResult(null));
  }

  @Keep
  @EventHandler
  private void onCraftItem(@NotNull CraftItemEvent event) {
    CraftingInventory inventory = event.getInventory();
    // Handle captchas in recipes and the uncaptcha crafting recipe.
    onRecipeUse(
        event.getRecipe(),
        inventory.getMatrix(),
        event::setCurrentItem,
        () -> {
          lang.sendComponent(event.getWhoClicked(), Messages.EVENT_CRAFT_DENIED);
          event.setCancelled(true);
        }
    );
  }

  @Keep
  @EventHandler
  private void onCrafterCraft(@NotNull CrafterCraftEvent event) {
    ItemStack[] matrix;
    if (!(event.getBlock().getState(false) instanceof Crafter crafter)) {
      matrix =  new ItemStack[0];
    } else {
      matrix = crafter.getInventory().getContents();
    }

    // Handle captchas in recipes and the uncaptcha crafting recipe.
    onRecipeUse(
        event.getRecipe(),
        matrix,
        event::setResult,
        () -> {
          event.setResult(ItemStack.of(Material.AIR));
          event.setCancelled(true);
        }
    );
  }

  private void onRecipeUse(
      @Nullable Recipe recipe,
      @Nullable ItemStack @NotNull [] matrix,
      @NotNull Consumer<@NotNull ItemStack> setResult,
      @NotNull Runnable cancel
  ) {
    // If this is the uncaptcha recipe, set result from input.
    if (recipe instanceof Keyed keyed && keyed.getKey().equals(CaptchaManager.KEY_UNCAPTCHA_RECIPE)) {
      setUncaptchaResult(matrix, setResult);
      return;
    }

    // Otherwise, if there is a captcha being used in a recipe, disallow.
    for (ItemStack itemStack : matrix) {
      if (CaptchaManager.isCaptcha(itemStack)) {
        cancel.run();
        return;
      }
    }
  }

  // Helper for setting result from crafting matrix.
  private void setUncaptchaResult(
      @Nullable ItemStack @NotNull [] matrix,
      @NotNull Consumer<@NotNull ItemStack> setResult
  ) {
    for (ItemStack itemStack : matrix) {
      if (itemStack == null || itemStack.getType() == Material.AIR) {
        continue;
      }
      if (!CaptchaManager.isUsedCaptcha(itemStack)) {
        setResult.accept(ItemStack.of(Material.AIR));
      } else {
        ItemStack contained = captchas.getItemByCaptcha(itemStack);
        setResult.accept(contained == null ? ItemStack.of(Material.AIR) : contained);
      }
      return;
    }
  }

}

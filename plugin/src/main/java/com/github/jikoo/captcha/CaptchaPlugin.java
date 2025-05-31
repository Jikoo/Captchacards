package com.github.jikoo.captcha;

import com.github.jikoo.captcha.command.BatchCaptchaCommand;
import com.github.jikoo.captcha.command.CaptchaCommand;
import com.github.jikoo.captcha.command.UpdateCaptchaCommand;
import com.github.jikoo.captcha.listener.UseListener;
import com.github.jikoo.captcha.listener.CraftingListener;
import com.github.jikoo.captcha.listener.MisuseListener;
import com.github.jikoo.captcha.util.lang.ComponentLangManager;
import com.github.jikoo.captcha.util.lang.Messages;
import com.github.jikoo.planarwrappers.lang.Message;
import com.github.jikoo.planarwrappers.lang.PluginLocaleProvider;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A plugin adding bulk storage in the form of captchacards.
 *
 * <p>A captchacard is a single item representing up to a stack of a single other item.
 */
public class CaptchaPlugin extends JavaPlugin {

  @Override
  public void onEnable() {
    ComponentLangManager lang = new ComponentLangManager(
        new PluginLocaleProvider(this) {
          @Override
          public @NotNull Iterable<@NotNull Message> getMessages() {
            return Messages.getMessages();
          }
        }
    );

    CaptchaManager captcha = new CaptchaManager(getDataPath().resolve("captcha"), lang, getLogger());

    // Add captchacard recipes.
    for (int i = 1; i < 5; ++i) {
      ItemStack captchaItem = captcha.newBlankCaptcha();
      captchaItem.setAmount(i);
      ShapelessRecipe captchaRecipe =
          new ShapelessRecipe(new NamespacedKey(this, "captcha" + i), captchaItem);
      captchaRecipe.addIngredient(2 * i, Material.PAPER);
      captchaRecipe.setGroup("captcha");
      getServer().addRecipe(captchaRecipe);
    }

    ShapelessRecipe uncaptchaRecipe = new ShapelessRecipe(CaptchaManager.KEY_UNCAPTCHA_RECIPE, new ItemStack(Material.DIRT));
    uncaptchaRecipe.addIngredient(Material.BOOK);
    getServer().addRecipe(uncaptchaRecipe);

    // TODO allow crafting x + blank captcha to captcha

    getServer().getPluginManager().registerEvents(new UseListener(captcha, getLogger()), this);
    getServer().getPluginManager().registerEvents(new CraftingListener(lang, captcha), this);
    getServer().getPluginManager().registerEvents(new MisuseListener(), this);

    getServer().getCommandMap().registerAll(
        "captcha",
        List.of(
            new BatchCaptchaCommand(this, lang, captcha),
            new UpdateCaptchaCommand(this, lang, captcha),
            new CaptchaCommand(this, lang, captcha)
        )
    );
  }

}

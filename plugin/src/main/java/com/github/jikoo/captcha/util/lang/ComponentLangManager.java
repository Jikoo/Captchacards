package com.github.jikoo.captcha.util.lang;

import com.github.jikoo.planarwrappers.lang.LanguageManager;
import com.github.jikoo.planarwrappers.lang.LocaleProvider;
import com.github.jikoo.planarwrappers.lang.Message;
import com.github.jikoo.planarwrappers.lang.Replacement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ComponentLangManager extends LanguageManager {

  private static final Replacement[] COLORS = Messages.getColors().toArray(new Replacement[0]);

  public ComponentLangManager(@NotNull LocaleProvider provider) {
    super(provider);
  }

  /**
   * Get the value of a {@link Message} in a locale.
   *
   * <p>If the message is {@code null}, the default value is returned.
   * <br>If the message is blank, it has been removed by the user, so {@code null} is returned.
   *
   * @param locale the locale
   * @param message the message
   * @return the configured message, the default, or {@code null}
   * @see #getValue(String, Message)
   */
  public @Nullable Component getComponent(@Nullable String locale, @NotNull Message message) {
    String value = getValue(locale, message, COLORS);
    if (value == null) {
      return null;
    }
    return MiniMessage.miniMessage().deserialize(value);
  }

  /**
   * Get the value of a {@link Message} in a locale.
   *
   * <p>If the message is {@code null}, the default value is returned.
   * <br>If the message is blank, it has been removed by the user, so {@code null} is returned.
   *
   * <p>Replacements may be other messages. If those messages are expected to contain other
   * replacements that are to be respected, those replacements should come after the message.
   * Replacements are performed in declaration order.
   * <br>Messages used in replacement are not allowed to be blank. Blank messages will be replaced with
   * the default value.
   *
   * @param locale the locale
   * @param message the message
   * @param replacements the replacements to make in the translated string
   * @return the configured message, the default, or {@code null}
   * @see #getComponent(String, Message)
   * @see #getValue(String, Message, Replacement...)
   */
  public @Nullable Component getComponent(
      @Nullable String locale,
      @NotNull Message message,
      Replacement @NotNull ... replacements
  ) {
    String value = getValue(locale, message, addColors(replacements));
    if (value == null) {
      return null;
    }
    return MiniMessage.miniMessage().deserialize(value);
  }

  /**
   * Get the value of a {@link Message} in a locale.
   *
   * <p>If the message is {@code null}, the default value is returned.
   * <br>If the message is blank, it has been removed by the user, so {@code null} is returned.
   *
   * @param sender the {@link CommandSender} who will be receiving the message
   * @param message the message
   * @return the configured message, the default, or {@code null}
   * @see #getComponent(String, Message)
   * @see #getValue(String, Message)
   */
  public @Nullable Component getComponent(@NotNull CommandSender sender, @NotNull Message message) {
    String value = getValue(sender, message, COLORS);
    if (value == null) {
      return null;
    }
    return MiniMessage.miniMessage().deserialize(value);
  }

  /**
   * Get the value of a {@link Message} in a locale.
   *
   * <p>If the message is {@code null}, the default value is returned.
   * <br>If the message is blank, it has been removed by the user, so {@code null} is returned.
   *
   * <p>Replacements may be other messages. If those messages are expected to contain other
   * replacements that are to be respected, those replacements should come after the message.
   * Replacements are performed in declaration order.
   * <br>Messages used in replacement are not allowed to be blank. Blank messages will be replaced with
   * the default value.
   *
   * @param sender the {@link CommandSender} who will be receiving the message
   * @param message the message
   * @param replacements the replacements to make in the translated string
   * @return the configured message, the default, or {@code null}
   * @see #getComponent(CommandSender, Message)
   * @see #getValue(String, Message, Replacement...)
   */
  public @Nullable Component getComponent(
      @NotNull CommandSender sender,
      @NotNull Message message,
      Replacement @NotNull ... replacements
  ) {
    String value = getValue(sender, message, addColors(replacements));
    if (value == null) {
      return null;
    }
    return MiniMessage.miniMessage().deserialize(value);
  }

  public void sendComponent(@NotNull CommandSender sender, @NotNull Message message) {
    Component component = getComponent(sender, message);
    if (component != null) {
      sender.sendMessage(component);
    }
  }

  public void sendComponent(@NotNull CommandSender sender, @NotNull Message message, @NotNull Replacement ... replacements) {
    Component component = getComponent(sender, message, replacements);
    if (component != null) {
      sender.sendMessage(component);
    }
  }

  private static @NotNull Replacement[] addColors(@NotNull Replacement @NotNull [] replacements) {
    Replacement[] combined = new Replacement[replacements.length + COLORS.length];

    System.arraycopy(replacements, 0, combined, 0, replacements.length);
    System.arraycopy(COLORS, 0, combined, replacements.length, COLORS.length);

    return combined;
  }

}

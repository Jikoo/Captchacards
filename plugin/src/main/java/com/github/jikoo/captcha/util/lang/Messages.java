package com.github.jikoo.captcha.util.lang;

import com.github.jikoo.planarwrappers.lang.Message;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum Messages {
  ;

  /// Empty captchacard name.
  public static final @NotNull Message ITEM_BLANK_NAME;
  /// Empty captchacard descriptor.
  public static final @NotNull Message ITEM_BLANK_DESCRIPTOR;
  /// Filled captchacard name.
  public static final @NotNull Message ITEM_FILLED_NAME;
  /// Filled captchacard descriptor.
  public static final @NotNull Message ITEM_FILLED_DESCRIPTOR;
  /// Filled captchacard contents.
  public static final @NotNull Message ITEM_FILLED_CONTENT;

  /// Denial message for using captchacard in a book crafting recipe.
  public static final @NotNull Message EVENT_CRAFT_DENIED;

  /// Denial message for console.
  public static final @NotNull Message COMMAND_DENIAL_REQUIRES_PLAYER;
  /// Denial message for items that cannot be put into a captchacard.
  public static final @NotNull Message COMMAND_DENIAL_CANNOT_CAPTCHA;

  /// Denial message for using batch captcha command on an item that is not a max stack.
  public static final @NotNull Message COMMAND_BATCH_DENIAL_NOT_MAX;
  /// Denial message for using batch captcha command with no blank cards.
  public static final @NotNull Message COMMAND_BATCH_DENIAL_NO_BLANKS;
  /// Success message for bulk operation.
  public static final @NotNull Message COMMAND_BATCH_SUCCESS;
  /// Parameter for free bulk captcha creation.
  public static final @NotNull Message COMMAND_BATCH_FREE;

  /// Denial message for trying to make a non-captcha item conversion-exempt.
  public static final @NotNull Message COMMAND_UNIQUE_DENIAL_NOT_CAPTCHA;
  /// Success message for making a captcha conversion-exempt.
  public static final @NotNull Message COMMAND_UNIQUE_SUCCESS;

  /// Denial message for trying to get a nonexistent ID.
  public static final @NotNull Message COMMAND_GET_DENIAL_INVALID;
  /// Success message for obtaining a captchacard by ID.
  public static final @NotNull Message COMMAND_GET_SUCCESS;

  private static final List<Message> messages = new ArrayList<>();
  private static final List<Message> colors = new ArrayList<>();

  static {
    // Color replacements.
    register("color.value", "<#FFFFAA>");
    register("color.end.value", "</#FFFFAA>");
    register("color.background", "<#00AAAA>");
    register("color.end.background", "</#00AAAA>");
    register("color.hash", "<#550055>");
    register("color.end.hash", "</#550055>");

    ITEM_BLANK_NAME = register("item.blank.name", "<!i>{color.value}Blank Captchacard");
    ITEM_BLANK_DESCRIPTOR = register("item.blank.descriptor", "<!i>{color.hash}Blank");
    ITEM_FILLED_NAME = register("item.filled.name", "<!i>{color.value}Captcha{color.background} of {color.end.background}{item.content}");
    ITEM_FILLED_DESCRIPTOR = register("item.filled.descriptor", "<!i>{color.hash}{hash}");
    ITEM_FILLED_CONTENT = register("item.content", "<!i>{color.value}{quantity}{color.background}x {color.end.background}{content}");

    EVENT_CRAFT_DENIED = register("messages.event.crafting_denial", "{color.value}You can't use a captchacard in place of a book!");

    COMMAND_DENIAL_REQUIRES_PLAYER = register("command.denial.requires_player", "{color.value}This command must be run by a player!");
    COMMAND_DENIAL_CANNOT_CAPTCHA = register("command.denial.cannot_captcha", "{color.value}Cannot captcha item in main hand!");

    COMMAND_BATCH_DENIAL_NOT_MAX = register("command.batch.denial.not_max", "{color.value}Max stacks only for batch captcha creation!");
    COMMAND_BATCH_DENIAL_NO_BLANKS = register("command.batch.denial.no_blanks", "{color.value}No blank captchas left to use!");
    COMMAND_BATCH_SUCCESS = register("command.batch.success", "{color.background}Filled {color.value}{quantity}{color.background} captchas!");
    COMMAND_BATCH_FREE = register("command.batch.free", "free");

    COMMAND_UNIQUE_DENIAL_NOT_CAPTCHA = register("command.unique.denial.not_captcha", "{color.value}Please hold a used captcha!");
    COMMAND_UNIQUE_SUCCESS = register("command.unique.success", "{color.background}Added tag to item in hand!");

    COMMAND_GET_DENIAL_INVALID = register("command.get.denial.invalid", "{color.value}Hash not in use!");
    COMMAND_GET_SUCCESS = register("command.get.success", "{color.background}Captcha get!");
  }

  @Contract(pure = true)
  public static @NotNull @UnmodifiableView List<Message> getMessages() {
    return Collections.unmodifiableList(messages);
  }

  @Contract(pure = true)
  public static @NotNull @UnmodifiableView List<Message> getColors() {
    return Collections.unmodifiableList(colors);
  }

  private static @NotNull Message register(@NotNull String key, @NotNull String value) {
    Message message = new Message(key, value);
    messages.add(message);
    if (key.startsWith("color.")) {
      colors.add(message);
    }
    return message;
  }

}

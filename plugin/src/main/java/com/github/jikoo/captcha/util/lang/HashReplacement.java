package com.github.jikoo.captcha.util.lang;

import com.github.jikoo.planarwrappers.lang.Replacement;
import org.jetbrains.annotations.NotNull;

public class HashReplacement implements Replacement {

  private final @NotNull String hash;

  public HashReplacement(@NotNull String hash) {
    this.hash = hash;
  }

  @Override
  public String getPlaceholder() {
    return "{hash}";
  }

  @Override
  public String getValue() {
    return hash;
  }

}

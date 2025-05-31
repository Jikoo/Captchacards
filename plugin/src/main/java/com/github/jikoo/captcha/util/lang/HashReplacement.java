package com.github.jikoo.captcha.util.lang;

import org.jetbrains.annotations.NotNull;

public class HashReplacement extends SimpleReplacement {

  public HashReplacement(@NotNull String hash) {
    super("hash", hash);
  }

}

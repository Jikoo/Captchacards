package com.github.jikoo.captcha.util.lang;

import com.github.jikoo.planarwrappers.lang.Replacement;

public class QuantityReplacement implements Replacement {

  private final int value;

  public QuantityReplacement(int value) {
    this.value = value;
  }

  @Override
  public String getPlaceholder() {
    return "{quantity}";
  }

  @Override
  public String getValue() {
    return String.valueOf(value);
  }

}

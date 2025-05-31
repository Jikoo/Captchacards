package com.github.jikoo.captcha.util.lang;

import com.github.jikoo.planarwrappers.lang.Replacement;

public class SimpleReplacement implements Replacement {

  private final String placeholder;
  private final String value;

  public SimpleReplacement(String placeholder, String value) {
    this.placeholder = '{' + placeholder + '}';
    this.value = value;
  }

  @Override
  public String getPlaceholder() {
    return placeholder;
  }

  @Override
  public String getValue() {
    return value;
  }

}

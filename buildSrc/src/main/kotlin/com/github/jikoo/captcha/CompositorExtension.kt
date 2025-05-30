package com.github.jikoo.captcha

import org.gradle.api.model.ObjectFactory

abstract class CompositorExtension(objects: ObjectFactory) {

  val type = objects.property(String::class.java).convention("item")
  val material = objects.property(String::class.java).convention("book")

}

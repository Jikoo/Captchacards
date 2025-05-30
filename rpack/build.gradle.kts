import com.github.jikoo.captcha.CaptchaCompositor
import com.github.jikoo.captcha.CompositorExtension

plugins {
  id("base")
}

apply<CaptchaCompositor>()

val compositor = extensions.getByType(CompositorExtension::class.java)
compositor.type = "item"
compositor.material = "book"

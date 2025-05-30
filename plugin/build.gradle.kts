plugins {
  id("java")
  alias(libs.plugins.shadow)
}

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
  maven("https://jitpack.io")
}

dependencies {
  compileOnly(libs.annotations)
  compileOnly(libs.paper.api)
  compileOnly(libs.caffeine)
  implementation(libs.planarwrappers)
}

tasks.processResources {
  var apiversion = libs.paper.api.get().version!!
  val index = apiversion.indexOfFirst{ it == '-'}
  apiversion = if (index < 0) apiversion else apiversion.subSequence(0, index).toString()
  val caffeine = libs.caffeine.get()
  expand(
    "version" to version,
    "apiversion" to apiversion,
    "caffeine" to "${caffeine.group}:${caffeine.name}:${caffeine.version}"
  )
}

tasks.jar {
  manifest.attributes("paperweight-mappings-namespace" to "mojang")
}

tasks.shadowJar {
  dependsOn(tasks.jar)
  relocate("com.github.jikoo.planarwrappers", "com.github.jikoo.captcha.lib.planarwrappers")
  minimize()

  archiveBaseName = "captchacards"
}

tasks.assemble {
  dependsOn(tasks.shadowJar)
}

package com.github.jikoo.captcha

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.register

abstract class CaptchaCompositor: Plugin<Project> {

  override fun apply(target: Project) {

    val ext = target.extensions.create("captcha", CompositorExtension::class.java, target.objects)

    val generate = target.tasks.register<CompositorTask>("generateComposite") {
      dir.set(target.provider { target.layout.buildDirectory.asFile.get().toPath().resolve("generated/pack") })
      type.set(ext.type)
      material.set(ext.material)
    }

    val zip = target.tasks.register<Zip>("assembleZip") {
      dependsOn(generate)
      archiveFileName.set("captchacard_pack.zip")

      from("pack", "build/generated/pack")
      with(target.copySpec {
        include("**/*.json", "**/*.png", "pack.mcmeta")
      })
    }

    target.tasks.named("assemble").configure {
      dependsOn(zip)
    }

  }

}
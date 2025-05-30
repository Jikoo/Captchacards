package com.github.jikoo.captcha

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.keys.BlockTypeKeys
import io.papermc.paper.registry.keys.ItemTypeKeys
import net.kyori.adventure.key.Key
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Arrays
import java.util.Objects

abstract class CompositorTask : DefaultTask() {

  @get:OutputDirectory
  abstract val dir: Property<Path>

  @get:Input
  abstract val type: Property<String>

  @get:Input
  abstract val material: Property<String>

  @TaskAction
  fun generate() {
    val typeString = type.get()
    val matString = material.get()

    val content = generateCard("${typeString}/${matString}")

    val file = dir.get().resolve("assets/minecraft/${typeString}s/${matString}.json")
    Files.createDirectories(file.parent)
    Files.writeString(file, Gson().toJson(content), StandardCharsets.UTF_8)
  }

  private fun generateCard(mat: String): JsonObject {
    val cases = JsonArray()

    // Pre-baked textures: blank captcha, captcha of blanks, captcha of unknown item
    addPreset(cases, "blank", "blank_captchacard")
    addPreset(cases, "blanks", "filled_captchacard_blanks")
    addPreset(cases, "unknown", "filled_captchacard_unknown")

    // Assemble list of block keys. This is used to determine if a model is a block or item.
    val blockTypeKeys = Arrays.stream(BlockTypeKeys::class.java.declaredFields)
      .filter {
        it.canAccess(null) && it.type.equals(TypedKey::class.java)
      }.map {
        val type = it.get(null) ?: return@map null
        type as TypedKey<*>
        return@map type.asString()
      }
      .filter(Objects::nonNull)
      .toList()

    // Assemble a composite for each inventory item.
    ItemTypeKeys::class.java.declaredFields.forEach {
      if (!it.canAccess(null) || !it.type.equals(TypedKey::class.java)) {
        return@forEach
      }
      val type = it.get(null) ?: return@forEach
      type as TypedKey<*>

      addComposite(cases, type, blockTypeKeys.contains(type.asString()))
    }

    return generateBaseObject(cases, mat)
  }

  private fun addComposite(cases: JsonArray, key: Key, isBlock: Boolean) {
    val model = JsonObject()
    model.addProperty("type", "composite")

    val type = if (isBlock) "block" else "item"

    val models = JsonArray()
    var submodel = JsonObject()
    submodel.addProperty("type", "model")
    submodel.addProperty("model", "captcha:item/filled_captchacard_${type}")

    models.add(submodel)

    submodel = JsonObject()
    submodel.addProperty("type", "model")
    submodel.addProperty("model", "${key.namespace()}:${type}/${key.value()}")

    models.add(submodel)

    model.add("models", models)

    val case = JsonObject()
    case.add("model", model)
    case.addProperty("when", "captcha:${key.asString()}")

    cases.add(case)
  }

  private fun addPreset(cases: JsonArray, key: String, item: String) {
    val model = JsonObject()
    model.addProperty("type", "model")
    model.addProperty("model", "captcha:item/${item}")

    val case = JsonObject()
    case.add("model", model)
    case.addProperty("when", "captcha:${key}")

    cases.add(case)
  }

  private fun generateBaseObject(cases: JsonArray, path: String): JsonObject {
    val model = JsonObject()
    model.addProperty("type", "select")
    model.addProperty("property", "custom_model_data")

    model.add("cases", cases)

    val fallback = JsonObject()
    fallback.addProperty("type", "model")
    fallback.addProperty("model", "minecraft:${path}")

    model.add("fallback", fallback)

    val obj = JsonObject()
    obj.add("model", model)

    return obj
  }

}

plugins {
  `kotlin-dsl`
}

repositories {
  maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
  val libs = project.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
  implementation(libs.findLibrary("paper-api").orElseThrow())
}

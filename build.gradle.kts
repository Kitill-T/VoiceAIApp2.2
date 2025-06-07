// Top-level build file

plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}

tasks.wrapper {
    gradleVersion = "8.4"
    distributionType = Wrapper.DistributionType.BIN
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

// Объявляем переменные версий для всех модулей
extra.apply {
    set("kotlinVersion", "1.9.22")
    set("composeCompilerVersion", "1.5.10")
}
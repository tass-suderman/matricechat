package extension

import org.gradle.api.Project

val Project.isMatriceTrimmed: Boolean
    get() = providers.gradleProperty("matriceTrimmed").map(String::toBooleanStrict).getOrElse(false)

package mihon.buildlogic

import org.gradle.api.Project

interface BuildConfig {
    val includeTelemetry: Boolean
    val enableUpdater: Boolean
    val enableCodeShrink: Boolean
    val includeDependencyInfo: Boolean
}

val Project.Config: BuildConfig get() = object : BuildConfig {
    override val includeTelemetry: Boolean = project.findProperty("include-telemetry")?.toString()?.toBoolean() == true
    override val enableUpdater: Boolean = project.findProperty("enable-updater")?.toString()?.toBoolean() == true
    override val enableCodeShrink: Boolean = project.findProperty("disable-code-shrink")?.toString()?.toBoolean() != true
    override val includeDependencyInfo: Boolean = project.findProperty("include-dependency-info")?.toString()?.toBoolean() == true
}
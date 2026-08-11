import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.internal.classpath.Instrumented.systemProperty

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.platform)
    alias(libs.plugins.ben.manes.versions)
}

group = "com.pambrose"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(libs.versions.intellijIdea)
        // Supplies GitRepositoryManager / GitRepository / GitRemote. Bundled in every IntelliJ IDE.
        bundledPlugin("Git4Idea")
    }

    // Version comes from the Kotlin plugin, so it stays out of the catalog.
    testImplementation(kotlin("test"))
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252"
            // Open-ended: the APIs used here are stable, so don't lock users out of newer IDEs.
            untilBuild = provider { null }
        }
    }
}

tasks.test {
    useJUnitPlatform()

    // These are plain unit tests: no IDE is started, so the plugin's platform bootstrap is not
    // needed. Its IntelliJPlatformArgumentProvider forces
    // -Djava.system.class.loader=com.intellij.util.lang.PathClassLoader onto the test JVM, and
    // Kotest's ClassGraph-based discovery cannot traverse that loader ("TestEngine with ID
    // 'kotest' failed to discover tests"). Drop the provider and the property it sets.
    jvmArgumentProviders.removeIf {
        it is org.jetbrains.intellij.platform.gradle.argumentProviders.IntelliJPlatformArgumentProvider
    }
    systemProperties.remove("java.system.class.loader")

    // Kotest scans the whole test classpath with ClassGraph to autodiscover config and extensions.
    // That classpath includes the entire IntelliJ Platform, which exhausts the heap mid-scan. This
    // project declares its specs directly, so the scan buys nothing.
    systemProperty("kotest.framework.classpath.scanning.config.disable", "true")
    systemProperty("kotest.framework.classpath.scanning.autoscan.disable", "true")

    // The platform's testFramework.jar registers a JUnit Platform LauncherSessionListener
    // (JUnit5TestEnvironmentInitializer) that needs JUnit 4 on the classpath. Gradle 9.7 opens a
    // launcher session, so that listener now runs and dies with NoClassDefFoundError:
    // org/junit/rules/TestRule. These tests never boot an IDE, so drop the jar.
    classpath = classpath.filter { it.name != "testFramework.jar" }

    maxHeapSize = "1g"
}

// A version counts as stable when it is nothing but digits and separators ("2025.2", "1.14.3") or
// carries an explicit release marker. Everything else — EAP, RC, M1, alpha, SNAPSHOT — does not.
fun isNonStable(version: String): Boolean {
    val hasReleaseMarker = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val looksNumeric = "^[0-9,.v-]+(-r)?$".toRegex().matches(version)
    return !hasReleaseMarker && !looksNumeric
}

// `./gradlew dependencyUpdates` reports newer releases for everything declared in
// gradle/libs.versions.toml, plus the Gradle wrapper itself.
tasks.withType<DependencyUpdatesTask>().configureEach {
    // The catalog only ever pins stable versions, and the IntelliJ repositories publish a constant
    // stream of EAP snapshots, so an unfiltered report is mostly noise. A dependency already on a
    // pre-release still gets offered pre-release updates.
    rejectVersionIf { isNonStable(candidate.version) && !isNonStable(currentVersion) }
}

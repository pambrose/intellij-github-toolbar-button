import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.changelog.Changelog

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.platform)
    alias(libs.plugins.ben.manes.versions)
    alias(libs.plugins.changelog)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.kover)
}

// `group` and `version` are set in gradle.properties.

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

    // No kotlin("test") here on purpose. Nothing imports from it, and it resolves to
    // kotlin-test-junit5, which drags junit-jupiter-engine onto the test runtime classpath — a
    // second JUnit Platform engine beside Kotest's, on a classpath `tasks.test` below already has
    // to hand-curate. It also reintroduces a kotlin-stdlib that gradle.properties works to keep off.
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

        // The plugin manager renders change notes as HTML with a small allowed tag set, so the
        // Markdown in CHANGELOG.md has to be converted rather than pasted. Sourcing them here means
        // the notes users see are the same text as the changelog, maintained once.
        changeNotes = providers.gradleProperty("version").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }
    }

    // Marketplace only accepts signed uploads. Credentials come from the environment so they can be
    // CI secrets and never touch the repository; with none set these tasks simply cannot run, which
    // is why `release.yml` skips them rather than failing.
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

changelog {
    repositoryUrl = "https://github.com/pambrose/intellij-github-toolbar-button"
    // Tags here carry no `v` (the release *title* does). The default prefix would make
    // `patchChangelog` write compare/tag links like `.../compare/v1.0.0...HEAD`, which 404.
    versionPrefix = ""
    // Sections are written out by hand as they are needed, rather than emitting a fixed set of
    // empty Added/Changed/Fixed headings into every release.
    groups.empty()
    // `patchChangelog` rewrites this file, and anything before the first version heading is only
    // preserved if the plugin knows about it. Keep this in step with CHANGELOG.md's preamble.
    introduction =
        """
        All notable changes to this project are documented in this file.

        The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
        adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Published artifacts are on the
        [releases page](https://github.com/pambrose/intellij-github-toolbar-button/releases).
        """.trimIndent()
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

// Coverage is measured over the layer the unit suite can actually reach. The action, group and
// configurable classes are excluded because they cannot be exercised without a running Application
// — the same boundary that keeps GitHubRepoLocator free of a settings lookup, and the reason
// `tasks.test` above never boots an IDE. Including them would report a number that mostly tracks
// how much platform glue exists: it would fall every time an action is added, with no change in how
// well anything is tested.
//
// The three patterns are exactly the platform-registered types. Nothing in the measured layer
// (GitHubUrlParser, GitHubDestination, GitHubBranchDestination, GitHubRepoLocator,
// GitHubHostSettings) ends in Action, Group or Configurable, and nothing excluded fails to.
//
// **The trailing `*` on each pattern is required.** Nested and anonymous classes are named
// `Outer$inner`, so a pattern ending at `Group` cannot match
// `GitHubRemotesActionGroup$getChildren$1$1`. Measured rather than assumed: without the trailing
// wildcard that anonymous action stayed in the report, and so did GitHubRemotesActionGroup itself.
//
// The bound sits well under the current 97% on purpose: it is there to catch a real regression, not
// to be re-tightened after every commit. The three uncovered lines are exactly the platform
// boundary — the `locate(Project, ...)` overload and `getInstance() = service()`, both of which
// need a running Application and so are unreachable from these tests by construction.
kover {
    reports {
        filters {
            excludes {
                classes(
                    "com.pambrose.githubtoolbar.*Action*",
                    "com.pambrose.githubtoolbar.*Group*",
                    "com.pambrose.githubtoolbar.*Configurable*",
                )
            }
        }

        verify {
            rule {
                minBound(90)
            }
        }
    }
}

// The IntelliJ Platform plugin registers both of these and wires neither into anything, so without
// this they never run at all — neither appears in `./gradlew build --dry-run`. Both are effectively
// free: prepareSandbox, their only real prerequisite, is already in the `build` graph.
//
// What this buys is visibility, not a gate: both *report* and neither fails the build. Measured,
// not assumed — flipping kotlin.stdlib.default.dependency back to true makes
// verifyPluginProjectConfiguration report the stdlib/platform conflict that setting exists to
// prevent, but the build still exits 0. And verifyPluginStructure does **not** notice a missing
// META-INF/pluginIcon.svg, so do not read it as cover for the icons — Marketplace rejecting an
// icon-less plugin is still the only thing enforcing those.
//
// `check` rather than `build` so `./gradlew check` covers them too; CI reaches them via `build`.
tasks.named("check") {
    dependsOn("verifyPluginProjectConfiguration", "verifyPluginStructure")
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

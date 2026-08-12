.PHONY: help build tests test-one clean run dist verify check coverage versions check-wrapper \
        upgrade-wrapper changelog patch-changelog lint format all _require-gradle-version
.DEFAULT_GOAL := help

GRADLE := ./gradlew
DIST_DIR := build/distributions
COVERAGE_REPORT := build/reports/kover/html/index.html
WRAPPER_PROPS := gradle/wrapper/gradle-wrapper.properties

# Deferred (=, not :=) so the sed only runs for the targets that read it, not on every make run.
GRADLE_VERSION = $(shell sed -n 's/^gradle-wrapper = "\(.*\)"/\1/p' gradle/libs.versions.toml)
WRAPPER_VERSION = $(shell sed -nE 's/^distributionUrl=.*gradle-(.+)-(bin|all)\.zip/\1/p' $(WRAPPER_PROPS))
VERSION := $(shell grep '^version=' gradle.properties | cut -d= -f2)

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'

build: ## Compile and run the tests
	$(GRADLE) build

tests: ## Run the tests
	$(GRADLE) test

test-one: ## Run one test class: make test-one TEST=com.pambrose.githubtoolbar.GitHubUrlParserTest
ifndef TEST
	$(error TEST is required, e.g. make test-one TEST=com.pambrose.githubtoolbar.GitHubUrlParserTest)
endif
	$(GRADLE) test --tests "$(TEST)"

clean: ## Delete build outputs
	$(GRADLE) clean

run: ## Launch a sandbox IDE with the plugin loaded
	$(GRADLE) runIde

dist: ## Build the installable plugin ZIP
	$(GRADLE) buildPlugin
	@echo
	@echo "Install via Settings -> Plugins -> gear icon -> Install Plugin from Disk:"
	@ls -1 $(DIST_DIR)/*.zip

verify: ## Check binary compatibility with the supported IDE range
	$(GRADLE) verifyPlugin

# Gradle's `check` lifecycle rather than the individual tasks: it already covers lintKotlin (wired
# in by Kotlinter) and test, and it picks up verifyPluginProjectConfiguration and
# verifyPluginStructure, which build.gradle.kts attaches to it. Naming the tasks by hand instead
# would leave this target running less than CI's `./gradlew build` does. verifyPlugin is separate
# because nothing attaches it to check — it downloads its own IDEs and is too slow to run by default.
check: check-wrapper ## Run ktlint, the tests, and the plugin verifier
	$(GRADLE) check verifyPlugin

# Coverage is measured over the layer these tests can reach; build.gradle.kts excludes the action,
# group and configurable classes, which need a running IDE. `check` already runs koverVerify, so
# this target is for reading the detail, not for gating.
coverage: ## Report test coverage and write the HTML report
	$(GRADLE) koverHtmlReport koverLog
	@echo
	@echo "Full report:"
	@ls -1 $(COVERAGE_REPORT)

lint: ## Report ktlint violations
	$(GRADLE) lintKotlin

format: ## Fix the ktlint violations that can be fixed automatically
	$(GRADLE) formatKotlin

versions:  ## Check for newer dependency versions
	$(GRADLE) dependencyUpdates --no-configuration-cache --no-parallel

# The catalog is the source of truth for the wrapper version, but nothing in the build resolves
# that entry, so the two can drift silently. `upgrade-wrapper` is the fix, hence no guard on it.
check-wrapper: _require-gradle-version ## Verify the wrapper matches the catalog version
	@[ "$(WRAPPER_VERSION)" = "$(GRADLE_VERSION)" ] || { \
		echo "ERROR: $(WRAPPER_PROPS) is on Gradle '$(WRAPPER_VERSION)' but gradle/libs.versions.toml" >&2; \
		echo "       pins gradle-wrapper = '$(GRADLE_VERSION)'. Run 'make upgrade-wrapper'." >&2; \
		exit 1; }

# Gradle's documented upgrade procedure: the first run rewrites
# gradle-wrapper.properties using the *old* wrapper jar; the second run
# regenerates the wrapper itself with the new version.
upgrade-wrapper: _require-gradle-version ## Upgrade the Gradle wrapper to the catalog version
	$(GRADLE) wrapper --gradle-version=$(GRADLE_VERSION) --distribution-type=bin
	$(GRADLE) wrapper --gradle-version=$(GRADLE_VERSION) --distribution-type=bin

changelog: ## Show the changelog notes for the current version
	@$(GRADLE) getChangelog --no-header --no-empty-sections -q

# patchChangelog moves the Unreleased notes into a section for the *current* version. If that
# section already exists it consumes them and writes nothing — a silent loss of the notes — so
# refuse to run before gradle.properties has been bumped.
patch-changelog: ## Move the Unreleased notes into a section for the current version
	@grep -q "^## \[$(VERSION)\]" CHANGELOG.md && { \
		echo "ERROR: CHANGELOG.md already has a [$(VERSION)] section." >&2; \
		echo "       Bump version= in gradle.properties first, or patchChangelog will discard" >&2; \
		echo "       the Unreleased notes instead of moving them." >&2; \
		exit 1; } || true
	$(GRADLE) patchChangelog

all: ## Clean, build, package, and verify from scratch
	$(GRADLE) clean build buildPlugin verifyPlugin

_require-gradle-version:
	@[ -n "$(GRADLE_VERSION)" ] || { echo "ERROR: Could not determine gradle version from gradle/libs.versions.toml" >&2; exit 1; }

# SU Club Fair — build / emulator helpers.
# Run `make` or `make help` for the target list.

# The debug build carries an applicationIdSuffix so it installs alongside a
# release build rather than replacing it (see app/build.gradle.kts). Every target
# here drives the debug build, so they all address the suffixed id — the launch
# and logcat targets silently did nothing once the suffix was added.
APP_ID       := com.su.clubfair.debug
MAIN_ACTIVITY:= com.su.clubfair.MainActivity
AVD          ?= Pixel_7

# SDK location: local.properties wins, then $ANDROID_HOME, then the default path.
SDK_DIR := $(shell sed -n 's/^sdk\.dir=//p' local.properties 2>/dev/null)
ifeq ($(strip $(SDK_DIR)),)
SDK_DIR := $(if $(ANDROID_HOME),$(ANDROID_HOME),$(HOME)/Android/Sdk)
endif

ADB      := $(SDK_DIR)/platform-tools/adb
EMULATOR := $(SDK_DIR)/emulator/emulator
GRADLE   := ./gradlew

# `make run HEADLESS=1` boots the emulator without a window.
# `make run COLD=1` ignores the saved snapshot and does a full boot.
EMU_FLAGS ?= -netdelay none -netspeed full
ifdef HEADLESS
EMU_FLAGS += -no-window
endif
ifdef COLD
EMU_FLAGS += -no-snapshot-load
endif

# Targets are order-sensitive (boot -> install -> launch); never run them in parallel.
.NOTPARALLEL:
.DEFAULT_GOAL := help

.PHONY: help run build install launch relaunch emulator stop-emulator \
        require-device logcat test connected-test lint release uninstall \
        clean devices reverse screenshots bundle

help: ## Show this help
	@echo "SU Club Fair — make targets:"
	@grep -E '^[a-z-]+:.*?## ' $(MAKEFILE_LIST) \
		| awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'
	@echo ""
	@echo "  AVD=$(AVD)   (override with: make run AVD=Other_Device)"
	@echo "  Add HEADLESS=1 to boot the emulator without a window."
	@echo "  Add COLD=1 to ignore the saved snapshot and boot from scratch."

run: emulator install launch ## Boot emulator, build, install and launch the app

build: ## Build the debug APK
	$(GRADLE) assembleDebug

install: emulator reverse ## Build and install the debug APK (boots the emulator if needed)
	$(GRADLE) installDebug

# The app talks to su-server over this tunnel.
#
# su-server publishes on 127.0.0.1:8080 on purpose, so the API is never on a
# public interface — and the emulator's usual 10.0.2.2 host alias cannot reach a
# loopback-only bind. `adb reverse` forwards the device's own localhost:8080 to
# the host's, over adb, with no firewall or compose change.
#
# It is per-device and does not survive an emulator restart, so `install` depends
# on it rather than leaving it as something to remember.
reverse: require-device ## Tunnel the device's localhost:8080 to this machine
	@$(ADB) reverse tcp:8080 tcp:8080 >/dev/null
	@echo "==> localhost:8080 on the device now reaches this machine."

launch: require-device ## Start the app's main activity
	@$(ADB) shell am start -n $(APP_ID)/$(MAIN_ACTIVITY)

relaunch: require-device ## Force-stop and start the app again (no rebuild)
	@$(ADB) shell am force-stop $(APP_ID)
	@$(ADB) shell am start -n $(APP_ID)/$(MAIN_ACTIVITY)

require-device: ## Fail with a clear message if no device is connected
	@$(ADB) devices | grep -qw device || { \
		echo "!! No device connected. Run 'make emulator' (or 'make run') first."; \
		exit 1; }

emulator: ## Boot the AVD emulator if it isn't already running
	@if $(ADB) devices | grep -qw device; then \
		echo "==> A device is already connected, skipping emulator boot."; \
	else \
		echo "==> Booting AVD '$(AVD)' (log: /tmp/emulator-$(AVD).log)..."; \
		setsid $(EMULATOR) -avd $(AVD) $(EMU_FLAGS) \
			>/tmp/emulator-$(AVD).log 2>&1 </dev/null & \
		timeout 300 $(ADB) wait-for-device || { \
			echo "!! Emulator never came online. Log: /tmp/emulator-$(AVD).log"; \
			exit 1; }; \
		echo "==> Waiting for Android to finish booting..."; \
		while [ "$$($(ADB) shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do \
			pgrep -f "[q]emu-system.*-avd $(AVD)" >/dev/null || { \
				echo "!! Emulator exited during boot. Log: /tmp/emulator-$(AVD).log"; \
				echo "   Try a cold boot: make run COLD=1"; \
				exit 1; }; \
			sleep 2; \
		done; \
		echo "==> Emulator ready."; \
	fi

stop-emulator: ## Shut the running emulator down
	@$(ADB) emu kill 2>/dev/null || echo "No emulator running."

logcat: ## Tail logcat for this app only
	@$(ADB) logcat --pid=$$($(ADB) shell pidof -s $(APP_ID))

test: ## Run JVM unit tests
	$(GRADLE) test

connected-test: emulator ## Run instrumented tests on the emulator
	$(GRADLE) connectedAndroidTest

lint: ## Run Android lint
	$(GRADLE) lintDebug

release: ## Build the minified release APK
	$(GRADLE) assembleRelease

# What Play takes, which is not what GitHub takes.
#
# `release` stays the APK: a student downloads one file from the releases page
# and installs it, and an AAB is not installable. Play has not accepted an APK
# for a new app in years, so uploads come from here instead.
#
# Signed with the **upload** key from `keystore.properties` — Play strips that
# signature and re-signs with its own app-signing key, which is why the two
# certificates both have to be registered as OAuth clients. See the README.
#
# `versionCode` in app/build.gradle.kts must increase on every upload, including
# after one Play rejected. It cannot be reused.
bundle: ## Build the signed release AAB for Play
	$(GRADLE) bundleRelease
	@ls -lh app/build/outputs/bundle/release/*.aab

# Store screenshots, straight out of the real composables.
#
# Deliberately NOT `connectedAndroidTest`: Gradle uninstalls both APKs when that
# task finishes, and it takes the app's data directory — and therefore every PNG
# just written — with it. So the APKs go on by hand and the runner is driven
# directly, which also lets the display be pinned first.
#
# 1080x1920 because Play rejects anything past a 2:1 side ratio, and this AVD is
# 1080x2400. The override is reset at the end whether or not the run succeeded.
#
# Files land in `art/play/screenshots/{en,th}/` as JPEG — see the note in
# StoreScreenshots.kt about why lossless was costing 47 MB a run. Store material,
# not packaged; see the note about `art/` in the README.
SHOT_DIR := art/play/screenshots
SHOT_PKG := com.su.clubfair.debug

screenshots: emulator ## Capture the Play Store screenshots into art/play/screenshots
	$(GRADLE) assembleDebug assembleDebugAndroidTest
	@$(ADB) install -r -t app/build/outputs/apk/debug/app-debug.apk
	@$(ADB) install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
	@$(ADB) shell wm size 1080x1920 >/dev/null
	@$(ADB) shell wm density 420 >/dev/null
	@$(ADB) shell pm grant $(SHOT_PKG) android.permission.CAMERA
	@$(ADB) shell run-as $(SHOT_PKG) rm -rf files/screenshots
	@$(ADB) shell am instrument -w \
		-e class com.su.clubfair.StoreScreenshots \
		$(SHOT_PKG).test/androidx.test.runner.AndroidJUnitRunner \
		|| ($(ADB) shell wm size reset >/dev/null; exit 1)
	@rm -rf $(SHOT_DIR) && mkdir -p $(SHOT_DIR)/en $(SHOT_DIR)/th
	@for f in $$($(ADB) shell run-as $(SHOT_PKG) ls files/screenshots | tr -d '\r'); do \
		$(ADB) exec-out run-as $(SHOT_PKG) cat "files/screenshots/$$f" \
			> "$(SHOT_DIR)/$${f%%-*}/$${f#*-}"; \
	done
	@$(ADB) shell wm size reset >/dev/null
	@$(ADB) shell wm density reset >/dev/null
	@echo "==> Screenshots in $(SHOT_DIR)/"


uninstall: ## Remove the app from the device
	@$(ADB) uninstall $(APP_ID) || true

devices: ## List attached devices / emulators
	@$(ADB) devices -l

clean: ## Delete build outputs
	$(GRADLE) clean

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
        clean devices

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

install: emulator ## Build and install the debug APK (boots the emulator if needed)
	$(GRADLE) installDebug

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

uninstall: ## Remove the app from the device
	@$(ADB) uninstall $(APP_ID) || true

devices: ## List attached devices / emulators
	@$(ADB) devices -l

clean: ## Delete build outputs
	$(GRADLE) clean

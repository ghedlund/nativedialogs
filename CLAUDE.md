# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Java library providing native file/message/font/color dialogs for macOS and Windows, with Swing fallback. Implemented in pure Java using the Foreign Function & Memory (FFM) API — no native binaries are built or shipped.

## Build Commands

```bash
# Compile Java sources (including module-info) + package JAR
./gradlew build

# Build with an explicit version (CI derives this from the v<n> tag)
./gradlew build -Pversion=24

# Run tests (headless: OS detection, provider selection, FFM symbol resolution,
# string round-trips). Interactive dialog behaviour is NOT tested here.
./gradlew test

# Run demo application
java -jar build/libs/native-dialogs-24.jar

# Run demo via JPMS module path
java --module-path build/libs -m ca.phon.nativedialogs/ca.phon.ui.nativedialogs.demo.NativeDialogsDemo

# Force Swing fallback for testing
java -Dca.phon.ui.nativedialogs.NativeDialogs.forceSwing=true -jar build/libs/native-dialogs-24.jar

# Publish to GitHub Packages
./gradlew publish
```

## Requirements

- **Java 25+** (FFM is final since Java 22; this project targets the 25 LTS toolchain).
- FFM requires native access. The JAR manifest grants it for classpath use
  (`Enable-Native-Access: ALL-UNNAMED`); module-path consumers pass
  `--enable-native-access=ca.phon.nativedialogs`.

## Architecture

**Entry Point:** `NativeDialogs` — facade class exposing all dialog types. Keeps a stable public API (including deprecated convenience methods) and dispatches to a provider, falling back to Swing on any failure or unsupported dialog.

**Dialog Types:** Open, Save, Message, Font, Color — each with a corresponding `*Properties` class for configuration.

**Provider SPI:** `ca.phon.ui.nativedialogs.spi.NativeDialogProvider` (interface) + `NativeDialogProviders` (cached OS-based selector, returns `null` → Swing). Internal package, not exported.

**Native implementation (pure-Java FFM, internal `ca.phon.ui.nativedialogs.ffm.*`):**
- macOS (`ffm.mac`, `MacDialogProvider`): Cocoa via `objc_msgSend` (libobjc) — `NSOpenPanel`/`NSSavePanel`/`NSAlert`, app-modal, run on the AppKit main thread via GCD `dispatch_*_f`. Font/colour fall back to Swing.
- Windows (`ffm.win`, `WindowsDialogProvider`): COM Common Item Dialog (`IFileOpenDialog`/`IFileSaveDialog`, invoked by vtable index), `TaskDialogIndirect`, `ChooseFontW`, `ChooseColorW`, on an STA thread.
- Linux/other: Swing.

**Threading:** Dialogs support synchronous (blocking) and asynchronous modes via `NativeDialogListener`. Each provider delivers the result by calling `props.getListener().nativeDialogEvent(...)`.

- The facade (`NativeDialogs.MessageWaitListener`) does the waiting for blocking requests. On the event dispatch thread it waits in a `java.awt.SecondaryLoop`, so Swing events keep being dispatched while the dialog is open.
- Never make the event dispatch thread wait for the AppKit thread. AppKit answers accessibility queries and input method callbacks by waiting on the event dispatch thread (`LWCToolkit.invokeAndWait`, no timeout); if both wait, the application freezes and the dialog may never open. `MacDialogProvider` therefore starts the dialog with `Gcd.onMainAsync` when called on the event dispatch thread.
- `Gcd.onMainAsync` runs all work through one upcall stub that is never freed. Do not free an upcall stub from inside its own invocation: the JVM crashes if a garbage collection walks the stack before the stub returns.

## Key Files

- `NativeDialogs.java` — main API facade; dispatches to the provider SPI.
- `NativeUtilities.java` — platform detection (`loadLibrary` is deprecated/unused).
- `*Properties.java` — configuration classes for each dialog type.
- `spi/` — provider interface, selector, and per-OS providers.
- `ffm/mac/`, `ffm/win/` — FFM bindings to Cocoa and Win32.
- `module-info.java` — Java module: `ca.phon.nativedialogs`.

## Releases / CI

`.github/workflows/release.yml` triggers on push of a `v<n>` tag (e.g. `v24`). It runs the headless tests on Linux/macOS/Windows, builds the single platform-independent JAR, publishes to GitHub Packages, and attaches the JAR to a GitHub Release. The build version is derived from the tag (`-Pversion=<n>`).

## Testing

No interactive dialog tests (a dialog needs a human to dismiss it). Automated tests cover OS detection, provider selection, FFM symbol resolution, string conversion, the blocking wait on the event dispatch thread, and (macOS) asynchronous main-thread dispatch under garbage collection, run in a forked JVM. Verify real dialog behaviour manually with the demo application (`NativeDialogsDemo`) on each platform.

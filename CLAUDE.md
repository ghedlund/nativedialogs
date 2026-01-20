# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Java library providing native file/message/font/color dialogs for macOS and Windows, with Swing fallback.

## Build Commands

```bash
# Compile Java + generate JNI headers
mvn compile

# Package JAR with embedded native libraries
mvn package

# Run demo application
java -jar target/native-dialogs-22.jar

# Force Swing fallback for testing
java -Dca.phon.ui.nativedialogs.NativeDialogs.forceSwing=true -jar target/native-dialogs-22.jar
```

### Native Library Build (C++)

```bash
cd src/main/cpp

# Link appropriate makefile for your platform
ln -s makefiles/makefile.defs.macos.arm64 makefile.defs   # macOS ARM64
ln -s makefiles/makefile.defs.macos.x64 makefile.defs     # macOS x64
ln -s makefiles/makefile.defs.mingw64 makefile.defs       # Windows 64-bit

make
make install  # Copies to src/main/resources/META-INF/lib/{platform}/
```

**Environment variables for native build:**
- `JAVA_HOME` or `JAVA_HOME_AARCH64` - JDK path for JNI headers
- `SDK` - macOS SDK path (defaults to Xcode SDK)
- `MINGW` - MinGW path for Windows builds

## Architecture

**Entry Point:** `NativeDialogs` - Facade class exposing all dialog types

**Dialog Types:** Open, Save, Message, Font, Color - each with a corresponding `*Properties` class for configuration

**Pattern:** Each dialog method dispatches to native JNI implementation (`nativeShow*Dialog`) or Swing fallback based on platform availability

**Native Implementation:**
- macOS: `src/main/cpp/mac/nativedialogs.mm` (Objective-C/Cocoa)
- Windows: `src/main/cpp/windows/nativedialogs.cpp` (currently disabled, falls back to Swing)

**Library Loading:** `NativeUtilities.loadLibrary()` extracts platform-specific library from `META-INF/lib/{platform}/` to temp directory at runtime

**Threading:** Dialogs support both synchronous (blocking) and asynchronous modes via `NativeDialogListener`

## Key Files

- `NativeDialogs.java` - Main API facade with JNI native method declarations
- `NativeUtilities.java` - Platform detection and library loading
- `*Properties.java` - Configuration classes for each dialog type
- `module-info.java` - Java module: `ca.phon.nativedialogs`

## Testing

No unit test suite. Use the demo application (`NativeDialogsDemo`) to verify dialog behavior.

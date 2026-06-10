# FFM Native Dialogs Rewrite Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the JNI/C++ native dialog layer with a pure-Java Foreign Function & Memory (FFM) implementation on Java 25, shipping one platform-independent JAR, and add GitHub Actions CI that publishes on a version-tag push.

**Architecture:** The existing `NativeDialogs` facade keeps its public API and sync/async contract but delegates to a `NativeDialogProvider` chosen once at init. `MacDialogProvider` drives Cocoa via `objc_msgSend` (FFM) app-modally on the main thread using GCD `dispatch_*_f`; `WindowsDialogProvider` drives the Win32 Common Item Dialog (COM), `TaskDialogIndirect`, `ChooseFontW`, and `ChooseColorW`. Any provider failure or unsupported dialog degrades to the existing Swing implementation. No native binaries are built or shipped.

**Tech Stack:** Java 25 (FFM / `java.lang.foreign`), Gradle (Kotlin DSL), JUnit 5, GitHub Actions. macOS: libobjc, Foundation, AppKit, libdispatch. Windows: ole32, shell32, comdlg32, comctl32.

**Spec:** `docs/superpowers/specs/2026-06-10-ffm-native-dialogs-design.md`

---

## Reference: facts the engineer needs

**Public API that must NOT change** (signatures in `src/main/java/ca/phon/ui/nativedialogs/NativeDialogs.java`): `showDialog`, `showOpenDialog`, `showSaveDialog`, `showMessageDialog`, `showFontDialog`, `showColorDialog`, and every `@deprecated` method (`browseForFile`, `browseForDirectory`, `showSaveFileDialog`, `showYesNo*`, `showOkCancel*`, `*Blocking`, etc.). The `*Properties`, `NativeDialogEvent`, `NativeDialogListener`, `NativeDialogAdapter`, `DefaultNativeDialogListener`, and `FileFilter` classes keep their current shapes.

**Result delivery contract:** every dialog ends by calling `props.getListener().nativeDialogEvent(new NativeDialogEvent(resultCode, data))`.
- `NativeDialogEvent.OK_OPTION == 0x01` (1), `NativeDialogEvent.CANCEL_OPTION == 0x02` (2). Always use the named constants, never literals.
- Open (single): data = `String` path. Open (multi): data = `String[]`. Save: data = `String` path. Message: resultCode = selected option **index** (0-based; the facade returns `getDialogResult()` directly, so this is NOT compared against `OK_OPTION`), data = `Boolean` suppression state (or `null`). Font: data = `java.awt.Font`. Colour: data = `java.awt.Color`.

**Properties class hierarchy** (verified): `OpenDialogProperties extends SaveDialogProperties extends NativeDialogProperties extends HashMap<String,Object>`; `MessageDialogProperties`, `FontDialogProperties`, `ColorDialogProperties` each extend `NativeDialogProperties` directly. Consequence: the file getters (`getPrompt`, `getNameFieldLabel`, `getInitialFile`, `getInitialFolder`, `getFileFilter`, `isShowHidden`, `isCanCreateDirectories`, `getMessage`) live on `SaveDialogProperties` and are inherited by `OpenDialogProperties` — so both file dialogs can be handled via a single `SaveDialogProperties` reference. Open-only getters: `isAllowMultipleSelection`, `isCanChooseFiles`, `isCanChooseDirectories`. `getTitle`/`getParentWindow`/`isRunAsync`/`isForceUseSwing`/`getListener` are on the base `NativeDialogProperties`. `FileFilter` exposes `getDescription()`, `getDefaultExtension()`, `getAllExtensions()` (`List<String>`).

**Sync vs async** (already implemented in the facade): when `props.isRunAsync()` is false the facade installs a `MessageWaitListener`, calls the show method, then blocks in `waitLoop()`. The provider must therefore deliver the event from a thread that does not deadlock the caller (see Task threading notes). When async, the facade returns immediately.

**macOS selectors used:** `NSOpenPanel openPanel`, `NSSavePanel savePanel`, `setCanCreateDirectories:`, `setShowsHiddenFiles:`, `setCanChooseFiles:`, `setCanChooseDirectories:`, `setAllowsMultipleSelection:`, `setTitle:`, `setAllowedFileTypes:`, `setDirectoryURL:`, `setNameFieldStringValue:`, `setPrompt:`, `setNameFieldLabel:`, `setMessage:`, `runModal`, `URL`, `URLs`, `path`, `NSAlert alloc/init`, `setAlertStyle:`, `setMessageText:`, `setInformativeText:`, `setShowsSuppressionButton:`, `suppressionButton`, `setTitle:`, `addButtonWithTitle:`, `state`. `runModal` returns `NSModalResponseOK == 1` for panels; `NSAlert runModal` returns `NSAlertFirstButtonReturn == 1000` plus the button index.

**Windows constants** (define in `Win32.java`):
- `SIGDN_FILESYSPATH = 0x80058000`
- `FOS_OVERWRITEPROMPT = 0x2`, `FOS_PICKFOLDERS = 0x20`, `FOS_FORCEFILESYSTEM = 0x40`, `FOS_ALLOWMULTISELECT = 0x200`, `FOS_FILEMUSTEXIST = 0x1000`, `FOS_PATHMUSTEXIST = 0x800`, `FOS_FORCESHOWHIDDEN = 0x10000000`
- `CLSCTX_INPROC_SERVER = 0x1`, `COINIT_APARTMENTTHREADED = 0x2`, `S_OK = 0`
- CLSIDs/IIDs (GUID byte order is little-endian for first three fields): `CLSID_FileOpenDialog = {DC1C5A9C-E88A-4DDE-A5A1-60F82A20AEF7}`, `CLSID_FileSaveDialog = {C0B4E2F3-BA21-4773-8DBA-335EC946EB8B}`, `IID_IFileOpenDialog = {D57C7288-D4AD-4768-BE02-9D969532D960}`, `IID_IFileSaveDialog = {84BCCD23-5FDE-4CDB-AEA4-AF64B83D78AB}`, `IID_IShellItem = {43826D1E-E718-42EE-BC55-A1E261C37BFE}`
- COM vtable indices (0-based; first 3 are `QueryInterface`/`AddRef`/`Release`):
  - `IModalWindow::Show = 3`
  - `IFileDialog`: `SetFileTypes = 4`, `SetFileTypeIndex = 5`, `SetOptions = 9`, `GetOptions = 10`, `SetDefaultFolder = 11`, `SetFolder = 12`, `SetFileName = 15`, `SetTitle = 17`, `SetOkButtonLabel = 18`, `SetFileNameLabel = 19`, `GetResult = 20`, `SetDefaultExtension = 22`
  - `IFileOpenDialog::GetResults = 27`
  - `IShellItem::GetDisplayName = 5`
  - `IShellItemArray`: `GetCount = 7`, `GetItemAt = 8`
- `SHCreateItemFromParsingName` (shell32) for the initial folder.
- `COMDLG_FILTERSPEC` = struct of two wide-string pointers `{ pszName, pszSpec }`.

**Native-access:** FFM downcalls require native access. Add `Enable-Native-Access: ALL-UNNAMED` to the JAR manifest; document `--enable-native-access=ca.phon.nativedialogs` for module-path consumers.

**Cross-thread memory:** on macOS the modal block runs on the main thread while the caller thread blocks, so segments shared with it MUST come from `Arena.ofShared()` (a confined arena would throw on cross-thread access). Close the arena only after the dialog completes.

---

## File structure

```
build.gradle.kts                         MODIFY  Java 25, version 24, -Pversion, JUnit, manifest, drop generateJniHeaders
settings.gradle.kts                      (unchanged)
src/main/java/module-info.java           MODIFY  drop nothing required for FFM; keep exports

src/main/java/ca/phon/ui/nativedialogs/
  NativeDialogs.java                     MODIFY  remove static loadLibrary + native decls; delegate to provider
  NativeUtilities.java                   MODIFY  deprecate loadLibrary(); keep OS detection

  spi/NativeDialogProvider.java          CREATE  provider interface
  spi/NativeDialogProviders.java         CREATE  OS-based provider selection (cached)
  spi/MacDialogProvider.java             CREATE  Cocoa impl (open/save/message)
  spi/WindowsDialogProvider.java         CREATE  Win32 impl (open/save/message/font/colour)

  ffm/mac/ObjC.java                      CREATE  objc_getClass/sel_registerName/objc_msgSend handles
  ffm/mac/Foundation.java                CREATE  NSString/NSArray/NSURL helpers
  ffm/mac/Gcd.java                       CREATE  main-queue dispatch_sync_f / dispatch_async_f
  ffm/win/Win32.java                     CREATE  constants, GUIDs, library lookups, downcall handles
  ffm/win/WinStr.java                    CREATE  UTF-16 string helpers
  ffm/win/Com.java                       CREATE  vtable method invocation helpers

src/test/java/ca/phon/ui/nativedialogs/
  NativeUtilitiesTest.java               CREATE  OS detection
  spi/NativeDialogProvidersTest.java     CREATE  provider selection per os.name
  ffm/win/WinStrTest.java                CREATE  UTF-16 round-trip (Windows only)
  ffm/mac/FoundationSmokeTest.java       CREATE  NSString round-trip via libobjc (macOS only)
  ffm/win/Win32SmokeTest.java            CREATE  CoInitialize/symbol resolution (Windows only)

.github/workflows/release.yml           CREATE  verify matrix + release/publish on v* tag

DELETE: src/main/cpp/ (entire tree), src/main/resources/META-INF/lib/, target/, pom.xml.versionsBackup
```

---

## Phase 0 — Build setup & scaffolding

### Task 0.1: Bump to Java 25, version 24, parameterized version, JUnit, manifest

**Files:**
- Modify: `build.gradle.kts`

- [ ] **Step 1: Rewrite `build.gradle.kts`**

```kotlin
plugins {
    `java-library`
    `maven-publish`
}

group = "ca.phon"
version = (findProperty("version") as String?)?.takeIf { it != "unspecified" } ?: "24"
description = "Native dialogs for Java with fallback to Swing."

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    modularity.inferModulePath = true
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "ca.phon.ui.nativedialogs.demo.NativeDialogsDemo",
            "Enable-Native-Access" to "ALL-UNNAMED"
        )
    }
}

tasks.test {
    useJUnitPlatform()
    // FFM downcalls; suppress the native-access warning during tests
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Native Dialogs"
                description = project.description
                developers {
                    developer {
                        id = "ghedlund"
                        name = "Greg Hedlund"
                        email = "greg.hedlund@gmail.com"
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ghedlund/nativedialogs")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String? ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as String? ?: ""
            }
        }
    }
}
```

- [ ] **Step 2: Verify it configures**

Run: `./gradlew help -Pversion=24`
Expected: `BUILD SUCCESSFUL`. (The `generateJniHeaders` task is now gone; that is intended.)

- [ ] **Step 3: Confirm the toolchain resolves to JDK 25**

Run: `./gradlew javaToolchains`
Expected: a JDK 25 entry is listed (Gradle auto-provisions if not present). If provisioning is disabled, install Temurin 25 and re-run.

- [ ] **Step 4: Commit**

```bash
git add build.gradle.kts
git commit -m "Target Java 25 and add JUnit + native-access manifest

Refs #1"
```

### Task 0.2: Create the test source root with a trivial passing test

**Files:**
- Create: `src/test/java/ca/phon/ui/nativedialogs/NativeUtilitiesTest.java`

- [ ] **Step 1: Write the test**

```java
package ca.phon.ui.nativedialogs;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeUtilitiesTest {

    @Test
    void exactlyOneOsFlagIsTrue() {
        int trueCount = 0;
        if (NativeUtilities.isMacOs()) trueCount++;
        if (NativeUtilities.isWindows()) trueCount++;
        if (NativeUtilities.isLinux()) trueCount++;
        assertTrue(trueCount <= 1, "OS detection must not report multiple platforms");
    }

    @Test
    void osDetectionMatchesSystemProperty() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) assertTrue(NativeUtilities.isMacOs());
        else if (os.contains("windows")) assertTrue(NativeUtilities.isWindows());
    }
}
```

- [ ] **Step 2: Run it**

Run: `./gradlew test`
Expected: `BUILD SUCCESSFUL`, 2 tests pass.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/ca/phon/ui/nativedialogs/NativeUtilitiesTest.java
git commit -m "Add JUnit test source root with OS detection tests

Refs #1"
```

---

## Phase 1 — Provider SPI and facade wiring (no native code yet)

This phase introduces the provider abstraction and rewires the facade to use it, with **all providers absent** so everything falls back to Swing. The library remains fully functional via Swing at the end of this phase. *Pause for review after this phase.*

### Task 1.1: Provider interface

**Files:**
- Create: `src/main/java/ca/phon/ui/nativedialogs/spi/NativeDialogProvider.java`

- [ ] **Step 1: Write the interface**

```java
package ca.phon.ui.nativedialogs.spi;

import ca.phon.ui.nativedialogs.*;

/**
 * A platform backend that shows dialogs natively. Each {@code show*} method must
 * deliver its result by calling {@code props.getListener().nativeDialogEvent(...)}.
 * The {@code supports*} methods let the facade fall back to Swing when a backend
 * does not implement a given dialog.
 */
public interface NativeDialogProvider {

    boolean supportsOpen();
    boolean supportsSave();
    boolean supportsMessage();
    boolean supportsFont();
    boolean supportsColor();

    void showOpen(OpenDialogProperties props);
    void showSave(SaveDialogProperties props);
    void showMessage(MessageDialogProperties props);
    void showFont(FontDialogProperties props);
    void showColor(ColorDialogProperties props);
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/ca/phon/ui/nativedialogs/spi/NativeDialogProvider.java
git commit -m "Add NativeDialogProvider SPI interface

Refs #1"
```

### Task 1.2: Provider selection

**Files:**
- Create: `src/main/java/ca/phon/ui/nativedialogs/spi/NativeDialogProviders.java`
- Create: `src/test/java/ca/phon/ui/nativedialogs/spi/NativeDialogProvidersTest.java`

- [ ] **Step 1: Write the failing test**

```java
package ca.phon.ui.nativedialogs.spi;

import org.junit.jupiter.api.Test;
import ca.phon.ui.nativedialogs.NativeUtilities;
import static org.junit.jupiter.api.Assertions.*;

class NativeDialogProvidersTest {

    @Test
    void linuxHasNoProvider() {
        if (NativeUtilities.isLinux()) {
            assertNull(NativeDialogProviders.get(),
                "Linux must have no native provider (Swing fallback)");
        }
    }

    @Test
    void getIsStable() {
        assertSame(NativeDialogProviders.get(), NativeDialogProviders.get(),
            "provider selection must be cached");
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew test --tests '*NativeDialogProvidersTest'`
Expected: FAIL — `NativeDialogProviders` does not exist.

- [ ] **Step 3: Write the implementation**

```java
package ca.phon.ui.nativedialogs.spi;

import java.util.logging.Level;
import java.util.logging.Logger;

import ca.phon.ui.nativedialogs.NativeUtilities;

/**
 * Chooses the native dialog provider for the current OS, once, lazily.
 * Returns {@code null} when no native backend is available or it fails to
 * initialise; the facade then uses Swing.
 */
public final class NativeDialogProviders {

    private static final Logger LOGGER = Logger.getLogger(NativeDialogProviders.class.getName());

    private static final NativeDialogProvider PROVIDER = select();

    private NativeDialogProviders() {}

    public static NativeDialogProvider get() {
        return PROVIDER;
    }

    private static NativeDialogProvider select() {
        try {
            if (NativeUtilities.isMacOs()) {
                return new MacDialogProvider();
            } else if (NativeUtilities.isWindows()) {
                return new WindowsDialogProvider();
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Native dialog provider unavailable; using Swing fallback", t);
        }
        return null;
    }
}
```

Note: `MacDialogProvider` and `WindowsDialogProvider` are created in Phases 2 and 3. Until they exist this will not compile, so create **temporary stubs** now and replace them later:

```java
// src/main/java/ca/phon/ui/nativedialogs/spi/MacDialogProvider.java  (TEMPORARY STUB)
package ca.phon.ui.nativedialogs.spi;
import ca.phon.ui.nativedialogs.*;
final class MacDialogProvider implements NativeDialogProvider {
    MacDialogProvider() { throw new UnsupportedOperationException("not yet implemented"); }
    public boolean supportsOpen() { return false; }
    public boolean supportsSave() { return false; }
    public boolean supportsMessage() { return false; }
    public boolean supportsFont() { return false; }
    public boolean supportsColor() { return false; }
    public void showOpen(OpenDialogProperties p) {}
    public void showSave(SaveDialogProperties p) {}
    public void showMessage(MessageDialogProperties p) {}
    public void showFont(FontDialogProperties p) {}
    public void showColor(ColorDialogProperties p) {}
}
```

```java
// src/main/java/ca/phon/ui/nativedialogs/spi/WindowsDialogProvider.java  (TEMPORARY STUB)
package ca.phon.ui.nativedialogs.spi;
import ca.phon.ui.nativedialogs.*;
final class WindowsDialogProvider implements NativeDialogProvider {
    WindowsDialogProvider() { throw new UnsupportedOperationException("not yet implemented"); }
    public boolean supportsOpen() { return false; }
    public boolean supportsSave() { return false; }
    public boolean supportsMessage() { return false; }
    public boolean supportsFont() { return false; }
    public boolean supportsColor() { return false; }
    public void showOpen(OpenDialogProperties p) {}
    public void showSave(SaveDialogProperties p) {}
    public void showMessage(MessageDialogProperties p) {}
    public void showFont(FontDialogProperties p) {}
    public void showColor(ColorDialogProperties p) {}
}
```

Because the constructors throw, `select()` catches the failure and returns `null`, so this phase yields Swing-only behaviour as intended.

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew test --tests '*NativeDialogProvidersTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ca/phon/ui/nativedialogs/spi/ src/test/java/ca/phon/ui/nativedialogs/spi/
git commit -m "Add provider selection with temporary stubs

Refs #1"
```

### Task 1.3: Rewire the facade to use providers

**Files:**
- Modify: `src/main/java/ca/phon/ui/nativedialogs/NativeDialogs.java`

- [ ] **Step 1: Remove the native library load and native method declarations**

Delete the `static { ... }` block (lines ~61-70), the `libraryFound` field, the `_PHONNATIVE_LIB_NAME` field, and the five `private native static void nativeShow*` declarations.

Add an import and a provider accessor near the top of the class body:

```java
import ca.phon.ui.nativedialogs.spi.NativeDialogProvider;
import ca.phon.ui.nativedialogs.spi.NativeDialogProviders;
```

```java
private static NativeDialogProvider provider() {
    return NativeDialogProviders.get();
}
```

- [ ] **Step 2: Replace each dispatch site**

In `showOpenDialog`, replace the `if(libraryFound && !properties.isForceUseSwing()) { try { nativeShowOpenDialog(properties); } catch (UnsatisfiedLinkError e) { ... swingShowOpenDialog(properties); } } else { swingShowOpenDialog(properties); }` block with:

```java
final NativeDialogProvider p = provider();
if (p != null && p.supportsOpen() && !properties.isForceUseSwing()) {
    try {
        p.showOpen(properties);
    } catch (Throwable t) {
        LOGGER.log(Level.SEVERE, "Native open dialog failed; falling back to Swing", t);
        swingShowOpenDialog(properties);
    }
} else {
    swingShowOpenDialog(properties);
}
```

Apply the identical pattern in `showSaveDialog` (`supportsSave`/`showSave`/`swingShowSaveDialog`), `showMessageDialog` (`supportsMessage`/`showMessage`/`swingShowMessageDialog`), `showFontDialog` (`supportsFont`/`showFont`/`swingShowFontDialog`), and `showColorDialog` (`supportsColor`/`showColor`/`swingShowColorDialog`).

- [ ] **Step 3: Compile**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL` — no remaining references to `nativeShow*` or `libraryFound`.

- [ ] **Step 4: Manual smoke test (Swing path)**

Run: `./gradlew build -Pversion=24 && java -jar build/libs/native-dialogs-24.jar`
Expected: the demo launches; opening any dialog shows the **Swing** dialog (no provider yet). Close the demo.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ca/phon/ui/nativedialogs/NativeDialogs.java
git commit -m "Delegate dialogs to provider SPI with Swing fallback

Removes JNI library loading and native method declarations. With no
provider yet, all dialogs use the existing Swing implementations.

Refs #1"
```

### Task 1.4: Deprecate the now-unused loader

**Files:**
- Modify: `src/main/java/ca/phon/ui/nativedialogs/NativeUtilities.java`

- [ ] **Step 1: Mark `loadLibrary` and `libraryPath` deprecated**

Add `@Deprecated` to `public static void loadLibrary(String libName)` with a Javadoc note: `@deprecated The library no longer ships native binaries; dialogs use the FFM provider in {@code ca.phon.ui.nativedialogs.spi}.` Leave the body intact (harmless if called). Keep all `isMacOs`/`isWindows`/`isLinux` methods unchanged.

- [ ] **Step 2: Compile & test**

Run: `./gradlew test`
Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/ca/phon/ui/nativedialogs/NativeUtilities.java
git commit -m "Deprecate NativeUtilities.loadLibrary

Refs #1"
```

> **PHASE 1 CHECKPOINT — pause for review.** The library now compiles on Java 25, runs Swing-only, and all tests pass. Confirm before starting the macOS provider.

---

## Phase 2 — macOS provider (Cocoa via FFM)

Covers open/save/message app-modal. Font and colour report unsupported (Swing). *Pause for review after this phase.* All work in this phase must be verified on a macOS machine.

### Task 2.1: ObjC binding helper (the spike)

This task proves the `objc_msgSend` mechanism end to end. If anything in the FFM binding approach is wrong, it surfaces here.

**Files:**
- Create: `src/main/java/ca/phon/ui/nativedialogs/ffm/mac/ObjC.java`

- [ ] **Step 1: Write `ObjC.java`**

```java
package ca.phon.ui.nativedialogs.ffm.mac;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;

/**
 * Minimal Objective-C runtime bridge over libobjc using FFM.
 *
 * <p>Loads Foundation and AppKit so that {@code objc_getClass} resolves the
 * Cocoa classes we need. Each {@code objc_msgSend} variant is bound with a
 * fixed {@link FunctionDescriptor}; this is valid on arm64/x86_64 macOS for
 * the integer/pointer/BOOL signatures used here.
 */
public final class ObjC {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final Arena GLOBAL = Arena.ofShared();

    static {
        // Loading the frameworks registers their classes with the runtime.
        SymbolLookup.libraryLookup(
            "/System/Library/Frameworks/Foundation.framework/Foundation", GLOBAL);
        SymbolLookup.libraryLookup(
            "/System/Library/Frameworks/AppKit.framework/AppKit", GLOBAL);
    }

    private static final SymbolLookup OBJC =
        SymbolLookup.libraryLookup("/usr/lib/libobjc.A.dylib", GLOBAL);

    private static MethodHandle h(String name, FunctionDescriptor desc) {
        return LINKER.downcallHandle(OBJC.find(name).orElseThrow(
            () -> new IllegalStateException("symbol not found: " + name)), desc);
    }

    private static final MethodHandle OBJC_GET_CLASS =
        h("objc_getClass", FunctionDescriptor.of(ADDRESS, ADDRESS));
    private static final MethodHandle SEL_REGISTER_NAME =
        h("sel_registerName", FunctionDescriptor.of(ADDRESS, ADDRESS));

    // objc_msgSend variants by signature
    private static final MethodHandle MSG_id =
        h("objc_msgSend", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
    private static final MethodHandle MSG_id_id =
        h("objc_msgSend", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));
    private static final MethodHandle MSG_void =
        h("objc_msgSend", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
    private static final MethodHandle MSG_void_id =
        h("objc_msgSend", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));
    private static final MethodHandle MSG_void_bool =
        h("objc_msgSend", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_BOOLEAN));
    private static final MethodHandle MSG_long =
        h("objc_msgSend", FunctionDescriptor.of(JAVA_LONG, ADDRESS, ADDRESS));

    private ObjC() {}

    public static MemorySegment cls(String name) {
        try (Arena a = Arena.ofConfined()) {
            return (MemorySegment) OBJC_GET_CLASS.invokeExact(a.allocateUtf8String(name));
        } catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static MemorySegment sel(String name) {
        try (Arena a = Arena.ofConfined()) {
            return (MemorySegment) SEL_REGISTER_NAME.invokeExact(a.allocateUtf8String(name));
        } catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static MemorySegment send(MemorySegment recv, String sel) {
        try { return (MemorySegment) MSG_id.invokeExact(recv, sel(sel)); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static MemorySegment send(MemorySegment recv, String sel, MemorySegment arg) {
        try { return (MemorySegment) MSG_id_id.invokeExact(recv, sel(sel), arg); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static void sendVoid(MemorySegment recv, String sel) {
        try { MSG_void.invokeExact(recv, sel(sel)); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static void sendVoid(MemorySegment recv, String sel, MemorySegment arg) {
        try { MSG_void_id.invokeExact(recv, sel(sel), arg); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static void sendBool(MemorySegment recv, String sel, boolean value) {
        try { MSG_void_bool.invokeExact(recv, sel(sel), value); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static long sendLong(MemorySegment recv, String sel) {
        try { return (long) MSG_long.invokeExact(recv, sel(sel)); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }
}
```

- [ ] **Step 2: Write a macOS-only smoke test**

Create `src/test/java/ca/phon/ui/nativedialogs/ffm/mac/FoundationSmokeTest.java`:

```java
package ca.phon.ui.nativedialogs.ffm.mac;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import java.lang.foreign.MemorySegment;
import static org.junit.jupiter.api.Assertions.*;

@EnabledOnOs(OS.MAC)
class FoundationSmokeTest {

    @Test
    void resolvesNSObjectClass() {
        MemorySegment cls = ObjC.cls("NSObject");
        assertNotEquals(MemorySegment.NULL, cls, "NSObject class must resolve");
    }

    @Test
    void resolvesNSOpenPanelClass() {
        MemorySegment cls = ObjC.cls("NSOpenPanel");
        assertNotEquals(MemorySegment.NULL, cls, "AppKit must be loaded for NSOpenPanel");
    }
}
```

- [ ] **Step 3: Run on macOS**

Run: `./gradlew test --tests '*FoundationSmokeTest'`
Expected: PASS on macOS (skipped on other OSes). If `NSOpenPanel` is NULL, AppKit failed to load — fix the framework path before continuing.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/ca/phon/ui/nativedialogs/ffm/mac/ObjC.java src/test/java/ca/phon/ui/nativedialogs/ffm/mac/FoundationSmokeTest.java
git commit -m "Add Objective-C FFM bridge and macOS smoke test

Refs #1"
```

### Task 2.2: Foundation helpers (NSString/NSURL/NSArray)

**Files:**
- Create: `src/main/java/ca/phon/ui/nativedialogs/ffm/mac/Foundation.java`
- Modify: `src/test/java/ca/phon/ui/nativedialogs/ffm/mac/FoundationSmokeTest.java`

- [ ] **Step 1: Write `Foundation.java`**

```java
package ca.phon.ui.nativedialogs.ffm.mac;

import java.lang.foreign.*;
import java.util.List;

import static java.lang.foreign.ValueLayout.*;

/** NSString / NSURL / NSArray helpers built on {@link ObjC}. */
public final class Foundation {

    private Foundation() {}

    /** Java String -> NSString* (autoreleased). */
    public static MemorySegment nsString(Arena arena, String s) {
        MemorySegment utf8 = arena.allocateUtf8String(s);
        return ObjC.send(ObjC.cls("NSString"), "stringWithUTF8String:", utf8);
    }

    /** NSString* -> Java String. Reads the UTF8String C pointer. */
    public static String toJavaString(MemorySegment nsString) {
        if (nsString.equals(MemorySegment.NULL)) return null;
        MemorySegment cstr = ObjC.send(nsString, "UTF8String");
        if (cstr.equals(MemorySegment.NULL)) return null;
        // length is unknown; reinterpret to read the null-terminated string
        return cstr.reinterpret(Long.MAX_VALUE).getUtf8String(0);
    }

    /** file path -> NSURL* (fileURLWithPath:). */
    public static MemorySegment fileUrl(Arena arena, String path) {
        return ObjC.send(ObjC.cls("NSURL"), "fileURLWithPath:", nsString(arena, path));
    }

    /** NSURL* -> file path Java String ([url path]). */
    public static String urlPath(MemorySegment url) {
        return toJavaString(ObjC.send(url, "path"));
    }

    /** List<String> -> NSArray<NSString>* via arrayWithObjects:count:. */
    public static MemorySegment nsStringArray(Arena arena, List<String> items) {
        MemorySegment buf = arena.allocate(ADDRESS.byteSize() * items.size());
        for (int i = 0; i < items.size(); i++) {
            buf.setAtIndex(ADDRESS, i, nsString(arena, items.get(i)));
        }
        // objc_msgSend(NSArray, arrayWithObjects:count:, buf, count)
        return ObjC.sendObjLong(ObjC.cls("NSArray"), "arrayWithObjects:count:", buf, items.size());
    }
}
```

This needs one more `ObjC` variant. Add to `ObjC.java`:

```java
private static final MethodHandle MSG_id_id_long =
    h("objc_msgSend", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_LONG));

public static MemorySegment sendObjLong(MemorySegment recv, String sel, MemorySegment arg, long n) {
    try { return (MemorySegment) MSG_id_id_long.invokeExact(recv, sel(sel), arg, n); }
    catch (Throwable t) { throw new RuntimeException(t); }
}
```

- [ ] **Step 2: Add round-trip test**

Append to `FoundationSmokeTest.java`:

```java
    @Test
    void nsStringRoundTrip() {
        try (java.lang.foreign.Arena a = java.lang.foreign.Arena.ofConfined()) {
            MemorySegment ns = Foundation.nsString(a, "héllo/世界.txt");
            assertEquals("héllo/世界.txt", Foundation.toJavaString(ns));
        }
    }
```

- [ ] **Step 3: Run on macOS**

Run: `./gradlew test --tests '*FoundationSmokeTest'`
Expected: PASS (3 tests on macOS).

- [ ] **Step 4: Commit**

```bash
git add src/main/java/ca/phon/ui/nativedialogs/ffm/mac/Foundation.java src/main/java/ca/phon/ui/nativedialogs/ffm/mac/ObjC.java src/test/java/ca/phon/ui/nativedialogs/ffm/mac/FoundationSmokeTest.java
git commit -m "Add Foundation NSString/NSURL/NSArray helpers

Refs #1"
```

### Task 2.3: GCD main-queue dispatch

**Files:**
- Create: `src/main/java/ca/phon/ui/nativedialogs/ffm/mac/Gcd.java`

- [ ] **Step 1: Write `Gcd.java`**

```java
package ca.phon.ui.nativedialogs.ffm.mac;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static java.lang.foreign.ValueLayout.*;

/**
 * Runs work on the AppKit main thread via {@code dispatch_*_f} (the
 * function-pointer GCD variants — no Objective-C block ABI needed).
 *
 * <p>{@code dispatch_get_main_queue()} is a macro expanding to the address of
 * the global {@code _dispatch_main_q}; we look that symbol up directly.
 */
public final class Gcd {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final Arena GLOBAL = Arena.ofShared();
    private static final SymbolLookup SYS = LINKER.defaultLookup();

    private static final MemorySegment MAIN_QUEUE =
        SYS.find("_dispatch_main_q").orElseThrow(
            () -> new IllegalStateException("_dispatch_main_q not found"));

    private static final MethodHandle DISPATCH_SYNC_F = LINKER.downcallHandle(
        SYS.find("dispatch_sync_f").orElseThrow(),
        FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));
    private static final MethodHandle DISPATCH_ASYNC_F = LINKER.downcallHandle(
        SYS.find("dispatch_async_f").orElseThrow(),
        FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));

    private static final FunctionDescriptor WORK_DESC =
        FunctionDescriptor.ofVoid(ADDRESS); // void (*)(void *context)
    private static final MethodHandle WORK_TARGET;
    static {
        try {
            WORK_TARGET = MethodHandles.lookup().findStatic(
                Gcd.class, "trampoline",
                MethodType.methodType(void.class, MemorySegment.class));
        } catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); }
    }

    private Gcd() {}

    private static void trampoline(MemorySegment context) {
        Runnable r = PENDING.remove(context.address());
        if (r != null) r.run();
    }

    private static final java.util.concurrent.ConcurrentHashMap<Long, Runnable> PENDING =
        new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicLong KEYGEN =
        new java.util.concurrent.atomic.AtomicLong(1);

    /** Run on the main thread and block the caller until it finishes. */
    public static void onMainSync(Runnable work) {
        if (isMainThread()) { work.run(); return; }
        long key = KEYGEN.getAndIncrement();
        PENDING.put(key, work);
        try (Arena a = Arena.ofShared()) {
            MemorySegment context = a.allocate(JAVA_LONG);
            context.set(JAVA_LONG, 0, key);
            MemorySegment stub = LINKER.upcallStub(WORK_TARGET, WORK_DESC, a);
            // Map by the context segment address so the trampoline can find the Runnable.
            PENDING.put(context.address(), PENDING.remove(key));
            DISPATCH_SYNC_F.invokeExact(MAIN_QUEUE, context, stub);
        } catch (Throwable t) { throw new RuntimeException(t); }
    }

    /** Run on the main thread without blocking the caller. */
    public static void onMainAsync(Runnable work) {
        // For async we keep the arena open until the trampoline runs.
        Arena a = Arena.ofShared();
        MemorySegment context = a.allocate(JAVA_LONG);
        PENDING.put(context.address(), () -> {
            try { work.run(); } finally { a.close(); }
        });
        MemorySegment stub = LINKER.upcallStub(WORK_TARGET, WORK_DESC, a);
        try { DISPATCH_ASYNC_F.invokeExact(MAIN_QUEUE, context, stub); }
        catch (Throwable t) { a.close(); throw new RuntimeException(t); }
    }

    private static boolean isMainThread() {
        // [NSThread isMainThread]
        MemorySegment isMain = ObjC.send(ObjC.cls("NSThread"), "isMainThread");
        return isMain.address() != 0;
    }
}
```

> **Spike note for the implementer:** `onMainSync` keys the `Runnable` by the context segment's address; simplify if your spike shows a cleaner mapping. The essential, validated facts are: (a) `_dispatch_main_q` is the main queue symbol, (b) `dispatch_sync_f`/`dispatch_async_f` take `(queue, context, fnptr)`, (c) the fnptr is an FFM upcall stub of type `void(void*)`. Confirm `isMainThread` returns a boolean-in-pointer correctly during the spike; if the BOOL return needs a dedicated descriptor, add `MSG_bool` to `ObjC`.

- [ ] **Step 2: Compile**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/ca/phon/ui/nativedialogs/ffm/mac/Gcd.java
git commit -m "Add GCD main-queue dispatch via dispatch_*_f upcall stubs

Refs #1"
```

### Task 2.4: MacDialogProvider — open and save

**Files:**
- Modify (replace stub): `src/main/java/ca/phon/ui/nativedialogs/spi/MacDialogProvider.java`

- [ ] **Step 1: Replace the stub with the real provider (open + save)**

```java
package ca.phon.ui.nativedialogs.spi;

import java.lang.foreign.*;
import java.util.ArrayList;
import java.util.List;

import ca.phon.ui.nativedialogs.*;
import ca.phon.ui.nativedialogs.ffm.mac.*;

import static java.lang.foreign.ValueLayout.*;

/**
 * macOS native dialogs via Cocoa (FFM, app-modal). Open/save/message native;
 * font/colour unsupported (facade falls back to Swing).
 */
final class MacDialogProvider implements NativeDialogProvider {

    private static final long NS_MODAL_RESPONSE_OK = 1L;          // NSModalResponseOK
    private static final long NS_ALERT_FIRST_BUTTON = 1000L;      // NSAlertFirstButtonReturn
    private static final int NS_WARNING_ALERT_STYLE = 0;          // NSAlertStyleWarning

    MacDialogProvider() {
        // Force class init so construction fails fast if FFM/AppKit is unavailable.
        ObjC.cls("NSOpenPanel");
    }

    @Override public boolean supportsOpen() { return true; }
    @Override public boolean supportsSave() { return true; }
    @Override public boolean supportsMessage() { return true; }
    @Override public boolean supportsFont() { return false; }
    @Override public boolean supportsColor() { return false; }

    @Override public void showFont(FontDialogProperties p) { throw new UnsupportedOperationException(); }
    @Override public void showColor(ColorDialogProperties p) { throw new UnsupportedOperationException(); }

    @Override
    public void showOpen(OpenDialogProperties props) {
        Runnable work = () -> runOpen(props);
        if (props.isRunAsync()) Gcd.onMainAsync(work); else Gcd.onMainSync(work);
    }

    private void runOpen(OpenDialogProperties props) {
        try (Arena a = Arena.ofShared()) {
            MemorySegment panel = ObjC.send(ObjC.cls("NSOpenPanel"), "openPanel");
            ObjC.sendBool(panel, "setCanCreateDirectories:", props.isCanCreateDirectories());
            ObjC.sendBool(panel, "setShowsHiddenFiles:", props.isShowHidden());
            ObjC.sendBool(panel, "setCanChooseFiles:", props.isCanChooseFiles());
            ObjC.sendBool(panel, "setCanChooseDirectories:", props.isCanChooseDirectories());
            ObjC.sendBool(panel, "setAllowsMultipleSelection:", props.isAllowMultipleSelection());

            applyCommon(a, panel, props.getTitle(), props.getPrompt(),
                props.getNameFieldLabel(), props.getMessage(),
                props.getInitialFolder(), props.getInitialFile(), props.getFileFilter());

            long response = ObjC.sendLong(panel, "runModal");
            NativeDialogEvent evt;
            if (response == NS_MODAL_RESPONSE_OK) {
                if (props.isAllowMultipleSelection()) {
                    MemorySegment urls = ObjC.send(panel, "URLs");
                    long count = ObjC.sendLong(urls, "count");
                    List<String> paths = new ArrayList<>();
                    for (long i = 0; i < count; i++) {
                        MemorySegment url = ObjC.sendIdLong(urls, "objectAtIndex:", i);
                        paths.add(Foundation.urlPath(url));
                    }
                    evt = new NativeDialogEvent(NativeDialogEvent.OK_OPTION, paths.toArray(new String[0]));
                } else {
                    MemorySegment url = ObjC.send(panel, "URL");
                    evt = new NativeDialogEvent(NativeDialogEvent.OK_OPTION, Foundation.urlPath(url));
                }
            } else {
                evt = new NativeDialogEvent(NativeDialogEvent.CANCEL_OPTION, null);
            }
            props.getListener().nativeDialogEvent(evt);
        }
    }

    @Override
    public void showSave(SaveDialogProperties props) {
        Runnable work = () -> runSave(props);
        if (props.isRunAsync()) Gcd.onMainAsync(work); else Gcd.onMainSync(work);
    }

    private void runSave(SaveDialogProperties props) {
        try (Arena a = Arena.ofShared()) {
            MemorySegment panel = ObjC.send(ObjC.cls("NSSavePanel"), "savePanel");
            ObjC.sendBool(panel, "setCanCreateDirectories:", props.isCanCreateDirectories());
            ObjC.sendBool(panel, "setShowsHiddenFiles:", props.isShowHidden());

            applyCommon(a, panel, props.getTitle(), props.getPrompt(),
                props.getNameFieldLabel(), props.getMessage(),
                props.getInitialFolder(), props.getInitialFile(), props.getFileFilter());

            long response = ObjC.sendLong(panel, "runModal");
            NativeDialogEvent evt;
            if (response == NS_MODAL_RESPONSE_OK) {
                MemorySegment url = ObjC.send(panel, "URL");
                evt = new NativeDialogEvent(NativeDialogEvent.OK_OPTION, Foundation.urlPath(url));
            } else {
                evt = new NativeDialogEvent(NativeDialogEvent.CANCEL_OPTION, null);
            }
            props.getListener().nativeDialogEvent(evt);
        }
    }

    private void applyCommon(Arena a, MemorySegment panel, String title, String prompt,
            String nameFieldLabel, String message, String initialFolder,
            String initialFile, FileFilter filter) {
        if (title != null) ObjC.sendVoid(panel, "setTitle:", Foundation.nsString(a, title));
        if (prompt != null) ObjC.sendVoid(panel, "setPrompt:", Foundation.nsString(a, prompt));
        if (nameFieldLabel != null) ObjC.sendVoid(panel, "setNameFieldLabel:", Foundation.nsString(a, nameFieldLabel));
        if (message != null) ObjC.sendVoid(panel, "setMessage:", Foundation.nsString(a, message));
        if (initialFolder != null) ObjC.sendVoid(panel, "setDirectoryURL:", Foundation.fileUrl(a, initialFolder));
        if (initialFile != null) ObjC.sendVoid(panel, "setNameFieldStringValue:", Foundation.nsString(a, initialFile));
        if (filter != null && filter.getAllExtensions() != null && !filter.getAllExtensions().isEmpty()) {
            ObjC.sendVoid(panel, "setAllowedFileTypes:",
                Foundation.nsStringArray(a, new ArrayList<>(filter.getAllExtensions())));
        }
    }

    @Override
    public void showMessage(MessageDialogProperties props) {
        // implemented in Task 2.5
        throw new UnsupportedOperationException("message not yet wired");
    }
}
```

> Getter names are verified: `getTitle`/`getPrompt`/`getNameFieldLabel`/`getMessage`/`getInitialFolder`/`getInitialFile`/`getFileFilter`/`isShowHidden`/`isCanCreateDirectories` are all reachable on `OpenDialogProperties` (inherited from `SaveDialogProperties`); `isCanChooseFiles`/`isCanChooseDirectories`/`isAllowMultipleSelection` are on `OpenDialogProperties`; `FileFilter.getAllExtensions()` returns `List<String>`. **`objectAtIndex:` takes a `long` index, not an object** — do NOT use `sendObjLong`. Add this exact variant to `ObjC` and use it in the multi-select loop:
> ```java
> private static final MethodHandle MSG_id_long =
>     h("objc_msgSend", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_LONG));
> public static MemorySegment sendIdLong(MemorySegment recv, String sel, long n) {
>     try { return (MemorySegment) MSG_id_long.invokeExact(recv, sel(sel), n); }
>     catch (Throwable t) { throw new RuntimeException(t); }
> }
> ```
> i.e. replace `ObjC.sendObjLong(urls, "objectAtIndex:", MemorySegment.NULL, i)` with `ObjC.sendIdLong(urls, "objectAtIndex:", i)`.

- [ ] **Step 2: Compile**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Manual verification on macOS**

Run: `./gradlew build -Pversion=24 && java -jar build/libs/native-dialogs-24.jar`
Open the demo's Open and Save actions.
Expected: **native macOS** open and save panels appear (not Swing). Selecting a file returns the correct path in the demo output; Cancel returns no selection. Test multi-select open and a file-type filter.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/ca/phon/ui/nativedialogs/spi/MacDialogProvider.java
git commit -m "Implement native macOS open and save dialogs via FFM

Refs #1"
```

### Task 2.5: MacDialogProvider — message (NSAlert)

**Files:**
- Modify: `src/main/java/ca/phon/ui/nativedialogs/spi/MacDialogProvider.java`

- [ ] **Step 1: Replace `showMessage` with the real implementation**

```java
    @Override
    public void showMessage(MessageDialogProperties props) {
        Runnable work = () -> runMessage(props);
        if (props.isRunAsync()) Gcd.onMainAsync(work); else Gcd.onMainSync(work);
    }

    private void runMessage(MessageDialogProperties props) {
        try (Arena a = Arena.ofShared()) {
            MemorySegment alert = ObjC.send(
                ObjC.send(ObjC.cls("NSAlert"), "alloc"), "init");
            ObjC.sendLongArg(alert, "setAlertStyle:", NS_WARNING_ALERT_STYLE);
            if (props.getHeader() != null)
                ObjC.sendVoid(alert, "setMessageText:", Foundation.nsString(a, props.getHeader()));
            if (props.getMessage() != null)
                ObjC.sendVoid(alert, "setInformativeText:", Foundation.nsString(a, props.getMessage()));

            boolean suppression = props.isShowSuppressionBox();
            ObjC.sendBool(alert, "setShowsSuppressionButton:", suppression);
            if (suppression && props.getSuppressionMessage() != null) {
                MemorySegment btn = ObjC.send(alert, "suppressionButton");
                ObjC.sendVoid(btn, "setTitle:", Foundation.nsString(a, props.getSuppressionMessage()));
            }

            String[] options = props.getOptions();
            if (options == null || options.length == 0) options = new String[] { "Ok" };
            for (String opt : options) {
                ObjC.send(alert, "addButtonWithTitle:", Foundation.nsString(a, opt));
            }

            long alertResult = ObjC.sendLong(alert, "runModal");
            int resultCode = (int) (alertResult - NS_ALERT_FIRST_BUTTON);

            Boolean suppressed = null;
            if (suppression) {
                MemorySegment btn = ObjC.send(alert, "suppressionButton");
                long state = ObjC.sendLong(btn, "state");
                suppressed = (state == 1L); // NSControlStateValueOn
            }
            props.getListener().nativeDialogEvent(new NativeDialogEvent(resultCode, suppressed));
        }
    }
```

Add the missing `ObjC` variant for `setAlertStyle:` (takes an `NSInteger`):

```java
private static final MethodHandle MSG_void_long =
    h("objc_msgSend", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_LONG));

public static void sendLongArg(MemorySegment recv, String sel, long value) {
    try { MSG_void_long.invokeExact(recv, sel(sel), value); }
    catch (Throwable t) { throw new RuntimeException(t); }
}
```

> Verified against `MessageDialogProperties`: `getHeader()`, `getMessage()`, `getOptions()` (`String[]`), `isShowSuppressionBox()`, `getSuppressionMessage()` all exist. The result-code convention (`button index from 0`) matches the existing Swing path and the old `.mm` (`returnCode - NSAlertFirstButtonReturn`).

- [ ] **Step 2: Compile**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Manual verification on macOS**

Run: `java -jar build/libs/native-dialogs-24.jar` (rebuild first).
Trigger Yes/No/Cancel, OK/Cancel, and a suppression-box message.
Expected: native NSAlert with the correct buttons; the returned index matches the clicked button (0 = first); suppression checkbox state returns as the event data.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/ca/phon/ui/nativedialogs/spi/MacDialogProvider.java src/main/java/ca/phon/ui/nativedialogs/ffm/mac/ObjC.java
git commit -m "Implement native macOS message dialog (NSAlert) via FFM

Refs #1"
```

> **PHASE 2 CHECKPOINT — pause for review.** macOS now shows native open/save/message; font/colour use Swing. Verify on a Mac before starting Windows.

---

## Phase 3 — Windows provider (Win32 via FFM)

Covers open/save (COM Common Item Dialog), message (`TaskDialogIndirect`), font (`ChooseFontW`), colour (`ChooseColorW`). *Pause for review after this phase.* All work verified on Windows.

### Task 3.1: Win32 constants, library lookups, and string helpers (the spike)

**Files:**
- Create: `src/main/java/ca/phon/ui/nativedialogs/ffm/win/Win32.java`
- Create: `src/main/java/ca/phon/ui/nativedialogs/ffm/win/WinStr.java`
- Create: `src/test/java/ca/phon/ui/nativedialogs/ffm/win/WinStrTest.java`
- Create: `src/test/java/ca/phon/ui/nativedialogs/ffm/win/Win32SmokeTest.java`

- [ ] **Step 1: Write `WinStr.java`**

```java
package ca.phon.ui.nativedialogs.ffm.win;

import java.lang.foreign.*;
import java.nio.charset.StandardCharsets;

import static java.lang.foreign.ValueLayout.*;

/** UTF-16LE (wide) string helpers for the Win32 W APIs. */
public final class WinStr {

    private WinStr() {}

    /** Java String -> null-terminated UTF-16LE buffer. */
    public static MemorySegment wide(Arena arena, String s) {
        byte[] utf16 = s.getBytes(StandardCharsets.UTF_16LE);
        MemorySegment seg = arena.allocate(utf16.length + 2L);
        MemorySegment.copy(utf16, 0, seg, JAVA_BYTE, 0, utf16.length);
        seg.set(JAVA_BYTE, utf16.length, (byte) 0);
        seg.set(JAVA_BYTE, utf16.length + 1, (byte) 0);
        return seg;
    }

    /** Pointer to a null-terminated UTF-16LE string -> Java String. */
    public static String fromWide(MemorySegment ptr) {
        if (ptr.equals(MemorySegment.NULL)) return null;
        MemorySegment p = ptr.reinterpret(Long.MAX_VALUE);
        StringBuilder sb = new StringBuilder();
        for (long i = 0; ; i += 2) {
            char c = p.get(JAVA_CHAR, i);
            if (c == 0) break;
            sb.append(c);
        }
        return sb.toString();
    }
}
```

- [ ] **Step 2: Write the UTF-16 round-trip test**

```java
package ca.phon.ui.nativedialogs.ffm.win;

import org.junit.jupiter.api.Test;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import static org.junit.jupiter.api.Assertions.*;

class WinStrTest {
    @Test
    void roundTrip() {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment w = WinStr.wide(a, "C:/héllo/世界.txt");
            assertEquals("C:/héllo/世界.txt", WinStr.fromWide(w));
        }
    }
}
```

This test is platform-independent (no native calls), so it runs everywhere.

- [ ] **Step 3: Write `Win32.java`**

```java
package ca.phon.ui.nativedialogs.ffm.win;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;

/** Win32 constants, GUIDs, library lookups, and shared downcall handles. */
public final class Win32 {

    public static final int S_OK = 0;
    public static final int CLSCTX_INPROC_SERVER = 0x1;
    public static final int COINIT_APARTMENTTHREADED = 0x2;
    public static final int SIGDN_FILESYSPATH = 0x80058000;

    public static final int FOS_OVERWRITEPROMPT  = 0x00000002;
    public static final int FOS_PICKFOLDERS      = 0x00000020;
    public static final int FOS_FORCEFILESYSTEM  = 0x00000040;
    public static final int FOS_ALLOWMULTISELECT = 0x00000200;
    public static final int FOS_PATHMUSTEXIST    = 0x00000800;
    public static final int FOS_FILEMUSTEXIST    = 0x00001000;
    public static final int FOS_FORCESHOWHIDDEN  = 0x10000000;

    private static final Linker LINKER = Linker.nativeLinker();
    private static final Arena GLOBAL = Arena.ofShared();

    private static final SymbolLookup OLE32  = SymbolLookup.libraryLookup("ole32.dll", GLOBAL);
    private static final SymbolLookup SHELL32 = SymbolLookup.libraryLookup("shell32.dll", GLOBAL);
    static final SymbolLookup COMDLG32 = SymbolLookup.libraryLookup("comdlg32.dll", GLOBAL);
    static final SymbolLookup COMCTL32 = SymbolLookup.libraryLookup("comctl32.dll", GLOBAL);

    public static final MethodHandle CoInitializeEx = LINKER.downcallHandle(
        OLE32.find("CoInitializeEx").orElseThrow(),
        FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
    public static final MethodHandle CoUninitialize = LINKER.downcallHandle(
        OLE32.find("CoUninitialize").orElseThrow(), FunctionDescriptor.ofVoid());
    public static final MethodHandle CoCreateInstance = LINKER.downcallHandle(
        OLE32.find("CoCreateInstance").orElseThrow(),
        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS));
    public static final MethodHandle CoTaskMemFree = LINKER.downcallHandle(
        OLE32.find("CoTaskMemFree").orElseThrow(), FunctionDescriptor.ofVoid(ADDRESS));
    public static final MethodHandle SHCreateItemFromParsingName = LINKER.downcallHandle(
        SHELL32.find("SHCreateItemFromParsingName").orElseThrow(),
        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    private Win32() {}

    /** Build a 16-byte GUID from its canonical fields. */
    public static MemorySegment guid(Arena a, int d1, short d2, short d3, long d4Hi8Bytes) {
        MemorySegment g = a.allocate(16);
        g.set(JAVA_INT, 0, d1);
        g.set(JAVA_SHORT, 4, d2);
        g.set(JAVA_SHORT, 6, d3);
        // d4 is the trailing 8 bytes in big-endian order as written in the GUID text
        for (int i = 0; i < 8; i++) {
            g.set(JAVA_BYTE, 8 + i, (byte) ((d4Hi8Bytes >>> (56 - 8 * i)) & 0xFF));
        }
        return g;
    }

    public static MemorySegment CLSID_FileOpenDialog(Arena a) {
        return guid(a, 0xDC1C5A9C, (short) 0xE88A, (short) 0x4DDE, 0xA5A160F82A20AEF7L);
    }
    public static MemorySegment CLSID_FileSaveDialog(Arena a) {
        return guid(a, 0xC0B4E2F3, (short) 0xBA21, (short) 0x4773, 0x8DBA335EC946EB8BL);
    }
    public static MemorySegment IID_IFileOpenDialog(Arena a) {
        return guid(a, 0xD57C7288, (short) 0xD4AD, (short) 0x4768, 0xBE029D969532D960L);
    }
    public static MemorySegment IID_IFileSaveDialog(Arena a) {
        return guid(a, 0x84BCCD23, (short) 0x5FDE, (short) 0x4CDB, 0xAEA4AF64B83D78ABL);
    }
    public static MemorySegment IID_IShellItem(Arena a) {
        return guid(a, 0x43826D1E, (short) 0xE718, (short) 0x42EE, 0xBC55A1E261C37BFEL);
    }
}
```

- [ ] **Step 4: Write the Windows symbol-resolution smoke test**

```java
package ca.phon.ui.nativedialogs.ffm.win;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import static org.junit.jupiter.api.Assertions.*;

@EnabledOnOs(OS.WINDOWS)
class Win32SmokeTest {
    @Test
    void comInitializesAndUninitializes() throws Throwable {
        int hr = (int) Win32.CoInitializeEx.invokeExact(
            java.lang.foreign.MemorySegment.NULL, Win32.COINIT_APARTMENTTHREADED);
        // S_OK (0) or S_FALSE (1, already initialised) are both acceptable
        assertTrue(hr == 0 || hr == 1, "CoInitializeEx returned 0x" + Integer.toHexString(hr));
        Win32.CoUninitialize.invokeExact();
    }
}
```

- [ ] **Step 5: Run tests**

Run (any OS): `./gradlew test --tests '*WinStrTest'` → PASS.
Run (Windows): `./gradlew test --tests '*Win32SmokeTest'` → PASS; verifies COM init and that all `ole32`/`shell32` symbols resolved at class load.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/ca/phon/ui/nativedialogs/ffm/win/ src/test/java/ca/phon/ui/nativedialogs/ffm/win/
git commit -m "Add Win32 FFM constants, GUIDs, and string helpers with smoke tests

Refs #1"
```

### Task 3.2: COM vtable invocation helper

**Files:**
- Create: `src/main/java/ca/phon/ui/nativedialogs/ffm/win/Com.java`

- [ ] **Step 1: Write `Com.java`**

```java
package ca.phon.ui.nativedialogs.ffm.win;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.foreign.ValueLayout.*;

/**
 * Invokes COM interface methods by vtable index.
 *
 * <p>A COM interface pointer points to an object whose first field is a pointer
 * to its vtable: an array of function pointers. Method {@code i} is at
 * {@code vtable[i]}; every method takes the interface pointer as its first
 * ("this") argument and (for the ones we use) returns an {@code HRESULT} int.
 */
public final class Com {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final ConcurrentHashMap<FunctionDescriptor, MethodHandle> CACHE =
        new ConcurrentHashMap<>();

    private Com() {}

    private static MemorySegment methodPtr(MemorySegment iface, int index) {
        MemorySegment vtbl = iface.reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0)
            .reinterpret(ADDRESS.byteSize() * (index + 1));
        return vtbl.getAtIndex(ADDRESS, index);
    }

    private static MethodHandle handle(MemorySegment fn, FunctionDescriptor desc) {
        return LINKER.downcallHandle(fn, desc);
    }

    /** Call an HRESULT-returning method whose only args (after this) are pointers. */
    public static int callHr(MemorySegment iface, int index, MemorySegment... ptrArgs) {
        MemoryLayout[] argLayouts = new MemoryLayout[ptrArgs.length + 1];
        argLayouts[0] = ADDRESS;
        for (int i = 0; i < ptrArgs.length; i++) argLayouts[i + 1] = ADDRESS;
        FunctionDescriptor desc = FunctionDescriptor.of(JAVA_INT, argLayouts);
        MethodHandle h = handle(methodPtr(iface, index), desc);
        Object[] args = new Object[ptrArgs.length + 1];
        args[0] = iface;
        System.arraycopy(ptrArgs, 0, args, 1, ptrArgs.length);
        try { return (int) h.invokeWithArguments(args); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    /** Call an HRESULT method taking a single int after this (e.g. SetOptions, Show(HWND as ptr)). */
    public static int callHrInt(MemorySegment iface, int index, int value) {
        FunctionDescriptor desc = FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT);
        MethodHandle h = handle(methodPtr(iface, index), desc);
        try { return (int) h.invokeExact(iface, value); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    /** Call an HRESULT method: (this, int, ptr) — used by SetFileTypes(count, specs) and GetItemAt(i, ppsi). */
    public static int callHrIntPtr(MemorySegment iface, int index, int value, MemorySegment ptr) {
        FunctionDescriptor desc = FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS);
        MethodHandle h = handle(methodPtr(iface, index), desc);
        try { return (int) h.invokeExact(iface, value, ptr); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    /** Call a method returning ULONG with no extra args (Release). */
    public static int release(MemorySegment iface) {
        if (iface == null || iface.equals(MemorySegment.NULL)) return 0;
        FunctionDescriptor desc = FunctionDescriptor.of(JAVA_INT, ADDRESS);
        MethodHandle h = handle(methodPtr(iface, 2), desc); // Release == index 2
        try { return (int) h.invokeExact(iface); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }
}
```

> **Spike note:** `Show(HWND)` (index 3) takes an `HWND` pointer; pass `MemorySegment.NULL` via `callHr(iface, 3, MemorySegment.NULL)`. Validate `methodPtr` reads the right slot by calling `GetOptions` (index 10) on a freshly created `IFileOpenDialog` and checking `HRESULT == S_OK` in the spike before building the full dialog.

- [ ] **Step 2: Compile**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/ca/phon/ui/nativedialogs/ffm/win/Com.java
git commit -m "Add COM vtable invocation helper

Refs #1"
```

### Task 3.3: WindowsDialogProvider — open and save

**Files:**
- Modify (replace stub): `src/main/java/ca/phon/ui/nativedialogs/spi/WindowsDialogProvider.java`

- [ ] **Step 1: Replace the stub (open + save; message/font/colour throw for now)**

```java
package ca.phon.ui.nativedialogs.spi;

import java.lang.foreign.*;
import java.util.ArrayList;
import java.util.List;

import ca.phon.ui.nativedialogs.*;
import ca.phon.ui.nativedialogs.ffm.win.*;

import static java.lang.foreign.ValueLayout.*;
import static ca.phon.ui.nativedialogs.ffm.win.Win32.*;

/** Windows native dialogs via the Common Item Dialog (COM) and comdlg32. */
final class WindowsDialogProvider implements NativeDialogProvider {

    // COM vtable indices
    private static final int SHOW = 3, SET_FILE_TYPES = 4, SET_OPTIONS = 9, GET_OPTIONS = 10,
        SET_DEFAULT_FOLDER = 11, SET_FILE_NAME = 15, SET_TITLE = 17, SET_OK_LABEL = 18,
        SET_FILE_NAME_LABEL = 19, GET_RESULT = 20, SET_DEFAULT_EXTENSION = 22, GET_RESULTS = 27;
    private static final int SI_GET_DISPLAY_NAME = 5;
    private static final int SIA_GET_COUNT = 7, SIA_GET_ITEM_AT = 8;

    WindowsDialogProvider() {
        // fail fast if COM symbols are unavailable
        Win32.class.getName();
    }

    @Override public boolean supportsOpen() { return true; }
    @Override public boolean supportsSave() { return true; }
    @Override public boolean supportsMessage() { return true; }
    @Override public boolean supportsFont() { return true; }
    @Override public boolean supportsColor() { return true; }

    @Override
    public void showOpen(OpenDialogProperties props) {
        runOnComThread(props.isRunAsync(), () -> runFileDialog(props, true));
    }

    @Override
    public void showSave(SaveDialogProperties props) {
        runOnComThread(props.isRunAsync(), () -> runFileDialog(props, false));
    }

    private void runOnComThread(boolean async, Runnable work) {
        Runnable sta = () -> {
            try {
                CoInitializeEx.invokeExact(MemorySegment.NULL, COINIT_APARTMENTTHREADED);
                work.run();
            } catch (Throwable t) {
                throw new RuntimeException(t);
            } finally {
                try { CoUninitialize.invokeExact(); } catch (Throwable ignore) {}
            }
        };
        if (async) {
            Thread th = new Thread(sta, "native-dialog-com");
            th.setDaemon(true);
            th.start();
        } else {
            sta.run();
        }
    }

    private void runFileDialog(NativeDialogProperties props, boolean open) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment ppv = a.allocate(ADDRESS);
            MemorySegment clsid = open ? CLSID_FileOpenDialog(a) : CLSID_FileSaveDialog(a);
            MemorySegment iid = open ? IID_IFileOpenDialog(a) : IID_IFileSaveDialog(a);

            int hr = (int) CoCreateInstance.invokeExact(
                clsid, MemorySegment.NULL, CLSCTX_INPROC_SERVER, iid, ppv);
            if (hr != S_OK) { deliverCancel(props); return; }
            MemorySegment dlg = ppv.get(ADDRESS, 0);

            try {
                applyFileOptions(a, dlg, props, open);
                int showHr = Com.callHr(dlg, SHOW, MemorySegment.NULL);
                if (showHr != S_OK) { deliverCancel(props); return; }

                if (open && isMulti(props)) {
                    deliverMulti(a, dlg, props);
                } else {
                    deliverSingle(a, dlg, props);
                }
            } finally {
                Com.release(dlg);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private boolean isMulti(NativeDialogProperties props) {
        return props instanceof OpenDialogProperties o && o.isAllowMultipleSelection();
    }

    private void applyFileOptions(Arena a, MemorySegment dlg, NativeDialogProperties props, boolean open) {
        // OpenDialogProperties extends SaveDialogProperties, so the shared file getters
        // are reachable via a single SaveDialogProperties reference for both dialogs.
        SaveDialogProperties s = (SaveDialogProperties) props;

        // options bitmask
        MemorySegment optBuf = a.allocate(JAVA_INT);
        Com.callHr(dlg, GET_OPTIONS, optBuf);
        int opts = optBuf.get(JAVA_INT, 0) | FOS_FORCEFILESYSTEM;
        if (s.isShowHidden()) opts |= FOS_FORCESHOWHIDDEN;
        if (open) {
            opts |= FOS_FILEMUSTEXIST | FOS_PATHMUSTEXIST;
            OpenDialogProperties o = (OpenDialogProperties) props;
            if (o.isCanChooseDirectories() && !o.isCanChooseFiles()) opts |= FOS_PICKFOLDERS;
            if (o.isAllowMultipleSelection()) opts |= FOS_ALLOWMULTISELECT;
        } else {
            opts |= FOS_OVERWRITEPROMPT | FOS_PATHMUSTEXIST;
        }
        Com.callHrInt(dlg, SET_OPTIONS, opts);

        if (s.getTitle() != null) Com.callHr(dlg, SET_TITLE, WinStr.wide(a, s.getTitle()));
        if (s.getPrompt() != null) Com.callHr(dlg, SET_OK_LABEL, WinStr.wide(a, s.getPrompt()));
        if (s.getNameFieldLabel() != null) Com.callHr(dlg, SET_FILE_NAME_LABEL, WinStr.wide(a, s.getNameFieldLabel()));
        if (s.getInitialFile() != null) Com.callHr(dlg, SET_FILE_NAME, WinStr.wide(a, s.getInitialFile()));

        FileFilter filter = s.getFileFilter();
        if (filter != null) {
            applyFilter(a, dlg, filter);
            String ext = filter.getDefaultExtension();
            if (ext != null && !ext.isEmpty())
                Com.callHr(dlg, SET_DEFAULT_EXTENSION, WinStr.wide(a, ext));
        }

        String initialFolder = s.getInitialFolder();
        if (initialFolder != null) {
            MemorySegment psiPP = a.allocate(ADDRESS);
            int shr = (int) SHCreateItemFromParsingName.invokeExact(
                WinStr.wide(a, initialFolder), MemorySegment.NULL, IID_IShellItem(a), psiPP);
            if (shr == S_OK) {
                MemorySegment psi = psiPP.get(ADDRESS, 0);
                Com.callHr(dlg, SET_DEFAULT_FOLDER, psi);
                Com.release(psi);
            }
        }
    }

    private void applyFilter(Arena a, MemorySegment dlg, FileFilter filter) {
        // Build COMDLG_FILTERSPEC[2] = { {desc, "*.ext;*.ext2"}, {"All Files","*.*"} }
        List<String> exts = new ArrayList<>(filter.getAllExtensions());
        StringBuilder spec = new StringBuilder();
        for (int i = 0; i < exts.size(); i++) {
            if (i > 0) spec.append(';');
            spec.append("*.").append(exts.get(i));
        }
        String desc = filter.getDescription() != null ? filter.getDescription() : "Files";

        MemorySegment specs = a.allocate(ADDRESS.byteSize() * 2 * 2); // 2 specs * 2 ptrs
        specs.setAtIndex(ADDRESS, 0, WinStr.wide(a, desc));
        specs.setAtIndex(ADDRESS, 1, WinStr.wide(a, spec.toString()));
        specs.setAtIndex(ADDRESS, 2, WinStr.wide(a, "All Files"));
        specs.setAtIndex(ADDRESS, 3, WinStr.wide(a, "*.*"));
        Com.callHrIntPtr(dlg, SET_FILE_TYPES, 2, specs);
    }

    private void deliverSingle(Arena a, MemorySegment dlg, NativeDialogProperties props) throws Throwable {
        MemorySegment psiPP = a.allocate(ADDRESS);
        int hr = Com.callHr(dlg, GET_RESULT, psiPP);
        if (hr != S_OK) { deliverCancel(props); return; }
        MemorySegment psi = psiPP.get(ADDRESS, 0);
        String path = displayName(a, psi);
        Com.release(psi);
        props.getListener().nativeDialogEvent(new NativeDialogEvent(NativeDialogEvent.OK_OPTION, path));
    }

    private void deliverMulti(Arena a, MemorySegment dlg, NativeDialogProperties props) throws Throwable {
        MemorySegment arrPP = a.allocate(ADDRESS);
        int hr = Com.callHr(dlg, GET_RESULTS, arrPP);
        if (hr != S_OK) { deliverCancel(props); return; }
        MemorySegment arr = arrPP.get(ADDRESS, 0);
        MemorySegment countBuf = a.allocate(JAVA_INT);
        Com.callHr(arr, SIA_GET_COUNT, countBuf);
        int count = countBuf.get(JAVA_INT, 0);
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            MemorySegment itemPP = a.allocate(ADDRESS);
            Com.callHrIntPtr(arr, SIA_GET_ITEM_AT, i, itemPP);
            MemorySegment psi = itemPP.get(ADDRESS, 0);
            paths.add(displayName(a, psi));
            Com.release(psi);
        }
        Com.release(arr);
        props.getListener().nativeDialogEvent(
            new NativeDialogEvent(NativeDialogEvent.OK_OPTION, paths.toArray(new String[0])));
    }

    private String displayName(Arena a, MemorySegment psi) throws Throwable {
        MemorySegment namePP = a.allocate(ADDRESS);
        // GetDisplayName(SIGDN_FILESYSPATH, &pszName) — (this, int, ptr)
        Com.callHrIntPtr(psi, SI_GET_DISPLAY_NAME, SIGDN_FILESYSPATH, namePP);
        MemorySegment pszName = namePP.get(ADDRESS, 0);
        String path = WinStr.fromWide(pszName);
        CoTaskMemFree.invokeExact(pszName);
        return path;
    }

    private void deliverCancel(NativeDialogProperties props) {
        props.getListener().nativeDialogEvent(new NativeDialogEvent(NativeDialogEvent.CANCEL_OPTION, null));
    }

    @Override public void showMessage(MessageDialogProperties p) { throw new UnsupportedOperationException(); }
    @Override public void showFont(FontDialogProperties p) { throw new UnsupportedOperationException(); }
    @Override public void showColor(ColorDialogProperties p) { throw new UnsupportedOperationException(); }
}
```

> Getter names and the `SaveDialogProperties` cast are verified against the sources; `isMulti` already special-cases `OpenDialogProperties`. The `(SaveDialogProperties) props` cast is safe for both open and save because `OpenDialogProperties extends SaveDialogProperties`.

- [ ] **Step 2: Compile**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Manual verification on Windows**

Run: `gradlew.bat build -Pversion=24` then `java -jar build\libs\native-dialogs-24.jar`.
Open the demo's Open and Save actions.
Expected: native Windows file dialogs (the modern Common Item Dialog) appear; single open returns a path, multi-select returns multiple paths, save returns a path with the default extension and prompts on overwrite, folder-pick mode works, and a file-type filter is applied. Cancel returns no selection.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/ca/phon/ui/nativedialogs/spi/WindowsDialogProvider.java
git commit -m "Implement native Windows open and save dialogs via COM/FFM

Refs #1"
```

### Task 3.4: WindowsDialogProvider — message (TaskDialogIndirect)

**Files:**
- Create: `src/main/java/ca/phon/ui/nativedialogs/ffm/win/TaskDialog.java`
- Modify: `src/main/java/ca/phon/ui/nativedialogs/spi/WindowsDialogProvider.java`

- [ ] **Step 1: Write `TaskDialog.java`**

```java
package ca.phon.ui.nativedialogs.ffm.win;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.List;

import static java.lang.foreign.ValueLayout.*;

/**
 * Wraps comctl32 {@code TaskDialogIndirect} with a custom button set.
 * Returns the 0-based index of the chosen custom button, or -1 if dismissed.
 */
public final class TaskDialog {

    // TASKDIALOG_BUTTON { int nButtonID; PCWSTR pszButtonText; } — packed, but the
    // pointer field is 8-byte aligned on x64, so the struct is 16 bytes.
    private static final MemoryLayout BUTTON = MemoryLayout.structLayout(
        JAVA_INT.withName("id"),
        MemoryLayout.paddingLayout(4),
        ADDRESS.withName("text"));

    // TASKDIALOGCONFIG — only the fields we set; total size must match the SDK (x64: 160 bytes).
    private static final long CONFIG_SIZE = 160;
    private static final long OFF_cbSize = 0;
    private static final long OFF_hwndParent = 8;
    private static final long OFF_pszWindowTitle = 24;     // after hInstance, dwFlags(uint), dwCommonButtons(uint)
    private static final long OFF_pszMainInstruction = 40; // after pszMainIcon (union ptr)
    private static final long OFF_pszContent = 48;
    private static final long OFF_cButtons = 56;
    private static final long OFF_pButtons = 64;
    private static final long OFF_nDefaultButton = 72;

    private static final int TDF_ALLOW_DIALOG_CANCELLATION = 0x0008;
    private static final int FIRST_ID = 1000;

    private static final MethodHandle TASK_DIALOG_INDIRECT = Linker.nativeLinker().downcallHandle(
        Win32.COMCTL32.find("TaskDialogIndirect").orElseThrow(),
        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    private TaskDialog() {}

    /**
     * @return index into {@code buttons} of the chosen button, or -1 if cancelled.
     */
    public static int show(String title, String header, String content, List<String> buttons) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment cfg = a.allocate(CONFIG_SIZE);
            cfg.fill((byte) 0);
            cfg.set(JAVA_INT, OFF_cbSize, (int) CONFIG_SIZE);
            // dwFlags lives at offset 20 (after cbSize, hwndParent, hInstance)
            cfg.set(JAVA_INT, 20, TDF_ALLOW_DIALOG_CANCELLATION);
            if (title != null) cfg.set(ADDRESS, OFF_pszWindowTitle, WinStr.wide(a, title));
            if (header != null) cfg.set(ADDRESS, OFF_pszMainInstruction, WinStr.wide(a, header));
            if (content != null) cfg.set(ADDRESS, OFF_pszContent, WinStr.wide(a, content));

            MemorySegment btnArray = a.allocate(BUTTON.byteSize() * buttons.size());
            for (int i = 0; i < buttons.size(); i++) {
                MemorySegment b = btnArray.asSlice(i * BUTTON.byteSize(), BUTTON.byteSize());
                b.set(JAVA_INT, 0, FIRST_ID + i);
                b.set(ADDRESS, 8, WinStr.wide(a, buttons.get(i)));
            }
            cfg.set(JAVA_INT, OFF_cButtons, buttons.size());
            cfg.set(ADDRESS, OFF_pButtons, btnArray);
            cfg.set(JAVA_INT, OFF_nDefaultButton, FIRST_ID);

            MemorySegment pressed = a.allocate(JAVA_INT);
            int hr = (int) TASK_DIALOG_INDIRECT.invokeExact(cfg, pressed, MemorySegment.NULL, MemorySegment.NULL);
            if (hr != Win32.S_OK) return -1;
            int id = pressed.get(JAVA_INT, 0);
            int idx = id - FIRST_ID;
            return (idx >= 0 && idx < buttons.size()) ? idx : -1;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
```

> **Spike note:** `TASKDIALOGCONFIG` offsets are ABI-sensitive. Before relying on them, verify `CONFIG_SIZE == 160` and each offset against `<commctrl.h>` for x64 (the struct begins `UINT cbSize; HWND hwndParent; HINSTANCE hInstance; TASKDIALOG_FLAGS dwFlags; TASKDIALOG_COMMON_BUTTON_FLAGS dwCommonButtons; PCWSTR pszWindowTitle; ...`). Adjust the `OFF_*` constants if the spike shows drift. The app must run with common-controls v6; the JVM enables this, but if the dialog fails to appear, add an activation-context note here.

- [ ] **Step 2: Wire `showMessage` in the provider**

Replace `showMessage` in `WindowsDialogProvider.java`:

```java
    @Override
    public void showMessage(MessageDialogProperties props) {
        Runnable work = () -> {
            String[] opts = props.getOptions();
            List<String> buttons = (opts == null || opts.length == 0)
                ? List.of("Ok") : List.of(opts);
            int idx = ca.phon.ui.nativedialogs.ffm.win.TaskDialog.show(
                props.getTitle(), props.getHeader(), props.getMessage(), buttons);
            int resultCode = (idx >= 0) ? idx : NativeDialogEvent.CANCEL_OPTION;
            // suppression box is not represented in TaskDialog; report null
            props.getListener().nativeDialogEvent(new NativeDialogEvent(resultCode, null));
        };
        runOnComThread(props.isRunAsync(), work);
    }
```

> Note: `TaskDialogIndirect` has a "verification checkbox" that maps to the suppression box; wiring it requires `pszVerificationText` (offset 80) and the `pfVerificationFlagChecked` out-param via the callback. The design accepts dropping suppression on Windows message dialogs for the first release (it was Swing-only before, so this is no regression). If suppression is required, add `pszVerificationText` here and read the checkbox state from the `TaskDialogIndirect` 4th arg (`pfVerificationFlagChecked`, a `BOOL*`).

- [ ] **Step 3: Compile**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Manual verification on Windows**

Rebuild and run the demo; trigger Yes/No/Cancel and OK/Cancel messages.
Expected: a native Task Dialog with the custom buttons; the returned index matches the clicked button (0 = first); closing via the X or Esc returns CANCEL.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ca/phon/ui/nativedialogs/ffm/win/TaskDialog.java src/main/java/ca/phon/ui/nativedialogs/spi/WindowsDialogProvider.java
git commit -m "Implement native Windows message dialog (TaskDialogIndirect)

Refs #1"
```

### Task 3.5: WindowsDialogProvider — colour (ChooseColorW)

**Files:**
- Create: `src/main/java/ca/phon/ui/nativedialogs/ffm/win/ChooseColor.java`
- Modify: `src/main/java/ca/phon/ui/nativedialogs/spi/WindowsDialogProvider.java`

- [ ] **Step 1: Write `ChooseColor.java`**

```java
package ca.phon.ui.nativedialogs.ffm.win;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;

/**
 * comdlg32 {@code ChooseColorW}. CHOOSECOLORW (x64) is 72 bytes:
 * DWORD lStructSize; HWND hwndOwner; HWND hInstance; COLORREF rgbResult;
 * COLORREF* lpCustColors; DWORD Flags; LPARAM lCustData; LPCCHOOKPROC lpfnHook;
 * LPCWSTR lpTemplateName;
 */
public final class ChooseColor {

    private static final int CC_RGBINIT = 0x00000001;
    private static final int CC_FULLOPEN = 0x00000002;
    private static final long SIZE = 72;
    private static final long OFF_rgbResult = 24;
    private static final long OFF_lpCustColors = 32;
    private static final long OFF_Flags = 40;

    private static final MethodHandle CHOOSE_COLOR = Linker.nativeLinker().downcallHandle(
        Win32.COMDLG32.find("ChooseColorW").orElseThrow(),
        FunctionDescriptor.of(JAVA_INT, ADDRESS));

    private ChooseColor() {}

    /** @return 0xRRGGBB chosen colour, or -1 if cancelled. */
    public static int show(int initialRgb) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment custom = a.allocate(JAVA_INT.byteSize() * 16); // 16 custom slots, must be non-null
            MemorySegment cc = a.allocate(SIZE);
            cc.fill((byte) 0);
            cc.set(JAVA_INT, 0, (int) SIZE);
            // COLORREF is 0x00BBGGRR; convert from 0xRRGGBB
            cc.set(JAVA_INT, OFF_rgbResult, toColorRef(initialRgb));
            cc.set(ADDRESS, OFF_lpCustColors, custom);
            cc.set(JAVA_INT, OFF_Flags, CC_RGBINIT | CC_FULLOPEN);

            int ok = (int) CHOOSE_COLOR.invokeExact(cc);
            if (ok == 0) return -1; // cancelled
            return fromColorRef(cc.get(JAVA_INT, OFF_rgbResult));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static int toColorRef(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        return (b << 16) | (g << 8) | r;
    }
    private static int fromColorRef(int cr) {
        int r = cr & 0xFF, g = (cr >> 8) & 0xFF, b = (cr >> 16) & 0xFF;
        return (r << 16) | (g << 8) | b;
    }
}
```

- [ ] **Step 2: Wire `showColor`**

Replace `showColor` in `WindowsDialogProvider.java`:

```java
    @Override
    public void showColor(ColorDialogProperties props) {
        Runnable work = () -> {
            java.awt.Color start = props.getColor();
            int initial = (start != null) ? (start.getRGB() & 0xFFFFFF) : 0xFFFFFF;
            int rgb = ca.phon.ui.nativedialogs.ffm.win.ChooseColor.show(initial);
            if (rgb < 0) {
                props.getListener().nativeDialogEvent(new NativeDialogEvent(NativeDialogEvent.CANCEL_OPTION, null));
            } else {
                java.awt.Color c = new java.awt.Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
                props.getListener().nativeDialogEvent(new NativeDialogEvent(NativeDialogEvent.OK_OPTION, c));
            }
        };
        runOnComThread(props.isRunAsync(), work);
    }
```

> Confirm `ColorDialogProperties.getColor()` exists (the Swing path uses it). If colour is exposed as separate `getRed/Green/Blue`, build the `int` from those instead.

- [ ] **Step 3: Compile, then manual-verify on Windows**

Run: `./gradlew compileJava`; rebuild; run the demo's colour action.
Expected: native Windows colour picker; the chosen colour returns to the demo; Cancel returns nothing.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/ca/phon/ui/nativedialogs/ffm/win/ChooseColor.java src/main/java/ca/phon/ui/nativedialogs/spi/WindowsDialogProvider.java
git commit -m "Implement native Windows colour dialog (ChooseColorW)

Refs #1"
```

### Task 3.6: WindowsDialogProvider — font (ChooseFontW)

**Files:**
- Create: `src/main/java/ca/phon/ui/nativedialogs/ffm/win/ChooseFont.java`
- Modify: `src/main/java/ca/phon/ui/nativedialogs/spi/WindowsDialogProvider.java`

- [ ] **Step 1: Write `ChooseFont.java`**

```java
package ca.phon.ui.nativedialogs.ffm.win;

import java.awt.Font;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;

/**
 * comdlg32 {@code ChooseFontW}. Uses LOGFONTW + CHOOSEFONTW.
 * LOGFONTW (x64) is 92 bytes; lfFaceName is WCHAR[32] at offset 28.
 * CHOOSEFONTW (x64) is 104 bytes; lpLogFont at offset 16, iPointSize at 24,
 * Flags at 28, rgbColors at 32.
 */
public final class ChooseFont {

    private static final long LOGFONT_SIZE = 92;
    private static final long LF_HEIGHT = 0;       // LONG
    private static final long LF_WEIGHT = 16;      // LONG
    private static final long LF_ITALIC = 20;      // BYTE
    private static final long LF_FACENAME = 28;    // WCHAR[32]

    private static final long CF_SIZE = 104;
    private static final long CF_OFF_lpLogFont = 16;
    private static final long CF_OFF_iPointSize = 24;
    private static final long CF_OFF_Flags = 28;

    private static final int CF_SCREENFONTS = 0x00000001;
    private static final int CF_INITTOLOGFONTSTRUCT = 0x00000040;
    private static final int FW_NORMAL = 400, FW_BOLD = 700;

    private static final MethodHandle CHOOSE_FONT = Linker.nativeLinker().downcallHandle(
        Win32.COMDLG32.find("ChooseFontW").orElseThrow(),
        FunctionDescriptor.of(JAVA_INT, ADDRESS));

    private ChooseFont() {}

    /** @return the chosen Font, or null if cancelled. */
    public static Font show(Font initial) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment lf = a.allocate(LOGFONT_SIZE);
            lf.fill((byte) 0);
            int pt = (initial != null) ? initial.getSize() : 12;
            // LOGFONT height in logical units: negative = character height; -MulDiv(pt,dpi,72).
            // ChooseFont fills iPointSize on return; for init we set lfHeight from pt at 96 dpi.
            lf.set(JAVA_INT, LF_HEIGHT, -(int) Math.round(pt * 96.0 / 72.0));
            lf.set(JAVA_INT, LF_WEIGHT, (initial != null && initial.isBold()) ? FW_BOLD : FW_NORMAL);
            lf.set(JAVA_BYTE, LF_ITALIC, (byte) ((initial != null && initial.isItalic()) ? 1 : 0));
            String face = (initial != null) ? initial.getFamily() : "Segoe UI";
            putFaceName(lf, face);

            MemorySegment cf = a.allocate(CF_SIZE);
            cf.fill((byte) 0);
            cf.set(JAVA_INT, 0, (int) CF_SIZE);
            cf.set(ADDRESS, CF_OFF_lpLogFont, lf);
            cf.set(JAVA_INT, CF_OFF_Flags, CF_SCREENFONTS | CF_INITTOLOGFONTSTRUCT);

            int ok = (int) CHOOSE_FONT.invokeExact(cf);
            if (ok == 0) return null;

            int pointSizeTenths = cf.get(JAVA_INT, CF_OFF_iPointSize); // in 1/10 pt
            int size = Math.max(1, Math.round(pointSizeTenths / 10f));
            int weight = lf.get(JAVA_INT, LF_WEIGHT);
            boolean bold = weight >= FW_BOLD;
            boolean italic = lf.get(JAVA_BYTE, LF_ITALIC) != 0;
            String chosenFace = readFaceName(lf);
            int style = (bold ? Font.BOLD : 0) | (italic ? Font.ITALIC : 0);
            return new Font(chosenFace, style, size);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static void putFaceName(MemorySegment lf, String face) {
        char[] chars = face.toCharArray();
        int n = Math.min(chars.length, 31);
        for (int i = 0; i < n; i++) lf.set(JAVA_CHAR, LF_FACENAME + i * 2L, chars[i]);
        lf.set(JAVA_CHAR, LF_FACENAME + n * 2L, '\0');
    }

    private static String readFaceName(MemorySegment lf) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            char c = lf.get(JAVA_CHAR, LF_FACENAME + i * 2L);
            if (c == 0) break;
            sb.append(c);
        }
        return sb.toString();
    }
}
```

> **Spike note:** confirm `iPointSize` is populated (requires `CF_SCREENFONTS`). If `iPointSize` reads 0 on your Windows build, derive the size from `lfHeight` instead: `size = round(abs(lfHeight) * 72 / 96)`.

- [ ] **Step 2: Wire `showFont`**

Replace `showFont` in `WindowsDialogProvider.java`:

```java
    @Override
    public void showFont(FontDialogProperties props) {
        Runnable work = () -> {
            java.awt.Font initial = props.createFont();
            java.awt.Font chosen = ca.phon.ui.nativedialogs.ffm.win.ChooseFont.show(initial);
            if (chosen == null) {
                props.getListener().nativeDialogEvent(new NativeDialogEvent(NativeDialogEvent.CANCEL_OPTION, null));
            } else {
                props.getListener().nativeDialogEvent(new NativeDialogEvent(NativeDialogEvent.OK_OPTION, chosen));
            }
        };
        runOnComThread(props.isRunAsync(), work);
    }
```

> `FontDialogProperties.createFont()` is used by the existing Swing path (see `NativeDialogs.ShowFontSelectionTask`), so it exists and returns the starting `Font`.

- [ ] **Step 3: Compile, then manual-verify on Windows**

Run: `./gradlew compileJava`; rebuild; run the demo's font action.
Expected: native Windows font picker initialised to the starting font; the chosen family/size/style returns to the demo; Cancel returns nothing.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/ca/phon/ui/nativedialogs/ffm/win/ChooseFont.java src/main/java/ca/phon/ui/nativedialogs/spi/WindowsDialogProvider.java
git commit -m "Implement native Windows font dialog (ChooseFontW)

Refs #1"
```

> **PHASE 3 CHECKPOINT — pause for review.** Windows now shows native open/save/message/font/colour. Verify on Windows before cleanup and CI.

---

## Phase 4 — Cleanup

*Pause for review after this phase.*

### Task 4.1: Delete the native C++ tree and bundled binaries

**Files:**
- Delete: `src/main/cpp/` (entire tree)
- Delete: `src/main/resources/META-INF/lib/`
- Delete: `target/`, `pom.xml.versionsBackup`

- [ ] **Step 1: Remove the directories**

```bash
git rm -r src/main/cpp
git rm -r src/main/resources/META-INF/lib
rm -rf target
git rm --cached pom.xml.versionsBackup 2>/dev/null || true
rm -f pom.xml.versionsBackup
```

- [ ] **Step 2: Confirm `target/` is ignored**

Run: `grep -q '^/\?target' .gitignore || echo 'target/' >> .gitignore`
Then: `git status -s`
Expected: no `target/` or `pom.xml.versionsBackup` in the status; only deletions staged.

- [ ] **Step 3: Build & test to confirm nothing depended on the deleted files**

Run: `./gradlew clean build -Pversion=24`
Expected: `BUILD SUCCESSFUL`; the JAR builds with no native resources.

- [ ] **Step 4: Verify the JAR contains no native libraries**

Run: `unzip -l build/libs/native-dialogs-24.jar | grep -i 'META-INF/lib' || echo 'no native libs (correct)'`
Expected: `no native libs (correct)`.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Remove C++/JNI sources and bundled native binaries

The dialog layer is now pure-Java FFM; no native binaries ship.

Refs #1"
```

### Task 4.2: Update docs and module metadata

**Files:**
- Modify: `README.md`
- Modify: `CLAUDE.md`
- Modify: `src/main/java/module-info.java` (only if a new exported package is needed)

- [ ] **Step 1: Rewrite `README.md`**

```markdown
# native dialogs

Native file, message, font, and colour dialogs for Java, with a Swing fallback.

Implemented in pure Java using the Foreign Function & Memory API (Java 25+):

- **macOS** — Cocoa (`NSOpenPanel`, `NSSavePanel`, `NSAlert`); font/colour use Swing.
- **Windows** — Common Item Dialog (`IFileOpenDialog`/`IFileSaveDialog`),
  `TaskDialogIndirect`, `ChooseFontW`, `ChooseColorW`.
- **Linux/other** — Swing.

No native binaries are bundled; the library is a single platform-independent JAR.

## Requirements

- Java 25 or newer.
- FFM needs native access. On the classpath the JAR manifest grants it; on the
  module path, run with `--enable-native-access=ca.phon.nativedialogs`.

## Usage

```java
OpenDialogProperties props = new OpenDialogProperties();
props.setCanChooseFiles(true);
props.setRunAsync(false);
java.util.List<String> selected = NativeDialogs.showOpenDialog(props);
```

## Demo

```bash
./gradlew build
java -jar build/libs/native-dialogs-24.jar
```

Force the Swing fallback for testing:

```bash
java -Dca.phon.ui.nativedialogs.NativeDialogs.forceSwing=true -jar build/libs/native-dialogs-24.jar
```
```

- [ ] **Step 2: Update `CLAUDE.md`**

Replace the "Native Library Build (C++)" section and the "Native Implementation" / "Library Loading" bullets with an FFM description:

- Remove the entire `### Native Library Build (C++)` block and the `generateJniHeaders` command.
- Under **Architecture → Native Implementation**, replace with: "Dialogs are implemented in pure Java via FFM in `ca.phon.ui.nativedialogs.ffm.{mac,win}` and dispatched through `ca.phon.ui.nativedialogs.spi.NativeDialogProvider`. macOS uses Cocoa app-modally on the main thread (GCD `dispatch_*_f`); Windows uses the COM Common Item Dialog plus comdlg32/comctl32. No native binaries are built or shipped."
- Update the demo/run commands to use `native-dialogs-24.jar`.

- [ ] **Step 3: Check module-info**

Run: `grep -n 'exports' src/main/java/module-info.java`
The new `spi` and `ffm.*` packages are internal — do **not** export them. No change needed unless a test in a different module requires access (tests run on the classpath, so no change). Leave `module-info.java` as-is.

- [ ] **Step 4: Build & commit**

```bash
./gradlew build -Pversion=24
git add README.md CLAUDE.md
git commit -m "Update docs for the FFM rewrite

Refs #1"
```

> **PHASE 4 CHECKPOINT — pause for review.**

---

## Phase 5 — Release CI

### Task 5.1: GitHub Actions workflow

**Files:**
- Create: `.github/workflows/release.yml`

- [ ] **Step 1: Write the workflow**

```yaml
name: Release

on:
  push:
    tags:
      - 'v*'

permissions:
  contents: write
  packages: write

jobs:
  verify:
    strategy:
      fail-fast: false
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '25'
      - name: Run smoke tests
        run: ./gradlew test
        shell: bash

  release:
    needs: verify
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '25'
      - name: Derive version from tag
        id: ver
        run: echo "version=${GITHUB_REF_NAME#v}" >> "$GITHUB_OUTPUT"
      - name: Build JAR
        run: ./gradlew build -Pversion=${{ steps.ver.outputs.version }}
      - name: Publish to GitHub Packages
        run: ./gradlew publish -Pversion=${{ steps.ver.outputs.version }}
        env:
          GITHUB_ACTOR: ${{ github.actor }}
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      - name: Create GitHub Release and attach JARs
        uses: softprops/action-gh-release@v2
        with:
          files: |
            build/libs/native-dialogs-${{ steps.ver.outputs.version }}.jar
            build/libs/native-dialogs-${{ steps.ver.outputs.version }}-sources.jar
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

- [ ] **Step 2: Validate the YAML locally**

Run: `python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/release.yml')); print('valid')"`
Expected: `valid`.

- [ ] **Step 3: Confirm the smoke tests are headless-safe**

Run: `./gradlew test`
Expected: PASS. The tests resolve symbols and convert strings; none open a dialog or require a display, so they pass on CI runners.

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/release.yml
git commit -m "Add release CI: verify on three OSes, publish on v* tag

On push of a v<n> tag, runs headless smoke tests on Linux/macOS/Windows,
then builds the JAR, publishes to GitHub Packages, and attaches the JAR
to a GitHub Release.

Refs #1"
```

### Task 5.2: Push branch and open the PR

- [ ] **Step 1: Push the branch**

```bash
git push -u origin ffm-native-dialogs
```

- [ ] **Step 2: Open the PR against `main`**

```bash
gh pr create --base main --head ffm-native-dialogs \
  --title "Rewrite native dialogs with Java FFM and add release CI" \
  --body "Closes #1

## Prior behaviour
Dialogs used a JNI/C++ native layer. Only a macOS .jnilib shipped; Windows
always fell back to Swing because no DLL was built. Font and colour were Swing
on every platform. There was no CI.

## New behaviour
Pure-Java FFM implementation on Java 25, shipped as one platform-independent
JAR (no native binaries):
- macOS: native open/save/message (Cocoa, app-modal); font/colour via Swing.
- Windows: native open/save (Common Item Dialog), message (TaskDialogIndirect),
  font (ChooseFontW), colour (ChooseColorW).
- Linux/other: Swing.
The public API is unchanged, including deprecated methods.

## Release CI
On push of a \`v<n>\` tag, GitHub Actions runs headless smoke tests on Linux,
macOS, and Windows, then builds the JAR, publishes to GitHub Packages, and
attaches the JAR to a GitHub Release.

## Testing
- Automated: \`./gradlew test\` (OS detection, provider selection, UTF-16/NSString
  round-trips, COM/objc symbol resolution).
- Manual: run \`java -jar build/libs/native-dialogs-24.jar\` on macOS and Windows
  and exercise each dialog (documented per task in the implementation plan).

## To cut the first release
\`git tag v24 && git push origin v24\`"
```

- [ ] **Step 3: Report the PR URL to the user.**

> **FINAL CHECKPOINT.** After the PR is open, the first release is cut by pushing a `v24` tag, which triggers the workflow. Do not push the tag until the PR is reviewed and merged (or until the user asks).

---

## Self-review notes (for the plan author / reviewer)

**Spec coverage:**
- Full FFM, one JAR, no native binaries → Phases 1–4 (provider SPI, mac/win FFM, cleanup), Task 4.1 verifies no libs in JAR. ✔
- Java 25 → Task 0.1. ✔
- App-modal everywhere → `runModal` (mac), modal COM/comdlg32 (win). ✔
- Native font/colour on Windows, Swing on macOS → Tasks 3.5/3.6 (win), `MacDialogProvider.supportsFont/Color()==false` (Task 2.4). ✔
- Linux Swing fallback → `NativeDialogProviders.select()` returns null on Linux (Task 1.2). ✔
- Public API unchanged incl. deprecated → facade edits only swap the dispatch body (Task 1.3); no signatures change. ✔
- Releases + GitHub Packages on v* tag → Task 5.1. ✔
- Tag format v<n>, version 24 → Tasks 0.1, 5.1, 5.2. ✔
- Delete cpp tree → Task 4.1. ✔
- Native-access manifest → Task 0.1. ✔
- Headless smoke tests (can't test interactive dialogs) → Tasks 2.1/2.2, 3.1, plus manual-verification steps. ✔

**Known ABI-sensitive risks flagged inline for the spike:** objc_msgSend fixed descriptors (2.1), GCD context/Runnable mapping and BOOL return (2.3), COM vtable indices and `methodPtr` (3.2/3.3), `TASKDIALOGCONFIG`/`CHOOSEFONTW`/`CHOOSECOLORW` struct offsets (3.4/3.5/3.6). Each task with native structs includes a spike note and a fallback.

**Property getter names — VERIFIED against the sources** (no longer open questions): `getTitle`/`getPrompt`/`getNameFieldLabel`/`getInitialFile`/`getInitialFolder`/`getFileFilter`/`isShowHidden`/`isCanCreateDirectories`/`getMessage` on `SaveDialogProperties` (inherited by `OpenDialogProperties`); `isCanChooseFiles`/`isCanChooseDirectories`/`isAllowMultipleSelection` on `OpenDialogProperties`; `FileFilter.getAllExtensions()`(`List<String>`)/`getDescription()`/`getDefaultExtension()`; `MessageDialogProperties.getHeader()/getMessage()/getOptions()`(`String[]`)`/isShowSuppressionBox()/getSuppressionMessage()`; `ColorDialogProperties.getColor()`(`java.awt.Color`); `FontDialogProperties.createFont()`(`java.awt.Font`); `NativeDialogEvent.OK_OPTION==1`, `CANCEL_OPTION==2`. One bug corrected during review: Cocoa `objectAtIndex:` takes a `long`, so Task 2.4 uses a dedicated `sendIdLong` variant, not `sendObjLong`.

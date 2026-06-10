# FFM Native Dialogs Rewrite — Design

**Date:** 2026-06-10
**Status:** Approved (ready for planning)

## Goal

Replace the entire JNI/C++ native layer with a pure-Java Foreign Function & Memory
(FFM) implementation on Java 25, so the library ships as one platform-independent
JAR with no bundled `.dll`/`.jnilib`. Add GitHub Actions CI that builds,
smoke-tests, and publishes the JAR on a version-tag push.

The public API stays byte-for-byte compatible — only the internals change.

## Decisions (locked)

- **Approach:** Full FFM rewrite; drop all C++/JNI. One pure-Java JAR.
- **Java version:** 25 (LTS). FFM is non-preview in 22+.
- **Modality:** App-modal everywhere (`runModal` on macOS). No window-attached
  sheets, no JAWT native-window lookup, no Objective-C block synthesis.
- **Font/colour:** Native on Windows (`ChooseFontW`/`ChooseColorW`); Swing on
  macOS (matches today's behaviour — `NSFontPanel`/`NSColorPanel` have no clean
  modal API).
- **Linux/other:** Swing fallback for all dialogs (unchanged).
- **Public API:** Unchanged, including all `@deprecated` methods, kept for
  compatibility with consumers (e.g. Phon).
- **Publishing:** GitHub Releases (attach JAR) **and** GitHub Packages (Maven),
  on push of a version tag.
- **Tag format:** `v<n>` (e.g. `v24`); workflow strips `v` and builds with
  `-Pversion=<n>`.
- **Version:** Bump to `24` for the first FFM release.
- **Cleanup:** Delete the entire `src/main/cpp/` tree (preserved in git history).

## Background (current state)

- Native code targets only **open, save, message** on macOS; **open, save** on
  Windows. Font and colour are declared `native` but never implemented, so they
  throw `UnsatisfiedLinkError` and already fall back to Swing on every platform.
- The Windows code already uses the modern Common Item Dialog
  (`IFileOpenDialog`/`IFileSaveDialog`) — the newest API available — but is never
  built or shipped (only the macOS `.jnilib` is bundled), so Windows always falls
  back to Swing.
- A path-naming bug compounds this: the MinGW makefile installs to `win32-x86`
  while `NativeUtilities` looks in `win32-x64`.
- No CI, no `.github`, no git tags. Native libs are built by hand via `make`;
  macOS is built universal (arm64+x64) via `lipo`.
- Build is Gradle (Kotlin DSL), version `23`, Java 21 toolchain.

The FFM rewrite makes the path bug and the unbuilt-binary problem moot by
removing native binaries entirely.

## Native coverage after rewrite

| Dialog  | macOS (Cocoa via FFM) | Windows (Win32 via FFM)     | Linux/other |
|---------|-----------------------|-----------------------------|-------------|
| Open    | `NSOpenPanel`         | `IFileOpenDialog` (COM)     | Swing       |
| Save    | `NSSavePanel`         | `IFileSaveDialog` (COM)     | Swing       |
| Message | `NSAlert`             | `TaskDialogIndirect`        | Swing       |
| Font    | **Swing**             | `ChooseFontW` (comdlg32)    | Swing       |
| Colour  | **Swing**             | `ChooseColorW` (comdlg32)   | Swing       |

## Architecture

The facade keeps its sync/async contract and `forceSwing` flag, but dispatches to
a provider chosen once at init.

```
ca.phon.ui.nativedialogs            (PUBLIC — unchanged API surface)
  NativeDialogs (facade)            native methods → provider.show*(props)
  *Properties, *Event, *Listener
  NativeUtilities                   OS detection kept; loadLibrary() deprecated/unused

ca.phon.ui.nativedialogs.spi        (NEW — internal)
  NativeDialogProvider              interface: showOpen/Save/Message/Font/Colour
                                    + per-dialog supports(...) query
  MacDialogProvider                 open/save/message; font+colour → unsupported
  WindowsDialogProvider             all five
  (no provider)                     Linux/unsupported → facade uses Swing tasks

ca.phon.ui.nativedialogs.ffm.mac    ObjC      (getClass/sel/msgSend handles)
                                    Foundation (NSString/NSArray/NSURL helpers)
                                    Gcd       (main-queue dispatch_{sync,async}_f)
ca.phon.ui.nativedialogs.ffm.win    Ole32, Com (vtable invocation), ShellItem
                                    Comdlg32  (ChooseFontW/ChooseColorW)
                                    Comctl32  (TaskDialogIndirect)
                                    WinStr    (UTF-16 helpers)
```

Each `*Provider` reports per-dialog support; the facade falls back to its existing
Swing `ShowXTask` whenever a provider is absent, reports the dialog unsupported, or
throws.

### Key risk-reducers

- **macOS main-thread requirement** is met with `dispatch_sync_f`/
  `dispatch_async_f` (the GCD *function-pointer* variants) onto the main queue,
  backed by an FFM upcall stub — no Objective-C block ABI work. `runModal` runs on
  the main thread; `sync_f` for blocking mode, `async_f` for async mode.
- **Windows COM** is driven by reading each interface's vtable pointer and invoking
  methods by fixed index via FFM downcall handles, under an STA
  (`CoInitializeEx(COINIT_APARTMENTTHREADED)`).

These two are the highest-risk areas and should be de-risked with small spikes
early in implementation.

## Data flow (threading model)

The facade's existing sync/async contract is preserved exactly:

- **Sync** (`isRunAsync == false`): facade installs `MessageWaitListener`, calls
  the provider, then `waitLoop()` blocks the caller until the result arrives.
- **Async** (`isRunAsync == true`): facade calls the provider and returns
  immediately; the user's listener fires later.

Provider behaviour:

- **macOS** — modal block dispatched to the main queue: `dispatch_sync_f` (sync) /
  `dispatch_async_f` (async). The upcall stub reads the panel result, builds a
  `NativeDialogEvent`, and invokes `listener.nativeDialogEvent(...)` in pure Java.
  AppKit's main thread is assumed to be up (brought up by AWT, since consumers are
  Swing apps).
- **Windows** — sync runs the modal call on the calling thread under STA; async
  runs it on a dedicated STA thread. Same event-delivery path.
- **Parent window** — app-modal; ignored on macOS, owner `HWND` left `NULL` on
  Windows. This is the one user-visible behaviour change (no sheets).

## Error handling

- Provider selection and FFM symbol resolution happen once at class init, wrapped
  so any failure (wrong OS, missing symbol, restricted native access) degrades the
  provider to "absent" → Swing.
- Per call, the facade wraps `provider.show*` in `try/catch (Throwable)`; on any
  failure it logs and runs the Swing task — broadening today's
  `catch (UnsatisfiedLinkError)`. A dialog can never hard-fail; worst case is a
  Swing dialog.
- **Native access:** add `Enable-Native-Access: ALL-UNNAMED` to the JAR manifest
  for classpath use; document `--enable-native-access=ca.phon.nativedialogs` for
  module-path consumers. Java 25 only warns without it, but we set it to silence
  the warning.

## Testing

CI cannot drive interactive dialogs, so automated coverage is non-interactive only:

- **Headless smoke tests** on macOS + Windows + Linux runners: OS detection,
  provider selection, FFM symbol resolution (resolve `objc_msgSend` /
  `CoInitializeEx` etc. *without showing a dialog*), and string-conversion
  round-trips (Java↔NSString UTF-8, Java↔UTF-16). Confirms bindings link on each
  real OS.
- **Manual verification:** the existing `NativeDialogsDemo` remains the way a human
  confirms real dialog behaviour on each platform before tagging a release.
- No Swing-rendering tests (needs a display).

## CI / release (`.github/workflows/release.yml`)

- **Trigger:** push of a tag matching `v*`. Workflow strips `v` and builds with
  `-Pversion=<n>`; `build.gradle.kts` no longer hardcodes the version (keeps a
  sensible default for non-tag builds).
- **`verify` job** — matrix `{macos-latest, windows-latest, ubuntu-latest}`,
  JDK 25 (Temurin), runs the headless smoke tests.
- **`release` job** (needs `verify`, ubuntu, JDK 25): builds the single pure-Java
  JAR, then:
  - creates a GitHub Release for the tag and attaches the JAR;
  - `./gradlew publish` to GitHub Packages (Maven).
- **Permissions:** `contents: write`, `packages: write`; auth via `GITHUB_TOKEN`.
- Only one build produces the release JAR (platform-independent); the OS matrix is
  purely for verification, keeping CI cheap.

## Cleanup

- Delete `src/main/cpp/` (all C++/Obj-C, makefiles, headers).
- Delete bundled `META-INF/lib/` binaries.
- Remove the `generateJniHeaders` Gradle task.
- Remove leftover Maven cruft (`target/`, `pom.xml.versionsBackup`).
- Update `README.md`, `CLAUDE.md`, and `module-info.java` as needed.

## Out of scope

- Native Linux (GTK/portal) dialogs.
- Native font/colour on macOS.
- Window-attached sheets / parent-window ownership.
- Any change to the public API signatures, including deprecated methods.

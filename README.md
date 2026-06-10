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

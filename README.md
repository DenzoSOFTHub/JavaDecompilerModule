# JavaDecompiler - NetBeans Plugin

A NetBeans plugin that allows you to view decompiled Java source code directly from `.class` files. Useful for debugging, understanding library internals, and navigating code when source is not available.

## Features

- **Multiple Decompilers**: Choose between 4 decompilation engines:
  - **CFR** (default) - Modern decompiler with excellent Java 8-21+ support
  - **JD-Core** - Fast and reliable decompiler
  - **Procyon** - Excellent generics and complex code support
  - **Fernflower (Vineflower)** - IntelliJ IDEA style decompilation

- **Line Number Preservation**: Decompiled code is aligned to original line numbers, so stack traces match the displayed source

- **Full Editor Features**: Syntax highlighting, line numbers, and code folding

- **Read-only Display**: Decompiled source is shown with a gray background to indicate it cannot be edited

- **Preferences Panel**: Easy switching between decompilers via Tools > Options

## Installation

1. Download `JavaDecompiler.nbm` from the releases
2. In NetBeans, go to **Tools > Plugins**
3. Click on the **Downloaded** tab
4. Click **Add Plugins...** and select the `JavaDecompiler.nbm` file
5. Click **Install** and follow the wizard
6. Restart NetBeans when prompted

## Usage

### Viewing Decompiled Classes

Simply double-click on any `.class` file in your project:
- Classes in your project's `target/classes` folder
- Classes inside JAR files in the **Dependencies** node
- Any `.class` file opened from the file system

The decompiled Java source will be displayed in the editor with syntax highlighting.

### Changing Decompiler

1. Go to **Tools > Options**
2. Select **Java** category
3. Click on **Decompiler** tab
4. Choose your preferred decompiler from the dropdown
5. Click **OK**

The new decompiler will be used for all subsequent class file openings.

### Decompiler Comparison

| Decompiler | Best For |
|------------|----------|
| **CFR** | Modern Java features (lambdas, records, sealed classes) |
| **JD-Core** | Fast decompilation, good overall quality |
| **Procyon** | Complex generics, good variable naming |
| **Fernflower** | IntelliJ-style output, analytical approach |

## Requirements

- **NetBeans IDE 11.0** (RELEASE110) or later
- Java 8 or later

The plugin is built against NetBeans Platform API version RELEASE110 and is compatible with NetBeans 11.0 and all subsequent versions (12.x, 13.x, 14.x, 15.x, etc.).

## Building from Source

```bash
# Clone the repository
git clone <repository-url>
cd JavaDecompilerModule

# Build with Maven
mvn clean package

# The plugin will be in target/nbm/JavaDecompiler.nbm
```

## License

MIT License - See [LICENSE](LICENSE) file for details.

## Credits

This plugin integrates the following open-source decompilers:
- [CFR](https://github.com/leibnitz27/cfr) by Lee Benfield
- [JD-Core](https://github.com/java-decompiler/jd-core) by Emmanuel Dupuy
- [Procyon](https://github.com/mstrobel/procyon) by Mike Strobel
- [Vineflower](https://github.com/Vineflower/vineflower) (Fernflower fork)

---

Developed by Denzosoft

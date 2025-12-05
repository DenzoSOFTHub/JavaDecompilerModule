package it.denzosoft.javadecompilermodule.decompiler;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

/**
 * Vineflower decompiler engine (improved Fernflower fork).
 * Used by IntelliJ IDEA, excellent analytical decompiler.
 */
public class VineflowerEngine implements DecompilerEngine {

    /**
     * Adds empty lines to align decompiled code with original line numbers.
     * The mapping array format: [decompiledLine1, originalLine1, decompiledLine2, originalLine2, ...]
     */
    private static String addLineNumbers(String content, int[] mapping) {
        if (mapping == null || mapping.length < 2) {
            return content;
        }

        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();

        // Build a map of decompiled line -> original line
        java.util.Map<Integer, Integer> lineMap = new java.util.HashMap<>();
        for (int i = 0; i < mapping.length - 1; i += 2) {
            int decompiledLine = mapping[i];
            int originalLine = mapping[i + 1];
            if (decompiledLine > 0 && originalLine > 0) {
                lineMap.put(decompiledLine, originalLine);
            }
        }

        int currentOutputLine = 1;
        for (int i = 0; i < lines.length; i++) {
            int decompiledLineNum = i + 1;
            Integer originalLine = lineMap.get(decompiledLineNum);

            // If we have original line info and need to add padding
            if (originalLine != null && originalLine > currentOutputLine) {
                // Add empty lines to reach the original line number
                while (currentOutputLine < originalLine) {
                    result.append("\n");
                    currentOutputLine++;
                }
            }

            result.append(lines[i]);
            if (i < lines.length - 1) {
                result.append("\n");
            }
            currentOutputLine++;
        }

        return result.toString();
    }

    @Override
    public String getId() {
        return "vineflower";
    }

    @Override
    public String getDisplayName() {
        return "Fernflower (Vineflower)";
    }

    @Override
    public String getDescription() {
        return "Fernflower/Vineflower - IntelliJ IDEA style decompilation";
    }

    @Override
    public String decompile(String className, byte[] bytecode, ClassProvider classProvider, boolean preserveLineNumbers) throws Exception {
        final StringBuilder result = new StringBuilder();

        // Create a unique temp file path for this class
        final String classFilePath = className.replace('/', File.separatorChar) + ".class";
        final File tempFile = new File(System.getProperty("java.io.tmpdir"), classFilePath);

        // Bytecode provider - returns bytecode for the class file path
        IBytecodeProvider bytecodeProvider = new IBytecodeProvider() {
            @Override
            public byte[] getBytecode(String externalPath, String internalPath) throws IOException {
                // Normalize paths for comparison
                String normalizedExternal = externalPath.replace(File.separatorChar, '/');
                String normalizedClassName = className + ".class";

                // Check if this is our main class
                if (normalizedExternal.endsWith(normalizedClassName) ||
                    normalizedExternal.endsWith(className.substring(className.lastIndexOf('/') + 1) + ".class")) {
                    return bytecode;
                }

                // Try to extract class name from path and load from provider
                String requestedClass = normalizedExternal;
                if (requestedClass.endsWith(".class")) {
                    requestedClass = requestedClass.substring(0, requestedClass.length() - 6);
                }
                // Get just the class name part
                int lastSlash = requestedClass.lastIndexOf('/');
                if (lastSlash >= 0) {
                    requestedClass = requestedClass.substring(lastSlash + 1);
                }

                if (classProvider.canLoad(requestedClass)) {
                    try {
                        return classProvider.load(requestedClass);
                    } catch (Exception e) {
                        throw new IOException(e);
                    }
                }
                return null;
            }
        };

        // Result saver to capture output
        IResultSaver resultSaver = new IResultSaver() {
            @Override
            public void saveFolder(String path) {
            }

            @Override
            public void copyFile(String source, String path, String entryName) {
            }

            @Override
            public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
                if (content != null && !content.isEmpty()) {
                    if (preserveLineNumbers && mapping != null && mapping.length > 0) {
                        // Add line number aligned content
                        result.append(addLineNumbers(content, mapping));
                    } else {
                        result.append(content);
                    }
                }
            }

            @Override
            public void createArchive(String path, String archiveName, Manifest manifest) {
            }

            @Override
            public void saveDirEntry(String path, String archiveName, String entryName) {
            }

            @Override
            public void copyEntry(String source, String path, String archiveName, String entry) {
            }

            @Override
            public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
                if (content != null && !content.isEmpty()) {
                    result.append(content);
                }
            }

            @Override
            public void closeArchive(String path, String archiveName) {
            }
        };

        // Logger - capture errors for debugging
        final StringBuilder logMessages = new StringBuilder();
        IFernflowerLogger logger = new IFernflowerLogger() {
            @Override
            public void writeMessage(String message, Severity severity) {
                if (severity == Severity.ERROR || severity == Severity.WARN) {
                    logMessages.append("// ").append(severity).append(": ").append(message).append("\n");
                }
            }

            @Override
            public void writeMessage(String message, Severity severity, Throwable t) {
                if (severity == Severity.ERROR || severity == Severity.WARN) {
                    logMessages.append("// ").append(severity).append(": ").append(message);
                    if (t != null) {
                        logMessages.append(" - ").append(t.getMessage());
                    }
                    logMessages.append("\n");
                }
            }
        };

        // Decompiler options
        Map<String, Object> options = new HashMap<>();
        options.put("dgs", "1"); // decompile generic signatures
        options.put("asc", "1"); // encode non-ASCII as unicode escapes
        options.put("ind", "    "); // indentation
        options.put("udv", "1"); // use debug variable names
        options.put("rsy", "1"); // remove synthetic members
        options.put("rbr", "1"); // remove bridge methods
        if (preserveLineNumbers) {
            options.put("bsm", "1"); // bytecode source mapping (provides mapping array)
        }

        Fernflower fernflower = new Fernflower(bytecodeProvider, resultSaver, options, logger);

        try {
            fernflower.addSource(tempFile);
            fernflower.decompileContext();
        } finally {
            fernflower.clearContext();
        }

        // If no output was generated, include log messages
        if (result.length() == 0) {
            result.append("// Decompilation produced no output\n");
            if (logMessages.length() > 0) {
                result.append(logMessages);
            }
        }

        // Add decompiler info at the end
        result.append("\n\n// Decompiled with Fernflower (Vineflower)\n");

        return result.toString();
    }
}

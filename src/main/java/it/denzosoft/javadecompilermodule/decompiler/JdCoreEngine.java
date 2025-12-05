package it.denzosoft.javadecompilermodule.decompiler;

import org.jd.core.v1.ClassFileToJavaSourceDecompiler;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.loader.LoaderException;
import org.jd.core.v1.api.printer.Printer;

/**
 * JD-Core decompiler engine.
 * Classic decompiler with good general support.
 */
public class JdCoreEngine implements DecompilerEngine {

    private static final ClassFileToJavaSourceDecompiler DECOMPILER = new ClassFileToJavaSourceDecompiler();

    @Override
    public String getId() {
        return "jd-core";
    }

    @Override
    public String getDisplayName() {
        return "JD-Core";
    }

    @Override
    public String getDescription() {
        return "JD-Core - Classic Java decompiler";
    }

    @Override
    public String decompile(String className, byte[] bytecode, ClassProvider classProvider, boolean preserveLineNumbers) throws Exception {
        final StringBuilder sourceBuilder = new StringBuilder();

        Loader loader = new Loader() {
            @Override
            public boolean canLoad(String name) {
                if (name.equals(className)) {
                    return true;
                }
                return classProvider.canLoad(name);
            }

            @Override
            public byte[] load(String name) throws LoaderException {
                try {
                    if (name.equals(className)) {
                        return bytecode;
                    }
                    return classProvider.load(name);
                } catch (Exception e) {
                    throw new LoaderException(e);
                }
            }
        };

        Printer printer = new Printer() {
            private int indentLevel = 0;
            private int currentLineNumber = 0;
            private boolean realignmentEnabled = false;
            private static final String INDENT = "    ";

            @Override
            public void start(int maxLineNumber, int majorVersion, int minorVersion) {
                // No header at start - will be added at end
                currentLineNumber = 1;
                realignmentEnabled = preserveLineNumbers && (maxLineNumber > 0);
            }

            @Override
            public void end() {
                // Add decompiler info at the end
                sourceBuilder.append("\n\n// Decompiled with JD-Core\n");
            }

            @Override
            public void printText(String text) {
                sourceBuilder.append(text);
            }

            @Override
            public void printNumericConstant(String constant) {
                sourceBuilder.append(constant);
            }

            @Override
            public void printStringConstant(String constant, String ownerInternalName) {
                sourceBuilder.append(constant);
            }

            @Override
            public void printKeyword(String keyword) {
                sourceBuilder.append(keyword);
            }

            @Override
            public void printDeclaration(int type, String internalTypeName, String name, String descriptor) {
                sourceBuilder.append(name);
            }

            @Override
            public void printReference(int type, String internalTypeName, String name, String descriptor, String ownerInternalName) {
                sourceBuilder.append(name);
            }

            @Override
            public void indent() {
                indentLevel++;
            }

            @Override
            public void unindent() {
                indentLevel--;
            }

            @Override
            public void startLine(int lineNumber) {
                if (realignmentEnabled && lineNumber > 0 && lineNumber > currentLineNumber) {
                    while (currentLineNumber < lineNumber) {
                        sourceBuilder.append("\n");
                        currentLineNumber++;
                    }
                }
                for (int i = 0; i < indentLevel; i++) {
                    sourceBuilder.append(INDENT);
                }
            }

            @Override
            public void endLine() {
                sourceBuilder.append("\n");
                currentLineNumber++;
            }

            @Override
            public void extraLine(int count) {
                for (int i = 0; i < count; i++) {
                    sourceBuilder.append("\n");
                    currentLineNumber++;
                }
            }

            @Override
            public void startMarker(int type) {
            }

            @Override
            public void endMarker(int type) {
            }
        };

        DECOMPILER.decompile(loader, printer, className);
        return sourceBuilder.toString();
    }
}

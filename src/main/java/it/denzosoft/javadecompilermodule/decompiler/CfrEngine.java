package it.denzosoft.javadecompilermodule.decompiler;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;

/**
 * CFR decompiler engine.
 * Excellent support for modern Java features (lambdas, records, sealed classes).
 */
public class CfrEngine implements DecompilerEngine {

    @Override
    public String getId() {
        return "cfr";
    }

    @Override
    public String getDisplayName() {
        return "CFR";
    }

    @Override
    public String getDescription() {
        return "CFR by Lee Benfield - Excellent Java 8-21+ support";
    }

    @Override
    public String decompile(String className, byte[] bytecode, ClassProvider classProvider, boolean preserveLineNumbers) throws Exception {
        final StringBuilder result = new StringBuilder();
        final Map<String, byte[]> classCache = new HashMap<>();
        final Map<Integer, Integer> lineMapping = new TreeMap<>(); // decompiled line -> original line
        classCache.put(className + ".class", bytecode);

        // Custom class file source
        ClassFileSource source = new ClassFileSource() {
            @Override
            public void informAnalysisRelativePathDetail(String usePath, String classFilePath) {
            }

            @Override
            public Collection<String> addJar(String jarPath) {
                return Collections.emptyList();
            }

            @Override
            public String getPossiblyRenamedPath(String path) {
                return path;
            }

            @Override
            public Pair<byte[], String> getClassFileContent(String path) {
                try {
                    byte[] content = classCache.get(path);
                    if (content == null) {
                        String internalName = path.replace(".class", "");
                        if (classProvider.canLoad(internalName)) {
                            content = classProvider.load(internalName);
                            classCache.put(path, content);
                        }
                    }
                    if (content != null) {
                        return Pair.make(content, path);
                    }
                } catch (Exception e) {
                    // Return null if class cannot be loaded
                }
                return null;
            }
        };

        // Output sink to capture decompiled source and optionally line mappings
        OutputSinkFactory sinkFactory = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
                if (preserveLineNumbers && sinkType == SinkType.LINENUMBER) {
                    return Collections.singletonList(SinkClass.LINE_NUMBER_MAPPING);
                }
                return Collections.singletonList(SinkClass.STRING);
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                if (preserveLineNumbers && sinkType == SinkType.LINENUMBER && sinkClass == SinkClass.LINE_NUMBER_MAPPING) {
                    return sinkable -> {
                        if (sinkable instanceof SinkReturns.LineNumberMapping) {
                            SinkReturns.LineNumberMapping mapping = (SinkReturns.LineNumberMapping) sinkable;
                            Map<Integer, Integer> classMap = mapping.getClassFileMappings();
                            if (classMap != null) {
                                lineMapping.putAll(classMap);
                            }
                        }
                    };
                }
                return sinkable -> {
                    if (sinkType == SinkType.JAVA && sinkClass == SinkClass.STRING) {
                        result.append(sinkable.toString());
                    }
                };
            }
        };

        // Configure CFR options
        Map<String, String> options = new HashMap<>();
        options.put("showversion", "false");
        options.put("hideutf", "false");
        options.put("innerclasses", "true");
        options.put("comments", "false"); // No comments to keep code compilable

        CfrDriver driver = new CfrDriver.Builder()
                .withClassFileSource(source)
                .withOutputSink(sinkFactory)
                .withOptions(options)
                .build();

        driver.analyse(Collections.singletonList(className + ".class"));

        String decompiledCode = result.toString();

        // Apply line alignment if needed, then add footer with decompiler info
        StringBuilder finalResult = new StringBuilder();

        if (preserveLineNumbers && !lineMapping.isEmpty()) {
            finalResult.append(alignToLineNumbers(decompiledCode, lineMapping, 1));
        } else {
            finalResult.append(decompiledCode);
        }

        // Add decompiler info at the end
        finalResult.append("\n\n// Decompiled with CFR\n");

        return finalResult.toString();
    }

    /**
     * Aligns decompiled code to original line numbers by inserting blank lines.
     * This ensures stack traces match the decompiled source.
     *
     * @param code the decompiled source code
     * @param lineMapping map from decompiled line number to original line number
     * @param startingLine the line number where the code starts (after header)
     * @return the aligned source code
     */
    private String alignToLineNumbers(String code, Map<Integer, Integer> lineMapping, int startingLine) {
        if (lineMapping.isEmpty()) {
            return code;
        }

        String[] lines = code.split("\n", -1);
        StringBuilder result = new StringBuilder();

        int currentOutputLine = startingLine;

        for (int i = 0; i < lines.length; i++) {
            int decompiledLine = i + 1;
            Integer originalLine = lineMapping.get(decompiledLine);

            // If we have a mapping, pad to reach the original line number
            if (originalLine != null && originalLine > currentOutputLine) {
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
}

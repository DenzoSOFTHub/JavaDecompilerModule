package it.denzosoft.javadecompilermodule;

import it.denzosoft.javadecompilermodule.decompiler.DecompilerEngine;
import it.denzosoft.javadecompilermodule.decompiler.DecompilerRegistry;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 * Wrapper class for decompilation.
 * Uses the selected decompiler engine from settings.
 */
public class Decompiler {

    /**
     * Decompiles a .class file and returns the Java source code.
     *
     * @param classFile the FileObject representing the .class file
     * @return the decompiled Java source code
     * @throws Exception if decompilation fails
     */
    public static String decompile(FileObject classFile) throws Exception {
        System.out.println("[JavaDecompiler] decompile called for: " + classFile);
        File file = FileUtil.toFile(classFile);
        if (file == null) {
            // File might be inside a JAR/ZIP
            System.out.println("[JavaDecompiler] File is inside archive, using decompileFromArchive");
            return decompileFromArchive(classFile);
        }
        System.out.println("[JavaDecompiler] File is on filesystem: " + file);
        return decompileFromFile(file, classFile);
    }

    private static String decompileFromFile(File file, FileObject classFile) throws Exception {
        final byte[] classBytes = readBytes(classFile);
        final String internalName = getInternalClassName(classFile);

        // Create class provider for loading dependencies
        DecompilerEngine.ClassProvider classProvider = new DecompilerEngine.ClassProvider() {
            @Override
            public boolean canLoad(String name) {
                if (name.equals(internalName)) {
                    return true;
                }
                FileObject parent = classFile.getParent();
                if (parent != null) {
                    String simpleName = name.substring(name.lastIndexOf('/') + 1);
                    FileObject related = parent.getFileObject(simpleName, "class");
                    return related != null;
                }
                return false;
            }

            @Override
            public byte[] load(String name) throws Exception {
                if (name.equals(internalName)) {
                    return classBytes;
                }
                FileObject parent = classFile.getParent();
                if (parent != null) {
                    String simpleName = name.substring(name.lastIndexOf('/') + 1);
                    FileObject related = parent.getFileObject(simpleName, "class");
                    if (related != null) {
                        return readBytes(related);
                    }
                }
                throw new IOException("Cannot load: " + name);
            }
        };

        return decompileInternal(internalName, classBytes, classProvider);
    }

    private static String decompileFromArchive(FileObject classFile) throws Exception {
        // Handle classes inside JAR/ZIP files
        FileObject archiveRoot = FileUtil.getArchiveFile(classFile);
        if (archiveRoot == null) {
            throw new IOException("Cannot determine archive for: " + classFile.getPath());
        }

        File archiveFile = FileUtil.toFile(archiveRoot);
        if (archiveFile == null) {
            throw new IOException("Cannot get file for archive: " + archiveRoot.getPath());
        }

        final String internalName = getInternalClassName(classFile);
        final Map<String, byte[]> classCache = new HashMap<>();

        // Load all classes from the JAR for dependency resolution
        try (JarFile jarFile = new JarFile(archiveFile)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    String entryInternalName = entry.getName().replace(".class", "");
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        classCache.put(entryInternalName, readAllBytes(is));
                    }
                }
            }
        }

        byte[] mainClassBytes = classCache.get(internalName);
        if (mainClassBytes == null) {
            throw new IOException("Cannot find class in archive: " + internalName);
        }

        DecompilerEngine.ClassProvider classProvider = new DecompilerEngine.ClassProvider() {
            @Override
            public boolean canLoad(String name) {
                return classCache.containsKey(name);
            }

            @Override
            public byte[] load(String name) throws Exception {
                byte[] bytes = classCache.get(name);
                if (bytes == null) {
                    throw new IOException("Cannot load: " + name);
                }
                return bytes;
            }
        };

        return decompileInternal(internalName, mainClassBytes, classProvider);
    }

    private static String decompileInternal(String internalName, byte[] bytecode,
            DecompilerEngine.ClassProvider classProvider) throws Exception {
        DecompilerEngine engine = DecompilerRegistry.getSelectedEngine();
        boolean preserveLineNumbers = DecompilerRegistry.isPreserveLineNumbers();

        try {
            return engine.decompile(internalName, bytecode, classProvider, preserveLineNumbers);
        } catch (Exception e) {
            // If decompilation fails, return error message
            return "// Decompilation failed with " + engine.getDisplayName() + "\n" +
                   "// Error: " + e.getMessage() + "\n" +
                   "// Try selecting a different decompiler in Tools > Options > Java > Decompiler\n";
        }
    }

    /**
     * Gets the internal class name from a FileObject.
     * Example: com/example/MyClass for MyClass.class in com/example/
     */
    private static String getInternalClassName(FileObject classFile) {
        System.out.println("[JavaDecompiler] getInternalClassName for: " + classFile);
        System.out.println("[JavaDecompiler] classFile.getPath(): " + classFile.getPath());

        // For files inside JAR/ZIP, get path relative to archive root
        FileObject archiveFile = FileUtil.getArchiveFile(classFile);
        System.out.println("[JavaDecompiler] archiveFile: " + archiveFile);

        if (archiveFile != null) {
            FileObject archiveRoot = FileUtil.getArchiveRoot(archiveFile);
            System.out.println("[JavaDecompiler] archiveRoot: " + archiveRoot);

            if (archiveRoot != null) {
                String relativePath = FileUtil.getRelativePath(archiveRoot, classFile);
                System.out.println("[JavaDecompiler] relativePath: " + relativePath);

                if (relativePath != null && relativePath.endsWith(".class")) {
                    String result = relativePath.substring(0, relativePath.length() - 6);
                    System.out.println("[JavaDecompiler] returning (from archive): " + result);
                    return result;
                }
            }
        }

        String path = classFile.getPath();

        // Remove .class extension
        if (path.endsWith(".class")) {
            path = path.substring(0, path.length() - 6);
        }

        // Try to extract package structure from filesystem path
        if (path.contains("/")) {
            String[] parts = path.split("/");
            // Find where the package structure likely starts
            for (int i = 0; i < parts.length; i++) {
                // Common source roots
                if (parts[i].equals("classes") || parts[i].equals("bin") ||
                    parts[i].equals("target") || parts[i].equals("build")) {
                    StringBuilder sb = new StringBuilder();
                    for (int j = i + 1; j < parts.length; j++) {
                        if (sb.length() > 0) sb.append("/");
                        sb.append(parts[j]);
                    }
                    if (sb.length() > 0) {
                        System.out.println("[JavaDecompiler] returning (from path heuristic): " + sb.toString());
                        return sb.toString();
                    }
                }
            }
        }

        // Fallback: just use the name without extension
        System.out.println("[JavaDecompiler] returning (fallback): " + classFile.getName());
        return classFile.getName();
    }

    private static byte[] readBytes(FileObject fo) throws IOException {
        try (InputStream is = fo.getInputStream()) {
            return readAllBytes(is);
        }
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }
}

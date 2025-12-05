package it.denzosoft.javadecompilermodule.decompiler;

/**
 * Interface for decompiler engines.
 * Each implementation wraps a specific decompilation library.
 */
public interface DecompilerEngine {

    /**
     * Returns the unique identifier for this decompiler.
     */
    String getId();

    /**
     * Returns the display name for this decompiler.
     */
    String getDisplayName();

    /**
     * Returns a description of this decompiler.
     */
    String getDescription();

    /**
     * Decompiles bytecode to Java source code.
     *
     * @param className the internal class name (e.g., "com/example/MyClass")
     * @param bytecode the class bytecode
     * @param classProvider provider for loading related classes
     * @param preserveLineNumbers whether to align output to original line numbers
     * @return the decompiled Java source code
     * @throws Exception if decompilation fails
     */
    String decompile(String className, byte[] bytecode, ClassProvider classProvider, boolean preserveLineNumbers) throws Exception;

    /**
     * Provider interface for loading class bytecode.
     */
    interface ClassProvider {
        /**
         * Checks if a class can be loaded.
         */
        boolean canLoad(String internalName);

        /**
         * Loads class bytecode.
         */
        byte[] load(String internalName) throws Exception;
    }
}

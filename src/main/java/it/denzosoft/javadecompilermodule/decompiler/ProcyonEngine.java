package it.denzosoft.javadecompilermodule.decompiler;

import com.strobel.assembler.InputTypeLoader;
import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;
import java.io.StringWriter;

/**
 * Procyon decompiler engine.
 * Good quality output with excellent generics support.
 */
public class ProcyonEngine implements DecompilerEngine {

    @Override
    public String getId() {
        return "procyon";
    }

    @Override
    public String getDisplayName() {
        return "Procyon";
    }

    @Override
    public String getDescription() {
        return "Procyon by Mike Strobel - Excellent generics support";
    }

    @Override
    public String decompile(String className, byte[] bytecode, ClassProvider classProvider, boolean preserveLineNumbers) throws Exception {
        DecompilerSettings settings = DecompilerSettings.javaDefaults();
        settings.setShowSyntheticMembers(false);
        settings.setForceExplicitImports(true);

        // Note: Procyon doesn't support line number alignment like JD-Core
        // setShowDebugLineNumbers adds comments which break compilation
        // So we just use debug variable names when preserveLineNumbers is enabled
        if (preserveLineNumbers) {
            settings.setRetainPointlessSwitches(false);
        }

        JavaFormattingOptions formatting = JavaFormattingOptions.createDefault();
        settings.setJavaFormattingOptions(formatting);

        // Custom type loader
        ITypeLoader typeLoader = new ITypeLoader() {
            private final ITypeLoader defaultLoader = new InputTypeLoader();

            @Override
            public boolean tryLoadType(String internalName, Buffer buffer) {
                if (internalName.equals(className)) {
                    buffer.reset(bytecode.length);
                    buffer.putByteArray(bytecode, 0, bytecode.length);
                    buffer.position(0);
                    return true;
                }
                if (classProvider.canLoad(internalName)) {
                    try {
                        byte[] data = classProvider.load(internalName);
                        buffer.reset(data.length);
                        buffer.putByteArray(data, 0, data.length);
                        buffer.position(0);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
                return defaultLoader.tryLoadType(internalName, buffer);
            }
        };

        MetadataSystem metadataSystem = new MetadataSystem(typeLoader);
        TypeReference typeRef = metadataSystem.lookupType(className);

        if (typeRef == null) {
            throw new Exception("Could not load type: " + className);
        }

        TypeDefinition typeDef = typeRef.resolve();
        if (typeDef == null) {
            throw new Exception("Could not resolve type: " + className);
        }

        StringWriter writer = new StringWriter();
        PlainTextOutput output = new PlainTextOutput(writer);
        DecompilationOptions options = new DecompilationOptions();
        options.setSettings(settings);

        settings.getLanguage().decompileType(typeDef, output, options);

        // Add decompiler info at the end
        writer.write("\n\n// Decompiled with Procyon\n");

        return writer.toString();
    }
}

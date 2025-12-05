package it.denzosoft.javadecompilermodule.decompiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

/**
 * Registry for managing available decompiler engines.
 */
public final class DecompilerRegistry {

    private static final String PREF_SELECTED_DECOMPILER = "selectedDecompiler";
    private static final String PREF_PRESERVE_LINE_NUMBERS = "preserveLineNumbers";
    private static final String DEFAULT_DECOMPILER = "cfr";
    private static final boolean DEFAULT_PRESERVE_LINE_NUMBERS = true;

    private static final Map<String, DecompilerEngine> ENGINES = new LinkedHashMap<>();

    static {
        // Register all available engines (order matters for UI)
        register(new CfrEngine());
        register(new JdCoreEngine());
        register(new ProcyonEngine());
        register(new VineflowerEngine());
    }

    private DecompilerRegistry() {
    }

    private static void register(DecompilerEngine engine) {
        ENGINES.put(engine.getId(), engine);
    }

    /**
     * Returns all available decompiler engines.
     */
    public static List<DecompilerEngine> getAvailableEngines() {
        return Collections.unmodifiableList(new ArrayList<>(ENGINES.values()));
    }

    /**
     * Returns a decompiler engine by its ID.
     */
    public static DecompilerEngine getEngine(String id) {
        return ENGINES.get(id);
    }

    /**
     * Returns the currently selected decompiler engine.
     */
    public static DecompilerEngine getSelectedEngine() {
        String selectedId = getPreferences().get(PREF_SELECTED_DECOMPILER, DEFAULT_DECOMPILER);
        DecompilerEngine engine = ENGINES.get(selectedId);
        if (engine == null) {
            engine = ENGINES.get(DEFAULT_DECOMPILER);
        }
        return engine;
    }

    /**
     * Sets the selected decompiler engine.
     */
    public static void setSelectedEngine(String id) {
        if (ENGINES.containsKey(id)) {
            getPreferences().put(PREF_SELECTED_DECOMPILER, id);
        }
    }

    /**
     * Returns the ID of the currently selected decompiler.
     */
    public static String getSelectedEngineId() {
        return getPreferences().get(PREF_SELECTED_DECOMPILER, DEFAULT_DECOMPILER);
    }

    /**
     * Returns whether to preserve original line numbers during decompilation.
     */
    public static boolean isPreserveLineNumbers() {
        return getPreferences().getBoolean(PREF_PRESERVE_LINE_NUMBERS, DEFAULT_PRESERVE_LINE_NUMBERS);
    }

    /**
     * Sets whether to preserve original line numbers during decompilation.
     */
    public static void setPreserveLineNumbers(boolean preserve) {
        getPreferences().putBoolean(PREF_PRESERVE_LINE_NUMBERS, preserve);
    }

    private static Preferences getPreferences() {
        return NbPreferences.forModule(DecompilerRegistry.class);
    }
}

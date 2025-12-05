package it.denzosoft.javadecompilermodule;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.VetoableChangeListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.awt.Component;
import java.awt.Container;
import java.util.Date;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.text.EditorKit;
import org.netbeans.api.editor.fold.FoldHierarchy;
import org.netbeans.api.editor.settings.SimpleValueNames;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.openide.cookies.CloseCookie;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.text.CloneableEditor;
import org.openide.text.CloneableEditorSupport;
import org.openide.windows.CloneableOpenSupport;

/**
 * Editor support for decompiled class files.
 * Provides a full NetBeans editor with line numbers and code folding.
 */
public class DecompiledEditorSupport extends CloneableEditorSupport
        implements OpenCookie, EditorCookie, CloseCookie {

    private final ClassFileDataObject dataObject;
    private final DecompiledEnv decompiledEnv;

    public DecompiledEditorSupport(ClassFileDataObject dataObject) {
        this(dataObject, new DecompiledEnv(dataObject));
    }

    private DecompiledEditorSupport(ClassFileDataObject dataObject, DecompiledEnv env) {
        super(env);
        this.dataObject = dataObject;
        this.decompiledEnv = env;
        // Pass reference to Env so it can access decompiled source
        env.setEditorSupport(this);
    }

    @Override
    protected String messageSave() {
        return "Saving " + dataObject.getPrimaryFile().getNameExt();
    }

    @Override
    protected String messageName() {
        return dataObject.getPrimaryFile().getNameExt();
    }

    @Override
    protected String messageToolTip() {
        return dataObject.getPrimaryFile().getPath();
    }

    @Override
    protected String messageOpening() {
        return "Opening " + dataObject.getPrimaryFile().getNameExt();
    }

    @Override
    protected String messageOpened() {
        return "Opened " + dataObject.getPrimaryFile().getNameExt();
    }

    @Override
    protected EditorKit createEditorKit() {
        // Use Java editor kit for syntax highlighting and code folding
        EditorKit kit = CloneableEditorSupport.getEditorKit("text/x-java");
        if (kit != null) {
            return kit;
        }
        return super.createEditorKit();
    }

    // Light gray background to indicate read-only content
    private static final Color READ_ONLY_BACKGROUND = new Color(240, 240, 240);

    @Override
    protected void initializeCloneableEditor(CloneableEditor editor) {
        super.initializeCloneableEditor(editor);
        // Enable line numbers and code folding
        final JEditorPane pane = editor.getEditorPane();
        if (pane != null) {
            pane.putClientProperty(SimpleValueNames.LINE_NUMBER_VISIBLE, Boolean.TRUE);
            pane.putClientProperty(SimpleValueNames.CODE_FOLDING_ENABLE, Boolean.TRUE);
            // Initialize the fold hierarchy to enable fold operations
            FoldHierarchy.get(pane);
            pane.setEditable(false);

            // Set gray background - needs to be done later as editor kit may override it
            applyReadOnlyBackground(pane);

            // Re-apply background if it gets changed
            pane.addPropertyChangeListener("background", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (!READ_ONLY_BACKGROUND.equals(evt.getNewValue())) {
                        SwingUtilities.invokeLater(() -> pane.setBackground(READ_ONLY_BACKGROUND));
                    }
                }
            });
        }
    }

    private void applyReadOnlyBackground(final JEditorPane pane) {
        // Apply immediately
        setBackgroundRecursive(pane);

        // Also apply later in case editor kit overrides it
        SwingUtilities.invokeLater(() -> setBackgroundRecursive(pane));

        // And even later to catch late initialization
        SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() -> setBackgroundRecursive(pane)));
    }

    private void setBackgroundRecursive(JEditorPane pane) {
        pane.setOpaque(true);
        pane.setBackground(READ_ONLY_BACKGROUND);

        // Walk up the component hierarchy
        Container parent = pane.getParent();
        while (parent != null) {
            if (parent instanceof JViewport) {
                parent.setBackground(READ_ONLY_BACKGROUND);
                ((JViewport) parent).setOpaque(true);
            } else if (parent instanceof JScrollPane) {
                parent.setBackground(READ_ONLY_BACKGROUND);
                ((JScrollPane) parent).getViewport().setBackground(READ_ONLY_BACKGROUND);
                ((JScrollPane) parent).getViewport().setOpaque(true);
            }
            parent = parent.getParent();
        }
    }

    @Override
    protected boolean asynchronousOpen() {
        return true;
    }

    /**
     * Performs decompilation. No caching - always uses current decompiler settings.
     */
    String getDecompiledSource() {
        System.out.println("[JavaDecompiler] getDecompiledSource called");
        try {
            FileObject fo = dataObject.getPrimaryFile();
            System.out.println("[JavaDecompiler] Decompiling: " + fo.getPath());
            String result = Decompiler.decompile(fo);
            System.out.println("[JavaDecompiler] Decompilation complete, length: " + result.length());
            return result;
        } catch (Exception e) {
            System.out.println("[JavaDecompiler] Decompilation error: " + e.getMessage());
            e.printStackTrace();
            return "// Error during decompilation: " + e.getMessage() + "\n" +
                    "// " + e.getClass().getName();
        }
    }

    /**
     * Environment for the decompiled editor.
     * Provides read-only access to the decompiled content.
     */
    private static class DecompiledEnv implements CloneableEditorSupport.Env {

        private static final long serialVersionUID = 1L;
        private final ClassFileDataObject dataObject;
        private final PropertyChangeSupport propSupport = new PropertyChangeSupport(this);
        private transient DecompiledEditorSupport editorSupport;

        public DecompiledEnv(ClassFileDataObject dataObject) {
            this.dataObject = dataObject;
        }

        void setEditorSupport(DecompiledEditorSupport support) {
            this.editorSupport = support;
        }

        @Override
        public InputStream inputStream() throws IOException {
            String source = editorSupport != null ? editorSupport.getDecompiledSource() : "// Decompilation failed";
            return new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public OutputStream outputStream() throws IOException {
            throw new IOException("Decompiled source is read-only");
        }

        @Override
        public Date getTime() {
            return dataObject.getPrimaryFile().lastModified();
        }

        @Override
        public String getMimeType() {
            return "text/x-java";
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener l) {
            propSupport.addPropertyChangeListener(l);
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener l) {
            propSupport.removePropertyChangeListener(l);
        }

        @Override
        public void addVetoableChangeListener(VetoableChangeListener l) {
            // No veto support needed for read-only
        }

        @Override
        public void removeVetoableChangeListener(VetoableChangeListener l) {
            // No veto support needed for read-only
        }

        @Override
        public boolean isValid() {
            return dataObject.isValid();
        }

        @Override
        public boolean isModified() {
            return false;
        }

        @Override
        public void markModified() throws IOException {
            throw new IOException("Decompiled source is read-only");
        }

        @Override
        public void unmarkModified() {
            // No-op for read-only
        }

        @Override
        public CloneableOpenSupport findCloneableOpenSupport() {
            return editorSupport;
        }
    }
}

package it.denzosoft.javadecompilermodule;

import java.io.IOException;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.CookieSet;
import org.openide.nodes.Node;
import org.openide.util.Lookup;

/**
 * DataObject for .class files.
 * Provides the integration with NetBeans file system and enables
 * opening .class files with the decompiler editor.
 */
public class ClassFileDataObject extends MultiDataObject {

    private final DecompiledEditorSupport editorSupport;

    public ClassFileDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        System.out.println("[JavaDecompiler] ClassFileDataObject created for: " + pf.getPath());
        editorSupport = new DecompiledEditorSupport(this);
        CookieSet cookies = getCookieSet();
        cookies.add(editorSupport);
    }

    @Override
    protected Node createNodeDelegate() {
        return new ClassFileNode(this, getLookup());
    }

    @Override
    public Lookup getLookup() {
        return getCookieSet().getLookup();
    }
}

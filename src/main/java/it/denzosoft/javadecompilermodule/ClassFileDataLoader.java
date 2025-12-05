package it.denzosoft.javadecompilermodule;

import java.io.IOException;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.UniFileLoader;
import org.openide.util.NbBundle;

/**
 * DataLoader for .class files.
 * Registers the handler for opening compiled Java class files.
 */
public class ClassFileDataLoader extends UniFileLoader {

    private static final long serialVersionUID = 1L;
    public static final String CLASS_MIME_TYPE = "application/x-class-file";

    public ClassFileDataLoader() {
        super("it.denzosoft.javadecompilermodule.ClassFileDataObject");
    }

    @Override
    protected void initialize() {
        super.initialize();
        getExtensions().addExtension("class");
    }

    @Override
    protected String defaultDisplayName() {
        return NbBundle.getMessage(ClassFileDataLoader.class, "ClassFileDataLoader_displayName");
    }

    @Override
    protected MultiDataObject createMultiObject(FileObject primaryFile) throws DataObjectExistsException, IOException {
        return new ClassFileDataObject(primaryFile, this);
    }

    @Override
    protected String actionsContext() {
        return "Loaders/" + CLASS_MIME_TYPE + "/Actions";
    }
}

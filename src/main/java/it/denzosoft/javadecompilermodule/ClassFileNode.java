package it.denzosoft.javadecompilermodule;

import java.awt.Image;
import org.openide.loaders.DataNode;
import org.openide.nodes.Children;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;

/**
 * Node for .class files with proper icon display.
 */
public class ClassFileNode extends DataNode {

    // NetBeans standard class file icon paths
    private static final String[] NETBEANS_ICON_PATHS = {
        "org/netbeans/modules/java/resources/class.png",
        "org/netbeans/modules/java/resources/class.gif",
        "org/netbeans/modules/java/source/resources/icons/class.png",
        "org/netbeans/modules/java/source/resources/icons/class.gif"
    };

    private static Image cachedIcon = null;

    public ClassFileNode(ClassFileDataObject dataObject, Lookup lookup) {
        super(dataObject, Children.LEAF, lookup);
    }

    @Override
    public Image getIcon(int type) {
        if (cachedIcon == null) {
            // Try to load NetBeans standard class icon
            for (String path : NETBEANS_ICON_PATHS) {
                cachedIcon = ImageUtilities.loadImage(path, true);
                if (cachedIcon != null) {
                    break;
                }
            }
        }
        if (cachedIcon != null) {
            return cachedIcon;
        }
        return super.getIcon(type);
    }

    @Override
    public Image getOpenedIcon(int type) {
        return getIcon(type);
    }
}

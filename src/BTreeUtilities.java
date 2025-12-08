import java.io.IOException;
import java.io.PrintWriter;

// Used to traverse BTree
public class BTreeUtilities
{
    // Print keys in order
    public static void printAll(BTree tree, long nodeId) throws IOException
    {
        BTreeNode node = tree.getManager().readNode(nodeId);
        for (int i = 0; i < node.getNumKeys(); i++)
        {
            if (!node.isLeaf()) printAll(tree, node.getChild(i));
            System.out.println(node.getKey(i) + "," + node.getValue(i));
        }
        if (!node.isLeaf()) printAll(tree, node.getChild(node.getNumKeys()));
    }

    // Write all keys in order
    public static void writeAllCSV(BTree tree, long nodeId, PrintWriter pw) throws IOException
    {
        BTreeNode node = tree.getManager().readNode(nodeId);
        for (int i = 0; i < node.getNumKeys(); i++)
        {
            if (!node.isLeaf()) writeAllCSV(tree, node.getChild(i), pw);
            pw.println(node.getKey(i) + "," + node.getValue(i));
        }
        if (!node.isLeaf()) writeAllCSV(tree, node.getChild(node.getNumKeys()), pw);
    }
}
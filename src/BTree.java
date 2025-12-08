import java.io.IOException;

public class BTree
{
    private final IndexFileManager idx;
    public static final int T = BTreeNode.T;

    // Constructs BTree
    public BTree(IndexFileManager idx)
    {
        this.idx = idx;
    }

    public IndexFileManager getManager()
    {
        return idx;
    }

    // Search for key in BTree
    public long search(long key) throws IOException
    {
        long rootId = idx.getRootBlockId();
        // Return value, or, if not found then -1
        if (rootId == 0L) return -1L; // empty tree
        return searchRecursive(rootId, key);
    }

    // Search helper
    private long searchRecursive(long nodeId, long key) throws IOException
    {
        BTreeNode node = idx.readNode(nodeId);
        int i = 0;
        while (i < node.getNumKeys() && key > node.getKey(i)) i++;
        if (i < node.getNumKeys() && key == node.getKey(i)) return node.getValue(i);
        if (node.isLeaf()) return -1L;
        return searchRecursive(node.getChild(i), key);
    }

    // Insert
    public void insert(long key, long value) throws IOException
    {
        long rootId = idx.getRootBlockId();
        if (rootId == 0L)
        {
            // If tree is empty, create new node
            long newRootId = idx.allocateBlock();
            BTreeNode root = new BTreeNode(newRootId, 0L, true);
            root.insertKey(0, key, value);
            idx.writeNode(root);
            idx.setRootBlockId(newRootId);
            return;
        }

        BTreeNode root = idx.readNode(rootId);
        if (root.getNumKeys() == BTreeNode.MAX_KEYS)
        {
            // If root is full, then split and create new root
            long newRootId = idx.allocateBlock();
            BTreeNode newRoot = new BTreeNode(newRootId, 0L, false);
            newRoot.setChild(0, rootId);
            root.setParentID(newRootId);

            splitChild(newRoot, 0, root);

            // Write new root
            idx.writeNode(root);
            idx.writeNode(newRoot);

            // Set it
            idx.setRootBlockId(newRootId);

            // Insert into NotFull node
            insertNotFull(newRoot, key, value);
        }
        else
        {
            insertNotFull(root, key, value);
        }
    }

    // InsertNotFull node
    private void insertNotFull(BTreeNode node, long key, long value) throws IOException
    {
        int i = node.getNumKeys() - 1;
        if (node.isLeaf())
        {
            // Shift and insert into leaf
            while (i >= 0 && key < node.getKey(i)) i--;
            node.insertKey(i + 1, key, value);
            // Mark node "dirty"
            idx.writeNode(node);
        }
        else
        {
            // Go to correct child
            while (i >= 0 && key < node.getKey(i)) i--;
            i++;
            long childId = node.getChild(i);
            BTreeNode child = idx.readNode(childId);

            if (child.getNumKeys() == BTreeNode.MAX_KEYS)
            {
                splitChild(node, i, child);
                if (key > node.getKey(i)) i++;
                child = idx.readNode(node.getChild(i));
            }
            insertNotFull(child, key, value);
        }
    }

    // Split child node
    private void splitChild(BTreeNode parent, int index, BTreeNode fullChild) throws IOException
    {
        int T = BTreeNode.T;

        long newChildId = idx.allocateBlock();
        BTreeNode newChild = new BTreeNode(newChildId, parent.getBlockID(), fullChild.isLeaf());

        long medianKey = fullChild.getKey(T - 1);
        long medianValue = fullChild.getValue(T - 1);

        // Copy to new node
        for (int j = 0; j < T - 1; j++)
        {
            newChild.insertKey(j, fullChild.getKey(j + T), fullChild.getValue(j + T));
        }

        // If full
        if (!fullChild.isLeaf())
        {
            for (int j = 0; j < T; j++)
            {
                long movedChildId = fullChild.getChild(j + T);
                newChild.setChild(j, movedChildId);
                if (movedChildId != 0L)
                {
                    BTreeNode movedChild = idx.readNode(movedChildId);
                    movedChild.setParentID(newChildId);
                    idx.writeNode(movedChild);
                }
            }
        }

        fullChild.setNumKeys(T - 1);
        fullChild.clearKeysFrom(T - 1);
        fullChild.clearChildrenFrom(T);

        // Insert new child
        for (int j = parent.getNumKeys(); j >= index + 1; j--)
        {
            parent.setChild(j + 1, parent.getChild(j));
        }
        parent.setChild(index + 1, newChildId);

        parent.insertKey(index, medianKey, medianValue);
        newChild.setParentID(parent.getBlockID());

        // Write to cache
        idx.writeNode(fullChild);
        idx.writeNode(newChild);
        idx.writeNode(parent);
    }

    public void close() throws IOException
    {
        idx.flushAndClose();
    }
}
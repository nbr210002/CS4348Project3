import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Random;
import java.io.IOException;
import java.io.RandomAccessFile;

public class BTree
{
    public static final int BLOCK_SIZE = 512;
    private static final long HEADER_BLOCK = 0L;

    private RandomAccessFile file;
    private long rootID;

    public BTree(String filename) throws IOException
    {
        file = new RandomAccessFile(filename, "rw");

        // If file is empty
        if (file.length() == 0)
        {
           // 512 bytes
            file.setLength(BLOCK_SIZE);

            // Allocate root node
            rootID = allocateBlock();
            BTreeNode root = new BTreeNode(rootID, -1L, true);
            writeNode(root);
            writeRootPointer();

        }
        else
        {
            rootID = readRootPointer();
        }
    }

    // Read
    private long readRootPointer() throws IOException
    {
        file.seek(0);
        return file.readLong();
    }

    // Write
    private void writeRootPointer() throws IOException
    {
        file.seek(0);
        file.writeLong(rootID);
    }

    // Allocate
    private long allocateBlock() throws IOException
    {
        long id = file.length();
        file.setLength(id + BLOCK_SIZE);
        return id;
    }

    public void writeNode(BTreeNode node) throws IOException
    {
        file.seek(node.getBlockID());
        file.write(node.toBytes());
    }

    public BTreeNode readNode(long blockID) throws IOException
    {
        file.seek(blockID);
        byte[] buf = new byte[BLOCK_SIZE];
        file.readFully(buf);
        return BTreeNode.fromBytes(buf);
    }

    // Search
    public long search(long key) throws IOException
    {
        return searchRecursive(rootID, key);
    }

    private long searchRecursive(long nodeID, long key) throws IOException
    {
        BTreeNode node = readNode(nodeID);

        int i = 0;
        while (i < node.getNumKeys() && key > node.getKey(i)) i++;

        if (i < node.getNumKeys() && key == node.getKey(i)) return node.getValue(i);

        if (node.isLeaf()) return -1L;

        long childID = node.getChild(i);
        return searchRecursive(childID, key);
    }

    // Insert
    public void insert(long key, long value) throws IOException
    {
        BTreeNode root = readNode(rootID);

        // Ir root is full, create new
        if (root.getNumKeys() == BTreeNode.MAX_KEYS)
        {
            long newRootID = allocateBlock();
            BTreeNode newRoot = new BTreeNode(newRootID, -1L, false);

            // Old root set to 0
            newRoot.setChild(0, rootID);
            root.setParentID(newRootID);

            // Split old root
            splitChild(newRoot, 0, root);

            // Write
            writeNode(root);
            writeNode(newRoot);

            // Update
            rootID = newRootID;
            writeRootPointer();

            // Insert
            insertNotFull(newRoot, key, value);
        }
        else
        {
            insertNotFull(root, key, value);
        }
    }

    // Insert into not full
    private void insertNotFull(BTreeNode node, long key, long value) throws IOException
    {
        int i = node.getNumKeys() - 1;

        if (node.isLeaf())
        {
            // find position and insert into leaf
            while (i >= 0 && key < node.getKey(i)) i--;
            node.insertKey(i + 1, key, value);
            writeNode(node);
        }
        else
        {
            // Find child
            while (i >= 0 && key < node.getKey(i)) i--;
            i++;

            long childID = node.getChild(i);
            BTreeNode child = readNode(childID);

            // Split if child is full
            if (child.getNumKeys() == BTreeNode.MAX_KEYS)
            {
                splitChild(node, i, child);

                // Pick which child
                if (key > node.getKey(i)) i++;
                child = readNode(node.getChild(i));
            }
            insertNotFull(child, key, value);
        }
    }


    private void splitChild(BTreeNode parent, int index, BTreeNode fullChild) throws IOException {
        int T = BTreeNode.T;

        // Create new child node for right half
        long newChildID = allocateBlock();
        BTreeNode newChild = new BTreeNode(newChildID, parent.getBlockID(), fullChild.isLeaf());

        // Median
        long medianKey = fullChild.getKey(T - 1);
        long medianValue = fullChild.getValue(T - 1);

        // Move last T-1 keys
        for (int j = 0; j < T - 1; j++)
        {
            newChild.insertKey(j, fullChild.getKey(j + T), fullChild.getValue(j + T));
        }

        // If !fullChild.isLeaf()
        if (!fullChild.isLeaf())
        {
            for (int j = 0; j < T; j++)
            {
                long movedChildID = fullChild.getChild(j + T);
                newChild.setChild(j, movedChildID);

                if (movedChildID != 0L)
                {
                    BTreeNode movedChild = readNode(movedChildID);
                    movedChild.setParentID(newChildID);
                    writeNode(movedChild);
                }
            }
        }

        fullChild.setNumKeys(T - 1);
        fullChild.clearKeysFrom(T - 1);
        fullChild.clearChildrenFrom(T);

        // Shift children
        for (int j = parent.getNumKeys(); j >= index + 1; j--)
        {
            parent.setChild(j + 1, parent.getChild(j));
        }
        parent.setChild(index + 1, newChildID);

        // Insert median into parent
        parent.insertKey(index, medianKey, medianValue);

        newChild.setParentID(parent.getBlockID());

        // Write
        writeNode(fullChild);
        writeNode(newChild);
        writeNode(parent);
    }

    // Close files
    public void close() throws IOException
    {
        file.close();
    }
}
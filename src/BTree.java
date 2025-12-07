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
    private static final long MAGIC_NUMBER = 0x3433343850524A33L;

    private RandomAccessFile file;
    private long rootID;
    private long nextBlockID;

    public BTree(String filename) throws IOException
    {
        file = new RandomAccessFile(filename, "rw");

        if (file.length() == 0)
        {
            file.setLength(BLOCK_SIZE);
            rootID = allocateBlock();
            nextBlockID = rootID + BLOCK_SIZE;

            // Create empty root node
            BTreeNode root = new BTreeNode(rootID, 0L, true);
            writeNode(root);
            writeHeader();
        }
        else
        {
            readHeader();
        }
    }

    private void writeHeader() throws IOException
    {
        file.seek(HEADER_BLOCK);
        ByteBuffer bb = ByteBuffer.allocate(BLOCK_SIZE);
        bb.putLong(MAGIC_NUMBER);
        bb.putLong(rootID);
        bb.putLong(file.length());

        file.write(bb.array());
    }

    private void readHeader() throws IOException
    {
        file.seek(HEADER_BLOCK);
        byte[] buf = new byte[BLOCK_SIZE];
        file.readFully(buf);
        ByteBuffer bb = ByteBuffer.wrap(buf);

        long magic = bb.getLong();
        if (magic != MAGIC_NUMBER)
        {
            throw new IOException("Invalid index file: magic number mismatch");
        }

        rootID = bb.getLong();
        nextBlockID = bb.getLong();
    }

    public long getRootID()
    {
        return rootID;
    }

    // Allocate
    private long allocateBlock() throws IOException
    {
        long id = file.length();
        file.setLength(id + BLOCK_SIZE);
        return id;
    }

    // Write
    public void writeNode(BTreeNode node) throws IOException
    {
        file.seek(node.getBlockID());
        file.write(node.toBytes());
    }

    // Read
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

    // Search helper
    private long searchRecursive(long nodeID, long key) throws IOException
    {
        BTreeNode node = readNode(nodeID);
        int i = 0;
        while (i < node.getNumKeys() && key > node.getKey(i)) i++;

        if (i < node.getNumKeys() && key == node.getKey(i)) return node.getValue(i);

        if (node.isLeaf()) return -1L;
        return searchRecursive(node.getChild(i), key);
    }

    // Insert
    public void insert(long key, long value) throws IOException
    {
        BTreeNode root = readNode(rootID);
        if (root.getNumKeys() == BTreeNode.MAX_KEYS)
        {
            long newRootID = allocateBlock();
            BTreeNode newRoot = new BTreeNode(newRootID, 0L, false);
            newRoot.setChild(0, rootID);
            root.setParentID(newRootID);

            splitChild(newRoot, 0, root);

            writeNode(root);
            writeNode(newRoot);

            rootID = newRootID;
            writeHeader();

            insertNotFull(newRoot, key, value);
        }
        else
        {
            insertNotFull(root, key, value);
        }
    }

    // Insert into not full node
    private void insertNotFull(BTreeNode node, long key, long value) throws IOException
    {
        int i = node.getNumKeys() - 1;

        // If node is a leaf, make room
        if (node.isLeaf())
        {
            while (i >= 0 && key < node.getKey(i)) i--;
            node.insertKey(i + 1, key, value);
            writeNode(node);
        }
        else
        {
            // Find space
            while (i >= 0 && key < node.getKey(i)) i--;
            i++;

            BTreeNode child = readNode(node.getChild(i));

            // Split child
            if (child.getNumKeys() == BTreeNode.MAX_KEYS)
            {
                splitChild(node, i, child);
                if (key > node.getKey(i)) i++;
                child = readNode(node.getChild(i));
            }
            insertNotFull(child, key, value);
        }
    }

    // Split
    private void splitChild(BTreeNode parent, int index, BTreeNode fullChild) throws IOException
    {
        int T = BTreeNode.T;

        long newChildID = allocateBlock();
        BTreeNode newChild = new BTreeNode(newChildID, parent.getBlockID(), fullChild.isLeaf());

        long medianKey = fullChild.getKey(T - 1);
        long medianValue = fullChild.getValue(T - 1);

        // Move T-1 keys
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

        // Shift to insert new child
        for (int j = parent.getNumKeys(); j >= index + 1; j--)
        {
            parent.setChild(j + 1, parent.getChild(j));
        }
        parent.setChild(index + 1, newChildID);

        parent.insertKey(index, medianKey, medianValue);
        newChild.setParentID(parent.getBlockID());

        writeNode(fullChild);
        writeNode(newChild);
        writeNode(parent);
    }

    // Close
    public void close() throws IOException
    {
        file.close();
    }
}
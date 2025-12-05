import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class BTreeNode
{
    public static final int T = 10;
    public static final int MAX_KEYS = 2 * T-1;
    public static final int MAX_CHILDREN = 2* T;

    // Nodes
    private long blockID;
    private long parentID;
    private int numKeys;
    private long[] keys;
    private long[] values;
    private long[] children;
    private boolean isLeaf;

    public BTreeNode(long blockID, long parentID, boolean isLeaf)
    {
        this.blockID = blockID;
        this.parentID = parentID;
        this.isLeaf = isLeaf;
        this.numKeys = 0;
        this.keys = new long[MAX_KEYS];
        this.values = new long[MAX_CHILDREN];
        this.children = new long[MAX_CHILDREN];
        Arrays.fill(this.keys, 0);
        Arrays.fill(this.values, 0);
        Arrays.fill(this.children, 0);
    }

    // Getters and setters
    public long getBlockID()
    {
        return blockID;
    }
    public long getParentID()
    {
        return parentID;
    }
    public void  setParentID(long parentID)
    {
        this.parentID = parentID;
    }
    public int getNumKeys()
    {
        return numKeys;
    }
    public boolean isLeaf()
    {
        return isLeaf;
    }

    // Insert key at position i
    public void insertKey(int i, long key, long value)
    {
        if (numKeys >= MAX_KEYS)
        {
            throw new IllegalStateException("Node Is Full");
        }

        for (int j = numKeys; j > i; j--)
        {
            keys[j] = keys[j-1];
            values[j] = values[j-1];
        }
        keys[i] = key;
        values[i] = value;
        numKeys++;
    }

    // Set child pointer
    public void setChild(int index, long childBlockInt)
    {
        children[index] = childBlockInt;
    }
    public long getKey(int i)
    {
        return keys[i];
    }
    public long getValue(int i)
    {
        return values[i];
    }
    public long getChild(int i)
    {
        return children[i];
    }

    public byte[] toBytes()
    {
        ByteBuffer bb = ByteBuffer.allocate(512);
        bb.putLong(blockID);
        bb.putLong(parentID);
        bb.putInt(numKeys);

        for (int i = 0; i <MAX_KEYS; i++) bb.putLong(keys[i]);
        for (int i = 0; i < MAX_KEYS; i++) bb.putLong(values[i]);
        for (int i = 0; i < MAX_CHILDREN; i++) bb.putLong(children[i]);

        // remaining unused bytes
        return bb.array();
    }

    // Deserialize nodes
    public static BTreeNode fromBytes(byte[] data)
    {
        ByteBuffer bb = ByteBuffer.wrap(data);
        long blockID = bb.getLong();
        long parentID = bb.getLong();
        int numKeys = (int)  bb.getLong();

        // Defaults to true
        BTreeNode node = new BTreeNode(blockID, parentID, true);
        node.numKeys = numKeys;

        for (int i = 0; i < MAX_KEYS; i++) node.keys[i] = bb.getLong();
        for (int i = 0; i < MAX_KEYS; i++) node.values[i] = bb.getLong();
        for (int i = 0; i < MAX_CHILDREN; i++) node.children[i] = bb.getLong();

        // Determine if node is a leaf
        node.isLeaf = true;
        for (long c : node.children)
        {
            if (c != 0)
            {
                node.isLeaf = false;
                break;
            }
        }
        return node;
    }

    // Debug print
    public void printNode()
    {
        System.out.print("BlockID: " + blockID + ", Keys: ");
        for (int i = 0; i < numKeys; i++)
        {
            System.out.print(keys[i] + "(" + values[i] + ") ");
        }
        System.out.println();
    }
}

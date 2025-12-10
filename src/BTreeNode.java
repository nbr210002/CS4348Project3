import java.nio.ByteBuffer;
import java.util.Arrays;

public class BTreeNode
{
    public static final int T = 10;
    public static final int MAX_KEYS = 2 * T - 1;    // 19
    public static final int MAX_CHILDREN = 2 * T;    // 20
    public static final int BLOCK_BYTES = 512;

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
        this.values = new long[MAX_KEYS];
        this.children = new long[MAX_CHILDREN];

        Arrays.fill(this.keys, 0L);
        Arrays.fill(this.values, 0L);
        Arrays.fill(this.children, 0L);
    }

    public long getBlockID()
    {
        return blockID;
    }
    public long getParentID()
    {
        return parentID;
    }
    public void setParentID(long parentID)
    {
        this.parentID = parentID;
    }
    public int getNumKeys()
    {
        return numKeys;
    }
    public void setNumKeys(int n)
    {
        this.numKeys = n;
    }
    public boolean isLeaf()
    {
        return isLeaf;
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

    // Sets child
    public void setChild(int index, long childBlockId)
    {
        children[index] = childBlockId;
        if (childBlockId != 0L) this.isLeaf = false;
    }

    // Inserts key at index
    public void insertKey(int i, long key, long value)
    {
        if (numKeys >= MAX_KEYS) throw new IllegalStateException("Node is full");
        for (int j = numKeys; j > i; j--)
        {
            keys[j] = keys[j - 1];
            values[j] = values[j - 1];
        }
        keys[i] = key;
        values[i] = value;
        numKeys++;
    }

    // Clears keys from start index
    public void clearKeysFrom(int startIndex)
    {
        for (int i = startIndex; i < MAX_KEYS; i++)
        {
            keys[i] = 0L;
            values[i] = 0L;
        }
    }

    // Clears children from starting index
    public void clearChildrenFrom(int startIndex)
    {
        for (int i = startIndex; i < MAX_CHILDREN; i++)
        {
            children[i] = 0L;
        }
        boolean leaf = true;
        for (long c : children)
        {
            if (c != 0L)
            {
                leaf = false; break;
            }
        }
        this.isLeaf = leaf;
    }

    // Serialize to 512 bytes
    public byte[] toBytes()
    {
        ByteBuffer bb = ByteBuffer.allocate(BLOCK_BYTES);
        bb.putLong(blockID);
        bb.putLong(parentID);
        bb.putLong((long) numKeys);
        for (int i = 0; i < MAX_KEYS; i++) bb.putLong(keys[i]);
        for (int i = 0; i < MAX_KEYS; i++) bb.putLong(values[i]);
        for (int i = 0; i < MAX_CHILDREN; i++) bb.putLong(children[i]);
        return bb.array();
    }

    // Deserialize node
    public static BTreeNode fromBytes(byte[] data)
    {
        ByteBuffer bb = ByteBuffer.wrap(data);
        long blockID = bb.getLong();
        long parentID = bb.getLong();
        int numKeys = (int) bb.getLong();

        BTreeNode node = new BTreeNode(blockID, parentID, true);
        node.numKeys = numKeys;

        for (int i = 0; i < MAX_KEYS; i++) node.keys[i] = bb.getLong();
        for (int i = 0; i < MAX_KEYS; i++) node.values[i] = bb.getLong();
        for (int i = 0; i < MAX_CHILDREN; i++) node.children[i] = bb.getLong();

        node.isLeaf = true;
        for (long c : node.children)
        {
            if (c != 0L)
            {
                node.isLeaf = false; break;
            }
        }
        return node;
    }

    // Print
    public void printNode()
    {
        System.out.print("BlockID: " + blockID + " Keys:");
        for (int i = 0; i < numKeys; i++)
        {
            System.out.print(" " + keys[i] + "(" + values[i] + ")");
        }
        System.out.println();
    }
}
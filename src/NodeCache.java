import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedHashMap;
import java.util.Map;

public class NodeCache
{
    private final int capacity;
    private final RandomAccessFile raf;
    private final IndexFileManager manager;

    private static class Entry
    {
        BTreeNode node;

        // needs a write back
        boolean dirty;

        Entry(BTreeNode n, boolean d)
        {
            node = n; dirty = d;
        }
    }

    private final LinkedHashMap<Long, Entry> map;

    public NodeCache(int capacity, RandomAccessFile raf, IndexFileManager manager)
    {
        this.capacity = capacity;
        this.raf = raf;
        this.manager = manager;
        this.map = new LinkedHashMap<Long, Entry>(capacity, 0.75f, true)
        {
            protected boolean removeEldestEntry(Map.Entry<Long, Entry> eldest)
            {
                if (size() > NodeCache.this.capacity)
                {
                    try
                    {
                        evictEntry(eldest.getKey(), eldest.getValue());
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                    return true;
                }
                return false;
            }
        };
    }

    // Get node from cache or load from disk
    public synchronized BTreeNode get(long blockId) throws IOException
    {
        Entry e = map.get(blockId);
        if (e != null) return e.node;

        byte[] block = manager.readBlockBytes(blockId);
        BTreeNode node = BTreeNode.fromBytes(block);
        map.put(blockId, new Entry(node, false));
        return node;
    }

    // Put node into cache and mark "dirty" if true
    public synchronized void put(BTreeNode node, boolean dirty) throws IOException
    {
        long id = node.getBlockID();
        Entry e = map.get(id);
        if (e != null)
        {
            e.node = node;
            e.dirty = e.dirty || dirty;
        }
        else
        {
            map.put(id, new Entry(node, dirty));
        }
    }

    // Writes a node if dirty
    private void evictEntry(Long blockId, Entry entry) throws IOException
    {
        if (entry.dirty)
        {
            // write to disk
            manager.writeBlockBytes(blockId, entry.node.toBytes());
        }
    }

    // Flush all to disk and clear the cache
    public synchronized void flushAll() throws IOException
    {
        for (Map.Entry<Long, Entry> e : map.entrySet())
        {
            if (e.getValue().dirty)
            {
                manager.writeBlockBytes(e.getKey(), e.getValue().node.toBytes());
                e.getValue().dirty = false;
            }
        }
        map.clear();
    }
} //
import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.File;

public class IndexFileManager
{
    public static final int BLOCK_SIZE = 512;
    private static final String MAGIC = "4348PRJ3"; // exactly 8 ASCII bytes
    private final RandomAccessFile raf;
    private final NodeCache cache;
    private long rootBlockId;
    private long nextBlockId;

    public IndexFileManager(String filename) throws IOException
    {
        File f = new File(filename);
        boolean create = !f.exists();

        // Open file
        this.raf = new RandomAccessFile(f, "rw");

        // Create new header block
        if (create)
        {
            raf.setLength(BLOCK_SIZE);
            this.rootBlockId = 0L;
            this.nextBlockId = 1L;
            writeHeader();
        }
        else
        {
            readHeader();
        }

        // NodeCache with capacity 3
        this.cache = new NodeCache(3, raf, this);
    }

    // Read header block
    private void readHeader() throws IOException
    {
        raf.seek(0);
        byte[] buf = new byte[BLOCK_SIZE];
        raf.readFully(buf);
        ByteBuffer bb = ByteBuffer.wrap(buf);

        byte[] magicBytes = new byte[8];
        bb.get(magicBytes);
        String magic = new String(magicBytes, "US-ASCII");
        if (!MAGIC.equals(magic))
        {
            throw new IOException("Invalid index file: magic mismatch");
        }
        rootBlockId = bb.getLong();
        nextBlockId = bb.getLong();
    }

    // Write header block
    private void writeHeader() throws IOException
    {
        raf.seek(0);
        ByteBuffer bb = ByteBuffer.allocate(BLOCK_SIZE);
        bb.put(MAGIC.getBytes("US-ASCII")); // 8 bytes
        bb.putLong(rootBlockId);
        bb.putLong(nextBlockId);
        // rest zero
        raf.write(bb.array());
    }

    // Read 512 byte block
    public byte[] readBlockBytes(long blockId) throws IOException
    {
        long offset = blockId * BLOCK_SIZE;
        if (offset + BLOCK_SIZE > raf.length())
        {
            throw new IOException("Attempt to read beyond EOF: block " + blockId);
        }
        raf.seek(offset);
        byte[] buf = new byte[BLOCK_SIZE];
        raf.readFully(buf);
        return buf;
    }

    // Write 512 byte block
    public void writeBlockBytes(long blockId, byte[] data) throws IOException
    {
        if (data.length != BLOCK_SIZE) throw new IllegalArgumentException("Block must be 512 bytes");
        long offset = blockId * BLOCK_SIZE;
        raf.seek(offset);
        raf.write(data);
        if (raf.length() < offset + BLOCK_SIZE) raf.setLength(offset + BLOCK_SIZE);
    }

    // Allocate a new block index
    public synchronized long allocateBlock() throws IOException
    {
        long id = nextBlockId;
        nextBlockId++;
        long newLength = id * BLOCK_SIZE + BLOCK_SIZE;
        if (raf.length() < newLength) raf.setLength(newLength);
        writeHeader();
        return id;
    }

    // Root id accessors
    public synchronized long getRootBlockId()
    {
        return rootBlockId;
    }

    public synchronized void setRootBlockId(long id) throws IOException
    {
        this.rootBlockId = id;
        writeHeader();
    }

    public BTreeNode readNode(long blockId) throws IOException
    {
        if (blockId == 0L) throw new IOException("BlockId 0 is header, not a node");
        return cache.get(blockId);
    }

    public void writeNode(BTreeNode node) throws IOException
    {
        cache.put(node, true);
    }

    // Flush and close
    public void flushAndClose() throws IOException
    {
        cache.flushAll();
        raf.close();
    }
}
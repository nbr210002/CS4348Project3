import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.Deque;
import java.util.ArrayDeque;

public class IndexFile
{
    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            System.err.println("Error: No command provided.");
            System.exit(1);
        }

        String command = args[0].toLowerCase();

        try
        {
            // Commands: create, insert, search, load, print, extract
            switch (command)
            {
                case "create":
                    cmdCreate(args);
                    break;
                case "insert":
                    cmdInsert(args);
                    break;
                case "search":
                    cmdSearch(args);
                    break;
                case "load":
                    cmdLoad(args);
                    break;
                case "print":
                    cmdPrint(args);
                    break;
                case "extract":
                    cmdExtract(args);
                    break;
                default:
                    System.err.println("Error: Unknown command '" + command + "'");
                    System.exit(1);
            }
        }
        catch (IOException e)
        {
            System.err.println("I/O Error: " + e.getMessage());
            System.exit(1);
        }
        catch (NumberFormatException e)
        {
            System.err.println("Error: Invalid number format.");
            System.exit(1);
        }
    }

    // Create
    private static void cmdCreate(String[] args) throws IOException
    {
        // If args aren't the right length
        if (args.length != 2)
        {
            System.err.println("Usage: create <indexfile>");
            System.exit(1);
        }

        String filename = args[1];
        File f = new File(filename);

        if (f.exists())
        {
            System.err.println("Error: File already exists.");
            System.exit(1);
        }

        IndexFileManager mgr = new IndexFileManager(filename);
        mgr.flushAndClose();
        System.out.println("Index file created: " + filename);
    }

    // Insert
    private static void cmdInsert(String[] args) throws IOException
    {
        // If args are not the right length
        if (args.length != 4)
        {
            System.err.println("Usage: insert <indexfile> <key> <value>");
            System.exit(1);
        }
        String filename = args[1];
        long key = Long.parseLong(args[2]);
        long value = Long.parseLong(args[3]);

        File f = new File(filename);
        if (!f.exists())
        {
            System.err.println("Error: Index file does not exist.");
            System.exit(1);
        }

        IndexFileManager mgr = new IndexFileManager(filename);
        BTree tree = new BTree(mgr);
        tree.insert(key, value);
        tree.close();
        System.out.println("Inserted key=" + key + ", value=" + value);
    }

    // Search
    private static void cmdSearch(String[] args) throws IOException
    {
        // If args aren't the right length
        if (args.length != 3)
        {
            System.err.println("Usage: search <indexfile> <key>");
            System.exit(1);
        }
        String filename = args[1];
        long key = Long.parseLong(args[2]);

        File f = new File(filename);
        // If file does not exist
        if (!f.exists())
        {
            System.err.println("Error: Index file does not exist.");
            System.exit(1);
        }

        IndexFileManager mgr = new IndexFileManager(filename);
        BTree tree = new BTree(mgr);
        long value = tree.search(key);
        if (value == -1L) System.out.println("Key " + key + " not found.");
        else System.out.println("Found: " + key + " -> " + value);
        tree.close();
    }

    // Load CSV File
    private static void cmdLoad(String[] args) throws IOException
    {
        // If args aren't the right length
        if (args.length != 3)
        {
            System.err.println("Usage: load <indexfile> <csvfile>");
            System.exit(1);
        }
        String indexFilename = args[1];
        String csvFilename = args[2];

        File idx = new File(indexFilename);
        File csv = new File(csvFilename);
        if (!idx.exists())
        {
            System.err.println("Error: Index file does not exist.");
            System.exit(1);
        }
        if (!csv.exists())
        {
            System.err.println("Error: CSV file does not exist.");
            System.exit(1);
        }

        IndexFileManager mgr = new IndexFileManager(indexFilename);
        BTree tree = new BTree(mgr);
        try (Scanner sc = new Scanner(csv))
        {
            while (sc.hasNextLine())
            {
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length != 2)
                {
                    System.err.println("Skipping invalid line: " + line);
                    continue;
                }
                long k = Long.parseLong(parts[0].trim());
                long v = Long.parseLong(parts[1].trim());
                tree.insert(k, v);
            }
        }
        tree.close();
        System.out.println("CSV file loaded into index: " + csvFilename);
    }

    // Print
    private static void cmdPrint(String[] args) throws IOException
    {
        // If args are not the right length
        if (args.length != 2)
        {
            System.err.println("Usage: print <indexfile>");
            System.exit(1);
        }
        String filename = args[1];
        File f = new File(filename);
        // If file exists
        if (!f.exists())
        {
            System.err.println("Error: Index file does not exist.");
            System.exit(1);
        }

        IndexFileManager mgr = new IndexFileManager(filename);
        try
        {
            long rootId = mgr.getRootBlockId();
            if (rootId != 0L)
            {
                diskInOrderPrint(mgr, rootId);
            }
        }
        finally
        {
            mgr.flushAndClose();
        }
    }

    private static void diskInOrderPrint(IndexFileManager mgr, long rootId) throws IOException
    {
        class Frame
        {
            long blockId; int idx; Frame(long b, int i)
            {
                blockId = b; idx = i;
            }
        }

        Deque<Frame> stack = new ArrayDeque<>();
        stack.addLast(new Frame(rootId, 0));

        while (!stack.isEmpty())
        {
            Frame top = stack.getLast();
            // Can load up to 3
            BTreeNode node = mgr.readNode(top.blockId);
            int nkeys = node.getNumKeys();

            if (top.idx < nkeys)
            {
                long childId = node.getChild(top.idx);
                if (childId != 0L)
                {
                    // push child
                    stack.addLast(new Frame(childId, 0));
                }
                else
                {
                    // if there is no child, print
                    System.out.println(node.getKey(top.idx) + "," + node.getValue(top.idx));
                    top.idx++;
                }
            }
            else
            {
                long rightChild = node.getChild(nkeys);
                if (rightChild != 0L)
                {
                    stack.addLast(new Frame(rightChild, 0));
                }
                else
                {
                    stack.removeLast();
                }
            }
            node = null;
        }
    }

    // Extract
    private static void cmdExtract(String[] args) throws IOException
    {
        if (args.length != 3)
        {
            System.err.println("Usage: extract <indexfile> <csvfile>");
            System.exit(1);
        }
        String indexFilename = args[1];
        String csvFilename = args[2];

        File idx = new File(indexFilename);
        File csv = new File(csvFilename);
        if (!idx.exists())
        {
            System.err.println("Error: Index file does not exist.");
            System.exit(1);
        }
        if (csv.exists())
        {
            System.err.println("Error: CSV output file already exists.");
            System.exit(1);
        }

        IndexFileManager mgr = new IndexFileManager(indexFilename);
        try (PrintWriter pw = new PrintWriter(csv))
        {
            long rootId = mgr.getRootBlockId();
            if (rootId != 0L) diskInOrderWriteCSV(mgr, rootId, pw);
        }
        finally
        {
            mgr.flushAndClose();
        }
        System.out.println("BTree extracted to CSV: " + csvFilename);
    }

    private static void diskInOrderWriteCSV(IndexFileManager mgr, long rootId, PrintWriter pw) throws IOException {
        class Frame
        { long blockId; int idx; Frame(long b, int i)
            {
                blockId = b; idx = i;
            }
        }

        Deque<Frame> stack = new ArrayDeque<>();
        stack.addLast(new Frame(rootId, 0));

        while (!stack.isEmpty())
        {
            Frame top = stack.getLast();
            BTreeNode node = mgr.readNode(top.blockId);
            int nkeys = node.getNumKeys();

            if (top.idx < nkeys)
            {
                long childId = node.getChild(top.idx);
                if (childId != 0L)
                {
                    stack.addLast(new Frame(childId, 0));
                }
                else
                {
                    pw.println(node.getKey(top.idx) + "," + node.getValue(top.idx));
                    top.idx++;
                }
            }
            else
            {
                long rightChild = node.getChild(nkeys);
                if (rightChild != 0L)
                {
                    stack.addLast(new Frame(rightChild, 0));
                }
                else
                {
                    stack.removeLast();
                }
            }
            node = null;
        }
    }
} //
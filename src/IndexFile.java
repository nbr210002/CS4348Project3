import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Random;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.*;
import java.util.Scanner;

public class IndexFile
{
    public static void main(String[] args)
    {
        // There must be at least 1 argument
        if (args.length < 1)
        {
            System.err.println("Error: No command provided.");
            System.exit(1);
        }

        // Convert command to lowercase
        String command = args[0].toLowerCase();

        try
        {
            // Commands: create, insert, search, load, print, and extract
            switch (command)
            {
                case "create":
                    handleCreate(args);
                    break;
                case "insert":
                    handleInsert(args);
                    break;
                case "search":
                    handleSearch(args);
                    break;
                case "load":
                    handleLoad(args);
                    break;
                case "print":
                    handlePrint(args);
                    break;
                case "extract":
                    handleExtract(args);
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
    private static void handleCreate(String[] args) throws IOException
    {
        if (args.length != 2)
        {
            System.err.println("Usage: create <indexfile>");
            System.exit(1);
        }

        String filename = args[1];
        File file = new File(filename);

        // If the file exists, then fails
        if (file.exists())
        {
            System.err.println("Error: File already exists.");
            System.exit(1);
        }

        // Initialize BTree
        BTree btree = new BTree(filename);
        btree.close();
        System.out.println("Index file created: " + filename);
    }

    // Insert
    private static void handleInsert(String[] args) throws IOException
    {
        if (args.length != 4)
        {
            System.err.println("Usage: insert <indexfile> <key> <value>");
            System.exit(1);
        }

        String filename = args[1];
        long key = Long.parseLong(args[2]);
        long value = Long.parseLong(args[3]);

        // If !file.exists(), fail
        File file = new File(filename);
        if (!file.exists())
        {
            System.err.println("Error: Index file does not exist.");
            System.exit(1);
        }

        // Open BTree, insert key-value, and then close file
        BTree btree = new BTree(filename);
        btree.insert(key, value);
        btree.close();
        System.out.println("Inserted key=" + key + ", value=" + value);
    }

    // Search
    private static void handleSearch(String[] args) throws IOException
    {
        if (args.length != 3)
        {
            System.err.println("Usage: search <indexfile> <key>");
            System.exit(1);
        }

        String filename = args[1];
        long key = Long.parseLong(args[2]);

        File file = new File(filename);
        if (!file.exists())
        {
            System.err.println("Error: Index file does not exist.");
            System.exit(1);
        }

        // Open BTree, search, and then display result
        BTree btree = new BTree(filename);
        long value = btree.search(key);
        if (value == -1)
            System.out.println("Key " + key + " not found.");
        else
            System.out.println("Found: " + key + " -> " + value);

        btree.close();
    }

    // Load CSV file into the BTree
    private static void handleLoad(String[] args) throws IOException
    {
        if (args.length != 3)
        {
            System.err.println("Usage: load <indexfile> <csvfile>");
            System.exit(1);
        }

        String indexFilename = args[1];
        String csvFilename = args[2];

        File indexFile = new File(indexFilename);
        File csvFile = new File(csvFilename);

        // If !indexFile.exists(), then error
        if (!indexFile.exists())
        {
            System.err.println("Error: Index file does not exist.");
            System.exit(1);
        }

        // If !csvFile.exists(), then error
        if (!csvFile.exists())
        {
            System.err.println("Error: CSV file does not exist.");
            System.exit(1);
        }

        BTree btree = new BTree(indexFilename);
        try (Scanner scanner = new Scanner(csvFile))
        {
            while (scanner.hasNextLine())
            {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length != 2)
                {
                    System.err.println("Skipping invalid line: " + line);
                    continue;
                }

                long key = Long.parseLong(parts[0].trim());
                long value = Long.parseLong(parts[1].trim());
                btree.insert(key, value);
            }
        }
        btree.close();
        System.out.println("CSV file loaded into index: " + csvFilename);
    }

    // Print
    private static void handlePrint(String[] args) throws IOException
    {
        if (args.length != 2)
        {
            System.err.println("Usage: print <indexfile>");
            System.exit(1);
        }

        String filename = args[1];
        File file = new File(filename);
        if (!file.exists())
        {
            System.err.println("Error: Index file does not exist.");
            System.exit(1);
        }

        BTree btree = new BTree(filename);
        printAll(btree, btree.getRootID());
        btree.close();
    }

    // Traverse BTree
    private static void printAll(BTree btree, long nodeID) throws IOException
    {
        BTreeNode node = btree.readNode(nodeID);

        for (int i = 0; i < node.getNumKeys(); i++)
        {
            if (!node.isLeaf())
                printAll(btree, node.getChild(i));
            System.out.println(node.getKey(i) + "," + node.getValue(i));
        }

        if (!node.isLeaf())
            printAll(btree, node.getChild(node.getNumKeys()));
    }

    // Save to CSV File
    private static void handleExtract(String[] args) throws IOException
    {
        if (args.length != 3)
        {
            System.err.println("Usage: extract <indexfile> <csvfile>");
            System.exit(1);
        }

        String indexFilename = args[1];
        String csvFilename = args[2];

        File indexFile = new File(indexFilename);
        File csvFile = new File(csvFilename);

        if (!indexFile.exists())
        {
            System.err.println("Error: Index file does not exist.");
            System.exit(1);
        }

        if (csvFile.exists())
        {
            System.err.println("Error: CSV output file already exists.");
            System.exit(1);
        }

        BTree btree = new BTree(indexFilename);
        try (PrintWriter pw = new PrintWriter(csvFile))
        {
            writeAllCSV(btree, btree.getRootID(), pw);
        }
        btree.close();
        System.out.println("BTree extracted to CSV: " + csvFilename);
    }

    // Traverse BTree in-order and write to CSV
    private static void writeAllCSV(BTree btree, long nodeID, PrintWriter pw) throws IOException
    {
        BTreeNode node = btree.readNode(nodeID);

        for (int i = 0; i < node.getNumKeys(); i++)
        {
            if (!node.isLeaf())
                writeAllCSV(btree, node.getChild(i), pw);
            pw.println(node.getKey(i) + "," + node.getValue(i));
        }

        if (!node.isLeaf())
            writeAllCSV(btree, node.getChild(node.getNumKeys()), pw);
    }
}
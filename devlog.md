12/7/2025 8:30PM

There will be six files: IndexFile.java, BTree.java, BTreeNode.java, BTreeUtilities.java, IndexFileManager.java, and the NodeCache.java.

The IndexFile.java handles the command line arguments. It runs commands such as create, insert, search, load, print, and extract.

The BTree.java implements the BTree logic. It handles insertion and searching, and will split nodes. It will work with the IndexFileManager.java and the NodeCache.java.

BTreeNode.java represents a node in the tree. Each node in this file will store keys, values, etc. This file also has methods that will serialize and deserialize to a 512 byte block for disk storage.

BTreeUtilities.java is a helper for traversing the B-Tree. It has functions to print the tree in order, and write the tree contents to the CSV file.

IndexFileManager.java manages the index file. It handles the block allocation, header, reading and writing blocks, and has methods to read and write BTreeNodes.

Lastly, NodeCache.java implements a cache for nodes. It keeps used nodes in memory, and marks them when modified, and then writes them to the disk when flushed. 
# CS4348Project3

This project implements a disk based B-Tree index system in Java. There are 6 files.

IndexFile.java is the main program, which handles all command line interactions.
BTree.java uses B-Tree logic, and splits nodes, searches for nodes, and inserts nodes.
BTreeNode.java stores keys, values, child pointers, and does the serialization and deserialization.
BTreeUtilities.java prints the tree in order, and writes the index contents into a CSV file.
IndexFileManager.java manages the header block, block allocation, reading and writing the blocks, and converting nodes in the disk.
Lastly, NodeCache.java keeps nodes that have already been accessed in memory. It also writes the dirty nodes in the disk when the disk is flushed.

How to Compile:

In the terminal type in: javac *.java

Create an index file: java IndexFile create myindex.idx

Insert a key/pair value: for example: java IndexFile insert myindex.idx 42 1000

Search for a key: for example: java IndexFile search myindex.idx 42

Print the index: java IndexFile print myindex.idx

Load key/value pairs from the CSV file: java IndexFile load myindex.idx input.csv

Extract the index into a CSV file: java IndexFile extract myindex.idx output.csv
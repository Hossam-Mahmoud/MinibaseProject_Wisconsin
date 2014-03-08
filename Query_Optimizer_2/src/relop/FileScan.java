package relop;

import global.RID;
import heap.HeapFile;
import heap.HeapScan;

/**
 * Wrapper for heap file scan, the most basic access method. This "iterator"
 * version takes schema into consideration and generates real tuples.
 */
public class FileScan extends Iterator {
	private HeapFile heap_file;
	private HeapScan heap_scan;
	private RID currRID;
	
  /**
   * Constructs a file scan, given the schema and heap file.
   */
  public FileScan(Schema schema, HeapFile file) {
	  setSchema(schema);
	  heap_file = file;
	  heap_scan = heap_file.openScan();
	  currRID = new RID();
	  
  }

  /**
   * Gives a one-line explaination of the iterator, repeats the call on any
   * child iterators, and increases the indent depth along the way.
   */
  public void explain(int depth) {
	  System.out.println("FileScan : "+heap_file.toString());
  }

  /**
   * Restarts the iterator, i.e. as if it were just constructed.
   */
  public void restart() {
	 heap_scan.close();
	 heap_scan = heap_file.openScan();
  }

  /**
   * Returns true if the iterator is open; false otherwise.
   */
  public boolean isOpen() {
	  return heap_scan != null;
  }

  /**
   * Closes the iterator, releasing any resources (i.e. pinned pages).
   */
  public void close() {
	  heap_scan.close();
  }

  /**
   * Returns true if there are more tuples, false otherwise.
   */
  public boolean hasNext() {
	  return heap_scan.hasNext();
  }

  /**
   * Gets the next tuple in the iteration.
   * 
   * @throws IllegalStateException if no more tuples
   */
  public Tuple getNext() {
	  byte[] data = heap_scan.getNext(currRID);
	  return new Tuple(getSchema(), data);
  }

  /**
   * Gets the RID of the last tuple returned.
   */
  public RID getLastRID() {
	  return new RID(currRID);
  }

} // public class FileScan extends Iterator

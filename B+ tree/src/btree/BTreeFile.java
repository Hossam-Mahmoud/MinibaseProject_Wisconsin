package btree;

import global.PageId;
import global.RID;
import global.SystemDefs;
import heap.InvalidSlotNumberException;
import heap.Tuple;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Stack;

import bufmgr.BufMgrException;
import bufmgr.BufferPoolExceededException;
import bufmgr.HashEntryNotFoundException;
import bufmgr.HashOperationException;
import bufmgr.InvalidBufferException;
import bufmgr.InvalidFrameNumberException;
import bufmgr.PageNotReadException;
import bufmgr.PagePinnedException;
import bufmgr.PageUnpinnedException;
import bufmgr.ReplacerException;
import diskmgr.DiskMgrException;
import diskmgr.DuplicateEntryException;
import diskmgr.FileEntryNotFoundException;
import diskmgr.FileIOException;
import diskmgr.FileNameTooLongException;
import diskmgr.InvalidPageNumberException;
import diskmgr.InvalidRunSizeException;
import diskmgr.OutOfSpaceException;

/* btfile.java This is the main definition of class BTreeFile,
 * which derives from abstract base class IndexFile. 
 * It provides an insert/delete interface. 
 */

public class BTreeFile extends IndexFile {
	private BTHeaderPage headerPage;// header page storing info about the tree
	private String fileName;// name of the index file
	private BTSortedPage rootPage;// root page
	private FileOutputStream log;// log file to trace the tree
	private boolean closed = false;

	/*
	 * BTreeFile class an index file with given filename, if index file exists,
	 * open it, else create it.
	 */
	public BTreeFile(String string, int keytype, int keysize, int delete_fashion)
			throws ConstructPageException, FileIOException,
			InvalidPageNumberException, DiskMgrException,
			FileNameTooLongException, InvalidRunSizeException,
			DuplicateEntryException, OutOfSpaceException, ReplacerException,
			PageUnpinnedException, HashEntryNotFoundException,
			InvalidFrameNumberException, IOException, HashOperationException,
			PageNotReadException, BufferPoolExceededException,
			PagePinnedException, BufMgrException, KeyNotMatchException,
			NodeNotMatchException, ConvertException {

		fileName = string;

		if (string == null) {// no file yet in the directory
			fileName = string = "temp";// setting it to its default name "temp"
		}

		PageId firstPageId;

		// Check if a file with the given name doesn't exist in the DB
		// then add it in the DB
		if ((firstPageId = SystemDefs.JavabaseDB.get_file_entry(fileName)) == null) {
			headerPage = new BTHeaderPage(keytype, keysize);
			rootPage = new BTLeafPage(keytype);
			headerPage.setRootId(new PageId(rootPage.getCurPage().pid));

			// adding file to the DB
			SystemDefs.JavabaseDB.add_file_entry(fileName, headerPage
					.getCurPage());

			SystemDefs.JavabaseBM.unpinPage(rootPage.getCurPage(), true);
			if (log != null) {
				log.write((" Creating a new file: \"" + fileName + "\"\n")
						.getBytes());
			}
		} else {
			if (log != null) {
				log
						.write(("File already exists, loading file: \""
								+ fileName + "\"\n").getBytes());
			}
			// loading stored file data
			initiateRoot(firstPageId);
		}
		if (log != null)
			log.write((" Header page info: [Key_type: "
					+ headerPage.getKeyType() + ", root page ID: "
					+ headerPage.getRootId().pid + ", key_max: "
					+ headerPage.getMaxFieldSize() + "\n\n").getBytes());
	}

	/*
	 * BTreeFile class an index file with given filename should already exist;
	 * this opens it.
	 */
	public BTreeFile(String string) throws ReplacerException,
			HashOperationException, PageUnpinnedException,
			InvalidFrameNumberException, PageNotReadException,
			BufferPoolExceededException, PagePinnedException, BufMgrException,
			IOException, FileIOException, InvalidPageNumberException,
			DiskMgrException, HashEntryNotFoundException,
			ConstructPageException {
		fileName = string;
		PageId tempId = SystemDefs.JavabaseDB.get_file_entry(fileName);

		// loading file data from DB
		initiateRoot(tempId);
		if (log != null) {
			log
					.write((" File already exists, loading file: \"" + fileName + "\"\n\n")
							.getBytes());
		}
	}

	/*
	 * Load data stored in the first page of the file
	 */
	private void initiateRoot(PageId firstPageId) throws ReplacerException,
			HashOperationException, PageUnpinnedException,
			InvalidFrameNumberException, PageNotReadException,
			BufferPoolExceededException, PagePinnedException, BufMgrException,
			IOException, HashEntryNotFoundException, ConstructPageException {

		// retrieving data about key type, length of the key, and root page ID
		// from the header page
		headerPage = new BTHeaderPage(firstPageId);

		// assigns rootPage pointer to the root stored in file
		rootPage = new BTSortedPage(new PageId(headerPage.getRootId().pid),
				headerPage.getKeyType());

		SystemDefs.JavabaseBM.unpinPage(rootPage.getCurPage(), true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see btree.IndexFile#insert(btree.KeyClass, global.RID)
	 */
	@Override
	public void insert(KeyClass data, RID rid) {
		KeyDataEntry insEntry = new KeyDataEntry(data, rid);
		try {
			rootPage = new BTSortedPage(headerPage.getRootId(), headerPage
					.getKeyType());
			if (rootPage.getType() == NodeType.LEAF) {
				// check if leaf page is full
				if (rootPage.insertRecord(insEntry) == null) {

					// allocating a new root page for the tree
					BTIndexPage newRoot = new BTIndexPage(headerPage
							.getKeyType());

					// Splitting current leaf node
					PageId lft = rootPage.getCurPage();
					PageId rgt = (new BTLeafPage(headerPage.getKeyType()))
							.getCurPage();
					KeyClass middleKey = splitLeaf(lft, rgt, insEntry);

					// assign new root pointers
					newRoot.setLeftLink(lft);
					newRoot.insertRecord(new KeyDataEntry(middleKey, rgt));
					SystemDefs.JavabaseBM
							.unpinPage(rootPage.getCurPage(), true);

					// advance rootPage pointer to the newRoot
					rootPage = newRoot;
					headerPage.setRootId(rootPage.getCurPage());
					SystemDefs.JavabaseBM.unpinPage(rgt, true);

					if (log != null)
						log.write(("A new tree level has been added, pageID: "
								+ rootPage.getCurPage() + "\n").getBytes());
				} else {
					if (log != null)
						log.write(("Inserting [" + data + "] into page of ID: "
								+ rootPage.getCurPage() + "\n").getBytes());
				}
			} else {
				// find leaf page to insert into
				Stack<PageId> st = new Stack<PageId>();

				// iteratorPage used to iterate over pages in the tree till a
				// leaf node is found
				BTSortedPage iteratorPage = new BTSortedPage(rootPage
						.getCurPage(), headerPage.getKeyType());

				KeyDataEntry itEntry = null;
				// traverse the tree finding the correct leaf
				while (iteratorPage.getType() != NodeType.LEAF) {
					st.push(iteratorPage.getCurPage());
					RID itRID = new RID();
					BTIndexPage idxItPage = new BTIndexPage(iteratorPage,
							headerPage.getKeyType());
					// itEntry is the page entry object used for iteration
					itEntry = idxItPage.getFirst(itRID);

					PageId nxtPageID = null;

					if (BT.keyCompare(data, itEntry.key) < 0) {
						nxtPageID = idxItPage.getLeftLink();
					} else {
						// we need an additional entry object to check
						// boundaries
						KeyDataEntry tempEntry = new KeyDataEntry(itEntry.key,
								itEntry.data);

						while (true) {
							if ((itEntry = idxItPage.getNext(itRID)) != null) {
								if (BT.keyCompare(tempEntry.key, data) <= 0
										&& BT.keyCompare(itEntry.key, data) > 0) {
									// log.write((data + " is between " +
									// + tempEntry.data + ", ").getBytes());
									break;
								} else {
									// if (log != null)
									// log.write((data + " cannot go into "
									// + tempEntry.data + ", ")
									// .getBytes());
									tempEntry.key = itEntry.key;
									tempEntry.data = itEntry.data;
								}
							} else {
								// if (log != null)
								// log.write((data + " must go into "
								// + tempEntry.data + " coz no more, ")
								// .getBytes());
								break;
							}

						}
						nxtPageID = ((IndexData) tempEntry.data).getData();
					}
					SystemDefs.JavabaseBM.unpinPage(iteratorPage.getCurPage(),
							false);
					iteratorPage = new BTSortedPage(nxtPageID, headerPage
							.getKeyType());
				}

				// try to insert into the selected leaf page
				if (iteratorPage.insertRecord(insEntry) == null) {
					// if this fails, split the leaf page
					PageId lft = iteratorPage.getCurPage();
					PageId rgt = (new BTLeafPage(headerPage.getKeyType()))
							.getCurPage();
					KeyClass middleKey = splitLeaf(lft, rgt, insEntry);

					SystemDefs.JavabaseBM.unpinPage(rgt, true);

					PageId parentId = st.pop();
					BTSortedPage parentPage = new BTSortedPage(parentId,
							headerPage.getKeyType());

					KeyDataEntry newIdxEntry = new KeyDataEntry(middleKey, rgt);
					// try to add the new index entry to the parent node
					while (parentPage.insertRecord(newIdxEntry) == null) {
						// if parent node full, split it
						lft.copyPageId(parentId);
						rgt
								.copyPageId((new BTIndexPage(headerPage
										.getKeyType()).getCurPage()));
						// do split and insert index entry to the correct node
						middleKey = splitIndex(lft, rgt, newIdxEntry);
						// carry up a new index entry for insertion
						newIdxEntry = new KeyDataEntry(middleKey, rgt);

						SystemDefs.JavabaseBM.unpinPage(rgt, true);
						SystemDefs.JavabaseBM.unpinPage(parentId, true);

						if (!st.empty()) {
							// if has parent, it's next to try to insert into
							parentId = st.pop();
						} else {
							// else, create a new root page
							BTIndexPage newRoot = new BTIndexPage(headerPage
									.getKeyType());
							newRoot.setLeftLink(lft);
							SystemDefs.JavabaseBM.unpinPage(rootPage
									.getCurPage(), true);
							rootPage = newRoot;
							headerPage.setRootId(rootPage.getCurPage());
							parentId = rootPage.getCurPage();
							if (log != null)
								log
										.write(("A new tree level has been added, pageID: "
												+ rootPage.getCurPage() + "\n")
												.getBytes());
						}
						parentPage = new BTSortedPage(parentId, headerPage
								.getKeyType());
					}
					SystemDefs.JavabaseBM.unpinPage(parentId, true);
				} else {
					if (log != null)
						log.write(("Inserting [" + data + "] into page of ID: "
								+ iteratorPage.getCurPage() + "\n").getBytes());
				}
				SystemDefs.JavabaseBM
						.unpinPage(iteratorPage.getCurPage(), true);
			}
			SystemDefs.JavabaseBM.unpinPage(rootPage.getCurPage(), true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private KeyClass splitIndex(PageId lftId, PageId rgtId,
			KeyDataEntry newIdxEntry) throws ConstructPageException,
			IOException, InvalidSlotNumberException, KeyNotMatchException,
			NodeNotMatchException, ConvertException, InsertRecException,
			DeleteRecException, ReplacerException, PageUnpinnedException,
			HashEntryNotFoundException, InvalidFrameNumberException {
		BTIndexPage lft = new BTIndexPage(lftId, headerPage.getKeyType());
		BTIndexPage rgt = new BTIndexPage(rgtId, headerPage.getKeyType());
		int cnt = lft.getSlotCnt();
		int i = 0;
		RID endRID = lft.firstRecord();
		// skip half the records
		while (i < cnt / 2) {
			endRID = lft.nextRecord(endRID);
			i++;
		}

		RID itRID;
		KeyClass middleKey = null;
		// move the second half
		i = 0;
		while ((itRID = lft.nextRecord(endRID)) != null) {
			i++;
			Tuple row = lft.getRecord(itRID);
			KeyDataEntry tmp = BT.getEntryFromBytes(row.getTupleByteArray(),
					row.getOffset(), row.getLength(), headerPage.getKeyType(),
					NodeType.INDEX);
			if (middleKey == null) {
				// move up the middle key
				middleKey = tmp.key;
				rgt.setLeftLink(((IndexData) tmp.data).getData());
				if (log != null)
					log.write(("Splitting index page:" + lftId + " around ["
							+ middleKey + "] into page:" + rgtId).getBytes());
			} else {
				rgt.insertRecord(tmp);
				lft.deleteSortedRecord(itRID);
			}
		}
		if (log != null)
			log.write((" moved " + i + " index records\n").getBytes());

		// insert the new entry into the correct page
		if (BT.keyCompare(newIdxEntry.key, middleKey) < 0) {
			lft.insertRecord(newIdxEntry);
		} else {
			rgt.insertRecord(newIdxEntry);
		}
		SystemDefs.JavabaseBM.unpinPage(lftId, true);
		SystemDefs.JavabaseBM.unpinPage(rgtId, true);
		return middleKey;
	}

	private KeyClass splitLeaf(PageId L1, PageId L2, KeyDataEntry newEntry)
			throws ConstructPageException, IOException,
			InvalidSlotNumberException, KeyNotMatchException,
			NodeNotMatchException, ConvertException, InsertRecException,
			DeleteRecException, ReplacerException, PageUnpinnedException,
			HashEntryNotFoundException, InvalidFrameNumberException {
		BTLeafPage lft = new BTLeafPage(L1, headerPage.getKeyType());
		BTLeafPage rgt = new BTLeafPage(L2, headerPage.getKeyType());
		// update the linked list
		rgt.setNextPage(lft.getNextPage());
		rgt.setPrevPage(lft.getCurPage());
		lft.setNextPage(rgt.getCurPage());
		int cnt = lft.getSlotCnt();
		int i = 0;
		RID endRID = lft.firstRecord();
		// skip half the records
		while (i < cnt / 2) {
			endRID = lft.nextRecord(endRID);
			i++;
		}

		RID itRID;
		KeyClass middleKey = null;
		i = 0;
		// move the second half
		while ((itRID = lft.nextRecord(endRID)) != null) {
			i++;
			KeyDataEntry tmp = lft.getCurrent(itRID);
			if (middleKey == null) {
				middleKey = tmp.key;
				if (log != null)
					log.write(("Splitting leaf page:" + L1 + " around ["
							+ middleKey + "] into page:" + L2).getBytes());
			} else {
				// log.write(("Moving " + tmp.key + " from " + L1 + "to " + L2 +
				// "\n")
				// .getBytes());
			}
			rgt.insertRecord(tmp);
			lft.deleteSortedRecord(itRID);

		}

		if (log != null)
			log.write((" moved " + i + " records\n").getBytes());

		// insert new entry into correct page
		if (BT.keyCompare(newEntry.key, middleKey) < 0) {
			lft.insertRecord(newEntry);
			if (log != null)
				log.write((" Inserting " + newEntry.key + " into page of ID"
						+ lft.getCurPage() + "\n").getBytes());
		} else {
			rgt.insertRecord(newEntry);
			if (log != null)
				log.write((" Inserting " + newEntry.key + " into page of ID"
						+ rgt.getCurPage() + "\n").getBytes());
		}
		SystemDefs.JavabaseBM.unpinPage(L1, true);
		SystemDefs.JavabaseBM.unpinPage(L2, true);
		return middleKey;
	}

	/**
	 * delete leaf entry given its pair. `rid' is IN the data entry; it is not
	 * the id of the data entry)
	 */
	@Override
	public boolean Delete(KeyClass data, RID rid) {
		boolean found = false;
		try {
			PageId leafPageId = traverseTree(data);

			// pinning the leaf page given its ID
			BTLeafPage leafPage = new BTLeafPage(leafPageId, headerPage
					.getKeyType());

			if (log != null)
				log.write((" Deleting [" + data + "]: ").getBytes());

			// delete entry, found=true if successfully deleted, otherwise false
			found = leafPage.delEntry(new KeyDataEntry(data, rid));

			if (log != null)
				log.write(((found) ? ("success\n")
						: ("failed, key not found\n")).getBytes());
			SystemDefs.JavabaseBM.unpinPage(leafPageId, true);
		} catch (Exception e) {
			// error occured while deleting
			if (log != null)
				try {
					log.write((" Error occured\n\t" + e.getStackTrace() + "\n")
							.getBytes());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			return false;
		}
		return found;
	}

	/**
	 * Close the B+ tree file. Unpin header page.
	 * 
	 * @throws ReplacerException
	 * @throws PageUnpinnedException
	 * @throws HashEntryNotFoundException
	 * @throws InvalidFrameNumberException
	 * @throws IOException
	 */
	public void close() throws ReplacerException, PageUnpinnedException,
			HashEntryNotFoundException, InvalidFrameNumberException,
			IOException {
		// Unpinning page as dirty as it was modified
		if (headerPage != null) {
			closed = true;
			SystemDefs.JavabaseBM.unpinPage(headerPage.getCurPage(), true);
			headerPage = null;
			if (log != null) {
				log.write((" Closing file ...\n").getBytes());
				log.close();
			}
		}
	}
	
	boolean isClosed(){
		return closed;
	}
	
	public void writeToLog(String s){
		if(log != null){
			try {
				log.write(s.getBytes());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/*
	 * Destroy entire B+ tree file.
	 */
	public void destroyFile() throws IOException, FileEntryNotFoundException,
			FileIOException, InvalidPageNumberException, DiskMgrException,
			CloneNotSupportedException, ConstructPageException,
			InvalidBufferException, ReplacerException, HashOperationException,
			InvalidFrameNumberException, PageNotReadException,
			BufferPoolExceededException, PagePinnedException,
			PageUnpinnedException, HashEntryNotFoundException, BufMgrException,
			KeyNotMatchException, NodeNotMatchException, ConvertException,
			InvalidSlotNumberException {
		if (log != null)
			log.write(("\n Destroying all data ...\n").getBytes());

		if (headerPage != null) {// if it's not already destroyed or closed

			// destroy all pages in the tree
			_deleteAllPages(headerPage.getRootId());

			// deleting file references from DB
			SystemDefs.JavabaseDB.delete_file_entry(fileName);

			// freeing header page
			SystemDefs.JavabaseBM.unpinPage(headerPage.getCurPage(), false);
			SystemDefs.JavabaseBM.freePage(headerPage.getCurPage());
			headerPage = null;
		}
		if (log != null) {
			log.write(("\n Data successfully destroyed.\n").getBytes());
			log.close();
			log = null;
		}
	}

	/*
	 * Deletes all pages in the tree as it will be totally destructed.
	 */
	private void _deleteAllPages(PageId pid) throws InvalidBufferException,
			ReplacerException, HashOperationException,
			InvalidFrameNumberException, PageNotReadException,
			BufferPoolExceededException, PagePinnedException,
			PageUnpinnedException, HashEntryNotFoundException, BufMgrException,
			DiskMgrException, IOException, KeyNotMatchException,
			NodeNotMatchException, ConvertException,
			InvalidSlotNumberException, ConstructPageException {

		BTSortedPage sortedPage = new BTSortedPage(pid, headerPage.getKeyType());

		if (sortedPage.getType() == NodeType.LEAF) {
			freeCurrentPage(sortedPage.getCurPage());
			return;
		}
		RID first = new RID();
		BTIndexPage idxItPage = new BTIndexPage(sortedPage, headerPage
				.getKeyType());

		// deleting left link page first
		_deleteAllPages(idxItPage.getLeftLink());

		KeyDataEntry entry = idxItPage.getFirst(first);
		PageId tempId = new PageId();
		// Depth first searching along all pages in the tree
		while ((entry = idxItPage.getNext(first)) != null) {
			tempId = ((IndexData) (entry.data)).getData();
			_deleteAllPages(tempId);
		}

		freeCurrentPage(pid);// free current index page
	}

	/*
	 * totally free a page, after unpinning it
	 */
	private void freeCurrentPage(PageId id) throws ReplacerException,
			PageUnpinnedException, HashEntryNotFoundException,
			InvalidFrameNumberException, InvalidBufferException,
			HashOperationException, PageNotReadException,
			BufferPoolExceededException, PagePinnedException, BufMgrException,
			DiskMgrException, IOException {
		SystemDefs.JavabaseBM.unpinPage(id, false);
		SystemDefs.JavabaseBM.freePage(id);
	}

	/**
	 * A tree structured trace to be written in a log file
	 * 
	 * @param string
	 * @throws IOException
	 */
	public void traceFilename(String string) throws IOException {
		try {
			// opening a new output stream to write tree trace
			log = new FileOutputStream(string);
			log
					.write(("\n------------Starting a trace on file \""
							+ fileName + "\"\n\n").getBytes());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method open a scan on the BTree file to scan the records on the
	 * range given
	 * 
	 * @param lowkey
	 *            The low key, minimum key if lowKey equals null
	 * @param hikey
	 *            The high key, maximum key if lowKey equals null
	 * @return A new scan object
	 * @throws ConstructPageException
	 * @throws ReplacerException
	 * @throws HashOperationException
	 * @throws PageUnpinnedException
	 * @throws InvalidFrameNumberException
	 * @throws PageNotReadException
	 * @throws BufferPoolExceededException
	 * @throws PagePinnedException
	 * @throws BufMgrException
	 * @throws KeyNotMatchException
	 * @throws NodeNotMatchException
	 * @throws ConvertException
	 * @throws InvalidSlotNumberException
	 * @throws HashEntryNotFoundException
	 * @throws IOException
	 */
	public BTFileScan new_scan(KeyClass lowkey, KeyClass hikey)
			throws ConstructPageException, ReplacerException,
			HashOperationException, PageUnpinnedException,
			InvalidFrameNumberException, PageNotReadException,
			BufferPoolExceededException, PagePinnedException, BufMgrException,
			KeyNotMatchException, NodeNotMatchException, ConvertException,
			InvalidSlotNumberException, HashEntryNotFoundException, IOException {

		PageId lowestOne = null;
		PageId highestOne = null;
		if (lowkey == null) {
			// get the first leaf page in the BTree file
			BTSortedPage tmpPage = new BTSortedPage(headerPage.getRootId(),
					headerPage.getKeyType());
			PageId pgId = tmpPage.getCurPage();
			while (tmpPage.getType() != NodeType.LEAF) {
				pgId = (new BTIndexPage(tmpPage, headerPage.getKeyType()))
						.getLeftLink();
				SystemDefs.JavabaseBM.unpinPage(tmpPage.getCurPage(), false);
				tmpPage = new BTSortedPage(pgId, headerPage.getKeyType());
			}
			lowestOne = pgId;
			BTLeafPage leaf = new BTLeafPage(tmpPage, headerPage.getKeyType());
			// get the first key in the first leaf page
			lowkey = (leaf).getCurrent(tmpPage.firstRecord()).key;
			SystemDefs.JavabaseBM.unpinPage(pgId, false);
		} else
			// find the leaf page which should contain the lowest key in range
			lowestOne = traverseTree(lowkey);
		if (hikey == null) {
			// get the last page in the BTree file
			BTSortedPage tmpPage = new BTSortedPage(headerPage.getRootId(),
					headerPage.getKeyType());
			PageId pgId = tmpPage.getCurPage();
			while (tmpPage.getType() != NodeType.LEAF) {
				pgId = (new BTIndexPage(tmpPage, headerPage.getKeyType()))
						.getLeftLink();
				SystemDefs.JavabaseBM.unpinPage(tmpPage.getCurPage(), false);
				tmpPage = new BTSortedPage(pgId, headerPage.getKeyType());
			}
			while (tmpPage.getNextPage().pid > 0) {
				pgId = tmpPage.getNextPage();
				SystemDefs.JavabaseBM.unpinPage(tmpPage.getCurPage(), false);
				tmpPage = new BTSortedPage(pgId, headerPage.getKeyType());
			}
			highestOne = pgId;

			BTLeafPage tmpPage2 = new BTLeafPage(tmpPage, headerPage
					.getKeyType());
			RID recId = new RID();
			hikey = tmpPage2.getFirst(recId).key;
			while (recId != null) {
				hikey = tmpPage2.getCurrent(recId).key;
				recId = tmpPage2.nextRecord(recId);
			}
			SystemDefs.JavabaseBM.unpinPage(pgId, false);

		} else
			// find the page which should contain the highest key in range
			highestOne = traverseTree(hikey);

		return new BTFileScan(this, lowestOne, highestOne, headerPage.getKeyType(),
				headerPage.getMaxFieldSize(), lowkey, hikey);
	}

	/**
	 * @return pointer to the header page
	 */
	public BTHeaderPage getHeaderPage() {
		return headerPage;
	}

	/*
	 * Traverse the tree searching for a page that may contain the given key
	 */
	private PageId traverseTree(KeyClass key) throws ConstructPageException,
			ReplacerException, HashOperationException, PageUnpinnedException,
			InvalidFrameNumberException, PageNotReadException,
			BufferPoolExceededException, PagePinnedException, BufMgrException,
			IOException, KeyNotMatchException, NodeNotMatchException,
			ConvertException, InvalidSlotNumberException,
			HashEntryNotFoundException {

		// writing down traversal operation in the log file
		if (log != null) {
			log.write(("\n Traversing tree searching for [").getBytes());
			if (key instanceof IntegerKey) {
				log.write((((IntegerKey) key).getKey().intValue() + "] :")
						.getBytes());
			} else {
				log.write((((StringKey) key).getKey() + "] :").getBytes());
			}
		}

		// iterator page used to iterate over pages in the tree
		BTSortedPage iteratorPage = new BTSortedPage(headerPage.getRootId(),
				headerPage.getKeyType());

		KeyDataEntry entry = null;
		while (iteratorPage.getType() != NodeType.LEAF) {

			RID first = new RID();
			BTIndexPage idxItPage = new BTIndexPage(iteratorPage, headerPage
					.getKeyType());
			entry = idxItPage.getFirst(first);

			// id of the page representing the most left child of the current
			// BTIndex
			PageId newPageID = idxItPage.getLeftLink();

			if (BT.keyCompare(key, entry.key) < 0) {// go to left
				SystemDefs.JavabaseBM.unpinPage(iteratorPage.getCurPage(),
						false);
				iteratorPage = new BTSortedPage(newPageID, headerPage
						.getKeyType());
			} else {
				KeyDataEntry tempEntry = new KeyDataEntry(entry.key, entry.data);

				// iterate over records in the page
				while ((entry = idxItPage.getNext(first)) != null) {
					if (BT.keyCompare(tempEntry.key, key) <= 0
							&& BT.keyCompare(entry.key, key) > 0) {
						break;
					} else {
						tempEntry.key = entry.key;
						tempEntry.data = entry.data;
					}
				}
				// advancing iterator page pointer to the next page
				newPageID = ((IndexData) tempEntry.data).getData();
				SystemDefs.JavabaseBM.unpinPage(iteratorPage.getCurPage(),
						false);
				iteratorPage = new BTSortedPage(newPageID, headerPage
						.getKeyType());
			}
		}

		// ID of the page which may stores the key value
		PageId ans = new PageId(iteratorPage.getCurPage().pid);
		if (log != null) {
			log.write((" concluded with Leaf page of ID: " + ans.pid + "\n")
					.getBytes());
		}
		SystemDefs.JavabaseBM.unpinPage(iteratorPage.getCurPage(), false);
		return ans;
	}
}

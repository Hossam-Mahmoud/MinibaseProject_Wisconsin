package bufmgr;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import global.PageId;
import global.SystemDefs;
import diskmgr.DiskMgrException;
import diskmgr.FileIOException;
import diskmgr.FreePageException;
import diskmgr.InvalidPageNumberException;
import diskmgr.InvalidRunSizeException;
import diskmgr.Page;
import diskmgr.PageUnpinnedException;

public class BufMgr {
	private Page[] bufPool;
	private descriptors[] bufDescr;
	private Queue<Integer> queue;
	private int numbufs;
	private HashTable<Integer, Integer> directory;

	/**
	 * Create the BufMgr object.
	 * 
	 * @param numbufs
	 *            number of buffers in the buffer pool.
	 * @param replacerArg
	 *            name of the buffer replacement policy.
	 */
	public BufMgr(int numbufs, String replacerArg) {
		// Allocate pages (frames) for the buffer pool in main memory
		this.numbufs = numbufs;
		bufPool = new Page[numbufs];
		bufDescr = new descriptors[numbufs];
		directory = new HashTable<Integer, Integer>();
		// make the buffer manage aware that the replacement policy is
		// specified by replacerArg (i.e. Clock, LRU, MRU etc.)
		//TODO handle policies
		if (replacerArg.charAt(0) == 'L') {
			//LRU POLICY
		} else {
			if (replacerArg.charAt(0) == 'C') {
			queue = new LinkedList<Integer>();
			initializeQueue();
			}
		}
	}

	/*
	 * Initializing the queue in beginning with all index of the array
	 */
	public void initializeQueue() {
		for (int i = 0; i < numbufs; i++)
			queue.add(i);
	}

	/*
	 * Return the first empty frame if the buffer is full throw an exception
	 */
	public int getFirstEmptyFrame() throws BufferPoolExceededException {
		if (queue.size() == 0)
			throw new BufferPoolExceededException(null, "BUFFER_POOL_EXCEED");
		else
			return queue.poll();
	}

	/*
	 * Check whether the buffer is full or not !
	 */
	public boolean isFull() {
		return (queue.size() == 0);
	}

	/*
	 * Return the frame that contains the given page id
	 */
	private int getFrameNumber(PageId pId) throws HashEntryNotFoundException {
		if (directory.conatin(pId.pid))
			return directory.get(pId.pid);
		else {
			throw new HashEntryNotFoundException(null,
					"BUF_MNGR:HASH_ENTRY_NOT_FOUND_EXCEPTION");
		}
	}

	/**
	 * @param Page_Id_in_a_DB
	 *            page number in the minibase.
	 * @param page
	 *            the pointer point to the page.
	 * @param emptyPage
	 *            true (empty page); false (non-empty page)
	 */
	public void pinPage(PageId pageno, Page page, boolean emptyPage)
			throws DiskMgrException, BufferPoolExceededException,
			PagePinnedException, InvalidPageNumberException, FileIOException,
			IOException, HashEntryNotFoundException 
		{
		// First check if this page is already in the buffer pool.
		boolean found = directory.conatin(pageno.pid);
		if (found) 
		{
			int index = directory.get(pageno.pid);
			// If the pin_count was 0 before the call, the page was a
			// replacement candidate, but is no longer a candidate.
			if (bufDescr[index].getPin_count() == 0)
				queue.remove(index);
			// If it is, increment the pin_count and return a pointer to this
			// page.
			bufDescr[index].setPin_count(bufDescr[index].getPin_count() + 1);
			page.setpage(bufPool[index].getpage());
		}
		// If the page is not in the pool,
		else 
		{
			int index = -1;
			if (queue.size() != 0) 
			{
				// choose a frame (from the
				// set of replacement candidates) to hold this page
				index = getFirstEmptyFrame();
				// Also, must write out the old page in chosen frame if it is
				// dirty before reading new page.
				if ((bufDescr[index] != null) && bufDescr[index].isDirtyBit()) {
					flushPage(bufDescr[index].getPageNumber());
					directory.remove(bufDescr[index].getPageNumber().pid);
				}

			} 
			else
				throw new BufferPoolExceededException(null,
						"BUFMGR:PAGE_PIN_FAILED");
			Page temp = new Page();
			try {
				// read the page(using the appropriate method from 
				//{\em diskmgr} package)
				SystemDefs.JavabaseDB.read_page(new PageId(pageno.pid), temp);
			} catch (Exception e) {
				throw new DiskMgrException(e, "DB.java: pinPage() failed");
			}
			// and pin it.
			//bufPool[index] = new Page();
			//bufPool[index].setpage((temp.getpage().clone()));
			bufPool[index]=temp;
			page.setpage(bufPool[index].getpage());
			bufDescr[index] = new descriptors(1, new PageId(pageno.pid), false);
			directory.put(pageno.pid, index);
		}
	}

	/**
	 * Unpin a page specified by a pageId.
	 * 
	 * @param globalPageId_in_a_DB
	 *            page number in the minibase.
	 * @param dirty
	 *            the dirty bit of the frame
	 * @throws DiskMgrException
	 */

	public void unpinPage(PageId pageno, boolean dirty)
			throws PageUnpinnedException, HashEntryNotFoundException,
			InvalidPageNumberException, FileIOException, IOException,
			DiskMgrException {

		if (directory.conatin(pageno.pid))
		{
			int index = directory.get(pageno.pid);
			/*
			 * If pin_count=0 before this call, throw an exception to report
			 * error. (For testing purposes, we ask you to throw an exception
			 * named PageUnpinnedException in case of error.)
			 */
			if (bufDescr[index].getPin_count() == 0) {
				throw new PageUnpinnedException(null,
						"BUFMGR:PAGE_UNPIN_FAILED");
			} 
			else
			{
				// Set the dirty bit for this frame.
				bufDescr[index].setDirtyBit(dirty);
				// Further, if pin_count>0, this method should decrement it
				bufDescr[index]
						.setPin_count(bufDescr[index].getPin_count() - 1);
				if (bufDescr[index].getPin_count() == 0){
					queue.add(index);
				}
			}
		}
		else 
			throw new HashEntryNotFoundException(null,
					"BUFMGR:PAGE_UNPIN_FAILED");
	}

	/**
	 * Allocate new pages.
	 * 
	 * @param firstpage
	 *            the address of the first page.
	 * @param howmany
	 *            total number of allocated new pages.
	 * 
	 * @return the first page id of the new pages. null, if error.
	 */

	public PageId newPage(Page firstpage, int howmany) throws DiskMgrException,
			FreePageException, BufferPoolExceededException,
			PagePinnedException, InvalidPageNumberException, FileIOException,
			HashEntryNotFoundException, IOException, InvalidRunSizeException {
		PageId id = new PageId();
		if (isFull()) 
			return null;
 
		try {
			// Call DB object to allocate a run of new pages
			SystemDefs.JavabaseDB.allocate_page(id, howmany);
		} catch (Exception e) {
			throw new DiskMgrException(e, "DB.java: newPage() failed");
		}
		/*
		 * If buffer is full, i.e., you can’t find a frame for the first page,
		 * ask DB to deallocate all these pages, and return null.
		 */
		/*
		 * find a frame in the buffer pool for the first page and pin it
		*/
		pinPage(id, firstpage, false);
		return id;

	}

	/**
	 * This method should be called to delete a page that is on disk. This
	 * routine must call the method in diskmgr package to deallocate the page.
	 * 
	 * @param globalPageId
	 *            the page number in the data base.
	 */

	public void freePage(PageId globalPageId) throws PagePinnedException,
			InvalidRunSizeException, InvalidPageNumberException,
			FileIOException, DiskMgrException, IOException {
		// Check whether this is a valid page or not
		if (directory.conatin(globalPageId.pid)) {
			int i;
			try {
				// Getting the index of this page
				i = getFrameNumber(globalPageId);
				// If it has more than one pin on it
				// throw an Exception
				if (bufDescr[i].getPin_count() > 1) {
					throw new PagePinnedException(null,
							"DB.java: freePage() failed");
				}
				// If pin count !=0 unpin this page
				//Not sure from this condition :S :S 
				if (bufDescr[i].getPin_count() == 1)
					unpinPage(bufDescr[i].getPageNumber(),
							bufDescr[i].isDirtyBit());
				// If it is dirty flush it
				if (bufDescr[i].isDirtyBit())
					try {
						flushPage(globalPageId);
					} catch (Exception e) {
						throw new FreePageException(null,
								"BUFMGR: FAIL_PAGE_FREE");
					}
				// Remove it from the hash,bufferPool,bufferDescriptor
				directory.remove(globalPageId.pid);
				bufPool[i] = null;
				bufDescr[i] = null;
				SystemDefs.JavabaseDB.deallocate_page(new PageId(
						globalPageId.pid));
			} catch (Exception e) {
				throw new PagePinnedException(null, "BUFMGR:FAIL_PAGE_FREE");
			}

		} else
			SystemDefs.JavabaseDB.deallocate_page(new PageId(globalPageId.pid));
	}

	/**
	 * Used to flush a particular page of the buffer pool to disk. This method
	 * calls the write_page method of the diskmgr package.
	 * 
	 * @param pageid
	 *            the page number in the database.
	 */
	public void flushPage(PageId pageid) throws HashEntryNotFoundException,
			DiskMgrException {
		Page apage = null;
		int i = getFrameNumber(pageid);
		if (bufPool[i] != null)
			apage = new Page(bufPool[i].getpage().clone());
		;
		try {
			if (apage != null) {
				SystemDefs.JavabaseDB.write_page(pageid, apage);
				bufDescr[i].setDirtyBit(false);
			} else
				throw new HashEntryNotFoundException(null,
						"BUF_MNGR: PAGE NOT FLUSHED ID EXCEPTION!");
		} catch (Exception e) {
			throw new DiskMgrException(e, "DB.java: flushPage() failed");
		}
	}

	public void flushAllPages() throws HashEntryNotFoundException,
			DiskMgrException {
		for (int i = 0; i < numbufs; i++) {
			if ((bufDescr[i] != null))
				flushPage(bufDescr[i].getPageNumber());
		}
	}

	public int getNumUnpinnedBuffers() {
		return queue.size();
	}
}

package heap;

//import java.io.IOException;
//import java.util.ArrayList;

import global.PageId;
import global.RID;
import global.SystemDefs;

import java.io.IOException;

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
import chainexception.ChainException;
import diskmgr.DiskMgrException;
import diskmgr.FileEntryNotFoundException;
import diskmgr.FileIOException;
import diskmgr.InvalidPageNumberException;

public class Heapfile {
	private HFPage hPage;
	private String fileName;
	private int recCnt;
	private int maxnum;

	/**
	 * Construct a new heap file for the given file name
	 * 
	 * @param file_name
	 *            The file name that should be opened
	 * @throws ChainException 
	 */
	public Heapfile(String file_name) throws IOException,
			ChainException {
		PageId headerId;
		if (file_name != null && !file_name.equals("")) {
			fileName = file_name;
			headerId = SystemDefs.JavabaseDB.get_file_entry(fileName);
			if (headerId == null) {
				hPage = new HFPage();
				headerId = SystemDefs.JavabaseBM.newPage(hPage, 1);
				SystemDefs.JavabaseDB.add_file_entry(fileName, headerId);
				hPage.init(headerId, hPage);
				SystemDefs.JavabaseBM.unpinPage(headerId, true);
				recCnt = 0;

			} else {
				hPage = new HFPage();
				SystemDefs.JavabaseBM.pinPage(headerId, hPage, false);
				SystemDefs.JavabaseBM.unpinPage(headerId, true);
				initializeRecCnt();
			}
		} else {
			fileName = "temp";
			HFPage hPage = new HFPage();
			headerId = SystemDefs.JavabaseBM.newPage(hPage, 1);
			SystemDefs.JavabaseDB.add_file_entry(fileName, headerId);
			hPage.init(headerId, hPage);
			SystemDefs.JavabaseBM.unpinPage(headerId, true);
			recCnt = 0;
		}
		maxnum = 1;
	}

	/**
	 * Get the header page of the heap file.
	 * @throws IOException 
	 */
	public PageId getHeader() throws IOException {
		return hPage.getCurPage();
	}

	/**
	 * Initialize the record counter.
	 * @throws ChainException 
	 */
	private void initializeRecCnt() throws IOException, ChainException {
		Scan s = this.openScan();
		 boolean finish = false;
		 RID rid = new RID();
		 recCnt = 0;
		 while(!finish)
		 {
		  if(s.getNext(rid) == null)
		   finish = true;
		  else
		   recCnt++;
		 }
		 s.closescan();
		
	}

	/**
	 * Move a page to the full pages linked list after inserting a record
	 */
	private void moveToFullPage(PageId id, HFPage page) throws IOException,
			ChainException {

		PageId previd = page.getPrevPage();
		HFPage prev = new HFPage();
		SystemDefs.JavabaseBM.pinPage(previd, prev, false);
		PageId currentid = prev.getCurPage();
		prev.getNextPage();
		prev.getPrevPage();
		HFPage next = new HFPage();
		PageId temp = next.getCurPage();
		SystemDefs.JavabaseBM.pinPage(temp, next, false);
		PageId nextid = next.getCurPage();
		next.getNextPage();
		next.getPrevPage();
		prev.setNextPage(nextid);
		next.setPrevPage(previd);
		SystemDefs.JavabaseBM.unpinPage(nextid, false);
		SystemDefs.JavabaseBM.unpinPage(previd, false);

		HFPage iterator = new HFPage();
		SystemDefs.JavabaseBM.pinPage(new PageId(2), iterator, false);
		iterator.getCurPage();
		iterator.getNextPage();
		iterator.getPrevPage();
		SystemDefs.JavabaseBM.unpinPage(new PageId(2), false);
		PageId myId = iterator.getCurPage();
		while (iterator.getNextPage().pid != -1) {
			myId = page.getNextPage();
			SystemDefs.JavabaseBM.pinPage(myId, iterator, false);
			iterator.getCurPage();
			iterator.getNextPage();
			iterator.getPrevPage();
		}

		iterator.setNextPage(id);
		page.setPrevPage(iterator.getCurPage());

	}

	/**
	 * Insert the given data record
	 * 
	 * @param byteArray
	 *            The given data
	 */
	@SuppressWarnings("static-access")
	public RID insertRecord(byte[] byteArray) throws ChainException,
			IOException {
		RID rid = new RID();
		if (byteArray.length > SystemDefs.JavabaseDB.MAX_SPACE) {
			throw new SpaceNotAvailableException(null, "SPACE_NOT_AVAILABLE");
		}
		HFPage candidate = new HFPage();
		PageId candidateId = hPage.getNextPage();
		while (candidateId.pid != -1) {
			SystemDefs.JavabaseBM.pinPage(candidateId, candidate, false);
			// CASE WE FOUND SPACE IN THE CURRENT PAGE
			if (candidate.available_space() >= byteArray.length) {
				RID temp = candidate.insertRecord(byteArray);
				rid.pageNo.pid = temp.pageNo.pid;
				rid.slotNo = temp.slotNo;
				recCnt++;
				// CASE THE CURRENT PAGE HAS NO MORE EMPTY SPACE
				if (candidate.available_space() == 0) {
					moveToFullPage(candidateId, candidate);
				}
				SystemDefs.JavabaseBM.unpinPage(candidateId, true);
				return rid;
			}
			SystemDefs.JavabaseBM.unpinPage(candidateId, true);
			candidateId = candidate.getNextPage();
		}
		// CASE WE'VE TO CREATE A NEW PAGE
		maxnum++;
		byte[] arr = hPage.getpage();
		arr[50] = (byte) maxnum;
		HFPage newPage = new HFPage();
		PageId newId = SystemDefs.JavabaseBM.newPage(newPage, 1);
		newPage.init(newId, newPage);
		SystemDefs.JavabaseBM.unpinPage(newId, true);
		recCnt++;
		SystemDefs.JavabaseBM.pinPage(newId, newPage, false);
		RID temp2 = newPage.insertRecord(byteArray);
		rid.pageNo.pid = temp2.pageNo.pid;
		rid.slotNo = temp2.slotNo;
		SystemDefs.JavabaseBM.unpinPage(newId, true);
		if (newPage.available_space() > 0) {
			// CASE WE'VE SPACE LEFT
			PageId indexId = hPage.getCurPage();
			HFPage indexPage = new HFPage();
			int currentNumber = indexId.pid;
			SystemDefs.JavabaseBM.pinPage(new PageId(currentNumber), indexPage,
					false);
			indexPage.getCurPage();
			indexPage.getNextPage();
			indexPage.getPrevPage();
			SystemDefs.JavabaseBM.unpinPage(new PageId(currentNumber), true);

			while (indexPage.getNextPage().pid != -1) {
				currentNumber = indexPage.getNextPage().pid;
				SystemDefs.JavabaseBM.pinPage(new PageId(currentNumber),
						indexPage, false);
				indexPage.getCurPage();
				indexPage.getNextPage();
				indexPage.getPrevPage();
				SystemDefs.JavabaseBM
						.unpinPage(new PageId(currentNumber), true);
			}
			newPage.setPrevPage(indexPage.getCurPage());
			indexPage.setNextPage(newId);
			SystemDefs.JavabaseBM.pinPage(new PageId(2), hPage, false);
			hPage.getCurPage();
			hPage.getNextPage();
			hPage.getPrevPage();
			SystemDefs.JavabaseBM.unpinPage(new PageId(2), true);
		} else {
			// CASE THE PAGE IS FULL
			moveToFullPage(newId, newPage);
		}
		return rid;
	}

	/**
	 * get the number of records in this heap file
	 */
	public int getRecCnt() {
		// TODO Auto-generated method stub
		return recCnt;
	}

	/**
	 * Construct a sequential scan for the user to iterate on the records
	 */
	public Scan openScan() throws ChainException, IOException {
		Scan scan = new Scan(this);
		return scan;
	}

	/**
	 * Delete the record specified with the given record id
	 * 
	 * @param rid
	 *            Id of record that should be deleted
	 * */
	public boolean deleteRecord(RID rid) throws IOException, ReplacerException,
			HashOperationException, PageUnpinnedException,
			InvalidFrameNumberException, PageNotReadException,
			BufferPoolExceededException, PagePinnedException, BufMgrException,
			HashEntryNotFoundException, InvalidSlotNumberException {

		PageId tmpId = new PageId(rid.pageNo.pid);
		HFPage pntr = hPage;
		boolean found = false;
		PageId id = pntr.getCurPage();
		while (id.pid != -1) {
			pntr = new HFPage();
			SystemDefs.JavabaseBM.pinPage(id, pntr, false);
			if (id.pid == tmpId.pid) {
				found = true;
				pntr.deleteRecord(rid);
				recCnt--;
				if (pntr.empty()) {
					PageId prevId = pntr.getPrevPage();
					HFPage prev = new HFPage();
					SystemDefs.JavabaseBM.pinPage(prevId, prev, false);
					prev.setNextPage(pntr.getNextPage());
					SystemDefs.JavabaseBM.unpinPage(prevId, false);
				}
				SystemDefs.JavabaseBM.unpinPage(tmpId, true);
				break;
			}
			SystemDefs.JavabaseBM.unpinPage(id, false);
			id = pntr.getNextPage();
		}
		if (!found) {
			pntr = hPage;
			id = pntr.getPrevPage();
			if (id.pid == tmpId.pid) {
				found = true;
				SystemDefs.JavabaseBM.pinPage(id, pntr, false);
				pntr.deleteRecord(rid);
				recCnt--;
				SystemDefs.JavabaseBM.unpinPage(id, true);
			} else {
				while (id.pid != -1 && (!found)) {
					id = pntr.getNextPage();
					pntr = new HFPage();
					SystemDefs.JavabaseBM.pinPage(id, pntr, false);
					if (id.pid == tmpId.pid) {
						found = true;
						pntr.deleteRecord(rid);
						recCnt--;
						SystemDefs.JavabaseBM.unpinPage(id, true);
						break;
					}
					SystemDefs.JavabaseBM.unpinPage(id, false);
				}
			}
			if (found) {
				PageId prevId = pntr.getPrevPage();
				HFPage prevPage = new HFPage();
				SystemDefs.JavabaseBM.pinPage(prevId, prevPage, false);
				prevPage.setNextPage(pntr.getNextPage());
				SystemDefs.JavabaseBM.unpinPage(prevId, false);
				PageId nextId = pntr.getNextPage();
				if (nextId.pid != -1) {
					HFPage nextPage = new HFPage();
					SystemDefs.JavabaseBM.pinPage(nextId, nextPage, false);
					nextPage.setPrevPage(prevId);
					SystemDefs.JavabaseBM.unpinPage(nextId, false);
				} else if (nextId.pid == -1) {
					prevPage.setNextPage(new PageId(-1));
				}
				// I want to add this page in the last position of the
				// free space pages list.
				PageId indexId = hPage.getCurPage();
				HFPage temp = hPage;
				while (temp.getNextPage().pid != -1) {
					indexId = temp.getNextPage();
					temp = new HFPage();
					SystemDefs.JavabaseBM.pinPage(indexId, temp, false);
					SystemDefs.JavabaseBM.unpinPage(indexId, false);
				}
				temp.setNextPage(pntr.getCurPage());
				pntr.setPrevPage(indexId);
				pntr.setNextPage(new PageId(-1));

			}
		}
		if (!found)
			throw new InvalidSlotNumberException();

		return found;
	}

	/**
	 * Update data of a specified record with the new data in the given tuple
	 * 
	 * @param rid
	 *            The id of the record which should be updated
	 * @param newTuple
	 *            Tuple contains the new data, data length and offset of the
	 *            record
	 */
	public boolean updateRecord(RID rid, Tuple newTuple) throws ChainException,
			IOException {
		PageId tmpId = new PageId(rid.pageNo.pid);
		HFPage pntr = hPage;
		boolean found = false;
		PageId id = pntr.getCurPage();
		while (id.pid != -1) {
			id = pntr.getNextPage();
			pntr = new HFPage();
			SystemDefs.JavabaseBM.pinPage(id, pntr, false);
			SystemDefs.JavabaseBM.unpinPage(id, false);
			if (id.pid == tmpId.pid) {
				found = true;
				break;
			}
		}
		if (!found) {
			pntr = hPage;
			id = pntr.getPrevPage();
			if (id.pid == tmpId.pid)
				found = true;
			else {
				pntr = new HFPage();
				SystemDefs.JavabaseBM.pinPage(id, pntr, false);
				SystemDefs.JavabaseBM.unpinPage(id, false);
			}
			while (id.pid != -1 && (!found)) {
				id = pntr.getNextPage();
				pntr = new HFPage();
				SystemDefs.JavabaseBM.pinPage(id, pntr, false);
				SystemDefs.JavabaseBM.unpinPage(id, false);
				if (id.pid == tmpId.pid) {
					found = true;
					break;
				}

			}
		}
		if (!found)
			throw new InvalidSlotNumberException();
		HFPage tmpPage = new HFPage();
		tmpPage.setCurPage(tmpId);
		SystemDefs.JavabaseBM.pinPage(tmpId, tmpPage, false);
		Tuple toModify = tmpPage.returnRecord(rid);
		if (toModify.getLength() == newTuple.getLength()) {
			System.arraycopy(newTuple.returnTupleByteArray(),
					newTuple.getOffset(), toModify.returnTupleByteArray(),
					toModify.getOffset(), newTuple.getLength());
			SystemDefs.JavabaseBM.unpinPage(tmpId, true);
			return true;

		} else {
			SystemDefs.JavabaseBM.unpinPage(tmpId, false);
			throw new InvalidUpdateException(new ChainException(),
					"Record size isn't suitable");
		}

	}

	/**
	 * Return the tuple of the record specified with the given id
	 * 
	 * @param rid
	 *            Id of the record that should be returned
	 */
	public Tuple getRecord(RID rid) throws ReplacerException,
			HashOperationException, PageUnpinnedException,
			InvalidFrameNumberException, PageNotReadException,
			BufferPoolExceededException, PagePinnedException, BufMgrException,
			HashEntryNotFoundException, IOException, InvalidSlotNumberException {

		HFPage pptr = null;
		boolean found = false;
		// get first page with free space
		PageId ptr = hPage.getNextPage();
		try {
			// loop until you reach the last
			// page of the pages with free space
			while (ptr.pid != -1 && !found) {
				pptr = new HFPage(); // create a HFPage
				SystemDefs.JavabaseBM.pinPage(ptr, pptr, false);
				// pin the page
				// with the ptr
				// pageId in the
				// BM and assign
				// the data to
				// pptr
				if (pptr.getCurPage().pid != rid.pageNo.pid)// not the requested
															// page
				{
					// get the next one
					ptr = pptr.getNextPage();
					// unpin it
					SystemDefs.JavabaseBM.unpinPage(pptr.getCurPage(), false);
				} else {
					// it is the requested page
					found = true; // get out of the loops
					ptr = pptr.getCurPage();

				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (!found)
			// get first page with no free space
			ptr = hPage.getPrevPage();
		try {
			// loop until you reach the last
			// page of the pages with no free
			// space
			while (ptr.pid != -1 && !found) {
				// create a HFPage
				pptr = new HFPage();
				// pin the page
				SystemDefs.JavabaseBM.pinPage(ptr, pptr, false);
				// with the pointer pageId in the
				// BM and assign the data to pptr
				if (pptr.getCurPage().pid != rid.pageNo.pid)// not the requested
															// page

				{
					// get the next one
					ptr = pptr.getNextPage();
					// unpin it
					SystemDefs.JavabaseBM.unpinPage(pptr.getCurPage(), false);
				} else {
					// it's the requested page
					found = true; // get out of the loops
					ptr = pptr.getCurPage();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (found)// the rid is in this heap file
		{
			Tuple out; // tuple to store record
			out = pptr.getRecord(rid); // assign the records
			SystemDefs.JavabaseBM.unpinPage(ptr, false); // unpin the page
			return out;
		} else
			// the rid isn't in this heap file
			return null;
	}

	/**
	 * Delete the heap file and deallocate all the pages in the heap file
	 */
	public void deleteFile() throws FileEntryNotFoundException,
			FileIOException, InvalidPageNumberException, DiskMgrException,
			IOException, InvalidBufferException, ReplacerException,
			HashOperationException, InvalidFrameNumberException,
			PageNotReadException, BufferPoolExceededException,
			PagePinnedException, PageUnpinnedException,
			HashEntryNotFoundException, BufMgrException {
		SystemDefs.JavabaseBM.freePage(hPage.getCurPage());
		SystemDefs.JavabaseDB.delete_file_entry(fileName);
	}

}

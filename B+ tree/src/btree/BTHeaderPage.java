package btree;

import global.Convert;
import global.PageId;
import global.RID;
import global.SystemDefs;
import heap.HFPage;
import heap.InvalidSlotNumberException;
import heap.SpaceNotAvailableException;

import java.io.IOException;

import bufmgr.BufMgrException;
import bufmgr.BufferPoolExceededException;
import bufmgr.HashOperationException;
import bufmgr.InvalidFrameNumberException;
import bufmgr.PageNotReadException;
import bufmgr.PagePinnedException;
import bufmgr.PageUnpinnedException;
import bufmgr.ReplacerException;
import diskmgr.Page;

/* Header page holds information about the tree as a whole,
 * such as the page id of the root page, the type of the search key,
 * and the length of the key field(s)
 */
public class BTHeaderPage extends HFPage {
	private int key_type;// type of keys
	private int maxFieldSize;// maximum number of records per page

	/**
	 * Initiates a header page with the given ID
	 * 
	 * @param pd
	 * @throws ReplacerException
	 * @throws HashOperationException
	 * @throws PageUnpinnedException
	 * @throws InvalidFrameNumberException
	 * @throws PageNotReadException
	 * @throws BufferPoolExceededException
	 * @throws PagePinnedException
	 * @throws BufMgrException
	 * @throws IOException
	 */
	public BTHeaderPage(PageId pd) throws ReplacerException,
			HashOperationException, PageUnpinnedException,
			InvalidFrameNumberException, PageNotReadException,
			BufferPoolExceededException, PagePinnedException, BufMgrException,
			IOException {

		// temporary page to pin the page with PageId pd
		Page tempPage = new Page();
		SystemDefs.JavabaseBM.pinPage(pd, tempPage, false);
		openHFpage(tempPage);// opening a HFPage

		try {
			// Storing key_type and max size as records in the header page
			key_type = Convert.getShortValue(0, getRecord(
					new RID(getCurPage(), 0)).getTupleByteArray());
			maxFieldSize = Convert.getIntValue(0, getRecord(
					new RID(getCurPage(), 1)).getTupleByteArray());
		} catch (InvalidSlotNumberException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initiates a header page with the given search key type, and max field
	 * size
	 * 
	 * @param keyType
	 * @param max
	 * @throws ConstructPageException
	 */
	public BTHeaderPage(int keyType, int max) throws ConstructPageException {
		key_type = keyType; // assigning key type
		maxFieldSize = max;// assigning max size

		try {
			// Allocating a new page
			Page pg = new Page();
			PageId headerId = new PageId();
			headerId = SystemDefs.JavabaseBM.newPage(pg, 1);
			if (headerId == null)// no space left in BufMgr
				throw new SpaceNotAvailableException();

			init(headerId, pg);// initiating first page

			// Storing key_type and max size as records in the header page
			byte[] keyArray = new byte[2];
			Convert.setShortValue((short) keyType, 0, keyArray);
			insertRecord(keyArray);
			keyArray = new byte[4];
			Convert.setIntValue(max, 0, keyArray);
			insertRecord(keyArray);

		} catch (Exception e) {
			throw new ConstructPageException(e,
					"Header page construction failed");
		}
	}

	/**
	 * Setting root Id as the next page of the header page
	 * 
	 * @param rootId
	 * @throws IOException
	 */
	public void setRootId(PageId rootId) throws IOException {
		setNextPage(rootId);
	}

	/**
	 * Retrieves the root page ID
	 * 
	 * @return
	 * @throws IOException
	 */
	public PageId getRootId() throws IOException {
		return getNextPage();
	}

	/**
	 * @return length of the key field
	 */
	public int getMaxFieldSize() {
		return maxFieldSize;
	}

	/**
	 * Sets the length of the key field
	 * 
	 * @param maxFieldSize
	 */
	public void setMaxFieldSize(int maxFieldSize) {
		this.maxFieldSize = maxFieldSize;
	}

	/**
	 * @return type of the search key
	 */
	public int getKeyType() {
		return key_type;
	}

	/**
	 * Sets the type of the search key
	 * 
	 * @param keyType
	 */
	public void setKeyType(int keyType) {
		key_type = keyType;
	}

}

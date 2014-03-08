package btree;

import heap.InvalidSlotNumberException;

import java.io.IOException;

import bufmgr.BufMgrException;
import bufmgr.BufferPoolExceededException;
import bufmgr.HashEntryNotFoundException;
import bufmgr.HashOperationException;
import bufmgr.InvalidFrameNumberException;
import bufmgr.PageNotReadException;
import bufmgr.PagePinnedException;
import bufmgr.PageUnpinnedException;
import bufmgr.ReplacerException;

/**
 * Base class for a index file scan
 */
public abstract class IndexFileScan {
	/**
	 * Get the next record.
	 * 
	 * @return the KeyDataEntry, which contains the key and data
	 * @throws IOException 
	 * @throws ConvertException 
	 * @throws NodeNotMatchException 
	 * @throws KeyNotMatchException 
	 * @throws InvalidSlotNumberException 
	 * @throws InvalidFrameNumberException 
	 * @throws HashEntryNotFoundException 
	 * @throws PageUnpinnedException 
	 * @throws ReplacerException 
	 * @throws ConstructPageException 
	 * @throws BufMgrException 
	 * @throws PagePinnedException 
	 * @throws BufferPoolExceededException 
	 * @throws PageNotReadException 
	 * @throws HashOperationException 
	 */
	abstract public KeyDataEntry get_next() throws InvalidSlotNumberException, KeyNotMatchException, NodeNotMatchException, ConvertException, IOException, ReplacerException, PageUnpinnedException, HashEntryNotFoundException, InvalidFrameNumberException, ConstructPageException, HashOperationException, PageNotReadException, BufferPoolExceededException, PagePinnedException, BufMgrException;

	/**
	 * Delete the current record.
	 * @throws IOException 
	 * @throws InvalidSlotNumberException 
	 * @throws Exception 
	 */
	abstract public void delete_current() throws InvalidSlotNumberException, IOException, Exception;

	/**
	 * Returns the size of the key
	 * 
	 * @return the keysize
	 */
	abstract public int keysize();
}

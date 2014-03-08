package btree;

import global.GlobalConst;
import global.PageId;
import global.RID;
import global.SystemDefs;
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

public class BTFileScan extends IndexFileScan implements GlobalConst {
	
	private PageId lowestPage;
	private PageId highestPage;
	private RID recPntr;
	private BTLeafPage pagePntr;
	private boolean dirty;
	private int keyType;
	private boolean lastIterate;
	private int keySize;
	private KeyClass startKey;
	private KeyClass endKey;
	private boolean justDel;
	private BTreeFile file;
	
	
	/**
	 *  Scan constructor to create a new scan iterator on the BTree file for the given range
	 * @param low	First page id in the range
	 * @param high	last page id in the range
	 * @param kType	key type (String, Integer, etc..)
	 * @param kSize	key size in the B tree file
	 * @throws IOException
	 * @throws ReplacerException
	 * @throws HashOperationException
	 * @throws PageUnpinnedException
	 * @throws InvalidFrameNumberException
	 * @throws PageNotReadException
	 * @throws BufferPoolExceededException
	 * @throws PagePinnedException
	 * @throws BufMgrException
	 * @throws ConstructPageException
	 */
	public BTFileScan(BTreeFile file, PageId low, PageId high, int kType, int kSize, KeyClass start, KeyClass end) throws IOException, ReplacerException, HashOperationException, PageUnpinnedException, InvalidFrameNumberException, PageNotReadException, BufferPoolExceededException, PagePinnedException, BufMgrException, ConstructPageException{
		this.file = file;
		lowestPage = low;
		highestPage = high;
		pagePntr = new BTLeafPage(lowestPage, kType);
		recPntr = null;
		dirty=false;
		keyType = kType;
		keySize = kSize;
		lastIterate = false;
		startKey = start;
		endKey = end;
		justDel = false;
		file.writeToLog("Scan started matched range from "+start+" to "+end+" \n");
	}
	
	/**
	 * Get the next record in the iterator, If I reached the end of a page, jump to
	 * next page in the BTree file until reach the last page specified in the range.
	 * If the last record was reached, a null pointer will be returned.
	 * @throws all Exceptions
	 */
	public KeyDataEntry get_next() throws InvalidSlotNumberException, KeyNotMatchException, NodeNotMatchException, ConvertException, IOException, ReplacerException, PageUnpinnedException, HashEntryNotFoundException, InvalidFrameNumberException, ConstructPageException, HashOperationException, PageNotReadException, BufferPoolExceededException, PagePinnedException, BufMgrException {
		if(file.isClosed()){
			this.DestroyBTreeFileScan();
			throw new IOException("File already closed");
		}
			
		//If a record was deleted, don't advance the pointer.
		if(justDel){
			justDel = false;
			if(lastIterate)
				return null;
			else
				return pagePntr.getCurrent(recPntr);
		}
		//If it was the first time calling the get_next 
		if(recPntr == null && (!lastIterate) && lowestPage.pid == pagePntr.getCurPage().pid){
			recPntr = new RID();
			KeyDataEntry entry = pagePntr.getFirst(recPntr);
			while(BT.keyCompare(startKey, entry.key) > 0){
				entry = pagePntr.getNext(recPntr);
				if(entry == null)
					return null;
			}
		}//if not, then advance the iterator
		else if(recPntr != null && !lastIterate)
			recPntr = pagePntr.nextRecord(recPntr);
		//Cases if I reached the last record I want.
		if(recPntr != null && BT.keyCompare(endKey, pagePntr.getCurrent(recPntr).key)<0 && pagePntr.getCurPage().pid == highestPage.pid)
			lastIterate =true;
		if(pagePntr.getCurPage().pid == highestPage.pid && recPntr == null)
			lastIterate = true;
		//If reached the end of a page, jumb to the next one
		if(recPntr == null && (!lastIterate)){
			PageId next = pagePntr.getNextPage();
			//unpin the used page
			SystemDefs.JavabaseBM.unpinPage(pagePntr.getCurPage(), dirty);
			//pin the new page that will be used in the next iterations	
			pagePntr = new BTLeafPage(next, keyType);
			while(pagePntr.getSlotCnt() == 0 && !lastIterate){
				if(next.pid == highestPage.pid){
					lastIterate = true;
					break;
				}
				next = pagePntr.getNextPage();
				SystemDefs.JavabaseBM.unpinPage(pagePntr.getCurPage(), dirty);
				pagePntr = new BTLeafPage(next, keyType);
			}
			//the page hasn't been changed yet
			dirty = false;
			//record pointer will point at the first record in the new page
			recPntr = pagePntr.firstRecord();
		}
		//if I reached the last record in the last page, then unpin this page.
		if(lastIterate){
			try{
				SystemDefs.JavabaseBM.unpinPage(pagePntr.getCurPage(), dirty);
			}catch(Exception e){
				
			}
			return null;
		}
		return pagePntr.getCurrent(recPntr);
	}
	
	/**
	 * Delete the current record in the scan.
	 * @throws Exception 
	 */
	public void delete_current() throws Exception, InvalidSlotNumberException, IOException {
		if(file.isClosed()){
			this.DestroyBTreeFileScan();
			throw new IOException("File already closed");
		}
		//If there is no record was scanned, throw exception
		if(recPntr == null || lastIterate)
			throw new Exception("No records have been scanned !!");
		KeyClass key = pagePntr.getCurrent(recPntr).key;
		RID recId = pagePntr.nextRecord(recPntr);
		//Delete the last scanned record from the page
		pagePntr.deleteSortedRecord(recPntr);
		file.writeToLog("Scan deleted record with key = "+key+" \n");
		//Set the dirty bit flag with true, because the page has been modified
		dirty = true;
		//When a record is deleted, may be it was the last record in the scan
		//so check that before continue
		if(recPntr != null && BT.keyCompare(endKey,key )==0 && pagePntr.getCurPage().pid == highestPage.pid)
			lastIterate = true;
		if(pagePntr.getCurPage().pid == highestPage.pid && recId == null)
			lastIterate = true;
		//If the deleted record was the last record in the page,
		//and It wasn't the last iteration in the scan, then advance the 
		//scanner pointer to the first record in the next page
		else if(recId == null && !lastIterate){
			PageId next = pagePntr.getNextPage();
			SystemDefs.JavabaseBM.unpinPage(pagePntr.getCurPage(), dirty);
			pagePntr = new BTLeafPage(next, keyType);
			while(pagePntr.getSlotCnt() == 0 && !lastIterate){
				if(next.pid == highestPage.pid){
					lastIterate = true;
					break;
				}
				next = pagePntr.getNextPage();
				SystemDefs.JavabaseBM.unpinPage(pagePntr.getCurPage(), dirty);
				pagePntr = new BTLeafPage(next, keyType);
			}
			dirty = false;
			recPntr = pagePntr.firstRecord();
		}
		//Set the just deleted flag with true.
		justDel = true;
	}
	
	/**
	 * returns the key size on the BTree file.
	 */
	@Override
	public int keysize() {
		return keySize;
	}
	
	/**
	 * Close the Scan iterator on the BTree file. 
	 */
	public void DestroyBTreeFileScan(){
		try{
			SystemDefs.JavabaseBM.unpinPage(pagePntr.getCurPage(), dirty);
		}catch(Exception e){
			
		}
		pagePntr = null;
		recPntr = null;
		lowestPage =null;
		highestPage = null;
		
	}
	
	
}

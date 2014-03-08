package heap;

import java.io.IOException;

import bufmgr.BufMgrException;
import chainexception.ChainException;
import global.PageId;
import global.RID;
import global.SystemDefs;

public class Scan {
	
	
	private	RID cursor;
	private PageId nextPage;
	private boolean fullChain;
	private PageId currPage;
	private HFPage hfCurr;
	private Heapfile cur;
	public Scan(Heapfile hf) throws ChainException, BufMgrException, IOException
	{
		cur = hf;
		initialaize(hf);
	}
	
	private void initialaize(Heapfile hf)throws ChainException, BufMgrException, IOException
	{
		// make a hfPage
		hfCurr = new HFPage();
		currPage = null;
		PageId previousPage =null;
		// pin the current page
		SystemDefs.JavabaseBM.pinPage(hf.getHeader(), hfCurr,false);
		// get the previous and next of the current as it's the header 
		nextPage = hfCurr.getNextPage();
		previousPage = hfCurr.getPrevPage();
		// unpin the header since its of no use;
		SystemDefs.JavabaseBM.unpinPage(hf.getHeader(), false);
        // check if there is previous pages to the header
		if(previousPage.pid!=-1) // if yes
		{
		fullChain= true;
		currPage = new PageId();
		currPage.pid = previousPage.pid;
		// make the hfpage object
		hfCurr = new HFPage();
		// pin the first previous page
		SystemDefs.JavabaseBM.pinPage(previousPage, hfCurr, false);
		// get the first record
		cursor = hfCurr.firstRecord();
		}
		else if(nextPage.pid!=-1) // no previous page but may be there is next pages 
		{
		fullChain = false;
		currPage = new PageId(); 
		currPage.pid = nextPage.pid;
		// make the hfpage object
		hfCurr = new HFPage();
		// pin the first next page
		SystemDefs.JavabaseBM.pinPage(nextPage, hfCurr, false);
		// get the first record
		cursor = hfCurr.firstRecord();		
		}
		else // there is no pages in the file so this means no records in it
		{
		// the first record is nothing
		cursor = null;	
		}

	}

	public Tuple getNext(RID rid) throws IOException, InvalidSlotNumberException, ChainException {
		if(rid == null)
			throw new InvalidSlotNumberException();
		
		if(cursor == null)
		{
			SystemDefs.JavabaseBM.unpinPage(currPage, true);
			return null;
		}
		else
		{
			rid.slotNo = cursor.slotNo;
			rid.pageNo = cursor.pageNo;
			
			Tuple toBeRet = hfCurr.getRecord(cursor);
			
			cursor = hfCurr.nextRecord(cursor);
			if(cursor == null)
			{
				PageId temp = hfCurr.getNextPage();
				
				if(temp.pid!=-1)
				{
					SystemDefs.JavabaseBM.unpinPage(currPage, true);
					currPage = new PageId();
					currPage.pid = temp.pid; 
					hfCurr = new HFPage();
					SystemDefs.JavabaseBM.pinPage(currPage, hfCurr, false);
					cursor = hfCurr.firstRecord();
				}
				else if(fullChain&&nextPage.pid!=-1)
				{
					SystemDefs.JavabaseBM.unpinPage(currPage, true);
					fullChain = false;
					currPage = new PageId();
					currPage.pid = nextPage.pid;
					hfCurr = new HFPage();
					SystemDefs.JavabaseBM.pinPage(currPage, hfCurr, false);
					cursor = hfCurr.firstRecord();
				}
				
			}
			
			return toBeRet;
		}
	}

	public boolean position(RID rid) throws  IOException, ChainException {
		
		if(rid == null)
			throw new InvalidSlotNumberException();
		RID rid1 = new RID();
		
		boolean found = false;
		
		RID rid2 = new RID();
		
		if(cursor!= null)
		{
			rid2.pageNo.pid = cursor.pageNo.pid;
			rid2.slotNo = cursor.slotNo;				
		}
		else
			rid2 = null;
		
		while(cursor!=null&&!found)
		{
			if(rid.equals(cursor))
				found = true;
			else
				this.getNext(rid1);
		}
		
		if(!found)
		{
			rid1 = new RID();
			initialaize(cur);
			while(cursor!=null&&!cursor.equals(rid2)&&!found)
			{
				if(rid.equals(cursor))
					found = true;
				else
					this.getNext(rid1);
			}
			if(found)
				return true;
			else
			    return false;
		}
		else
		return true;
	}

	
	public void closescan(){
		cursor = null;
		nextPage = null;
		fullChain = false;
		currPage = null;
		hfCurr = null;
		try {
			this.finalize();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	

}
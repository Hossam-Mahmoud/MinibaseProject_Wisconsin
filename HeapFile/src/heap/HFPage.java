package heap;

import global.Convert;
import global.GlobalConst;
import global.PageId;
import global.RID;

import java.io.IOException;

import diskmgr.Page;

// Referenced classes of package heap:
//            ConstSlot, InvalidSlotNumberException, Tuple

public class HFPage extends Page implements ConstSlot, GlobalConst {

	public static final int SIZE_OF_SLOT = 4;
	public static final int DPFIXED = 20;
	public static final int SLOT_CNT = 0;
	public static final int USED_PTR = 2;
	public static final int FREE_SPACE = 4;
	public static final int TYPE = 6;
	public static final int PREV_PAGE = 8;
	public static final int NEXT_PAGE = 12;
	public static final int CUR_PAGE = 16;
	private short slotCnt;
	private short usedPtr;
	private short freeSpace;
	private short type;
	private PageId prevPage;
	private PageId nextPage;
	protected PageId curPage;

	public HFPage() {
		prevPage = new PageId();
		nextPage = new PageId();
		curPage = new PageId();
	}

	public HFPage(Page page) {
		prevPage = new PageId();
		nextPage = new PageId();
		curPage = new PageId();
		super.data = page.getpage();
	}

	public void openHFpage(Page page) {
		super.data = page.getpage();
	}

	public void init(PageId pageid, Page page) throws IOException {
		super.data = page.getpage();
		slotCnt = 0;
		Convert.setShortValue(slotCnt, 0, super.data);
		curPage.pid = pageid.pid;
		Convert.setIntValue(curPage.pid, 16, super.data);
		nextPage.pid = prevPage.pid = -1;
		Convert.setIntValue(prevPage.pid, 8, super.data);
		Convert.setIntValue(nextPage.pid, 12, super.data);
		usedPtr = 1024;
		Convert.setShortValue(usedPtr, 2, super.data);
		freeSpace = 1004;
		Convert.setShortValue(freeSpace, 4, super.data);
	}

	public byte[] getHFpageArray() {
		return super.data;
	}

	public void dumpPage() throws IOException {
		curPage.pid = Convert.getIntValue(16, super.data);
		nextPage.pid = Convert.getIntValue(12, super.data);
		usedPtr = Convert.getShortValue(2, super.data);
		freeSpace = Convert.getShortValue(4, super.data);
		slotCnt = Convert.getShortValue(0, super.data);
		System.out.println("dumpPage");
		System.out.println("curPage= " + curPage.pid);
		System.out.println("nextPage= " + nextPage.pid);
		System.out.println("usedPtr= " + usedPtr);
		System.out.println("freeSpace= " + freeSpace);
		System.out.println("slotCnt= " + slotCnt);
		int i = 0;
		int j = 20;
		for (; i < slotCnt; i++) {
			short word0 = Convert.getShortValue(j, super.data);
			short word1 = Convert.getShortValue(j + 2, super.data);
			System.out.println("slotNo " + i + " offset= " + word1);
			System.out.println("slotNo " + i + " length= " + word0);
			j += 4;
		}

	}

	public PageId getPrevPage() throws IOException {
		prevPage.pid = Convert.getIntValue(8, super.data);
		return prevPage;
	}

	public void setPrevPage(PageId pageid) throws IOException {
		prevPage.pid = pageid.pid;
		Convert.setIntValue(prevPage.pid, 8, super.data);
	}

	public PageId getNextPage() throws IOException {
		nextPage.pid = Convert.getIntValue(12, super.data);
		return nextPage;
	}

	public void setNextPage(PageId pageid) throws IOException {
		nextPage.pid = pageid.pid;
		Convert.setIntValue(nextPage.pid, 12, super.data);
	}

	public PageId getCurPage() throws IOException {
		curPage.pid = Convert.getIntValue(16, super.data);
		return curPage;
	}

	public void setCurPage(PageId pageid) throws IOException {
		curPage.pid = pageid.pid;
		Convert.setIntValue(curPage.pid, 16, super.data);
	}

	public short getType() throws IOException {
		type = Convert.getShortValue(6, super.data);
		return type;
	}

	public void setType(short word0) throws IOException {
		type = word0;
		Convert.setShortValue(type, 6, super.data);
	}

	public short getSlotCnt() throws IOException {
		slotCnt = Convert.getShortValue(0, super.data);
		return slotCnt;
	}

	public void setSlot(int i, int j, int k) throws IOException {
		int l = 20 + i * 4;
		Convert.setShortValue((short) j, l, super.data);
		Convert.setShortValue((short) k, l + 2, super.data);
	}

	public short getSlotLength(int i) throws IOException {
		int j = 20 + i * 4;
		short word0 = Convert.getShortValue(j, super.data);
		return word0;
	}

	public short getSlotOffset(int i) throws IOException {
		int j = 20 + i * 4;
		short word0 = Convert.getShortValue(j + 2, super.data);
		return word0;
	}

	public RID insertRecord(byte abyte0[]) throws IOException {
		RID rid = new RID();
		int i = abyte0.length;
		int j = i + 4;
		freeSpace = Convert.getShortValue(4, super.data);
		if (j > freeSpace)
			return null;
		slotCnt = Convert.getShortValue(0, super.data);
		int k;
		for (k = 0; k < slotCnt; k++) {
			short word0 = getSlotLength(k);
			if (word0 == -1)
				break;
		}

		if (k == slotCnt) {
			freeSpace -= j;
			Convert.setShortValue(freeSpace, 4, super.data);
			slotCnt++;
			Convert.setShortValue(slotCnt, 0, super.data);
		} else {
			freeSpace -= i;
			Convert.setShortValue(freeSpace, 4, super.data);
		}
		usedPtr = Convert.getShortValue(2, super.data);
		usedPtr -= i;
		Convert.setShortValue(usedPtr, 2, super.data);
		setSlot(k, i, usedPtr);
		System.arraycopy(abyte0, 0, super.data, usedPtr, i);
		curPage.pid = Convert.getIntValue(16, super.data);
		rid.pageNo.pid = curPage.pid;
		rid.slotNo = k;
		return rid;
	}

	public void deleteRecord(RID rid) throws IOException,
			InvalidSlotNumberException {
		int i = rid.slotNo;
		short word0 = getSlotLength(i);
		slotCnt = Convert.getShortValue(0, super.data);
		if (i >= 0 && i < slotCnt && word0 > 0) {
			short word1 = getSlotOffset(i);
			usedPtr = Convert.getShortValue(2, super.data);
			int j = usedPtr + word0;
			int k = word1 - usedPtr;
			System.arraycopy(super.data, usedPtr, super.data, j, k);
			int l = 0;
			int i1 = 20;
			for (; l < slotCnt; l++) {
				if (getSlotLength(l) >= 0) {
					int j1 = getSlotOffset(l);
					if (j1 < word1) {
						j1 += word0;
						Convert.setShortValue((short) j1, i1 + 2, super.data);
					}
				}
				i1 += 4;
			}

			usedPtr += word0;
			Convert.setShortValue(usedPtr, 2, super.data);
			freeSpace = Convert.getShortValue(4, super.data);
			freeSpace += word0;
			Convert.setShortValue(freeSpace, 4, super.data);
			setSlot(i, -1, 0);
			return;
		} else {
			throw new InvalidSlotNumberException(null,
					"HEAPFILE: INVALID_SLOTNO");
		}
	}

	public RID firstRecord() throws IOException {
		RID rid = new RID();
		slotCnt = Convert.getShortValue(0, super.data);
		int i;
		for (i = 0; i < slotCnt; i++) {
			short word0 = getSlotLength(i);
			if (word0 != -1)
				break;
		}

		if (i == slotCnt) {
			return null;
		} else {
			rid.slotNo = i;
			curPage.pid = Convert.getIntValue(16, super.data);
			rid.pageNo.pid = curPage.pid;
			return rid;
		}
	}

	public RID nextRecord(RID rid) throws IOException {
		RID rid1 = new RID();
		slotCnt = Convert.getShortValue(0, super.data);
		int i = rid.slotNo;
		for (i++; i < slotCnt; i++) {
			short word0 = getSlotLength(i);
			if (word0 != -1)
				break;
		}

		if (i >= slotCnt) {
			return null;
		} else {
			rid1.slotNo = i;
			curPage.pid = Convert.getIntValue(16, super.data);
			rid1.pageNo.pid = curPage.pid;
			return rid1;
		}
	}

	public Tuple getRecord(RID rid) throws IOException,
			InvalidSlotNumberException {
		PageId pageid = new PageId();
		pageid.pid = rid.pageNo.pid;
		curPage.pid = Convert.getIntValue(16, super.data);
		int i = rid.slotNo;
		short word0 = getSlotLength(i);
		slotCnt = Convert.getShortValue(0, super.data);
		if (i >= 0 && i < slotCnt && word0 > 0 && pageid.pid == curPage.pid) {
			short word1 = getSlotOffset(i);
			byte abyte0[] = new byte[word0];
			System.arraycopy(super.data, word1, abyte0, 0, word0);
			Tuple tuple = new Tuple(abyte0, 0, word0);
			return tuple;
		} else {
			throw new InvalidSlotNumberException(null,
					"HEAPFILE: INVALID_SLOTNO");
		}
	}

	public Tuple returnRecord(RID rid) throws IOException,
			InvalidSlotNumberException {
		PageId pageid = new PageId();
		pageid.pid = rid.pageNo.pid;
		curPage.pid = Convert.getIntValue(16, super.data);
		int i = rid.slotNo;
		short word0 = getSlotLength(i);
		slotCnt = Convert.getShortValue(0, super.data);
		if (i >= 0 && i < slotCnt && word0 > 0 && pageid.pid == curPage.pid) {
			short word1 = getSlotOffset(i);
			Tuple tuple = new Tuple(super.data, word1, word0);
			return tuple;
		} else {
			throw new InvalidSlotNumberException(null,
					"HEAPFILE: INVALID_SLOTNO");
		}
	}

	public int available_space() throws IOException {
		freeSpace = Convert.getShortValue(4, super.data);
		return freeSpace - 4;
	}

	public boolean empty() throws IOException {
		slotCnt = Convert.getShortValue(0, super.data);
		for (int i = 0; i < slotCnt; i++) {
			short word0 = getSlotLength(i);
			if (word0 != -1)
				return false;
		}

		return true;
	}

	protected void compact_slot_dir() throws IOException {
		int i = 0;
		int j = -1;
		boolean flag = false;
		slotCnt = Convert.getShortValue(0, super.data);
		freeSpace = Convert.getShortValue(4, super.data);
		for (; i < slotCnt; i++) {
			short word0 = getSlotLength(i);
			if (word0 == -1 && !flag) {
				flag = true;
				j = i;
			} else if (word0 != -1 && flag) {
				short word1 = getSlotOffset(i);
				setSlot(j, word0, word1);
				setSlot(i, -1, 0);
				for (j++; getSlotLength(j) != -1; j++)
					;
			}
		}

		if (flag) {
			freeSpace += 4 * (slotCnt - j);
			slotCnt = (short) j;
			Convert.setShortValue(freeSpace, 4, super.data);
			Convert.setShortValue(slotCnt, 0, super.data);
		}
	}

}
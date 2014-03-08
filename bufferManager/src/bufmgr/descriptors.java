package bufmgr;

import global.PageId;

public class descriptors {
	private int pin_count;
	private PageId pageNumber;
	private boolean dirtyBit;

	public descriptors() {
		this.pin_count = 0;
		this.pageNumber = null;
		this.dirtyBit = false;

	}

	public descriptors(int pin_count, PageId pageNumber, boolean dirtyBit) {
		this.pin_count = pin_count;
		this.pageNumber = pageNumber;
		this.dirtyBit = dirtyBit;
	}

	public void setPin_count(int pin_count) {
		this.pin_count = pin_count;
	}

	public int getPin_count() {
		return pin_count;
	}

	public void setPageNumber(PageId pageNumber) {
		this.pageNumber = pageNumber;
	}

	public PageId getPageNumber() {
		return pageNumber;
	}

	public void setDirtyBit(boolean dirtyBit) {
		this.dirtyBit = dirtyBit;
	}

	public boolean isDirtyBit() {
		return dirtyBit;
	}
	public String toString()
	{
		return "pin_count "+getPin_count()+" id "+getPageNumber().pid+" dirty "+isDirtyBit();
	}
}

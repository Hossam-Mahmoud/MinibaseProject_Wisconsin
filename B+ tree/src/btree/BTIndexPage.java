package btree;

import global.PageId;
import global.RID;
import heap.InvalidSlotNumberException;
import heap.Tuple;

import java.io.IOException;

import diskmgr.Page;

/* A BTIndexPage is an index page on a B+ tree. 
 * It holds abstract {key, PageId} pairs;
 * it doesn't know anything about the keys (their lengths or their types),
 * instead relying on the abstract interface in BT.java */
public class BTIndexPage extends BTSortedPage {
	/**
	 * Allocates a new index SortedPage with the given key type
	 * 
	 * @param key_type
	 * @throws ConstructPageException
	 * @throws IOException
	 */
	public BTIndexPage(int key_type) throws ConstructPageException, IOException {
		super(key_type);
		setType(NodeType.INDEX);// setting type
	}

	/**
	 * associate the BTIndexPage instance with the Page instance, also it sets
	 * the type of node to be NodeType.INDEX.
	 * 
	 * @param page
	 *            : page to be associated with
	 * @param key_type
	 *            : type of keys in the page
	 * @throws ConstructPageException
	 * @throws IOException
	 */
	public BTIndexPage(Page page, int key_type) throws ConstructPageException,
			IOException {
		super(page, key_type);
		setType(NodeType.INDEX);// setting type
	}

	/**
	 * pin the page with pageno, and get the corresponding BTIndexPage, also it
	 * sets the type of node to be NodeType.INDEX.
	 * 
	 * @param pageno
	 *            : page Id of the page
	 * @param key_type
	 *            : type of keys in the page
	 * @throws ConstructPageException
	 * @throws IOException
	 */
	public BTIndexPage(PageId pageno, int key_type)
			throws ConstructPageException, IOException {
		super(pageno, key_type);
		setType(NodeType.INDEX);// setting type
	}

	/**
	 * Inserts a record in a sorted order in the BTIndexPage
	 * 
	 * @param key
	 * @param pageNo
	 * @return RecordId of the new entry
	 * @throws InsertRecException
	 */
	public RID insertKey(KeyClass key, PageId pageNo) throws InsertRecException {
		KeyDataEntry kde = new KeyDataEntry(key, pageNo);
		return super.insertRecord(kde);// insert a sorted entry in the page
	}

	/**
	 * Returns first data entry in the page, and assigns rid to the first record
	 * ID
	 * 
	 * @param rid
	 * @return First data entry in the page
	 * @throws IOException
	 * @throws InvalidSlotNumberException
	 * @throws KeyNotMatchException
	 * @throws NodeNotMatchException
	 * @throws ConvertException
	 */
	public KeyDataEntry getFirst(RID rid) throws IOException,
			InvalidSlotNumberException, KeyNotMatchException,
			NodeNotMatchException, ConvertException {
		RID nrid = firstRecord();// get first record id
		if (nrid == null)// empty page
			return null;
		else
			rid.copyRid(nrid);

		// Retrieving data of the first entry
		Tuple row = getRecord(nrid);
		return BT.getEntryFromBytes(row.getTupleByteArray(), row.getOffset(),
				row.getLength(), keyType, getType());
	}

	/**
	 * Returns next data entry in the page, and assigns rid to the next record
	 * ID
	 * 
	 * @param rid
	 * @return Next data entry in the page
	 * @throws IOException
	 * @throws KeyNotMatchException
	 * @throws NodeNotMatchException
	 * @throws ConvertException
	 * @throws InvalidSlotNumberException
	 */
	public KeyDataEntry getNext(RID rid) throws IOException,
			KeyNotMatchException, NodeNotMatchException, ConvertException,
			InvalidSlotNumberException {
		RID nrid = nextRecord(rid);// get next record id
		if (nrid == null)// empty page
			return null;
		else
			rid.copyRid(nrid);

		// Retrieving data of the next entry
		Tuple row = getRecord(nrid);
		return BT.getEntryFromBytes(row.getTupleByteArray(), row.getOffset(),
				row.getLength(), keyType, getType());
	}

	/**
	 * Get page id of the page containing the specified key
	 * 
	 * @param key
	 * @return PageID of the page containing the specified key
	 * @throws IOException
	 * @throws KeyNotMatchException
	 * @throws NodeNotMatchException
	 * @throws ConvertException
	 * @throws InvalidSlotNumberException
	 */
	public PageId getPageNoByKey(KeyClass key) throws IOException,
			KeyNotMatchException, NodeNotMatchException, ConvertException,
			InvalidSlotNumberException {
		RID rid = firstRecord();// first record id
		if (rid == null)// empty page
			return null;

		Tuple row = getRecord(rid);
		KeyDataEntry iEntry = BT.getEntryFromBytes(row.getTupleByteArray(), row
				.getOffset(), row.getLength(), keyType, getType());

		// Iterating until key is smaller than current entry key
		while (BT.keyCompare(iEntry.key, key) < 0) {
			rid = nextRecord(rid);
			if (rid == null)
				return null;
			row = getRecord(rid);
			iEntry = BT.getEntryFromBytes(row.getTupleByteArray(), row
					.getOffset(), row.getLength(), keyType, getType());
		}

		// key found in the page
		if (BT.keyCompare(iEntry.key, key) == 0) {
			return ((IndexData) iEntry.data).getData();
		} else {// key not found
			return null;
		}
	}

	/**
	 * Returns left pointer of the page as its previous page
	 * 
	 * @return
	 * @throws IOException
	 */
	public PageId getLeftLink() throws IOException {
		return getPrevPage();
	}

	/**
	 * Assigns left pointer of the page as its previous page
	 * 
	 * @param left
	 * @throws IOException
	 */
	public void setLeftLink(PageId left) throws IOException {
		setPrevPage(left);
	}
}

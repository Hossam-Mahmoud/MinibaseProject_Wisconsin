package btree;

import global.PageId;
import global.RID;
import heap.InvalidSlotNumberException;
import heap.Tuple;

import java.io.IOException;

import diskmgr.Page;

/* A BTLeafPage is a leaf page on a B+ tree.
 * It holds abstract pairs; it doesn't know anything
 * about the keys (their lengths or their types),
 * instead relying on the abstract interface consisting of BT.java. */
public class BTLeafPage extends BTSortedPage {
	/**
	 * Allocates a new leaf SortedPage with the given key type
	 * 
	 * @param key_type
	 * @throws ConstructPageException
	 * @throws IOException
	 */
	public BTLeafPage(int keyType) throws ConstructPageException, IOException {
		super(keyType);
		setType(NodeType.LEAF);// setting type
	}

	/**
	 * associate the BTLeafPage instance with the Page instance, also it sets
	 * the type of node to be NodeType.LEAF.
	 * 
	 * @param page
	 *            : page to be associated with
	 * @param key_type
	 *            : type of keys in the page
	 * @throws ConstructPageException
	 * @throws IOException
	 */
	public BTLeafPage(Page page, int keyType) throws IOException {
		super(page, keyType);
		setType(NodeType.LEAF);// setting type
	}

	/**
	 * pin the page with pageno, and get the corresponding BTLeafPage, also it
	 * sets the type of node to be NodeType.LEAF.
	 * 
	 * @param pageno
	 *            : page Id of the page
	 * @param key_type
	 *            : type of keys in the page
	 * @throws ConstructPageException
	 * @throws IOException
	 */
	public BTLeafPage(PageId pageNo, int keyType)
			throws ConstructPageException, IOException {
		super(pageNo, keyType);
		setType(NodeType.LEAF);// setting type
	}

	/**
	 * Inserts a record in a sorted order in the BTLeafPage
	 * 
	 * @param key
	 * @param pageNo
	 * @return RecordId of the new entry
	 * @throws InsertRecException
	 */
	public RID insertRecord(KeyClass key, RID dataRid)
			throws InsertRecException {
		KeyDataEntry data = new KeyDataEntry(key, dataRid);
		return super.insertRecord(data);// insert a sorted entry in the page
	}

	public boolean delEntry(KeyDataEntry dEntry) throws IOException,
			InvalidSlotNumberException, KeyNotMatchException,
			NodeNotMatchException, ConvertException, DeleteRecException {
		RID rid = firstRecord();// first record id

		if (rid == null)// empty page
			return false;

		Tuple row = getRecord(rid);
		KeyDataEntry iEntry = BT.getEntryFromBytes(row.getTupleByteArray(), row
				.getOffset(), row.getLength(), keyType, getType());

		// Iterating until key is smaller than current entry key
		while (BT.keyCompare(iEntry.key, dEntry.key) < 0) {
			rid = nextRecord(rid);
			if (rid == null)
				return false;
			row = getRecord(rid);
			iEntry = BT.getEntryFromBytes(row.getTupleByteArray(), row
					.getOffset(), row.getLength(), keyType, getType());
		}

		// key found in the page
		if (BT.keyCompare(iEntry.key, dEntry.key) == 0) {
			deleteSortedRecord(rid);
			return true;
		} else {// key not found
			return false;
		}
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
			InvalidSlotNumberException, KeyNotMatchException,
			NodeNotMatchException, ConvertException {
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
	 * Get data from this leafPage, and assigns rid to this record id
	 * 
	 * @param rid
	 * @return
	 * @throws InvalidSlotNumberException
	 * @throws IOException
	 * @throws KeyNotMatchException
	 * @throws NodeNotMatchException
	 * @throws ConvertException
	 */
	public KeyDataEntry getCurrent(RID rid) throws InvalidSlotNumberException,
			IOException, KeyNotMatchException, NodeNotMatchException,
			ConvertException {
		RID nrid = new RID();
		nrid.copyRid(rid);// copying record
		Tuple row = getRecord(nrid);
		if (row == null)// empty page
			return null;

		return BT.getEntryFromBytes(row.getTupleByteArray(), row.getOffset(),
				row.getLength(), keyType, getType());
	}
}

package query;

import index.HashIndex;
import global.Minibase;
import global.RID;
import global.SearchKey;
import heap.HeapFile;
import parser.AST_Insert;
import relop.Schema;
import relop.Tuple;

/**
 * Execution plan for inserting tuples.
 */
class Insert implements Plan {
	protected String fileName;
	protected Object[] values;
	protected Schema schema;

	public Insert(AST_Insert tree) throws QueryException {
		fileName = tree.getFileName();
		QueryCheck.tableExists(fileName);

		values = tree.getValues();
		schema = Minibase.SystemCatalog.getSchema(fileName);
		QueryCheck.insertValues(schema, values);
	}

	public void execute() {
		Tuple tuple = new Tuple(schema, values);

		HeapFile hfile = new HeapFile(fileName);
		RID rid = hfile.insertRecord(tuple.getData());

		IndexDesc[] inds = Minibase.SystemCatalog.getIndexes(fileName);
		for (IndexDesc ind : inds) {
			new HashIndex(ind.indexName).insertEntry(
					new SearchKey(tuple.getField(ind.columnName)), rid);
		}
		System.out.println("record Inserted");

	} // public void execute()

} // class Insert implements Plan

package query;

import index.HashIndex;
import global.Minibase;
import parser.AST_CreateIndex;
import relop.Schema;

/**
 * Execution plan for creating indexes.
 */
class CreateIndex implements Plan {
	protected String fileName;
	protected String ixTable;
	protected String ixColumn;
	protected Schema schema;

	/**
	 * Optimizes the plan, given the parsed query.
	 * 
	 * @throws QueryException
	 *             if index already exists or table/column invalid
	 */
	public CreateIndex(AST_CreateIndex tree) throws QueryException {

		fileName = tree.getFileName();
		QueryCheck.fileNotExists(fileName);
		// get and validate the requested schema

		ixTable = tree.getIxTable();
		QueryCheck.tableExists(ixTable);

		schema = Minibase.SystemCatalog.getSchema(ixTable);
		ixColumn = tree.getIxColumn();
		QueryCheck.columnExists(schema, ixColumn);
	} // public CreateIndex(AST_CreateIndex tree) throws QueryException

	/**
	 * Executes the plan and prints applicable output.
	 */
	public void execute() {

		// print the output message
		new HashIndex(fileName);
		Minibase.SystemCatalog.createIndex(fileName, ixTable, ixColumn);
		System.out.println("Index created.");

	} // public void execute()

} // class CreateIndex implements Plan

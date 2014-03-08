package query;

import java.util.ArrayList;

import global.Minibase;
import heap.HeapFile;
import parser.AST_Select;
import relop.FileScan;
import relop.Iterator;
import relop.Predicate;
import relop.Projection;
import relop.Schema;
import relop.Selection;
import relop.SimpleJoin;

/**
 * Execution plan for selecting tuples.
 */
class Select implements Plan {
	private String[] cols, tables;
	private Predicate[][] predicates;
	private Schema schema;
	private Iterator[] scans;

	/**
	 * Optimizes the plan, given the parsed query.
	 * 
	 * @throws QueryException
	 *             if validation fails
	 */
	public Select(AST_Select tree) throws QueryException {
		cols = tree.getColumns();
		predicates = tree.getPredicates();
		tables = tree.getTables();
		validateSelect();
		scans = new Iterator[tables.length];
		for (int i = 0; i < scans.length; i++) {
			schema = Minibase.SystemCatalog.getSchema(tables[i]);
			HeapFile hp = new HeapFile(tables[i]);
			scans[i] = new FileScan(schema, hp);
			Predicate[][] tablePredicates = tablePredicates(schema);
			if (tablePredicates.length > 0)
				for (int j = 0; j < tablePredicates.length; j++)
					scans[i] = new Selection(scans[i], tablePredicates[j]);
		}
	}

	private Predicate[][] tablePredicates(Schema schema) {
		boolean flag = true;
		ArrayList<Predicate[]> tablePredicates = new ArrayList<Predicate[]>();
		for (int i = 0; i < predicates.length; i++) {
			flag = true;
			if (predicates[i] != null) {
				for (int j = 0; j < predicates[i].length; j++) {
					if (!predicates[i][j].validate(schema)) {
						flag = false;
						break;
					}
				}
				if (flag == true) {
					tablePredicates.add(predicates[i]);
					predicates[i] = null;
				}
			}
		}
		Predicate[][] result = new Predicate[tablePredicates.size()][];

		for (int i = 0; i < tablePredicates.size(); i++) {
			result[i] = tablePredicates.get(i);
		}
		return result;
	}

	private void validateSelect() throws QueryException {
		Schema bigSchema = Minibase.SystemCatalog.getSchema(tables[0]);

		for (int i = 0; i < tables.length; i++)
			bigSchema = Schema.join(bigSchema,
					QueryCheck.tableExists(tables[i]));

		for (int i = 0; i < cols.length; i++)
			QueryCheck.columnExists(bigSchema, cols[i]);

		QueryCheck.predicates(bigSchema, predicates);

	}

	/**
	 * Executes the plan and prints applicable output.
	 */

	public void execute() {
		Iterator simpleJoin;
		if (tables.length > 1) {
			simpleJoin = new SimpleJoin(scans[0], scans[1]);
			for (int i = 2; i < tables.length; i++){
				simpleJoin = new SimpleJoin(simpleJoin, scans[i]);
			}
		} else
			simpleJoin = scans[0];

		for (int j = 0; j < predicates.length; j++)
			if (predicates[j] != null){
				simpleJoin = new Selection(simpleJoin, predicates[j]);
			}

		if (cols.length != 0) {
			Integer[] pCols = new Integer[cols.length];
			for (int i = 0; i < pCols.length; i++)
				pCols[i] = (Integer) simpleJoin.getSchema()
						.fieldNumber(cols[i]);
			
			
			Projection projection = new Projection(simpleJoin, pCols);
			System.out.println(projection.execute() + " row selected.");
			projection.explain(0);
			projection.close();
		} else {
			System.out.println(simpleJoin.execute() + " row selected.");
			simpleJoin.explain(0);
			simpleJoin.close();
		}
		
	}
}
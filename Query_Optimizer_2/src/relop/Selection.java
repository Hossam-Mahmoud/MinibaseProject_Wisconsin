package relop;

import java.util.Arrays;

import global.AttrType;

/**
 * The selection operator specifies which tuples to retain under a condition; in
 * Minibase, this condition is simply a set of independent predicates logically
 * connected by AND operators.
 */
public class Selection extends Iterator {

	private Iterator iter;
	private Predicate[] preds;
	private Tuple currTuple;
	private boolean check;
	private int i, sz;

	/**
	 * Constructs a selection, given the underlying iterator and predicates.
	 */
	public Selection(Iterator iter, Predicate... preds) {
		// throw new UnsupportedOperationException("Not implemented");
		this.iter = iter;
		this.preds = preds;

		this.setSchema(this.iter.getSchema());

		sz = this.preds.length;
		currTuple = null;
		check = false;
		while (iter.hasNext() && !check) {
			currTuple = iter.getNext();
			check = false;
			for (i = 0; !check && i < sz; ++i)
				check |= this.preds[i].evaluate(currTuple);
		}
	}

	/**
	 * Gives a one-line explaination of the iterator, repeats the call on any
	 * child iterators, and increases the indent depth along the way.
	 */
	public void explain(int depth) {
		String fls;
		fls = "";

		
//		this.preds[0].ltype = AttrType.STRING;
//		this.preds[0].rtype = AttrType.STRING;
		fls += this.preds[0].toString();
		for (i = 1; i < sz; ++i)
		{
//			this.preds[i].ltype = AttrType.STRING;
//			this.preds[i].rtype = AttrType.STRING;
			fls += " OR " + this.preds[i].toString();
		}
		System.out.println("Selection : " + fls);
		indent(depth + 1);
		iter.explain(depth+1);

	}

	/**
	 * Restarts the iterator, i.e. as if it were just constructed.
	 */
	public void restart() {
		iter.restart();
		currTuple = null;
		check = false;
		while (iter.hasNext() && !check) {
			currTuple = iter.getNext();
			check = false;
			for (i = 0; !check && (i < sz); ++i)
				check |= this.preds[i].evaluate(currTuple);
		}
		// throw new UnsupportedOperationException("Not implemented");
	}

	/**
	 * Returns true if the iterator is open; false otherwise.
	 */
	public boolean isOpen() {
		// throw new UnsupportedOperationException("Not implemented");
		return iter.isOpen();
	}

	/**
	 * Closes the iterator, releasing any resources (i.e. pinned pages).
	 */
	public void close() {
		// throw new UnsupportedOperationException("Not implemented");
		iter.close();
	}

	/**
	 * Returns true if there are more tuples, false otherwise.
	 */
	public boolean hasNext() {
		// throw new UnsupportedOperationException("Not implemented");
		return currTuple != null;
	}

	/**
	 * Gets the next tuple in the iteration.
	 * 
	 * @throws IllegalStateException
	 *             if no more tuples
	 */
	public Tuple getNext() {
		// throw new UnsupportedOperationException("Not implemented");

		if (currTuple == null) {
			throw new IllegalStateException("No More Tuples");
		}

		Tuple out = currTuple;
		currTuple = null;

		check = false;

		while (!check) {
			if (iter.hasNext()) {
				currTuple = iter.getNext();
				check = false;
				for (i = 0; !check && (i < sz); ++i)
					check |= this.preds[i].evaluate(currTuple);
			} else {
				currTuple = null;
				break;
			}

		}

		return out;
	}

} // public class Selection extends Iterator

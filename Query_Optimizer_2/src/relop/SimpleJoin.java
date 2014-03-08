package relop;

/**
 * The simplest of all join algorithms: nested loops (see textbook, 3rd edition,
 * section 14.4.1, page 454).
 */
public class SimpleJoin extends Iterator {

	private Iterator l;
	private Iterator r;
	private Predicate[] preds; 
	private Tuple currTuple;
	private Tuple currLTuple;
	private boolean check;
	private int i,sz;
	
  /**
   * Constructs a join, given the left and right iterators and join predicates
   * (relative to the combined schema).
   */
  public SimpleJoin(Iterator left, Iterator right, Predicate... preds) {
//    throw new UnsupportedOperationException("Not implemented");
	  l = left;
	  r = right;
	  this.preds = preds;
	  sz = this.preds.length;
	  
	  Schema s;
	  s = Schema.join(l.getSchema(), r.getSchema());
	  this.setSchema(s);
	  
	  
	  
	  
	  	currTuple = null;
	  	check = false;
	  	if(l.hasNext())
	  	{
	  	currLTuple = l.getNext();
	  	while(!check)
		{
			while(r.hasNext()&&!check)
			{
			   currTuple = Tuple.join(currLTuple,r.getNext(),s);		  
			   check = true;
			   for(i = 0 ; check && (i < sz);++i)
				   check&=this.preds[i].evaluate(currTuple);
			}
			
			if(l.hasNext()&&!check)
			{
				r.restart();
				currLTuple = l.getNext();
			}
		}	

	  	} 
  }

  /**
   * Gives a one-line explaination of the iterator, repeats the call on any
   * child iterators, and increases the indent depth along the way.
   */
  public void explain(int depth) {
//    throw new UnsupportedOperationException("Not implemented");
   System.out.println("SimpleJoin : (cross)");
   indent(depth+1);
   l.explain(depth+1);
   indent(depth+1);
   r.explain(depth+1);
  }

  /**
   * Restarts the iterator, i.e. as if it were just constructed.
   */
  public void restart() {
//    throw new UnsupportedOperationException("Not implemented");
	  l.restart();
	  r.restart();
	  // TODO reset the currTuple
	  	currTuple = null;
	  	check = false;
	  	
	  	if(l.hasNext())
	  	{
	  	currLTuple = l.getNext();
	  	
	  	while(!check)
		{
			while(r.hasNext()&&!check)
			{
			   currTuple = Tuple.join(currLTuple,r.getNext(),this.getSchema());		  
			   check = true;
			   for(i = 0 ; check && (i < sz);++i)
				   check&=this.preds[i].evaluate(currTuple);
			}
			
			if(!check)
			{
				r.restart();
				currLTuple = l.getNext();
			}
		}	
	  	}
  }

  /**
   * Returns true if the iterator is open; false otherwise.
   */
  public boolean isOpen() {
//    throw new UnsupportedOperationException("Not implemented");
  return l.isOpen()&&r.isOpen();
  }

  /**
   * Closes the iterator, releasing any resources (i.e. pinned pages).
   */
  public void close() {
//    throw new UnsupportedOperationException("Not implemented");
	  l.close();
	  r.close();
  }

  /**
   * Returns true if there are more tuples, false otherwise.
   */
  public boolean hasNext() {
//    throw new UnsupportedOperationException("Not implemented");
  return currTuple != null; 
  }

  /**
   * Gets the next tuple in the iteration.
   * 
   * @throws IllegalStateException if no more tuples
   */
  public Tuple getNext() {
//    throw new UnsupportedOperationException("Not implemented");
	 
	  	if(currTuple == null)
	  	{
	  		throw new IllegalStateException("No More Tuples");
	  	}
	  
	  
	  	Tuple out;	  	
	  	out = currTuple;
	  	currTuple = null;
	  	check = false;
	  	while((r.hasNext()||l.hasNext())&&!check)
		{
			while(r.hasNext()&&!check)
			{
			   currTuple = Tuple.join(currLTuple,r.getNext(),this.getSchema());		  
			   check = true;
			   for(i = 0 ; check && (i < sz);++i)
				   check&=this.preds[i].evaluate(currTuple);
			}
			
			if(!check)
			{
				r.restart();
				currLTuple = l.getNext();
			}
		}	
	
	  	return out;
  }

} // public class SimpleJoin extends Iterator

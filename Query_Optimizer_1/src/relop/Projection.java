package relop;

/**
 * The projection operator extracts columns from a relation; unlike in
 * relational algebra, this operator does NOT eliminate duplicate tuples.
 */
public class Projection extends Iterator {

	private Iterator iter;
	private Integer [] fields;
	private Tuple currTuple;
	private int i,sz;
	private Tuple temp;
	private Object[] valuesa;
  /**
   * Constructs a projection, given the underlying iterator and field numbers.
   */
  public Projection(Iterator iter, Integer... fields) {
//    throw new UnsupportedOperationException("Not implemented");
	  this.iter = iter;
	  this.fields = fields;
	  sz = this.fields.length;
	  
	  Schema s = new Schema (sz);
	  for(i = 0 ; i < sz ;++i)
		  s.initField(i, this.iter.getSchema(), this.fields[i]);
	  
	  this.setSchema(s);
	  currTuple = null;
		   
	  		if(iter.hasNext())
	  		{
			   
	  		   temp = iter.getNext();
	  		   valuesa = new Object[sz];
			   for(i = 0 ; (i < sz);++i)
				   valuesa[i]=temp.getField(this.fields[i]);
			   currTuple = new Tuple(s, valuesa);
			   
	  		}
  
  }

  /**
   * Gives a one-line explaination of the iterator, repeats the call on any
   * child iterators, and increases the indent depth along the way.
   */
  public void explain(int depth) {
//    throw new UnsupportedOperationException("Not implemented");
	  String fls;
	  fls= "";
	  
	  fls += this.getSchema().fieldName(this.fields[0]);
	  for(i = 1;i<sz;++i)
		  fls +=", "+this.getSchema().fieldName(this.fields[i]);  
	  
	  
	  System.out.println("Projection : "+fls);
	   indent(depth+1);
  
  }

  /**
   * Restarts the iterator, i.e. as if it were just constructed.
   */
  public void restart() {
//    throw new UnsupportedOperationException("Not implemented");
	  iter.restart();
	  currTuple = null;
	   
		if(iter.hasNext())
		{
		   
		   temp = iter.getNext();
		   valuesa = new Object[sz];
		   for(i = 0 ; (i < sz);++i)
			   valuesa[i]=temp.getField(this.fields[i]);
		   currTuple = new Tuple(this.getSchema(), valuesa);
		   
		}

  
  }

  /**
   * Returns true if the iterator is open; false otherwise.
   */
  public boolean isOpen() {
//    throw new UnsupportedOperationException("Not implemented");
	return  iter.isOpen();
  }

  /**
   * Closes the iterator, releasing any resources (i.e. pinned pages).
   */
  public void close() {
//    throw new UnsupportedOperationException("Not implemented");
	  iter.close();
  }

  /**
   * Returns true if there are more tuples, false otherwise.
   */
  public boolean hasNext() {
//    throw new UnsupportedOperationException("Not implemented");
	  return currTuple!=null;
  }

  /**
   * Gets the next tuple in the iteration.
   * 
   * @throws IllegalStateException if no more tuples
   */
  public Tuple getNext() {
//    throw new UnsupportedOperationException("Not implemented");
	  
	  if(currTuple==null)
	  {
		  throw new IllegalStateException("No More Tuples");
	  }
	  
	  Tuple out;
	  out = currTuple;
	  currTuple = null;
	   
		if(iter.hasNext())
		{
		   
		   temp = iter.getNext();
		   valuesa = new Object[sz];
		   for(i = 0 ; (i < sz);++i)
			   valuesa[i]=temp.getField(this.fields[i]);
		   currTuple = new Tuple(this.getSchema(), valuesa);
		   
		}

  return out;
  }

} // public class Projection extends Iterator

package bufmgr;

import chainexception.*;

public class BufferPoolExceededException extends ChainException {
	  public BufferPoolExceededException(Exception ex, String name)
	    { 
	      super(ex, name); 
	    }

}

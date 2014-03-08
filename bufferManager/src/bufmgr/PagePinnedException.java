package bufmgr;
import chainexception.*;

public class PagePinnedException extends ChainException {
	  public PagePinnedException(Exception ex, String name)
	    { 
	      super(ex, name); 
	    }

}

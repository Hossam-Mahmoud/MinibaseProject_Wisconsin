package diskmgr;
import chainexception.*;

public class FreePageException extends ChainException {
	public FreePageException(Exception ex, String name)
    { 
      super(ex, name); 
    }

}

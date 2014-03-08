package diskmgr;

import chainexception.ChainException;

public class InvalidPageAddressException extends ChainException{
	public InvalidPageAddressException(Exception e, String name)
    {
      super(e, name); 
    }
}

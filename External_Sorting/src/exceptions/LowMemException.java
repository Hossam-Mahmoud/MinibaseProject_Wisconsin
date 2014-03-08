package exceptions;
import chainexception.ChainException;


public class LowMemException extends ChainException {
	public LowMemException (Exception ex, String name){
		super(ex, name);
	}
}

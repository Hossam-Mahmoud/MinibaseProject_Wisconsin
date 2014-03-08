package exceptions;
import chainexception.ChainException;


public class SortException extends ChainException {
	public SortException(Exception ex, String name){
		super(ex, name);
	}
}

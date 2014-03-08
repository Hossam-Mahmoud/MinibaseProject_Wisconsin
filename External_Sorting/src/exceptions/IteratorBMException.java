package exceptions;
import chainexception.ChainException;


public class IteratorBMException extends ChainException {
	public IteratorBMException(Exception ex, String name){
		super(ex, name);
	}
}

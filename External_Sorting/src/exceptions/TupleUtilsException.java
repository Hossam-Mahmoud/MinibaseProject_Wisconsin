package exceptions;
import chainexception.ChainException;


public class TupleUtilsException extends ChainException {
	public TupleUtilsException(Exception ex, String name){
		super(ex, name);
	}
}

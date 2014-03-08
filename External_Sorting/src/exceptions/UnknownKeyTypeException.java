package exceptions;
import chainexception.ChainException;


public class UnknownKeyTypeException extends ChainException {
	public UnknownKeyTypeException(Exception ex, String name){
		super(ex, name);
	}
}

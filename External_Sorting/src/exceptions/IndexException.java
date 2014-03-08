package exceptions;
import chainexception.ChainException;


public class IndexException extends ChainException {
	public IndexException(Exception ex, String name){
		super(ex, name);
	}
}

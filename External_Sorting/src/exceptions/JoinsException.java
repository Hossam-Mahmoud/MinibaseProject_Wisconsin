package exceptions;
import chainexception.ChainException;


public class JoinsException extends ChainException {
	public JoinsException(Exception ex, String name){
		super(ex, name);
	}
}

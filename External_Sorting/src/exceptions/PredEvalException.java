package exceptions;
import chainexception.ChainException;


public class PredEvalException extends ChainException {
	public PredEvalException(Exception ex, String name){
		super(ex, name);
	}
}

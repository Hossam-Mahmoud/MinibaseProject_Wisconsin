package exceptions;
import chainexception.ChainException;


public class UnknowAttrType extends ChainException {
	public UnknowAttrType(Exception ex, String name){
		super(ex, name);
	}
}

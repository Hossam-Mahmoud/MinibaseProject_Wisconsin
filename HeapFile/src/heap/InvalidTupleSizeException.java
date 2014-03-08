package heap;

import chainexception.ChainException;

@SuppressWarnings("serial")
public class InvalidTupleSizeException extends ChainException {

	public InvalidTupleSizeException() {
	}

	public InvalidTupleSizeException(Exception exception, String s) {
		super(exception, s);
	}
}
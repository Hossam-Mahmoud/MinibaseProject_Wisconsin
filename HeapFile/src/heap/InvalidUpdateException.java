package heap;

import chainexception.ChainException;

public class InvalidUpdateException extends ChainException {
	public InvalidUpdateException() {
	}

	public InvalidUpdateException(Exception exception, String s) {
		super(exception, s);
	}

}

package heap;
 
import chainexception.ChainException;
 
@SuppressWarnings("serial")
public class FieldNumberOutOfBoundException extends ChainException
{
    public FieldNumberOutOfBoundException(Exception exception, String s)
    {
        super(exception, s);
    }
}
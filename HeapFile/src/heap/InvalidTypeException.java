package heap;
 
import chainexception.ChainException;
 
@SuppressWarnings("serial")
public class InvalidTypeException extends ChainException
{
 
    public InvalidTypeException()
    {
    }
 
    public InvalidTypeException(Exception exception, String s)
    {
        super(exception, s);
    }
}
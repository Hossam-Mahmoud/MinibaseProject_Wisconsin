package heap;
 
import chainexception.ChainException;
 
@SuppressWarnings("serial")
public class InvalidSlotNumberException extends ChainException
{
 
    public InvalidSlotNumberException()
    {
    }
 
    public InvalidSlotNumberException(Exception exception, String s)
    {
        super(exception, s);
    }
}
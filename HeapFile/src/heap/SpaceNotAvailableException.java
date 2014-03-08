package heap;
 
import chainexception.ChainException;
 
@SuppressWarnings("serial")
public class SpaceNotAvailableException extends ChainException
{
 
    public SpaceNotAvailableException()
    {
    }
 
    public SpaceNotAvailableException(Exception exception, String s)
    {
        super(exception, s);
    }
}
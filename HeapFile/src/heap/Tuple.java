package heap;
 
import global.*;
import java.io.IOException;
 
// Referenced classes of package heap:
//            FieldNumberOutOfBoundException, InvalidTupleSizeException, InvalidTypeException
 
public class Tuple
    implements GlobalConst
{
	  public static final int max_size = 1024;
	    private byte data[];
	    private int tuple_offset;
	    private int tuple_length;
	    private short fldCnt;
	    private short fldOffset[];
 
    public Tuple()
    {
        data = new byte[1024];
        tuple_offset = 0;
        tuple_length = 1024;
    }
 
    public Tuple(byte abyte0[], int i, int j)
    {
        data = abyte0;
        tuple_offset = i;
        tuple_length = j;
    }
 
    public Tuple(Tuple tuple)
    {
        data = tuple.getTupleByteArray();
        tuple_length = tuple.getLength();
        tuple_offset = 0;
        fldCnt = tuple.noOfFlds();
        fldOffset = tuple.copyFldOffset();
    }
 
    public Tuple(int i)
    {
        data = new byte[i];
        tuple_offset = 0;
        tuple_length = i;
    }
 
    public void tupleCopy(Tuple tuple)
    {
        byte abyte0[] = tuple.getTupleByteArray();
        System.arraycopy(abyte0, 0, data, tuple_offset, tuple_length);
    }
 
    public void tupleInit(byte abyte0[], int i, int j)
    {
        data = abyte0;
        tuple_offset = i;
        tuple_length = j;
    }
 
    public void tupleSet(byte abyte0[], int i, int j)
    {
        System.arraycopy(abyte0, i, data, 0, j);
        tuple_offset = 0;
        tuple_length = j;
    }
 
    public int getLength()
    {
        return tuple_length;
    }
 
    public short size()
    {
        return (short)(fldOffset[fldCnt] - tuple_offset);
    }
 
    public int getOffset()
    {
        return tuple_offset;
    }
 
    public byte[] getTupleByteArray()
    {
        byte abyte0[] = new byte[tuple_length];
        System.arraycopy(data, tuple_offset, abyte0, 0, tuple_length);
        return abyte0;
    }
 
    public byte[] returnTupleByteArray()
    {
        return data;
    }
 
    public int getIntFld(int i)
        throws IOException, FieldNumberOutOfBoundException
    {
        if(i > 0 && i <= fldCnt)
        {
            int j = Convert.getIntValue(fldOffset[i - 1], data);
            return j;
        } else
        {
            throw new FieldNumberOutOfBoundException(null, "TUPLE:TUPLE_FLDNO_OUT_OF_BOUND");
        }
    }
 
    public float getFloFld(int i)
        throws IOException, FieldNumberOutOfBoundException
    {
        if(i > 0 && i <= fldCnt)
        {
            float f = Convert.getFloValue(fldOffset[i - 1], data);
            return f;
        } else
        {
            throw new FieldNumberOutOfBoundException(null, "TUPLE:TUPLE_FLDNO_OUT_OF_BOUND");
        }
    }
 
    public String getStrFld(int i)
        throws IOException, FieldNumberOutOfBoundException
    {
        if(i > 0 && i <= fldCnt)
        {
            String s = Convert.getStrValue(fldOffset[i - 1], data, fldOffset[i] - fldOffset[i - 1]);
            return s;
        } else
        {
            throw new FieldNumberOutOfBoundException(null, "TUPLE:TUPLE_FLDNO_OUT_OF_BOUND");
        }
    }
 
    public char getCharFld(int i)
        throws IOException, FieldNumberOutOfBoundException
    {
        if(i > 0 && i <= fldCnt)
        {
            char c = Convert.getCharValue(fldOffset[i - 1], data);
            return c;
        } else
        {
            throw new FieldNumberOutOfBoundException(null, "TUPLE:TUPLE_FLDNO_OUT_OF_BOUND");
        }
    }
 
    public Tuple setIntFld(int i, int j)
        throws IOException, FieldNumberOutOfBoundException
    {
        if(i > 0 && i <= fldCnt)
        {
            Convert.setIntValue(j, fldOffset[i - 1], data);
            return this;
        } else
        {
            throw new FieldNumberOutOfBoundException(null, "TUPLE:TUPLE_FLDNO_OUT_OF_BOUND");
        }
    }
 
    public Tuple setFloFld(int i, float f)
        throws IOException, FieldNumberOutOfBoundException
    {
        if(i > 0 && i <= fldCnt)
        {
            Convert.setFloValue(f, fldOffset[i - 1], data);
            return this;
        } else
        {
            throw new FieldNumberOutOfBoundException(null, "TUPLE:TUPLE_FLDNO_OUT_OF_BOUND");
        }
    }
 
    public Tuple setStrFld(int i, String s)
        throws IOException, FieldNumberOutOfBoundException
    {
        if(i > 0 && i <= fldCnt)
        {
            Convert.setStrValue(s, fldOffset[i - 1], data);
            return this;
        } else
        {
            throw new FieldNumberOutOfBoundException(null, "TUPLE:TUPLE_FLDNO_OUT_OF_BOUND");
        }
    }
 
    public void setHdr(short word0, AttrType aattrtype[], short aword0[])
        throws IOException, InvalidTypeException, InvalidTupleSizeException
    {
        if((word0 + 2) * 2 > 1024)
            throw new InvalidTupleSizeException(null, "TUPLE: TUPLE_TOOBIG_ERROR");
        fldCnt = word0;
        Convert.setShortValue(word0, tuple_offset, data);
        fldOffset = new short[word0 + 1];
        int i = tuple_offset + 2;
        fldOffset[0] = (short)((word0 + 2) * 2 + tuple_offset);
        Convert.setShortValue(fldOffset[0], i, data);
        i += 2;
        short word1 = 0;
        int j;
        for(j = 1; j < word0; j++)
        {
            short word2;
            switch(aattrtype[j - 1].attrType)
            {
            case 1: // '\001'
                word2 = 4;
                break;
 
            case 2: // '\002'
                word2 = 4;
                break;
 
            case 0: // '\0'
                word2 = (short)(aword0[word1] + 2);
                word1++;
                break;
 
            default:
                throw new InvalidTypeException(null, "TUPLE: TUPLE_TYPE_ERROR");
            }
            fldOffset[j] = (short)(fldOffset[j - 1] + word2);
            Convert.setShortValue(fldOffset[j], i, data);
            i += 2;
        }
 
        short word3;
        switch(aattrtype[word0 - 1].attrType)
        {
        case 1: // '\001'
            word3 = 4;
            break;
 
        case 2: // '\002'
            word3 = 4;
            break;
 
        case 0: // '\0'
            word3 = (short)(aword0[word1] + 2);
            break;
 
        default:
            throw new InvalidTypeException(null, "TUPLE: TUPLE_TYPE_ERROR");
        }
        fldOffset[word0] = (short)(fldOffset[j - 1] + word3);
        Convert.setShortValue(fldOffset[word0], i, data);
        tuple_length = fldOffset[word0] - tuple_offset;
        if(tuple_length > 1024)
            throw new InvalidTupleSizeException(null, "TUPLE: TUPLE_TOOBIG_ERROR");
        else
            return;
    }
 
    public short noOfFlds()
    {
        return fldCnt;
    }
 
    public short[] copyFldOffset()
    {
        short aword0[] = new short[fldCnt + 1];
        for(int i = 0; i <= fldCnt; i++)
            aword0[i] = fldOffset[i];
 
        return aword0;
    }
 
    public void print(AttrType aattrtype[])
        throws IOException
    {
        System.out.print("[");
        int i;
        for(i = 0; i < fldCnt - 1; i++)
        {
            switch(aattrtype[i].attrType)
            {
            case 1: // '\001'
                int j = Convert.getIntValue(fldOffset[i], data);
                System.out.print(j);
                break;
 
            case 2: // '\002'
                float f = Convert.getFloValue(fldOffset[i], data);
                System.out.print(f);
                break;
 
            case 0: // '\0'
                String s = Convert.getStrValue(fldOffset[i], data, fldOffset[i + 1] - fldOffset[i]);
                System.out.print(s);
                break;
            }
            System.out.print(", ");
        }
 
        switch(aattrtype[fldCnt - 1].attrType)
        {
        case 1: // '\001'
            int k = Convert.getIntValue(fldOffset[i], data);
            System.out.print(k);
            break;
 
        case 2: // '\002'
            float f1 = Convert.getFloValue(fldOffset[i], data);
            System.out.print(f1);
            break;
 
        case 0: // '\0'
            String s1 = Convert.getStrValue(fldOffset[i], data, fldOffset[i + 1] - fldOffset[i]);
            System.out.print(s1);
            break;
        }
        System.out.println("]");
    }
 
}
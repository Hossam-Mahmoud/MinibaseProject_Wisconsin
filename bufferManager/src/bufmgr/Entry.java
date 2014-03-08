package bufmgr;
//this class act as the node of the hash table 
public class Entry<k,v>{
	protected k key;
	protected v value;
	protected boolean avilable;
	public Entry(k k,v v)
	{
		key=k;
		value=v;
		avilable =false;
	}
	// this method used to set the key
	public void setKey(k key) {
		this.key = key;
	}
	//this method used to return the key
	public k getKey() {
		return key;
	}
	//this method used to set the value
	public void setValue(v value) {
		this.value = value;
	}
	//this method used to return the value
	public v getValue() {
		return value;
	}
	
	//check if the node is empty
	public boolean isEmpty(){
		return key==null&&value==null;
	}
	//this method used to print the node
	public String toString(){
		if(isEmpty())
			return null;
		return key.toString()+" "+value.toString();
	}
	//this method used to clear the method
	public void clear() {
		key=null;
		value=null;
		
	}
}
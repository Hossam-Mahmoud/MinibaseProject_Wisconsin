package bufmgr;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

public class HashTable<k, v> {
	private int capacity;
	private int scale;
	private int shift;
	private LinkedList<Entry<k, v>> a[];

	// hash table constructor arrays of lists with random scale and shift
	public HashTable() {
		capacity = 1023;
		Random r = new Random();
		scale = r.nextInt(capacity - 1) + 1;
		shift = r.nextInt(capacity);
		a = (LinkedList<Entry<k, v>>[]) new LinkedList[1024];
		for (int i = 0; i < a.length; i++)
			a[i] = new LinkedList<Entry<k, v>>();
	}

	// this method used to check if the hash contain the key
	public boolean conatin(k Key) {
		return get(Key) == null ? false : true;
	}

	// this method take key and value as parameters and add them in the hash
	// table
	// if the hash contain a value with this key it change it with the parameter
	// value
	public void put(k key, v value) {
		int i = hashValue(key);
		Iterator<Entry<k, v>> it = a[i].iterator();
		Entry<k, v> e = null;
		boolean found = false;
		while (it.hasNext() && !found) {
			e = it.next();
			if (e.getKey() == key) {
				found = true;
				e.setValue(value);
			}
		}
		if (!found) {
			a[i].push(new Entry(key, value));
		}
	}

	// this method take the key the as a parameter and return it's hash value
	private int hashValue(k key) {
		return Math.abs((scale * key.hashCode() + shift) % capacity);
	}

	// this method take the key as a parameter and return the value with this
	// key
	// if the hash didn't a value with this key at return null
	public v get(k key) {
		int i = hashValue(key);
		Iterator<Entry<k, v>> it = a[i].iterator();

		Entry<k, v> e = null;
		while (it.hasNext()) {
			e = it.next();
			if (e.getKey().hashCode() == key.hashCode()) {
				return e.getValue();
			}
		}
		return null;
	}

	//this method take the key as a parameter and remove the appropriate value if contained in the hash
	public void remove(k key) {
		int i = hashValue(key);
		Iterator<Entry<k, v>> it = a[i].iterator();
		Entry<k, v> e = null;
		int count = -1;
		while (it.hasNext() && count != -2) {
			count++;
			e = it.next();
			if (e.getKey().hashCode() == key.hashCode()) {
				a[i].remove(count);
				count = -2;
			}

		}
	}

	//this method used to print the hash
	public String toString() {
		int count = 0;
		for (int i = 0; i < capacity; i++)
			if (a[i] != null && !a[i].isEmpty())
				count++;
		return Arrays.toString(a);

	}


}

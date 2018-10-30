package Server.LockManager;

import java.util.Vector;
import java.util.Enumeration;

/* HashTable class for the Lock Manager */

public class TPHashTable
{
	private static final int HASH_DEPTH = 8;

	private Vector<Vector<TransactionObject>> m_vector;
	private int m_tableSize;

	TPHashTable(int p_tableSize)
	{
		m_tableSize = p_tableSize;

		m_vector = new Vector<Vector<TransactionObject>>(p_tableSize);
		for (int i = 0; i < p_tableSize; i++)
		{
			m_vector.addElement(new Vector<TransactionObject>(HASH_DEPTH));
		}
	}

	public int getSize()
	{
		return m_tableSize;
	}

	public synchronized void add(TransactionObject xobj)
	{
		if (xobj == null) return;

		Vector<TransactionObject> vectSlot;

		int hashSlot = (xobj.hashCode() % m_tableSize);
		if (hashSlot < 0){
			hashSlot = -hashSlot;
		}
		vectSlot = m_vector.elementAt(hashSlot);
		vectSlot.addElement(xobj);
	}

	public synchronized Vector<TransactionObject> elements(TransactionObject xobj)
	{
		if (xobj == null) return (new Vector<TransactionObject>());

		Vector<TransactionObject> vectSlot; // hash slot
		Vector<TransactionObject> elemVect = new Vector<TransactionObject>(24); // return object

		int hashSlot = (xobj.hashCode() % m_tableSize);
		if (hashSlot < 0) {
			hashSlot = -hashSlot;
		}

		vectSlot = m_vector.elementAt(hashSlot);

		TransactionObject xobj2;
		int size = vectSlot.size();
		for (int i = (size - 1); i >= 0; i--) {
			xobj2 = vectSlot.elementAt(i);
			if (xobj.key() == xobj2.key()) {
				elemVect.addElement(xobj2);
			}
		}
		return elemVect;
	}

	public synchronized boolean contains(TransactionObject xobj)
	{
		if (xobj == null) return false;

		Vector<TransactionObject> vectSlot;

		int hashSlot = (xobj.hashCode() % m_tableSize);
		if (hashSlot < 0) {
			hashSlot = -hashSlot;
		}

		vectSlot = m_vector.elementAt(hashSlot);
		return vectSlot.contains(xobj);
	}

	public synchronized boolean remove(TransactionObject xobj)
	{
		if (xobj == null) return false;

		Vector<TransactionObject> vectSlot;

		int hashSlot = (xobj.hashCode() % m_tableSize);
		if (hashSlot < 0) {
			hashSlot = -hashSlot;
		}

		vectSlot = m_vector.elementAt(hashSlot);
		return vectSlot.removeElement(xobj);
	}

	public synchronized TransactionObject get(TransactionObject xobj)
	{
		if (xobj == null) return null;

		Vector<TransactionObject> vectSlot;

		int hashSlot = (xobj.hashCode() % m_tableSize);
		if (hashSlot < 0) {
			hashSlot = -hashSlot;
		}

		vectSlot = m_vector.elementAt(hashSlot);

		TransactionObject xobj2;
		int size = vectSlot.size();
		for (int i = 0; i < size; i++) {
			xobj2 = (TransactionObject)vectSlot.elementAt(i);
			if (xobj.equals(xobj2)) {
				return xobj2;
			}
		}
		return null;
	}

	private void printStatus(String msg, int hashSlot, TransactionObject xobj)
	{
		System.out.println(this.getClass() + "::" + msg + "(slot" + hashSlot + ")::" + xobj.toString());
	}

	public Vector<TransactionObject> allElements()
	{
		Vector<TransactionObject> vectSlot = null;
		TransactionObject xobj = null;
		Vector<TransactionObject> hashContents = new Vector<TransactionObject>(1024);

		for (int i = 0; i < m_tableSize; i++) { // walk down hashslots
			if (m_vector.size() > 0) { // contains elements?
				vectSlot = m_vector.elementAt(i);

				for (int j = 0; j < vectSlot.size(); j++) { // walk down single hash slot, adding elements.
					xobj = vectSlot.elementAt(j);
					hashContents.addElement(xobj);
				}
			}
			// else contributes nothing.
		}

		return hashContents;
	}

	public synchronized void removeAll(TransactionObject xobj)
	{
		if (xobj == null) return;

		Vector<TransactionObject> vectSlot;

		int hashSlot = (xobj.hashCode() % m_tableSize);
		if (hashSlot < 0) {
			hashSlot = -hashSlot;
		}

		vectSlot = m_vector.elementAt(hashSlot);

		TransactionObject xobj2;
		int size = vectSlot.size();
		for (int i = (size - 1); i >= 0; i--) {
			xobj2 = vectSlot.elementAt(i);
			if (xobj.key() == xobj2.key()) {
				vectSlot.removeElementAt(i);
			}
		}
	}
}

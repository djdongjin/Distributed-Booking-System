package Server.LockManager;

public class DataLockObject extends TransactionLockObject
{
	// The data members inherited are
	// TransactionObject:: protected int xid;
	// TransactionLockObject:: protected String data;
	// TransactionLockObject:: protected int lockType;

	DataLockObject()
	{
		super();
	}

	DataLockObject(int xid, String data, LockType lockType)
	{
		super(xid, data, lockType);
	}

	public int hashCode()
	{
		return m_data.hashCode();
	}

	public int key()
	{
		return m_data.hashCode();
	}

	public Object clone()
	{
		return new DataLockObject(m_xid, m_data, m_lockType);
	}
}

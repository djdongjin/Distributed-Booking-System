package Server.LockManager;

public class TransactionLockObject extends TransactionObject
{
	public enum LockType {
		LOCK_READ,
		LOCK_WRITE,
		LOCK_UNKNOWN
	};

	protected String m_data = null;
	protected LockType m_lockType = LockType.LOCK_UNKNOWN;

	// The data members inherited are 
	// TransactionObject::protected int m_xid = 0;

	TransactionLockObject()
	{
		super();
		m_data = null;
		m_lockType = LockType.LOCK_UNKNOWN;
	}

	TransactionLockObject(int xid, String data, LockType lockType)
	{
		super(xid);
		m_data = new String(data);
		m_lockType = lockType;
	}

	public String toString()
	{
		return super.toString() + "::strData(" + m_data + ")::lockType(" + m_lockType + ")";
	}

	public boolean equals(Object t)
	{
		if (t == null) { 
			return false;
		}
		if (t instanceof TransactionLockObject) {
			if (m_xid == ((TransactionLockObject)t).getXId()) {
				if (m_data.equals(((TransactionLockObject)t).getDataName())) {
					if (m_lockType == ((TransactionLockObject)t).getLockType()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public Object clone()
	{
		return new TransactionLockObject(m_xid, m_data,m_lockType);
	}

	public void setDataName(String data)
	{
		m_data = new String(data);
	}

	public String getDataName()
	{
		return new String(m_data);
	}

	public void setLockType(LockType lockType)
	{
		m_lockType = lockType;
	}

	public LockType getLockType()
	{
		return m_lockType;
	}
}

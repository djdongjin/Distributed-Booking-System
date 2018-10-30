package Server.LockManager;

public class TransactionObject
{
	protected int m_xid = 0;

	TransactionObject()
	{
		super();
		m_xid = 0;
	}

	TransactionObject(int xid)
	{
		super();

		if (xid > 0) {
			m_xid = xid;
		} else {
			m_xid = 0;
		}
	}

	public String toString()
	{
		return new String(this.getClass() + "::xid(" + m_xid + ")");
	}

	public int getXId()
	{
		return m_xid;
	}

	public int hashCode()
	{
		return m_xid;
	}

	public boolean equals(Object xobj)
	{
		if (xobj == null) return false;

		if (xobj instanceof TransactionObject) {
			if (m_xid == ((TransactionObject)xobj).getXId()) {
				return true;
			}
		}
		return false;
	}

	public Object clone()
	{
		try {
			TransactionObject xobj = (TransactionObject)super.clone();
			xobj.setXId(m_xid);
			return xobj;
		}
		catch (CloneNotSupportedException e) {
			return null;
		}
	}

	public int key()
	{
		return m_xid;
	}

	// Used by clone
	public void setXId(int xid) {
		if (xid > 0) {
			m_xid = xid;
		}
		return;
	}
}

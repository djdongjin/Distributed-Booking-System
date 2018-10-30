package Server.LockManager;

/* The transaction requested a lock that it already had. */ 

public class RedundantLockRequestException extends Exception
{
	protected int m_xid = 0;

	public RedundantLockRequestException(int xid, String msg)
	{
		super(msg);
		m_xid = xid;
	}

	public int getXId()
	{
		return m_xid;
	}
}

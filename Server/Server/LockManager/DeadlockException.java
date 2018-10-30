package Server.LockManager;

/* The transaction is deadlocked. Somebody should abort it. */

public class DeadlockException extends Exception
{
	private int m_xid = 0;

	public DeadlockException(int xid, String msg)
	{
		super("The transaction " + xid + " is deadlocked:" + msg);
		m_xid = xid;
	}

	int getXId()
	{
		return m_xid;
	}
}

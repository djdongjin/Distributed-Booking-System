package Server.LockManager;

import java.util.Date;

public class TimeObject extends TransactionObject
{
	private Date m_date = new Date();

	// The data members inherited are
	// TransactionObject:: private int m_xid;

	TimeObject()
	{
		super();
	}

	TimeObject(int xid)
	{
		super(xid);
	}

	public long getTime()
	{
		return m_date.getTime();
	}
}

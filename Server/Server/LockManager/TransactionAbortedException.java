package Server.LockManager;

public class TransactionAbortedException extends Exception
{
    protected int m_xid = 0;

    public TransactionAbortedException(int xid, String msg)
    {
        super(msg);
        m_xid = xid;
    }

    public int getXId()
    {
        return m_xid;
    }
}
package Server.LockManager;

public class InvalidTransactionException extends Exception
{
    protected int m_xid = 0;

    public InvalidTransactionException(int xid, String msg)
    {
        super(msg);
        m_xid = xid;
    }

    public int getXId()
    {
        return m_xid;
    }
}

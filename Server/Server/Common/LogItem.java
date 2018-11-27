package Server.Common;

import java.io.Serializable;

public class LogItem implements Serializable {
    public int xid;
    public String info;
    public LogItem(int xid, String info)
    {
        this.xid = xid;
        this.info = info;
    }
}

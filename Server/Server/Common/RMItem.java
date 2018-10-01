// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import java.io.*;

// Resource manager data item
public abstract class RMItem implements Serializable, Cloneable
{
	RMItem()
	{
		super();
	}

	public Object clone()
	{
		try {
			return super.clone();
		}
		catch (CloneNotSupportedException e) {
			return null;
		}
	}
}


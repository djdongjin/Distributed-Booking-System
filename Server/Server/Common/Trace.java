// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

// A simple wrapper around System.out.println, allows us to disable some
// of the verbose output from RM, TM, and WC if we want
public class Trace
{
	public static void info(String msg)
	{
		System.out.println(getThreadID() + " INFO: " + msg);
	}
	public static void warn(String msg)
	{
		System.out.println(getThreadID() + " WARN: " + msg);
	}
	public static void error(String msg)
	{
		System.err.println(getThreadID() + " ERROR: " + msg);
	}
	private static String getThreadID()
	{
		String s = Thread.currentThread().getName();

		// Shorten
		// 	"RMI TCP Connection(x)-hostname/99.99.99.99"
		// to
		// 	"RMI TCP Cx(x)"
		if(s.startsWith("RMI TCP Connection("))
		{
			return "RMI Cx" +  s.substring(s.indexOf('('), s.indexOf(')')) + ")";
		}
		return s;
	}
}


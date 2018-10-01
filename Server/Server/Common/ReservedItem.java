// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

// Represents a customer's "reserved item" (e.g. Flight, Car, or Room)
// NOTE: if a customer reserves more than one item of the same kind, this is stored as a single
// instance of ReservedItem reflecting the *latest price*
public class ReservedItem extends RMItem
{
	private int m_nCount;
	private int m_nPrice;
	private String m_strReservableItemKey;
	private String m_strLocation;

	ReservedItem(String key, String location, int count, int price)
	{
		super();
		m_strReservableItemKey = key;
		m_strLocation = location;
		m_nCount = count;
		m_nPrice = price;
	}

	public String getReservableItemKey()
	{
		return m_strReservableItemKey;
	}

	public String getLocation()
	{
		return m_strLocation;
	}

	public void setCount(int count)
	{
		m_nCount = count;
	}

	public int getCount()
	{
		return m_nCount;
	}

	public void setPrice(int price)
	{
		m_nPrice = price;
	}

	public int getPrice()
	{
		return m_nPrice;
	}

	public String toString()
	{
		return "hashkey='" + getKey() + "', reservableItemKey='" + getReservableItemKey() +
			"', count='" + getCount() + "', price='" + getPrice() + "'";
	}

	// NOTE: hashKey is the same as the ReservableItem hashkey--this would have to change if we
	// weren't lumping all reservable items under the same price...
	public String getKey()
	{
		String s = getReservableItemKey();
		return s.toLowerCase();
	}
}


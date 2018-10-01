// -------------------------------
// Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import java.io.*;

// Superclass for the three reservable items: Flight, Car, and Room
public abstract class ReservableItem extends RMItem implements Serializable
{
	private int m_nCount;
	private int m_nPrice;
	private int m_nReserved;
	private String m_location;

	public ReservableItem(String location, int count, int price)
	{
		super();
		m_location = location;
		m_nCount = count;
		m_nPrice = price;
		m_nReserved = 0;
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

	public void setReserved(int r)
	{
		m_nReserved = r;
	}

	public int getReserved()
	{
		return m_nReserved;
	}

	public String getLocation()
	{
		return m_location;
	}

	public String toString()
	{
		return "RESERVABLEITEM key='" + getKey() + "', location='" + getLocation() +
			"', count='" + getCount() + "', price='" + getPrice() + "'";
	}

	public abstract String getKey();

	public Object clone()
	{
		ReservableItem obj = (ReservableItem)super.clone();
		obj.m_location = m_location;
		obj.m_nCount = m_nCount;
		obj.m_nPrice = m_nPrice;
		obj.m_nReserved = m_nReserved;
		return obj;
	}
}


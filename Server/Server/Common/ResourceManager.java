// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import Server.Interface.*;
import Server.LockManager.TransactionAbortedException;
import Server.LockManager.InvalidTransactionException;

import java.io.*;

import java.util.*;
import java.rmi.RemoteException;

public class ResourceManager implements IResourceManager {

	protected Vector<Integer> master_rec = new Vector<>();
	protected String m_name = "";
	protected RMHashMap m_data = new RMHashMap();
	protected Hashtable<Integer, Long> xid_time = new Hashtable<>();
	protected Hashtable<Integer, Boolean> xid_yn = new Hashtable<>();
	private static long MAX_EXIST_TIME_RM = 100000;

	// TODO: Crash mode 5 in RM recovery
	protected ArrayList<Boolean> crash_rm = new ArrayList<>(6);

	// protected HashMap<Integer, RMHashMap> origin_data = new HashMap<>();
	protected HashMap<Integer, RMHashMap> local_copy = new HashMap<>();

	public ResourceManager(String p_name) {
		m_name = p_name;
		for (int i=0; i<6; i++)
			crash_rm.add(false);
		master_rec.add(0);
		master_rec.add(0);
	}

	// Reads a data item
	protected RMItem readData(int xid, String key) {
		if (!xid_time.containsKey(xid))
			writeLog(new LogItem(xid, "INIT"));
		xid_time.put(xid, System.currentTimeMillis());
		xid_yn.put(xid, true);
		synchronized (local_copy) {
			if (local_copy.containsKey(xid)) {
				RMHashMap local_copy_xid = local_copy.get(xid);
				RMItem item = local_copy_xid.get(key);
				if (item != null) {
					return (RMItem) item.clone();
				}
			}
		}
		synchronized (m_data) {
			RMItem item = m_data.get(key);
			if (item != null) {
				return (RMItem) item.clone();
			}
			return null;
		}
	}

	// Writes a data item
	protected void writeData(int xid, String key, RMItem value) {
		if (!xid_time.containsKey(xid))
			writeLog(new LogItem(xid, "INIT"));
		xid_time.put(xid, System.currentTimeMillis());
		xid_yn.put(xid, true);
		synchronized (local_copy) {
			if (!local_copy.containsKey(xid)) {
				RMHashMap new_hashmap = new RMHashMap();
				local_copy.put(xid, new_hashmap);
			}
			RMHashMap xid_hashmap = local_copy.get(xid);
			xid_hashmap.put(key, value);
			local_copy.put(xid, xid_hashmap);
		}
	}

	// Remove the item out of storage
	protected void removeData(int xid, String key) {
		if (!xid_time.containsKey(xid))
			writeLog(new LogItem(xid, "INIT"));
		xid_time.put(xid, System.currentTimeMillis());
		xid_yn.put(xid, true);
		synchronized (local_copy) {
			if (!local_copy.containsKey(xid)) {
				RMHashMap new_hashmap = new RMHashMap();
				local_copy.put(xid, new_hashmap);
			}
			RMHashMap xid_hashmap = local_copy.get(xid);
			xid_hashmap.put(key, null);
			local_copy.put(xid, xid_hashmap);
		}
	}

	public int modify(int xid, String key, int num)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		ReservableItem item = (ReservableItem) readData(xid, key);
		if (item == null) {
			return -1;
		} else if (item.getCount() + num < 0) {
			return 0;
		}
		int original = item.getCount();
		item.setCount(item.getCount() + num);
		item.setReserved(item.getReserved() - num);
		writeData(xid, key, item);
		int new_count = item.getCount();
		Trace.info(
				"RM::modify (" + xid + ", " + key + ") called, old count: " + original + " , new count: " + new_count);
		return item.getPrice();
	}

	// Deletes the encar item
	protected boolean deleteItem(int xid, String key) {
		Trace.info("RM::deleteItem(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem) readData(xid, key);
		// Check if there is such an item in the storage
		if (curObj == null) {
			Trace.warn("RM::deleteItem(" + xid + ", " + key + ") failed--item doesn't exist");
			return false;
		} else {
			if (curObj.getReserved() == 0) {
				removeData(xid, curObj.getKey());
				Trace.info("RM::deleteItem(" + xid + ", " + key + ") item deleted");
				return true;
			} else {
				Trace.info("RM::deleteItem(" + xid + ", " + key
						+ ") item can't be deleted because some customers have reserved it");
				return false;
			}
		}
	}

	// Query the number of available seats/rooms/cars
	protected int queryNum(int xid, String key) {
		Trace.info("RM::queryNum(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem) readData(xid, key);
		int value = 0;
		if (curObj != null) {
			value = curObj.getCount();
		}
		Trace.info("RM::queryNum(" + xid + ", " + key + ") returns count=" + value);
		return value;
	}

	// Query the price of an item
	protected int queryPrice(int xid, String key) {
		Trace.info("RM::queryPrice(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem) readData(xid, key);
		int value = 0;
		if (curObj != null) {
			value = curObj.getPrice();
		}
		Trace.info("RM::queryPrice(" + xid + ", " + key + ") returns cost=$" + value);
		return value;
	}

	// Reserve an item
	protected boolean reserveItem(int xid, int customerID, String key, String location) {
		Trace.info("RM::reserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called");
		// Read customer object if it exists (and read lock it)
		Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
		if (customer == null) {
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location
					+ ")  failed--customer doesn't exist");
			return false;
		}

		// Check if the item is available
		ReservableItem item = (ReservableItem) readData(xid, key);
		if (item == null) {
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location
					+ ") failed--item doesn't exist");
			return false;
		} else if (item.getCount() == 0) {
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location
					+ ") failed--No more items");
			return false;
		} else {
			customer.reserve(key, location, item.getPrice());
			writeData(xid, customer.getKey(), customer);

			// Decrease the number of available items in the storage
			item.setCount(item.getCount() - 1);
			item.setReserved(item.getReserved() + 1);
			writeData(xid, item.getKey(), item);

			Trace.info("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
			return true;
		}
	}

	// Create a new flight, or add seats to existing flight
	// NOTE: if flightPrice <= 0 and the flight already exists, it maintains its
	// current price
	public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
		Flight curObj = (Flight) readData(xid, Flight.getKey(flightNum));
		if (curObj == null) {
			// Doesn't exist yet, add it
			Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
			writeData(xid, newObj.getKey(), newObj);
			Trace.info("RM::addFlight(" + xid + ") created new flight " + flightNum + ", seats=" + flightSeats
					+ ", price=$" + flightPrice);
		} else {
			// Add seats to existing flight and update the price if greater than zero
			curObj.setCount(curObj.getCount() + flightSeats);
			if (flightPrice > 0) {
				curObj.setPrice(flightPrice);
			}
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::addFlight(" + xid + ") modified existing flight " + flightNum + ", seats="
					+ curObj.getCount() + ", price=$" + flightPrice);
		}
		return true;
	}

	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its current
	// price
	public boolean addCars(int xid, String location, int count, int price)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		Trace.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		Car curObj = (Car) readData(xid, Car.getKey(location));
		if (curObj == null) {
			// Car location doesn't exist yet, add it
			Car newObj = new Car(location, count, price);
			writeData(xid, newObj.getKey(), newObj);
			Trace.info("RM::addCars(" + xid + ") created new location " + location + ", count=" + count + ", price=$"
					+ price);
		} else {
			// Add count to existing car location and update price if greater than zero
			curObj.setCount(curObj.getCount() + count);
			if (price > 0) {
				curObj.setPrice(price);
			}
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::addCars(" + xid + ") modified existing location " + location + ", count="
					+ curObj.getCount() + ", price=$" + price);
		}
		return true;
	}

	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains its
	// current price
	public boolean addRooms(int xid, String location, int count, int price)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		Trace.info("RM::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		Room curObj = (Room) readData(xid, Room.getKey(location));
		if (curObj == null) {
			// Room location doesn't exist yet, add it
			Room newObj = new Room(location, count, price);
			writeData(xid, newObj.getKey(), newObj);
			Trace.info("RM::addRooms(" + xid + ") created new room location " + location + ", count=" + count
					+ ", price=$" + price);
		} else {
			// Add count to existing object and update price if greater than zero
			curObj.setCount(curObj.getCount() + count);
			if (price > 0) {
				curObj.setPrice(price);
			}
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::addRooms(" + xid + ") modified existing location " + location + ", count="
					+ curObj.getCount() + ", price=$" + price);
		}
		return true;
	}

	// Deletes flight
	public boolean deleteFlight(int xid, int flightNum)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		return deleteItem(xid, Flight.getKey(flightNum));
	}

	// Delete cars at a location
	public boolean deleteCars(int xid, String location)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		return deleteItem(xid, Car.getKey(location));
	}

	// Delete rooms at a location
	public boolean deleteRooms(int xid, String location)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		return deleteItem(xid, Room.getKey(location));
	}

	// Returns the number of empty seats in this flight
	public int queryFlight(int xid, int flightNum)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		return queryNum(xid, Flight.getKey(flightNum));
	}

	// Returns the number of cars available at a location
	public int queryCars(int xid, String location)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		return queryNum(xid, Car.getKey(location));
	}

	// Returns the amount of rooms available at a location
	public int queryRooms(int xid, String location)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		return queryNum(xid, Room.getKey(location));
	}

	// Returns price of a seat in this flight
	public int queryFlightPrice(int xid, int flightNum)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		return queryPrice(xid, Flight.getKey(flightNum));
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int xid, String location)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		return queryPrice(xid, Car.getKey(location));
	}

	// Returns room price at this location
	public int queryRoomsPrice(int xid, String location)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		return queryPrice(xid, Room.getKey(location));
	}

	public String queryCustomerInfo(int xid, int customerID)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
		if (customer == null) {
			Trace.warn("RM::queryCustomerInfo(" + xid + ", " + customerID + ") failed--customer doesn't exist");
			// NOTE: don't change this--WC counts on this value indicating a customer does
			// not exist...
			return "";
		} else {
			Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ")");
			System.out.println(customer.getBill());
			return customer.getBill();
		}
	}

	public int newCustomer(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		Trace.info("RM::newCustomer(" + xid + ") called");
		// Generate a globally unique ID for the new customer
		int cid = Integer
				.parseInt(String.valueOf(xid) + String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND))
						+ String.valueOf(Math.round(Math.random() * 100 + 1)));
		Customer customer = new Customer(cid);
		writeData(xid, customer.getKey(), customer);
		Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
		return cid;
	}

	public boolean newCustomer(int xid, int customerID)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
		if (customer == null) {
			customer = new Customer(customerID);
			writeData(xid, customer.getKey(), customer);
			Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") created a new customer");
			return true;
		} else {
			Trace.info("INFO: RM::newCustomer(" + xid + ", " + customerID + ") failed--customer already exists");
			return false;
		}
	}

	public boolean deleteCustomer(int xid, int customerID)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
		if (customer == null) {
			Trace.warn("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
			return false;
		} else {
			// Increase the reserved numbers of all reservable items which the customer
			// reserved.
			RMHashMap reservations = customer.getReservations();
			for (String reservedKey : reservations.keySet()) {
				ReservedItem reserveditem = customer.getReservedItem(reservedKey);
				Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey()
						+ " " + reserveditem.getCount() + " times");
				ReservableItem item = (ReservableItem) readData(xid, reserveditem.getKey());
				Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey()
						+ " which is reserved " + item.getReserved() + " times and is still available "
						+ item.getCount() + " times");
				item.setReserved(item.getReserved() - reserveditem.getCount());
				item.setCount(item.getCount() + reserveditem.getCount());
				writeData(xid, item.getKey(), item);
			}

			// Remove the customer from the storage
			removeData(xid, customer.getKey());
			Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
			return true;
		}
	}

	// Adds flight reservation to this customer
	public boolean reserveFlight(int xid, int customerID, int flightNum)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		return reserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
	}

	// Adds car reservation to this customer
	public boolean reserveCar(int xid, int customerID, String location)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		return reserveItem(xid, customerID, Car.getKey(location), location);
	}

	// Adds room reservation to this customer
	public boolean reserveRoom(int xid, int customerID, String location)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		return reserveItem(xid, customerID, Room.getKey(location), location);
	}

	// Reserve bundle
	public boolean bundle(int xid, int customerId, Vector<String> flightNumbers, String location, boolean car,
			boolean room) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		return false;
	}

	public String getName() throws RemoteException {
		return m_name;
	}

	public int start() {
		return -1;
	}

	public boolean commit(int id) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		if (!xid_time.keySet().contains(id)) {
			System.out.println("||| " + id + " has been cimmitted/aborted before!!");
			return true;
		}
		// Crash mode 4
		if (crash_rm.get(4)) {
			System.out.println("crash mode 4: crash after receiving decision but before committing");
			System.exit(1);
		}

		RMHashMap data = local_copy.get(id);
		if (data != null) {
			for (String key : data.keySet()) {
				if (data.get(key) == null) {
					synchronized (m_data) {
						m_data.remove(key);
					}
				} else {
					synchronized (m_data) {
						m_data.put(key, data.get(key));
					}
				}
			}
			local_copy.remove(id);
			xid_time.remove(id);
			xid_yn.remove(id);
		}
		// write COMMIT record in log
		writeLog(new LogItem(id, "COMMIT"));
		commitShadowing(id);
		return true;
	}

	public boolean abort(int id) throws RemoteException, TransactionAbortedException, InvalidTransactionException
	{
		if (!xid_time.keySet().contains(id)) {
			System.out.println("||| " + id + " has been cimmitted/aborted before!!");
			return true;
		}
		// Crash mode 4
		if (crash_rm.get(4)) {
			System.out.println("crash mode 4: crash after receiving decision but before aborting");
			System.exit(1);
		}

		local_copy.remove(id);
		xid_time.remove(id);
		xid_yn.remove(id);
		// write ABORT record in log
		writeLog(new LogItem(id, "ABORT"));
		return true;
	}

	public void shutdown()
	{
		try {
			System.out.println(getName() + "shutdown successfully!");
			System.exit(1);
		} catch (RemoteException e) {
			System.exit(1);
		}
	}

	public boolean prepare(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		// Crash mode 1
		if (crash_rm.get(1)) {
			System.out.println("crash mode 1: crash after receiving vote request but before sending answer");
			System.exit(1);
		}
		if (!xid_time.keySet().contains(xid)) {
			// Crash mode 2
			if (crash_rm.get(2)) {
				System.out.println("crash mode 2: crash after deciding which answer to send");
				System.exit(1);
			}
			abort(xid);
			System.out.println("Transaction [" + xid + "] has been aborted caused by VOTING NO!");
			return false;
		}

		// Crash mode 2
		if (crash_rm.get(2)) {
			System.out.println("crash mode 2: crash after deciding which answer to send");
			System.exit(1);
		}

		// TODO: shadowing, (write undo/redo information in log)
		// Write a YES record in log
		writeLog(new LogItem(xid, "YES"));
		return true;
	}

	public boolean twoPC(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		return true;
	}

	public boolean commitShadowing(int xid)
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		String working = "./" + m_name + ".B";
		String master = "./" + m_name + ".master";
		if (master_rec.get(0) == 1)
			working = "./" + m_name + ".A";
		try {
			ObjectOutputStream working_file = new ObjectOutputStream(new FileOutputStream(working));
			ObjectOutputStream master_file = new ObjectOutputStream(new FileOutputStream(master));
			working_file.writeObject(m_data);
			working_file.writeObject(local_copy);
			working_file.close();
			master_rec.set(0, 1 - master_rec.get(0));
			master_rec.set(1, xid);
			master_file.writeObject(master_rec);
			master_file.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	public boolean recoverShadowing()
			throws RemoteException, TransactionAbortedException, InvalidTransactionException {
		try {
			String master = "./" + m_name + ".master";
			ObjectInputStream master_file = new ObjectInputStream(new FileInputStream(master));
			master_rec = (Vector<Integer>) master_file.readObject();
			master_file.close();

			String committed = "./" + m_name + ".A";

			if (master_rec.get(0) == 1)
				committed = "./" + m_name + ".B";

			ObjectInputStream committed_file = new ObjectInputStream(new FileInputStream(committed));
			m_data = (RMHashMap) committed_file.readObject();
			local_copy = (HashMap<Integer, RMHashMap>) committed_file.readObject();
			committed_file.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	public void writeLog(LogItem lg) {
		System.out.println(">>>LOG: <xid,info>:" + lg.xid + ", " + lg.info);
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(m_name + ".log", true));
			out.writeObject(lg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void timeoutDetect() {
		try {
			for (Integer xid : xid_time.keySet()) {
				if (xid_yn.get(xid) && System.currentTimeMillis() - xid_time.get(xid) > MAX_EXIST_TIME_RM) {
					abort(xid);
					// ABORT
					System.out.println("Transaction [" + xid + "] has been aborted caused by timeout!");
					break;
				}
			}
			Thread.sleep(100);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void resetCrashes() throws RemoteException {
		for (int i = 1; i <= 6; i++)
			crash_rm.set(i, false);
	}

	public void crashMiddleware(int mode) throws RemoteException {
		;
	}

	public void crashResourceManager(String name /* RM Name */, int mode) throws RemoteException {
		crash_rm.set(mode, true);
	}

	public void restart()
	{
		try {
            recoverShadowing();
			HashMap<Integer, ParticipantStatue> xid_status = new HashMap<>();
			ObjectInputStream log_in = new ObjectInputStream(new FileInputStream(m_name + ".log"));
			LogItem it = null;
			while ((it = (LogItem) log_in.readObject()) != null) {
				int xid = it.xid;
				String info = it.info;
				if (info.equals("INIT")) {
					xid_status.put(xid, ParticipantStatue.INIT);
				} else if (info.equals("YES")) {
					xid_status.put(xid, ParticipantStatue.YES);
				} else if (info.equals("COMMIT")) {
					xid_status.put(xid, ParticipantStatue.COMMIT);
				} else if (info.equals("ABORT")) {
					xid_status.put(xid, ParticipantStatue.ABORT);
				} else {
					System.out.println("!!! Please check log info:" + xid + ", " + info);
				}
			}
			log_in.close();
			for (Integer xid : xid_status.keySet()) {
				switch (xid_status.get(xid)) {
					case INIT: {
						abort(xid);
						break;
					}
					case YES: {
						System.out.println("RESTART: Found YES log of " + xid +".");
						System.out.println("RESTART:" + xid +" will be blocked.");
					}
					case COMMIT:
					case ABORT:
						break;
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println("RESTART: Not found log file!");
		} catch (IOException e) {
			System.out.println("RESTART: IO Exception at " + m_name);
		} catch (ClassNotFoundException e) {
			System.out.println("RESTART: ClassNotFoundException at " + m_name);
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void pingTest() {
		int i = 0;
	}
}

enum ParticipantStatue
{
	INIT,
	YES,
	COMMIT,
	ABORT,
}
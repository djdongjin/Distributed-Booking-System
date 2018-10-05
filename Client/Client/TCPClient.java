package Client;

import java.io.*;
import java.net.*;
import java.util.Vector;

public class TCPClient extends Client {

    private String serverHost = "localhost";
    private int serverPort = 9999;

    private Socket clientSocket = null;
    private BufferedReader reader = null;
    private ObjectOutputStream writer = null;

    public TCPClient(String serverHost, int serverPort)
    {
        super();
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public static void main(String[] args)
    {
        String serverHost = "localhost";
        int serverPort = 9999;

        if (args.length > 0)
            serverHost = args[0];
        if (args.length > 1)
            serverPort = toInt(args[1]);
        if (args.length > 2)
        {
            System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUsage: java client.TCPClient [serverHost, serverPort]");
            System.exit(1);
        }

        // Get client-server connection socket & IO stream.
        try {
            TCPClient client = new TCPClient(serverHost, serverPort);
            client.connectServer();
            client.start();
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }
    // Connect to Middleware and obtain socket, IO stream.
    public void connectServer()
    {
        try {
            boolean first = true;
            while (true) {
                try {
                    clientSocket = new Socket(serverHost, serverPort);
                    reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    writer = new ObjectOutputStream(clientSocket.getOutputStream());
                    System.out.println("Connected to server [" + serverHost + ":" + serverPort +"]");
                    break;
                }
                catch(UnknownHostException e) {
                    System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[UnknownHost exception");
                    e.printStackTrace();
                    System.exit(1);
                }
                catch(IOException e) {
                    System.err.println((char)27 + "[31;1mIO exception: " + (char)27 + "[IO exception");
                    e.printStackTrace();
                    System.exit(1);
                }
                Thread.sleep(500);
            }
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void disconnect()
    {
        try
        {
            reader.close();
            writer.close();
            clientSocket.close();
        }
        catch(IOException e) {
            System.err.println((char)27 + "[31;1mIO exception: " + (char)27 + "[IO exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void execute(Command cmd, Vector<String> arguments) throws NumberFormatException
    {
        switch (cmd)
        {
            // checked
            case Help:
            {
                if (arguments.size() == 1) {
                    System.out.println(Command.description());
                } else if (arguments.size() == 2) {
                    Command l_cmd = Command.fromString((String)arguments.elementAt(1));
                    System.out.println(l_cmd.toString());
                } else {
                    System.err.println((char)27 + "[31;1mCommand exceptions: " + (char)27 + "[0mImproper use of help command. Location \"help\" or \"help,<CommandName>\"");
                }
                break;
            }
            // checked
            case AddFlight: {
                checkArgumentsCount(5, arguments.size());

                System.out.println("Adding a new flight [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Flight Number: " + arguments.elementAt(2));
                System.out.println("-Flight Seats: " + arguments.elementAt(3));
                System.out.println("-Flight Price: " + arguments.elementAt(4));

                if (send_msg_boolean(arguments)) {
                    System.out.println("Flight added");
                } else {
                    System.out.println("Flight could not be added");
                }
                break;
            }
            // checked
            case AddCars: {
                checkArgumentsCount(5, arguments.size());

                System.out.println("Adding new cars [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Car Location: " + arguments.elementAt(2));
                System.out.println("-Number of Cars: " + arguments.elementAt(3));
                System.out.println("-Car Price: " + arguments.elementAt(4));

                if (send_msg_boolean(arguments)) {
                    System.out.println("Cars added");
                } else {
                    System.out.println("Cars could not be added");
                }
                break;
            }
            // checked
            case AddRooms: {
                checkArgumentsCount(5, arguments.size());

                System.out.println("Adding new rooms [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Room Location: " + arguments.elementAt(2));
                System.out.println("-Number of Rooms: " + arguments.elementAt(3));
                System.out.println("-Room Price: " + arguments.elementAt(4));

                int id = toInt(arguments.elementAt(1));
                String location = arguments.elementAt(2);
                int numRooms = toInt(arguments.elementAt(3));
                int price = toInt(arguments.elementAt(4));

                if (send_msg_boolean(arguments)) {
                    System.out.println("Rooms added");
                } else {
                    System.out.println("Rooms could not be added");
                }
                break;
            }
            // Checked
            case AddCustomer: {
                checkArgumentsCount(2, arguments.size());

                System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");

                int id = toInt(arguments.elementAt(1));

                int customer = send_msg_int(arguments);
                System.out.println("Add customer ID: " + customer);
                break;
            }
            // checked
            case AddCustomerID: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Customer ID: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                int customerID = toInt(arguments.elementAt(2));

                if (send_msg_boolean(arguments)) {
                    System.out.println("Add customer ID: " + customerID);
                } else {
                    System.out.println("Customer could not be added");
                }
                break;
            }
            // checked
            case DeleteFlight: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Deleting a flight [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Flight Number: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                int flightNum = toInt(arguments.elementAt(2));

                if (send_msg_boolean(arguments)) {
                    System.out.println("Flight Deleted");
                } else {
                    System.out.println("Flight could not be deleted");
                }
                break;
            }
            // checked
            case DeleteCars: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Deleting all cars at a particular location [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Car Location: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                String location = arguments.elementAt(2);

                if (send_msg_boolean(arguments)) {
                    System.out.println("Cars Deleted");
                } else {
                    System.out.println("Cars could not be deleted");
                }
                break;
            }
            // checked
            case DeleteRooms: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Deleting all rooms at a particular location [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Car Location: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                String location = arguments.elementAt(2);

                if (send_msg_boolean(arguments)) {
                    System.out.println("Rooms Deleted");
                } else {
                    System.out.println("Rooms could not be deleted");
                }
                break;
            }
            // checked
            case DeleteCustomer: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Deleting a customer from the database [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Customer ID: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                int customerID = toInt(arguments.elementAt(2));

                if (send_msg_boolean(arguments)) {
                    System.out.println("Customer Deleted");
                } else {
                    System.out.println("Customer could not be deleted");
                }
                break;
            }
            // checked
            case QueryFlight: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Querying a flight [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Flight Number: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                int flightNum = toInt(arguments.elementAt(2));

                int seats = send_msg_int(arguments);
                System.out.println("Number of seats available: " + seats);
                break;
            }
            // checked
            case QueryCars: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Querying cars location [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Car Location: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                String location = arguments.elementAt(2);

                int numCars = send_msg_int(arguments);
                System.out.println("Number of cars at this location: " + numCars);
                break;
            }
            // checked
            case QueryRooms: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Querying rooms location [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Room Location: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                String location = arguments.elementAt(2);

                int numRoom = send_msg_int(arguments);
                System.out.println("Number of rooms at this location: " + numRoom);
                break;
            }
            // TODO
            case QueryCustomer: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Querying customer information [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Customer ID: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                int customerID = toInt(arguments.elementAt(2));

                String[] bills = send_msg(arguments).split(";");
                // String bill = send_msg(arguments);
                for (String bill: bills)
                    System.out.println(bill);
                break;
            }
            // checked
            case QueryFlightPrice: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Querying a flight price [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Flight Number: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                int flightNum = toInt(arguments.elementAt(2));

                int price = send_msg_int(arguments);
                System.out.println("Price of a seat: " + price);
                break;
            }
            // checked
            case QueryCarsPrice: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Querying cars price [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Car Location: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                String location = arguments.elementAt(2);

                int price = send_msg_int(arguments);
                System.out.println("Price of cars at this location: " + price);
                break;
            }
            // checked
            case QueryRoomsPrice: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Querying rooms price [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Room Location: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                String location = arguments.elementAt(2);

                int price = send_msg_int(arguments);
                System.out.println("Price of rooms at this location: " + price);
                break;
            }
            // checked
            case ReserveFlight: {
                checkArgumentsCount(4, arguments.size());

                System.out.println("Reserving seat in a flight [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Customer ID: " + arguments.elementAt(2));
                System.out.println("-Flight Number: " + arguments.elementAt(3));

                int id = toInt(arguments.elementAt(1));
                int customerID = toInt(arguments.elementAt(2));
                int flightNum = toInt(arguments.elementAt(3));

                if (send_msg_boolean(arguments)) {
                    System.out.println("Flight Reserved");
                } else {
                    System.out.println("Flight could not be reserved");
                }
                break;
            }
            // checked
            case ReserveCar: {
                checkArgumentsCount(4, arguments.size());

                System.out.println("Reserving a car at a location [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Customer ID: " + arguments.elementAt(2));
                System.out.println("-Car Location: " + arguments.elementAt(3));

                int id = toInt(arguments.elementAt(1));
                int customerID = toInt(arguments.elementAt(2));
                String location = arguments.elementAt(3);

                if (send_msg_boolean(arguments)) {
                    System.out.println("Car Reserved");
                } else {
                    System.out.println("Car could not be reserved");
                }
                break;
            }
            case ReserveRoom: {
                checkArgumentsCount(4, arguments.size());

                System.out.println("Reserving a room at a location [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Customer ID: " + arguments.elementAt(2));
                System.out.println("-Room Location: " + arguments.elementAt(3));

                int id = toInt(arguments.elementAt(1));
                int customerID = toInt(arguments.elementAt(2));
                String location = arguments.elementAt(3);

                if (send_msg_boolean(arguments)) {
                    System.out.println("Room Reserved");
                } else {
                    System.out.println("Room could not be reserved");
                }
                break;
            }
            case Bundle: {
                if (arguments.size() < 7) {
                    System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mBundle command expects at least 7 arguments. Location \"help\" or \"help,<CommandName>\"");
                    break;
                }

                System.out.println("Reserving an bundle [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Customer ID: " + arguments.elementAt(2));
                for (int i = 0; i < arguments.size() - 6; ++i)
                {
                    System.out.println("-Flight Number: " + arguments.elementAt(3+i));
                }
                System.out.println("-Car Location: " + arguments.elementAt(arguments.size()-2));
                System.out.println("-Room Location: " + arguments.elementAt(arguments.size()-1));


                int id = toInt(arguments.elementAt(1));
                int customerID = toInt(arguments.elementAt(2));
                Vector<String> flightNumbers = new Vector<String>();
                for (int i = 0; i < arguments.size() - 6; ++i) {
                    flightNumbers.addElement(arguments.elementAt(3+i));
                }
                String location = arguments.elementAt(arguments.size()-3);
                boolean car = toBoolean(arguments.elementAt(arguments.size()-2));
                boolean room = toBoolean(arguments.elementAt(arguments.size()-1));

                if (send_msg_boolean(arguments)) {
                    System.out.println("Bundle Reserved");
                } else {
                    System.out.println("Bundle could not be reserved");
                }
                break;
            }
            // checked
            case Analytics: {
                //TODO Analytics
                checkArgumentsCount(4, arguments.size());
                int id = Integer.parseInt(arguments.elementAt(1));
                String item = arguments.elementAt(2).toLowerCase();
                int threshold = Integer.parseInt(arguments.elementAt(3));
                arguments.setElementAt(item, 2);

                if (item.equals("flight") || item.equals("car") || item.equals("room")) {
                    item = item.equals("flight") ? "seat(s)" : arguments.elementAt(2) + "(s)";

                    String[] results = send_msg(arguments).split(";");
                    System.out.println("-----------------");
                    for (String res : results) {
                        String[] param = res.split(",");
                        System.out.println("There are(is) " + param[1] + item + " left in " + param[0] + " with price " + param[2]);
                    }
                }
                else
                {
                    System.out.println("Analytics input error, input: " + item + ", expected: flight, room, car.");
                }
                break;
            }
            // checked
            case Quit: {
                checkArgumentsCount(1, arguments.size());

                send_msg(arguments);

                System.out.println("Quitting client");
                System.exit(0);
            }
        }
    }

    private boolean send_msg_boolean(Vector<String> arguments)
    {
        String ret = send_msg(arguments);
        return ret.equals("true");
    }

    private int send_msg_int(Vector<String> arguments)
    {
        String ret = send_msg(arguments);
        return toInt(ret);
    }

    private String send_msg(Vector<String> arguments) {
        String ret = null;
        try {
            writer.writeObject(arguments);
            writer.flush();
            System.out.println("Message sent.");
            while ((ret = reader.readLine()) != null) {
                System.out.println("Response received.");
                break;
            }
        } catch (IOException e) {
            System.err.println((char) 27 + "[31;1mIO exception: " + (char) 27 + "[0mIO exception");
            e.printStackTrace();
        }
        return ret;
    }

}

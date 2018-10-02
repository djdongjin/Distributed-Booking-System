package Server.TCP;

import Server.Common.*;
import Server.Interface.*;
import Client.Client;

import java.io.*;
import java.net.*;

public class TCPResourceManager extends ResourceManager {

    private static String s_serverName = "Server";
    private static int s_serverPort = 9999;

    private int port;

    TCPResourceManager(String name, int port)
    {
        super(name);
        this.port = port;
    }

    public static void main()
    {
        if (args.length > 0)
            s_serverName = args[0];
        if (args.length > 1)
            s_serverPort = Integer.parseInt(args[1]);

        TCPResourceManager tcp_ResourceManager = new TCPResourceManager(s_serverName, s_serverPort);
        tcp_ResourceManager.acceptConnections();
    }

    public void acceptConnections()
    {
        try
        {
            ServerSocket server = new ServerSocket(port, 10);
            Socket client_request = null;
            while (true)
            {
                client_request = server.accept();
                (new Thread(new RequestHandler(client_request))).start();
            }
        }
        catch (BindException e) {
            // TODO
        }
        catch (IOException e) {
            // TODO
        }
    }

}

package Server.TCP;

import Server.Common.*;
import Server.Interface.*;
import Client.Client;

import java.io.*;
import java.net.*;

public class TCPResourceManager extends ResourceManager {

    private static String s_serverName = "Server";
    private static int s_serverPort = 9999;

    private int listenPort;

    TCPResourceManager(String name, int port)
    {
        super(name);
        this.listenPort = port;
    }

    public static void main(String[] args)
    {
        if (args.length > 0)
            s_serverName = args[0];
        if (args.length > 1)
            s_serverPort = Integer.parseInt(args[1]);

        TCPResourceManager tcp_ResourceManager = new TCPResourceManager(s_serverName, s_serverPort);
        tcp_ResourceManager.acceptConnection();
    }

    public void acceptConnection()
    {
        try
        {
            ServerSocket server = new ServerSocket(listenPort, 10);
            Socket client_request = null;
            while (true)
            {
                client_request = server.accept();
                (new Thread(new RequestHandler(this, client_request))).start();
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

package Server.TCP;

import java.net.Socket;

public class RequestHandler implements Runnable {

    private Socket client_request = null;

    RequestHandler(Socket sock)
    {
        super();
        client_request = sock;
    }

    void run()
    {

    }
}

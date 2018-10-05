# comp512-project

To run the RMI resource manager:

```
cd Server/
./run_server.sh [<rmi_name>] # starts a single ResourceManager
./run_servers.sh # convenience script for starting multiple resource managers
```

To run the RMI client:

```
cd Client
./run_client.sh [<server_hostname> [<server_rmi_name>]]
```


Make first:

1. cd Server/, modify makefile: complie-server-rmi (line 1)
2. cd Client/, make
3. cd Server/, modify makefile: compile-server-tcp(line 1)

To run the TCP Server

```
cd Server/
./run_server_tcp.sh [<serverName> <listenPort>]
```

To run the Middleware
```
cd Server/
./run_middleware_tcp.sh [<middlewareName> <listenPort> <s1_host> <s1_port> (*3)]
```

To run the Client
```
cd Client/
./run_client.tcp.sh [<middleware_host> <middleware_port>]
```

#Usage: ./run_server_tcp.sh [<serverName, serverPort>]

# ./run_rmi.sh > /dev/null 2>&1
java -cp ../Client/Command.jar:. -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$(pwd)/ Server.TCP.TCPResourceManager $1 $2

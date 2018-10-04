# Usage: ./run_client_tcp.sh [<serverHost> [<serverPort>]]

java -Djava.security.policy=java.policy Client.TCPClient $1 $2

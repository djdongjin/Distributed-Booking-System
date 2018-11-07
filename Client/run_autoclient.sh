# Usage: ./run_client.sh [<server_hostname> [<server_rmiobject>]]

java -Djava.security.policy=java.policy -cp ../Server/TransactionException.jar:../Server/RMIInterface.jar:. Client.AutoClient $1 $2 $3 $4 $5

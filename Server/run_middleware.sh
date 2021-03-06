./run_rmi.sh > /dev/null

echo "Edit file run_middleware.sh to include instructions for launching the middleware"
echo '  $1 - servername of middleware'
echo '  $2 - hostname of Flights'
echo '  $3 - servername of Flights'
echo '  $4 - hostname of Cars'
echo '  $5 - servername of Cars'
echo '  $6 - hostname of Rooms'
echo '  $7 - servername of Rooms'

java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$(pwd)/ Server.Common.RMIMiddleware $1 $2 $3 $4 $5 $6 $7

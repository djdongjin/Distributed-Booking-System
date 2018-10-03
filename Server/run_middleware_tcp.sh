# ./run_rmi.sh > /dev/null

echo "Edit file run_middleware_tcp.sh to include instructions for launching the middleware"
echo '  $1 - servername of Middleware'
echo '  $2 - port of Middleware'
echo '  $3 - hostname of Flights'
echo '  $4 - port of Flights'
echo '  $5 - hostname of Cars'
echo '  $6 - port of Cars'
echo '  $7 - hostname of Rooms'
echo '  $8 - port of Rooms'

java -cp ../Client/Command.jar:. -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$(pwd)/ Server.TCP.TCPMiddleWare $1 $2 $3 $4 $5 $6 $7 $8

all: java.policy compile-client

java.policy: FORCE
	@echo "Creating client java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "\tpermission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

Command.jar: Client/Command.java
	@echo "Compiling Command class"
	javac Client/Command.java
	jar cvf Command.jar Client/Command.class

../Server/RMIInterface.jar: ../Server/Server/Interface/IResourceManager.java
	make -C ../Server/ RMIInterface.jar

../Server/TransactionException.jar: ../Server/Server/LockManager/*.java
	make -C ../Server/ TransactionException.jar

#../Server/InvalidTransactionException.jar: ../Server/Server/LockManager/InvalidTransactionException.java
#	make -C ../Server/ InvalidTransactionException.jar

compile-client: ../Server/RMIInterface.jar ../Server/TransactionException.jar
	javac -cp ../Server/TransactionException.jar:../Server/RMIInterface.jar Client/*.java

clean:
	rm -f Client/*.class
	rm -f ../Server/RMIInterface.jar
	rm -f java.policy

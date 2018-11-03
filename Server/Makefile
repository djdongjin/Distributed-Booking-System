all: java.policy compile-server-rmi

java.policy: FORCE
	@echo "Creating server java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "\tpermission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

RMIInterface.jar: Server/Interface/IResourceManager.java
	@echo "Compiling RMI server interface"
	javac Server/Interface/IResourceManager.java
	jar cvf RMIInterface.jar Server/Interface/IResourceManager.class

Transaction.jar: Server/LockManager/*.java
	@echo "Compiling Transaction server interface"
	javac Server/LockManager/*.java
	jar cvf Transaction.jar Server/LockManager/*.class

# InvalidTransactionException.jar: Server/LockManager/InvalidTransactionException.java
#	@echo "Compiling InvalidTransactionException server interface"
#	javac Server/LockManager/InvalidTransactionException.java
#	jar cvf InvalidTransactionException.jar Server/LockManager/InvalidTransactionException.class

../Client/Command.jar: ../Client/Client/Command.java
	make -C ../Client/ Command.jar

compile-server-rmi: Server/RMI/*.java Server/Interface/IResourceManager.java Server/LockManager/*.java
	javac Server/RMI/*.java Server/Interface/IResourceManager.java Server/Common/*.java  Server/LockManager/*.java

compile-server-tcp: ../Client/Command.jar Server/TCP/*.java Server/Interface/IResourceManager.java Server/Common/*.java
	javac -cp ../Client/Command.jar Server/TCP/*.java Server/Interface/IResourceManager.java Server/Common/*.java

clean:
	rm -f Server/Interface/*.class Server/Common/*.class Server/RMI/*.class Server/LockManager/*.class
	rm -f RMIInterface.jar
	rm -f java.policy
